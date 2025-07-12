package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import com.tathanhloc.faceattendance.Enum.GioiTinhEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelService {

    @Autowired
    private final SinhVienService sinhVienService;
    private final LopRepository lopRepository;
    private final FileUploadService fileUploadService;

    private static final String[] REQUIRED_HEADERS = {"maSv", "hoTen", "maLop"};
    private static final String[] OPTIONAL_HEADERS = {"email", "gioiTinh", "ngaySinh"};
    private static final String[] ALL_HEADERS = {"maSv", "hoTen", "email", "gioiTinh", "ngaySinh", "maLop"};
    @Autowired
    private final LichHocService lichHocService;

    /**
     * Import sinh viên từ file Excel
     */
    @Transactional
    public ImportResultDTO importStudentsFromExcel(MultipartFile file, boolean createAccounts) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        try {
            log.info("Starting Excel import from file: {}", file.getOriginalFilename());

            // Validate file
            validateExcelFile(file);

            // Parse Excel data
            List<Map<String, Object>> excelData = parseExcelFile(file);

            if (excelData.isEmpty()) {
                errors.add("File Excel không có dữ liệu hợp lệ");
                return new ImportResultDTO(0, 1, errors);
            }

            // Process each row
            for (int i = 0; i < excelData.size(); i++) {
                try {
                    Map<String, Object> row = excelData.get(i);

                    // Validate và convert row data
                    SinhVienDTO sinhVienDTO = mapRowToSinhVienDTO(row, i + 2); // +2 because row 1 is header

                    // Kiểm tra trùng mã sinh viên
                    try {
                        sinhVienService.getByMaSv(sinhVienDTO.getMaSv());
                        errors.add("Dòng " + (i + 2) + ": Mã sinh viên " + sinhVienDTO.getMaSv() + " đã tồn tại");
                        failedCount++;
                        continue;
                    } catch (Exception e) {
                        // Sinh viên chưa tồn tại - OK
                    }

                    // Tạo sinh viên
                    SinhVienDTO created = sinhVienService.create(sinhVienDTO);

                    // Tạo folder cho sinh viên
                    fileUploadService.createStudentDirectory(created.getMaSv());

                    // TODO: Tạo tài khoản nếu được yêu cầu
                    if (createAccounts) {
                        warnings.add("Tính năng tạo tài khoản tự động chưa được triển khai cho " + created.getMaSv());
                    }

                    successCount++;
                    log.debug("Imported student: {}", created.getMaSv());

                } catch (Exception e) {
                    log.error("Error importing row {}: ", i + 2, e);
                    errors.add("Dòng " + (i + 2) + ": " + e.getMessage());
                    failedCount++;
                }
            }

            log.info("Import completed: {} success, {} failed", successCount, failedCount);

            ImportResultDTO result = new ImportResultDTO();
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalProcessed(excelData.size());
            result.setErrors(errors);
            result.setWarnings(warnings);
            result.setTimestamp(java.time.LocalDateTime.now());
            result.setStatus(failedCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");

            return result;

        } catch (Exception e) {
            log.error("Error during Excel import: ", e);
            errors.add("Lỗi xử lý file Excel: " + e.getMessage());
            return new ImportResultDTO(successCount, failedCount + 1, errors);
        }
    }

    /**
     * Export sinh viên ra file Excel
     */
    public byte[] exportStudentsToExcel(String search, String classFilter, String status) throws IOException {
        log.info("Exporting students to Excel with filters - search: {}, class: {}, status: {}",
                search, classFilter, status);

        // Lấy dữ liệu sinh viên (sử dụng service hiện có)
        List<SinhVienDTO> students = sinhVienService.getAll();

        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.toLowerCase();
            students = students.stream()
                    .filter(s -> s.getMaSv().toLowerCase().contains(searchTerm) ||
                            s.getHoTen().toLowerCase().contains(searchTerm) ||
                            (s.getEmail() != null && s.getEmail().toLowerCase().contains(searchTerm)))
                    .collect(Collectors.toList());
        }

        if (classFilter != null && !classFilter.trim().isEmpty()) {
            students = students.stream()
                    .filter(s -> classFilter.equals(s.getMaLop()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.trim().isEmpty()) {
            boolean isActive = "active".equals(status);
            students = students.stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive() == isActive)
                    .collect(Collectors.toList());
        }

        // Tạo Excel workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách sinh viên");

            // Tạo header style
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã SV", "Họ tên", "Email", "Giới tính", "Ngày sinh", "Mã lớp", "Trạng thái", "Có ảnh", "Có embedding"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Tạo data rows
            int rowNum = 1;
            for (SinhVienDTO student : students) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(student.getMaSv());
                row.createCell(1).setCellValue(student.getHoTen());
                row.createCell(2).setCellValue(student.getEmail() != null ? student.getEmail() : "");
                row.createCell(3).setCellValue(student.getGioiTinh() != null ? student.getGioiTinh().name() : "");
                row.createCell(4).setCellValue(student.getNgaySinh() != null ? student.getNgaySinh().toString() : "");
                row.createCell(5).setCellValue(student.getMaLop() != null ? student.getMaLop() : "");
                row.createCell(6).setCellValue(student.getIsActive() != null && student.getIsActive() ? "Hoạt động" : "Không hoạt động");
                row.createCell(7).setCellValue(student.getHinhAnh() != null ? "Có" : "Không");
                row.createCell(8).setCellValue(student.getEmbedding() != null ? "Có" : "Không");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            log.info("Exported {} students to Excel", students.size());
            return outputStream.toByteArray();
        }
    }

    /**
     * Tạo template Excel để import
     */
    public byte[] generateImportTemplate() throws IOException {
        log.info("Generating Excel import template");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Create instruction style
            CellStyle instructionStyle = workbook.createCellStyle();
            Font instructionFont = workbook.createFont();
            instructionFont.setBold(true);
            instructionFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            instructionStyle.setFont(instructionFont);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ALL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ALL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample data row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("SV001");
            sampleRow.createCell(1).setCellValue("Nguyễn Văn A");
            sampleRow.createCell(2).setCellValue("sva@example.com");
            sampleRow.createCell(3).setCellValue("NAM");
            sampleRow.createCell(4).setCellValue("01/01/2000");
            sampleRow.createCell(5).setCellValue("CNTT01");

            // Instructions sheet
            Sheet instructionsSheet = workbook.createSheet("Hướng dẫn");

            int instructionRow = 0;

            // Title
            Row titleRow = instructionsSheet.createRow(instructionRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HƯỚNG DẪN IMPORT SINH VIÊN");
            titleCell.setCellStyle(instructionStyle);

            instructionRow++; // Empty row

            // Required fields
            Row reqFieldsTitle = instructionsSheet.createRow(instructionRow++);
            Cell reqFieldsCell = reqFieldsTitle.createCell(0);
            reqFieldsCell.setCellValue("CÁC CỘT BẮT BUỘC:");
            reqFieldsCell.setCellStyle(instructionStyle);

            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- maSv: Mã sinh viên (bắt buộc, duy nhất, tối đa 20 ký tự)");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- hoTen: Họ và tên sinh viên (bắt buộc, tối đa 100 ký tự)");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- maLop: Mã lớp (bắt buộc, phải tồn tại trong hệ thống)");

            instructionRow++; // Empty row

            // Optional fields
            Row optFieldsTitle = instructionsSheet.createRow(instructionRow++);
            Cell optFieldsCell = optFieldsTitle.createCell(0);
            optFieldsCell.setCellValue("CÁC CỘT TÙY CHỌN:");
            optFieldsCell.setCellStyle(instructionStyle);

            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- email: Email sinh viên (phải đúng định dạng email)");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- gioiTinh: NAM hoặc NU");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- ngaySinh: Định dạng dd/MM/yyyy (ví dụ: 01/01/2000)");

            instructionRow++; // Empty row

            // Notes
            Row notesTitle = instructionsSheet.createRow(instructionRow++);
            Cell notesCell = notesTitle.createCell(0);
            notesCell.setCellValue("GHI CHÚ:");
            notesCell.setCellStyle(instructionStyle);

            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- Không được để trống các cột bắt buộc");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- Mã sinh viên không được trùng với dữ liệu đã có");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- Mã lớp phải tồn tại trong hệ thống");
            instructionsSheet.createRow(instructionRow++).createCell(0)
                    .setCellValue("- File chỉ hỗ trợ định dạng .xlsx hoặc .xls");

            // Auto-size columns
            for (int i = 0; i < ALL_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            instructionsSheet.autoSizeColumn(0);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Validate Excel file
     */
    private void validateExcelFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("File phải có định dạng Excel (.xlsx hoặc .xls)");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("Kích thước file không được vượt quá 10MB");
        }
    }

    /**
     * Parse Excel file to list of maps
     */
    private List<Map<String, Object>> parseExcelFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("File Excel phải có ít nhất 2 dòng (header + data)");
            }

            // Read header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Không tìm thấy dòng header");
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            // Validate required headers
            validateHeaders(headers);

            // Read data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size() && j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String header = headers.get(j);
                    Object value = getCellValue(cell);
                    rowData.put(header, value);
                }

                data.add(rowData);
            }
        }

        return data;
    }

    /**
     * Validate headers
     */
    private void validateHeaders(List<String> headers) {
        List<String> missingHeaders = new ArrayList<>();

        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!headers.contains(requiredHeader)) {
                missingHeaders.add(requiredHeader);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("Thiếu các cột bắt buộc: " + String.join(", ", missingHeaders));
        }
    }

    /**
     * Map Excel row to SinhVienDTO
     */
    private SinhVienDTO mapRowToSinhVienDTO(Map<String, Object> row, int rowNumber) {
        try {
            SinhVienDTO dto = new SinhVienDTO();

            // Mã sinh viên (required)
            Object maSvObj = row.get("maSv");
            if (maSvObj == null || maSvObj.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Mã sinh viên không được để trống");
            }
            String maSv = maSvObj.toString().trim();
            if (maSv.length() > 20) {
                throw new IllegalArgumentException("Mã sinh viên không được vượt quá 20 ký tự");
            }
            dto.setMaSv(maSv);

            // Họ tên (required)
            Object hoTenObj = row.get("hoTen");
            if (hoTenObj == null || hoTenObj.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Họ tên không được để trống");
            }
            String hoTen = hoTenObj.toString().trim();
            if (hoTen.length() > 100) {
                throw new IllegalArgumentException("Họ tên không được vượt quá 100 ký tự");
            }
            dto.setHoTen(hoTen);

            // Email (optional)
            Object emailObj = row.get("email");
            if (emailObj != null && !emailObj.toString().trim().isEmpty()) {
                String email = emailObj.toString().trim();
                if (!isValidEmail(email)) {
                    throw new IllegalArgumentException("Email không hợp lệ: " + email);
                }
                dto.setEmail(email);
            }

            // Giới tính (optional)
            Object gioiTinhObj = row.get("gioiTinh");
            if (gioiTinhObj != null && !gioiTinhObj.toString().trim().isEmpty()) {
                String gioiTinh = gioiTinhObj.toString().trim().toUpperCase();
                if ("NAM".equals(gioiTinh) || "MALE".equals(gioiTinh) || "M".equals(gioiTinh)) {
                    dto.setGioiTinh(GioiTinhEnum.NAM);
                } else if ("NU".equals(gioiTinh) || "NỮ".equals(gioiTinh) || "FEMALE".equals(gioiTinh) || "F".equals(gioiTinh)) {
                    dto.setGioiTinh(GioiTinhEnum.NU);
                } else {
                    throw new IllegalArgumentException("Giới tính không hợp lệ: " + gioiTinh + " (chỉ chấp nhận NAM hoặc NU)");
                }
            }

            // Ngày sinh (optional)
            Object ngaySinhObj = row.get("ngaySinh");
            if (ngaySinhObj != null && !ngaySinhObj.toString().trim().isEmpty()) {
                try {
                    String dateStr = ngaySinhObj.toString().trim();
                    // Try different date formats
                    LocalDate ngaySinh = parseDate(dateStr);
                    dto.setNgaySinh(ngaySinh);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Ngày sinh không hợp lệ: " + ngaySinhObj + " (định dạng phải là dd/MM/yyyy)");
                }
            }

            // Mã lớp (required)
            Object maLopObj = row.get("maLop");
            if (maLopObj == null || maLopObj.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Mã lớp không được để trống");
            }
            String maLop = maLopObj.toString().trim();
            if (!lopRepository.existsById(maLop)) {
                throw new IllegalArgumentException("Lớp không tồn tại: " + maLop);
            }
            dto.setMaLop(maLop);

            // Mặc định là active
            dto.setIsActive(true);

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi dữ liệu dòng " + rowNumber + ": " + e.getMessage(), e);
        }
    }

    /**
     * Helper methods
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        return headerStyle;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (!value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private LocalDate parseDate(String dateStr) {
        // Try different date formats
        String[] formats = {"dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"};

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        throw new IllegalArgumentException("Không thể parse ngày: " + dateStr);
    }
    /**
     * Export lịch học ra Excel
     */
    public byte[] exportSchedulesToExcel(String semester, String year, String teacher, String room, String subject) throws IOException {
        log.info("Exporting schedules to Excel with filters - semester: {}, year: {}, teacher: {}, room: {}, subject: {}",
                semester, year, teacher, room, subject);

        // Lấy dữ liệu lịch học
        List<LichHocDTO> schedules = lichHocService.getAll();

        // Apply filters
        schedules = applyScheduleFilters(schedules, semester, year, teacher, room, subject);

        return createScheduleExcel(schedules, semester, year);
    }

    /**
     * Export lịch học từ dữ liệu đã filter
     */
    public byte[] exportSchedulesToExcelFromData(List<Map<String, Object>> scheduleData, String semester, String year) throws IOException {
        log.info("Exporting {} schedules to Excel", scheduleData != null ? scheduleData.size() : 0);

        // Convert Map to DTO if needed
        List<LichHocDTO> schedules = convertMapToScheduleDTO(scheduleData);

        return createScheduleExcel(schedules, semester, year);
    }

    private List<LichHocDTO> applyScheduleFilters(List<LichHocDTO> schedules, String semester, String year,
                                                  String teacher, String room, String subject) {
        return schedules.stream()
                .filter(s -> semester == null || semester.equals(s.getHocKy()))
                .filter(s -> year == null || year.equals(s.getNamHoc()))
                .filter(s -> teacher == null || teacher.equals(s.getMaGv()))
                .filter(s -> room == null || room.equals(s.getMaPhong()))
                .filter(s -> subject == null || subject.equals(s.getMaMh()))
                .filter(s -> s.getIsActive() == null || s.getIsActive())
                .collect(Collectors.toList());
    }

    private byte[] createScheduleExcel(List<LichHocDTO> schedules, String semester, String year) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch học");

            // Tạo các style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // Tạo title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            String title = String.format("LỊCH HỌC %s - %s",
                    semester != null ? semester : "TẤT CẢ HỌC KỲ",
                    year != null ? year : "TẤT CẢ NĂM HỌC");
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            // Tạo thông tin tổng quan
            Row infoRow = sheet.createRow(1);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(String.format("Tổng số lịch học: %d | Ngày xuất: %s",
                    schedules.size(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 11));

            // Tạo header row
            Row headerRow = sheet.createRow(3);
            String[] headers = {
                    "STT", "Mã lịch", "Mã LHP", "Môn học", "Giảng viên", "Phòng học",
                    "Thứ", "Tiết bắt đầu", "Số tiết", "Thời gian", "Học kỳ", "Năm học"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Tạo data rows
            int rowNum = 4;
            int stt = 1;
            for (LichHocDTO schedule : schedules) {
                Row row = sheet.createRow(rowNum++);

                // STT - sử dụng number style
                Cell sttCell = row.createCell(0);
                sttCell.setCellValue(stt++);
                sttCell.setCellStyle(numberStyle);

                // Data cells
                createDataCell(row, 1, schedule.getMaLich(), dataStyle);
                createDataCell(row, 2, schedule.getMaLhp(), dataStyle);
                createDataCell(row, 3, schedule.getTenMonHoc(), dataStyle);
                createDataCell(row, 4, schedule.getTenGiangVien(), dataStyle);
                createDataCell(row, 5, schedule.getTenPhong() != null ? schedule.getTenPhong() : schedule.getMaPhong(), dataStyle);
                createDataCell(row, 6, getDayName(schedule.getThu()), dataStyle);

                // Number cells
                Cell startPeriodCell = row.createCell(7);
                startPeriodCell.setCellValue(schedule.getTietBatDau() != null ? schedule.getTietBatDau() : 0);
                startPeriodCell.setCellStyle(numberStyle);

                Cell numPeriodsCell = row.createCell(8);
                numPeriodsCell.setCellValue(schedule.getSoTiet() != null ? schedule.getSoTiet() : 0);
                numPeriodsCell.setCellStyle(numberStyle);

                // Time range
                String timeRange = getTimeRange(schedule.getTietBatDau(), schedule.getSoTiet());
                createDataCell(row, 9, timeRange, dataStyle);

                createDataCell(row, 10, schedule.getHocKy(), dataStyle);
                createDataCell(row, 11, schedule.getNamHoc(), dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Set minimum width
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3000));
            }

            // Convert to byte array
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * Helper method để tạo data cell với style
     */
    private void createDataCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String getDayName(Integer dayNumber) {
        if (dayNumber == null) return "";

        Map<Integer, String> days = Map.of(
                2, "Thứ 2",
                3, "Thứ 3",
                4, "Thứ 4",
                5, "Thứ 5",
                6, "Thứ 6",
                7, "Thứ 7",
                8, "Chủ nhật"
        );
        return days.getOrDefault(dayNumber, "");
    }

    private String getTimeRange(Integer startPeriod, Integer numPeriods) {
        if (startPeriod == null || numPeriods == null) return "";

        // Time slots mapping
        String[] timeSlots = {
                "", "7:00-7:45", "7:50-8:35", "8:40-9:25", "9:35-10:20", "10:25-11:10", "11:15-12:00",
                "13:00-13:45", "13:50-14:35", "14:40-15:25", "15:35-16:20", "16:25-17:10", "17:15-18:00"
        };

        if (startPeriod < 1 || startPeriod >= timeSlots.length) return "";

        int endPeriod = startPeriod + numPeriods - 1;
        if (endPeriod >= timeSlots.length) return "";

        String startTime = timeSlots[startPeriod].split("-")[0];
        String endTime = timeSlots[endPeriod].split("-")[1];

        return startTime + "-" + endTime;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private List<LichHocDTO> convertMapToScheduleDTO(List<Map<String, Object>> scheduleData) {
        if (scheduleData == null) return new ArrayList<>();

        return scheduleData.stream()
                .map(this::mapToScheduleDTO)
                .collect(Collectors.toList());
    }

    private LichHocDTO mapToScheduleDTO(Map<String, Object> map) {
        LichHocDTO dto = new LichHocDTO();
        dto.setMaLich((String) map.get("maLich"));
        dto.setMaLhp((String) map.get("maLhp"));
        dto.setTenMonHoc((String) map.get("tenMonHoc"));
        dto.setTenGiangVien((String) map.get("tenGiangVien"));
        dto.setTenPhong((String) map.get("tenPhong"));
        dto.setMaPhong((String) map.get("maPhong"));
        dto.setThu((Integer) map.get("thu"));
        dto.setTietBatDau((Integer) map.get("tietBatDau"));
        dto.setSoTiet((Integer) map.get("soTiet"));
        dto.setHocKy((String) map.get("hocKy"));
        dto.setNamHoc((String) map.get("namHoc"));
        dto.setIsActive((Boolean) map.get("isActive"));
        return dto;
    }
    /**
     * Tạo style cho data cells
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        // Border
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());

        // Alignment
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);

        return style;
    }

    /**
     * Tạo style cho số (STT, số tiết, etc.)
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    /**
     * Xuất báo cáo điểm danh ra Excel
     */
    public byte[] exportAttendanceReport(List<Map<String, Object>> data, LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo điểm danh");

            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Tạo style cho data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Tạo style cho số
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Tạo title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO ĐIỂM DANH");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);

            // Merge title row
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            // Tạo subtitle
            Row subtitleRow = sheet.createRow(1);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Từ ngày " + fromDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " đến ngày " + toDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subtitleCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

            // Tạo header
            Row headerRow = sheet.createRow(3);
            String[] headers = {
                    "STT", "Ngày", "Môn học", "Lớp HP", "Giảng viên", "Phòng học",
                    "Ca học", "Có mặt", "Vắng mặt", "Đi trễ", "Có phép", "Tổng SV", "Tỷ lệ (%)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Tạo data rows
            int rowNum = 4;
            int stt = 1;

            for (Map<String, Object> record : data) {
                Row row = sheet.createRow(rowNum++);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(stt++);
                cell0.setCellStyle(numberStyle);

                // Ngày
                Cell cell1 = row.createCell(1);
                LocalDate date = (LocalDate) record.get("date");
                cell1.setCellValue(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                cell1.setCellStyle(dataStyle);

                // Môn học
                Cell cell2 = row.createCell(2);
                cell2.setCellValue((String) record.get("subjectName"));
                cell2.setCellStyle(dataStyle);

                // Lớp HP
                Cell cell3 = row.createCell(3);
                cell3.setCellValue((String) record.get("className"));
                cell3.setCellStyle(dataStyle);

                // Giảng viên
                Cell cell4 = row.createCell(4);
                cell4.setCellValue((String) record.get("lecturerName"));
                cell4.setCellStyle(dataStyle);

                // Phòng học
                Cell cell5 = row.createCell(5);
                cell5.setCellValue((String) record.get("roomName"));
                cell5.setCellStyle(dataStyle);

                // Ca học
                Cell cell6 = row.createCell(6);
                cell6.setCellValue((String) record.get("session"));
                cell6.setCellStyle(dataStyle);

                // Có mặt
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(((Number) record.get("present")).intValue());
                cell7.setCellStyle(numberStyle);

                // Vắng mặt
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(((Number) record.get("absent")).intValue());
                cell8.setCellStyle(numberStyle);

                // Đi trễ
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(((Number) record.get("late")).intValue());
                cell9.setCellStyle(numberStyle);

                // Có phép
                Cell cell10 = row.createCell(10);
                cell10.setCellValue(((Number) record.get("excused")).intValue());
                cell10.setCellStyle(numberStyle);

                // Tổng SV
                Cell cell11 = row.createCell(11);
                int totalStudents = ((Number) record.get("totalStudents")).intValue();
                cell11.setCellValue(totalStudents);
                cell11.setCellStyle(numberStyle);

                // Tỷ lệ (%)
                Cell cell12 = row.createCell(12);
                int present = ((Number) record.get("present")).intValue();
                double rate = totalStudents > 0 ? (double) present / totalStudents * 100 : 0;
                cell12.setCellValue(String.format("%.1f%%", rate));
                cell12.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 1000, 6000));
            }

            // Tạo summary row
            if (!data.isEmpty()) {
                Row summaryRow = sheet.createRow(rowNum + 1);

                Cell summaryLabelCell = summaryRow.createCell(0);
                summaryLabelCell.setCellValue("TỔNG CỘNG");
                summaryLabelCell.setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, 0, 6));

                // Tính tổng
                int totalPresent = data.stream().mapToInt(r -> ((Number) r.get("present")).intValue()).sum();
                int totalAbsent = data.stream().mapToInt(r -> ((Number) r.get("absent")).intValue()).sum();
                int totalLate = data.stream().mapToInt(r -> ((Number) r.get("late")).intValue()).sum();
                int totalExcused = data.stream().mapToInt(r -> ((Number) r.get("excused")).intValue()).sum();
                int totalAll = data.stream().mapToInt(r -> ((Number) r.get("totalStudents")).intValue()).sum();

                Cell totalPresentCell = summaryRow.createCell(7);
                totalPresentCell.setCellValue(totalPresent);
                totalPresentCell.setCellStyle(headerStyle);

                Cell totalAbsentCell = summaryRow.createCell(8);
                totalAbsentCell.setCellValue(totalAbsent);
                totalAbsentCell.setCellStyle(headerStyle);

                Cell totalLateCell = summaryRow.createCell(9);
                totalLateCell.setCellValue(totalLate);
                totalLateCell.setCellStyle(headerStyle);

                Cell totalExcusedCell = summaryRow.createCell(10);
                totalExcusedCell.setCellValue(totalExcused);
                totalExcusedCell.setCellStyle(headerStyle);

                Cell totalAllCell = summaryRow.createCell(11);
                totalAllCell.setCellValue(totalAll);
                totalAllCell.setCellStyle(headerStyle);

                Cell avgRateCell = summaryRow.createCell(12);
                double avgRate = totalAll > 0 ? (double) totalPresent / totalAll * 100 : 0;
                avgRateCell.setCellValue(String.format("%.1f%%", avgRate));
                avgRateCell.setCellStyle(headerStyle);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error creating Excel file", e);
        }
    }

    /**
     * Tạo báo cáo điểm danh Excel chi tiết cho giảng viên
     */
    public byte[] generateAttendanceReport(Map<String, Object> data) throws IOException {
        log.info("Generating detailed attendance report Excel");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo điểm danh");

            // Lấy dữ liệu
            LopHocPhanDTO lopHocPhan = (LopHocPhanDTO) data.get("lopHocPhan");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reportData = (List<Map<String, Object>>) data.get("reportData");
            String title = (String) data.get("title");
            String generatedDate = (String) data.get("generatedDate");

            // Tạo styles
            CellStyle headerStyle = createAttendanceReportHeaderStyle(workbook);
            CellStyle titleStyle = createAttendanceReportTitleStyle(workbook);
            CellStyle dataStyle = createAttendanceReportDataStyle(workbook);
            CellStyle numberStyle = createAttendanceReportNumberStyle(workbook);
            CellStyle percentStyle = createAttendanceReportPercentStyle(workbook);
            CellStyle summaryStyle = createAttendanceReportSummaryStyle(workbook);

            int rowNum = 0;

            // Header thông tin báo cáo
            rowNum = createAttendanceReportHeader(sheet, lopHocPhan, title, generatedDate, titleStyle, rowNum);

            // Thống kê tổng quan
            rowNum = createAttendanceReportSummary(sheet, reportData, summaryStyle, dataStyle, numberStyle, rowNum);

            // Bảng chi tiết sinh viên
            rowNum = createAttendanceReportDetailTable(sheet, reportData, headerStyle, dataStyle, numberStyle, percentStyle, rowNum);

            // Ghi chú và hướng dẫn
            createAttendanceReportNotes(sheet, dataStyle, rowNum);

            // Auto-size columns
            for (int i = 0; i < 12; i++) {
                sheet.autoSizeColumn(i);
                // Set minimum width cho readability
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth, 2500));
            }

            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating attendance report Excel: {}", e.getMessage());
            throw new IOException("Cannot generate attendance report Excel", e);
        }
    }

    /**
     * Tạo header thông tin báo cáo điểm danh
     */
    private int createAttendanceReportHeader(Sheet sheet, LopHocPhanDTO lopHocPhan, String title,
                                             String generatedDate, CellStyle titleStyle, int startRow) {
        int rowNum = startRow;

        // Logo và tên trường (có thể customize)
        Row schoolRow = sheet.createRow(rowNum++);
        Cell schoolCell = schoolRow.createCell(0);
        schoolCell.setCellValue("TRƯỜNG ĐẠI HỌC CẦN THƠ");
        schoolCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(schoolRow.getRowNum(), schoolRow.getRowNum(), 0, 11));

        Row systemRow = sheet.createRow(rowNum++);
        Cell systemCell = systemRow.createCell(0);
        systemCell.setCellValue("HỆ THỐNG ĐIỂM DANH KHUÔN MẶT");
        systemCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(systemRow.getRowNum(), systemRow.getRowNum(), 0, 11));

        // Tiêu đề báo cáo
        Row reportTitleRow = sheet.createRow(rowNum++);
        Cell reportTitleCell = reportTitleRow.createCell(0);
        reportTitleCell.setCellValue(title.toUpperCase());
        reportTitleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(reportTitleRow.getRowNum(), reportTitleRow.getRowNum(), 0, 11));

        rowNum++; // Empty row

        // Thông tin lớp học
        if (lopHocPhan != null) {
            CellStyle infoStyle = createInfoStyle(sheet.getWorkbook());

            rowNum = createInfoRow(sheet, "Môn học:", lopHocPhan.getTenMonHoc(), infoStyle, rowNum);
            rowNum = createInfoRow(sheet, "Mã lớp học phần:", lopHocPhan.getMaLhp(), infoStyle, rowNum);
            rowNum = createInfoRow(sheet, "Nhóm:", String.valueOf(lopHocPhan.getNhom()), infoStyle, rowNum);
            rowNum = createInfoRow(sheet, "Học kỳ:", lopHocPhan.getHocKy() + " - " + lopHocPhan.getNamHoc(), infoStyle, rowNum);
            rowNum = createInfoRow(sheet, "Giảng viên:", lopHocPhan.getTenGiangVien(), infoStyle, rowNum);
            rowNum = createInfoRow(sheet, "Số tín chỉ:", String.valueOf(lopHocPhan.getSoTinChi()), infoStyle, rowNum);
        }

        rowNum = createInfoRow(sheet, "Ngày tạo báo cáo:", generatedDate, createInfoStyle(sheet.getWorkbook()), rowNum);

        return rowNum + 2; // Add empty rows
    }

    /**
     * Tạo dòng thông tin với style
     */
    private int createInfoRow(Sheet sheet, String label, String value, CellStyle style, int rowNum) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(style);

        Cell valueCell = row.createCell(2);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(style);

        return rowNum + 1;
    }

    /**
     * Tạo phần thống kê tổng quan
     */
    private int createAttendanceReportSummary(Sheet sheet, List<Map<String, Object>> reportData,
                                              CellStyle summaryStyle, CellStyle dataStyle,
                                              CellStyle numberStyle, int startRow) {
        int rowNum = startRow;

        // Tiêu đề phần thống kê
        Row summaryTitleRow = sheet.createRow(rowNum++);
        Cell summaryTitleCell = summaryTitleRow.createCell(0);
        summaryTitleCell.setCellValue("THỐNG KÊ TỔNG QUAN");
        summaryTitleCell.setCellStyle(summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(summaryTitleRow.getRowNum(), summaryTitleRow.getRowNum(), 0, 11));

        rowNum++; // Empty row

        if (!reportData.isEmpty()) {
            // Tính toán thống kê
            int totalStudents = reportData.size();
            int totalSessions = reportData.isEmpty() ? 0 : (Integer) reportData.get(0).get("totalSessions");

            int totalPresent = reportData.stream()
                    .mapToInt(student -> (Integer) student.get("presentCount"))
                    .sum();

            int totalAbsent = reportData.stream()
                    .mapToInt(student -> (Integer) student.get("absentCount"))
                    .sum();

            int totalLate = reportData.stream()
                    .mapToInt(student -> (Integer) student.get("lateCount"))
                    .sum();

            int totalExcused = reportData.stream()
                    .mapToInt(student -> (Integer) student.get("excusedCount"))
                    .sum();

            double avgAttendanceRate = reportData.stream()
                    .mapToDouble(student -> (Double) student.get("attendanceRate"))
                    .average()
                    .orElse(0.0);

            // Phân loại sinh viên theo tỷ lệ điểm danh
            long excellentStudents = reportData.stream()
                    .filter(s -> (Double) s.get("attendanceRate") >= 90)
                    .count();

            long goodStudents = reportData.stream()
                    .filter(s -> (Double) s.get("attendanceRate") >= 80 && (Double) s.get("attendanceRate") < 90)
                    .count();

            long warningStudents = reportData.stream()
                    .filter(s -> (Double) s.get("attendanceRate") >= 60 && (Double) s.get("attendanceRate") < 80)
                    .count();

            long poorStudents = reportData.stream()
                    .filter(s -> (Double) s.get("attendanceRate") < 60)
                    .count();

            // Tạo bảng thống kê 2 cột
            String[][] summaryData = {
                    {"Tổng số sinh viên:", String.valueOf(totalStudents)},
                    {"Tổng số buổi học:", String.valueOf(totalSessions)},
                    {"Tổng lượt có mặt:", String.valueOf(totalPresent)},
                    {"Tổng lượt vắng mặt:", String.valueOf(totalAbsent)},
                    {"Tổng lượt đi muộn:", String.valueOf(totalLate)},
                    {"Tổng lượt vắng có phép:", String.valueOf(totalExcused)},
                    {"Tỷ lệ điểm danh trung bình:", String.format("%.1f%%", avgAttendanceRate)},
                    {"", ""}, // Empty row
                    {"PHÂN LOẠI SINH VIÊN:", ""},
                    {"Xuất sắc (≥90%):", String.valueOf(excellentStudents)},
                    {"Tốt (80-89%):", String.valueOf(goodStudents)},
                    {"Cảnh báo (60-79%):", String.valueOf(warningStudents)},
                    {"Yếu kém (<60%):", String.valueOf(poorStudents)}
            };

            for (String[] rowData : summaryData) {
                Row row = sheet.createRow(rowNum++);

                Cell labelCell = row.createCell(0);
                labelCell.setCellValue(rowData[0]);
                labelCell.setCellStyle(dataStyle);

                Cell valueCell = row.createCell(2);
                valueCell.setCellValue(rowData[1]);
                valueCell.setCellStyle(numberStyle);
            }
        }

        return rowNum + 2; // Add empty rows
    }

    /**
     * Tạo bảng chi tiết điểm danh sinh viên
     */
    private int createAttendanceReportDetailTable(Sheet sheet, List<Map<String, Object>> reportData,
                                                  CellStyle headerStyle, CellStyle dataStyle,
                                                  CellStyle numberStyle, CellStyle percentStyle, int startRow) {
        int rowNum = startRow;

        // Tiêu đề bảng chi tiết
        Row detailTitleRow = sheet.createRow(rowNum++);
        Cell detailTitleCell = detailTitleRow.createCell(0);
        detailTitleCell.setCellValue("CHI TIẾT ĐIỂM DANH TỪNG SINH VIÊN");
        detailTitleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(detailTitleRow.getRowNum(), detailTitleRow.getRowNum(), 0, 11));

        rowNum++; // Empty row

        // Header của bảng
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
                "STT", "Mã sinh viên", "Họ và tên", "Tổng buổi", "Có mặt",
                "Vắng mặt", "Đi muộn", "Vắng có phép", "Tỷ lệ (%)", "Xếp loại", "Đánh giá", "Ghi chú"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Dữ liệu chi tiết
        for (int i = 0; i < reportData.size(); i++) {
            Map<String, Object> student = reportData.get(i);
            Row row = sheet.createRow(rowNum++);

            // STT
            Cell sttCell = row.createCell(0);
            sttCell.setCellValue(i + 1);
            sttCell.setCellStyle(numberStyle);

            // Mã sinh viên
            Cell maSvCell = row.createCell(1);
            maSvCell.setCellValue((String) student.get("maSv"));
            maSvCell.setCellStyle(dataStyle);

            // Họ tên
            Cell hoTenCell = row.createCell(2);
            hoTenCell.setCellValue((String) student.get("hoTen"));
            hoTenCell.setCellStyle(dataStyle);

            // Tổng buổi
            Cell totalSessionsCell = row.createCell(3);
            totalSessionsCell.setCellValue((Integer) student.get("totalSessions"));
            totalSessionsCell.setCellStyle(numberStyle);

            // Có mặt
            Cell presentCell = row.createCell(4);
            presentCell.setCellValue((Integer) student.get("presentCount"));
            presentCell.setCellStyle(numberStyle);

            // Vắng mặt
            Cell absentCell = row.createCell(5);
            absentCell.setCellValue((Integer) student.get("absentCount"));
            absentCell.setCellStyle(numberStyle);

            // Đi muộn
            Cell lateCell = row.createCell(6);
            lateCell.setCellValue((Integer) student.get("lateCount"));
            lateCell.setCellStyle(numberStyle);

            // Vắng có phép
            Cell excusedCell = row.createCell(7);
            excusedCell.setCellValue((Integer) student.get("excusedCount"));
            excusedCell.setCellStyle(numberStyle);

            // Tỷ lệ %
            Cell rateCell = row.createCell(8);
            Double attendanceRate = (Double) student.get("attendanceRate");
            rateCell.setCellValue(String.format("%.1f%%", attendanceRate));
            rateCell.setCellStyle(percentStyle);

            // Xếp loại
            Cell classificationCell = row.createCell(9);
            String classification = getAttendanceClassification(attendanceRate);
            classificationCell.setCellValue(classification);
            classificationCell.setCellStyle(dataStyle);

            // Đánh giá
            Cell evaluationCell = row.createCell(10);
            String evaluation = getAttendanceEvaluation(attendanceRate);
            evaluationCell.setCellValue(evaluation);
            evaluationCell.setCellStyle(dataStyle);

            // Ghi chú
            Cell noteCell = row.createCell(11);
            String note = getAttendanceNote(attendanceRate);
            noteCell.setCellValue(note);
            noteCell.setCellStyle(dataStyle);
        }

        return rowNum;
    }

    /**
     * Tạo ghi chú và hướng dẫn
     */
    private void createAttendanceReportNotes(Sheet sheet, CellStyle dataStyle, int startRow) {
        int rowNum = startRow + 3;

        Row notesTitle = sheet.createRow(rowNum++);
        Cell notesTitleCell = notesTitle.createCell(0);
        notesTitleCell.setCellValue("GHI CHÚ VÀ HƯỚNG DẪN:");
        notesTitleCell.setCellStyle(createBoldStyle(sheet.getWorkbook()));

        String[] notes = {
                "1. Tỷ lệ điểm danh = (Số buổi có mặt + đi muộn) / Tổng số buổi học × 100%",
                "2. Xếp loại: Xuất sắc (≥90%), Tốt (80-89%), Khá (70-79%), Trung bình (60-69%), Yếu (50-59%), Kém (<50%)",
                "3. Sinh viên có tỷ lệ điểm danh dưới 80% sẽ không được dự thi cuối kỳ",
                "4. Vắng có phép: Sinh viên có đơn xin phép hợp lệ",
                "5. Báo cáo được tạo tự động từ hệ thống điểm danh khuôn mặt"
        };

        for (String note : notes) {
            Row noteRow = sheet.createRow(rowNum++);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue(note);
            noteCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(noteRow.getRowNum(), noteRow.getRowNum(), 0, 11));
        }

        // Footer
        rowNum += 2;
        Row footerRow = sheet.createRow(rowNum);
        Cell footerCell = footerRow.createCell(8);
        footerCell.setCellValue("Cần Thơ, ngày " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        footerCell.setCellStyle(dataStyle);

        Row signatureRow = sheet.createRow(rowNum + 2);
        Cell signatureCell = signatureRow.createCell(8);
        signatureCell.setCellValue("Giảng viên phụ trách");
        signatureCell.setCellStyle(createBoldStyle(sheet.getWorkbook()));
    }

    // Helper methods for creating styles
    private CellStyle createAttendanceReportTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createAttendanceReportHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createAttendanceReportDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

        return style;
    }

    private CellStyle createAttendanceReportNumberStyle(Workbook workbook) {
        CellStyle style = createAttendanceReportDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createAttendanceReportPercentStyle(Workbook workbook) {
        CellStyle style = createAttendanceReportDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createAttendanceReportSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    // Helper methods for classification
    private String getAttendanceClassification(Double attendanceRate) {
        if (attendanceRate >= 90) return "Xuất sắc";
        else if (attendanceRate >= 80) return "Tốt";
        else if (attendanceRate >= 70) return "Khá";
        else if (attendanceRate >= 60) return "Trung bình";
        else if (attendanceRate >= 50) return "Yếu";
        else return "Kém";
    }

    private String getAttendanceEvaluation(Double attendanceRate) {
        if (attendanceRate >= 80) return "Đủ điều kiện dự thi";
        else if (attendanceRate >= 60) return "Cần cải thiện";
        else return "Không đủ điều kiện dự thi";
    }

    private String getAttendanceNote(Double attendanceRate) {
        if (attendanceRate >= 90) return "Rất tốt";
        else if (attendanceRate >= 80) return "Đạt yêu cầu";
        else if (attendanceRate >= 60) return "Cần theo dõi";
        else return "Cần can thiệp";
    }
}