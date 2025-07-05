package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import com.tathanhloc.faceattendance.Enum.GioiTinhEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelService {

    private final SinhVienService sinhVienService;
    private final LopRepository lopRepository;
    private final FileUploadService fileUploadService;

    private static final String[] REQUIRED_HEADERS = {"maSv", "hoTen", "maLop"};
    private static final String[] OPTIONAL_HEADERS = {"email", "gioiTinh", "ngaySinh"};
    private static final String[] ALL_HEADERS = {"maSv", "hoTen", "email", "gioiTinh", "ngaySinh", "maLop"};

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
}