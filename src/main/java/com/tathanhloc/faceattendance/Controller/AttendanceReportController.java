package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller xử lý báo cáo điểm danh cho giảng viên
 */
@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Slf4j
public class AttendanceReportController {

    private final LopHocPhanService lopHocPhanService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;
    private final SinhVienService sinhVienService;
    private final DangKyHocService dangKyHocService;
    private final ExcelService excelService;

    /**
     * Trang báo cáo điểm danh
     */
    @GetMapping("/baocao-diemdanh")
    public String baoCaoDiemDanh(@RequestParam(required = false) String classId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        if (userDetails == null || userDetails.getTaiKhoan().getGiangVien() == null) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading attendance report for lecturer {} and class {}", maGv, classId);

            // Nếu có classId, kiểm tra quyền truy cập
            if (classId != null && !classId.isEmpty()) {
                LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(classId);

                if (!maGv.equals(lopHocPhan.getMaGv())) {
                    model.addAttribute("error", "Bạn không có quyền truy cập lớp học này");
                    return "lecturer/baocao-diemdanh";
                }

                model.addAttribute("lopHocPhan", lopHocPhan);
            }

            // Lấy danh sách lớp của giảng viên
            List<LopHocPhanDTO> myClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()) && Boolean.TRUE.equals(lhp.getIsActive()))
                    .collect(Collectors.toList());

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("selectedClass", classId);

            return "lecturer/baocao-diemdanh";

        } catch (Exception e) {
            log.error("Error loading attendance report: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải báo cáo điểm danh: " + e.getMessage());
            return "lecturer/baocao-diemdanh";
        }
    }

    /**
     * API lấy thông tin học kỳ
     */
    @GetMapping("/api/lichhoc/semester-info/{maLhp}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSemesterInfo(
            @PathVariable String maLhp,
            @RequestParam String semester,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            // Parse semester (format: "2024-2025-1")
            String[] semesterParts = semester.split("-");
            String hocKy = semesterParts.length > 2 ? semesterParts[2] : "1";
            String namHoc = semesterParts.length > 1 ? semesterParts[0] + "-" + semesterParts[1] : "2024-2025";

            // Tính toán thông tin học kỳ
            Map<String, Object> semesterInfo = calculateSemesterInfo(maLhp, hocKy, namHoc);

            return ResponseEntity.ok(semesterInfo);

        } catch (Exception e) {
            log.error("Error getting semester info: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API lấy báo cáo điểm danh theo học kỳ
     */
    @GetMapping("/api/diemdanh/semester-report/{maLhp}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSemesterReport(
            @PathVariable String maLhp,
            @RequestParam String semester,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            // Parse semester
            String[] semesterParts = semester.split("-");
            String hocKy = semesterParts.length > 2 ? semesterParts[2] : "1";
            String namHoc = semesterParts.length > 1 ? semesterParts[0] + "-" + semesterParts[1] : "2024-2025";

            // Lấy báo cáo điểm danh
            List<Map<String, Object>> report = generateSemesterAttendanceReport(maLhp, hocKy, namHoc);

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error getting semester report: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API lấy báo cáo điểm danh theo khoảng thời gian
     */
    @GetMapping("/api/diemdanh/period-report/{maLhp}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPeriodReport(
            @PathVariable String maLhp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            // Lấy báo cáo theo khoảng thời gian
            Map<String, Object> report = generatePeriodAttendanceReport(maLhp, fromDate, toDate);

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error getting period report: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API xuất báo cáo Excel
     */
    @GetMapping("/api/diemdanh/export/excel/{maLhp}")
    public ResponseEntity<byte[]> exportExcelReport(
            @PathVariable String maLhp,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            byte[] excelData;
            String filename;

            if (semester != null && !semester.isEmpty()) {
                // Xuất theo học kỳ
                String[] semesterParts = semester.split("-");
                String hocKy = semesterParts.length > 2 ? semesterParts[2] : "1";
                String namHoc = semesterParts.length > 1 ? semesterParts[0] + "-" + semesterParts[1] : "2024-2025";

                List<Map<String, Object>> reportData = generateSemesterAttendanceReport(maLhp, hocKy, namHoc);
                excelData = createExcelReport(lopHocPhan, reportData, "Báo cáo điểm danh học kỳ " + semester);
                filename = String.format("BaoCao_DiemDanh_%s_HK%s.xlsx", maLhp, semester);
            } else {
                // Xuất theo khoảng thời gian
                if (fromDate == null || toDate == null) {
                    return ResponseEntity.badRequest().build();
                }

                Map<String, Object> periodReport = generatePeriodAttendanceReport(maLhp, fromDate, toDate);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reportData = (List<Map<String, Object>>) periodReport.get("studentReports");

                String title = String.format("Báo cáo điểm danh từ %s đến %s",
                        fromDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        toDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                excelData = createExcelReport(lopHocPhan, reportData, title);
                filename = String.format("BaoCao_DiemDanh_%s_%s_%s.xlsx",
                        maLhp,
                        fromDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")),
                        toDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("Error exporting Excel report: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Tính toán thông tin học kỳ
     */
    private Map<String, Object> calculateSemesterInfo(String maLhp, String hocKy, String namHoc) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy thông tin lớp học phần
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);

            // Lấy tất cả lịch học của lớp trong học kỳ hiện tại
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp).stream()
                    .filter(lh -> hocKy.equals(lopHocPhan.getHocKy()) && namHoc.equals(lopHocPhan.getNamHoc()))
                    .collect(Collectors.toList());

            // Tính số tuần học (giả sử 1 học kỳ = 16 tuần, có thể lấy từ cấu hình)
            int totalWeeks = 16;

            // Tính số buổi học mỗi tuần (dựa trên số ngày trong tuần có lịch)
            Set<Integer> daysOfWeek = lichHocList.stream()
                    .map(LichHocDTO::getThu)
                    .collect(Collectors.toSet());
            int sessionsPerWeek = daysOfWeek.size();

            // Tổng số buổi học dự kiến trong học kỳ
            int totalSessions = totalWeeks * sessionsPerWeek;

            // Đếm số buổi học đã thực hiện (có điểm danh)
            long completedSessions = 0;
            for (LichHocDTO lichHoc : lichHocList) {
                List<DiemDanhDTO> attendanceList = diemDanhService.getByMaLich(lichHoc.getMaLich());
                if (!attendanceList.isEmpty()) {
                    completedSessions++;
                }
            }

            // Tính tỷ lệ điểm danh trung bình của cả lớp
            double avgAttendanceRate = calculateAverageAttendanceRateForClass(maLhp);

            // Tính tiến độ học kỳ dựa trên thời gian thực tế
            double semesterProgress = calculateSemesterProgress(hocKy, namHoc);

            result.put("totalWeeks", totalWeeks);
            result.put("sessionsPerWeek", sessionsPerWeek);
            result.put("totalSessions", totalSessions);
            result.put("completedSessions", completedSessions);
            result.put("avgAttendanceRate", avgAttendanceRate);
            result.put("semester", hocKy);
            result.put("academicYear", namHoc);
            result.put("semesterProgress", semesterProgress);
            result.put("daysOfWeek", daysOfWeek);

        } catch (Exception e) {
            log.error("Error calculating semester info: {}", e.getMessage());
            // Trả về giá trị mặc định
            result.put("totalWeeks", 16);
            result.put("sessionsPerWeek", 2);
            result.put("totalSessions", 32);
            result.put("completedSessions", 0);
            result.put("avgAttendanceRate", 0.0);
            result.put("semesterProgress", 0.0);
        }

        return result;
    }

    /**
     * Tính tiến độ học kỳ dựa trên thời gian thực tế
     */
    private double calculateSemesterProgress(String hocKy, String namHoc) {
        try {
            // Giả sử học kỳ 1 bắt đầu từ tháng 9, học kỳ 2 từ tháng 2
            LocalDate semesterStart;
            LocalDate semesterEnd;

            String[] namHocParts = namHoc.split("-");
            int startYear = Integer.parseInt(namHocParts[0]);
            int endYear = Integer.parseInt(namHocParts[1]);

            if ("1".equals(hocKy)) {
                // Học kỳ 1: từ tháng 9 năm trước đến tháng 1 năm sau
                semesterStart = LocalDate.of(startYear, 9, 1);
                semesterEnd = LocalDate.of(endYear, 1, 31);
            } else {
                // Học kỳ 2: từ tháng 2 đến tháng 6 năm sau
                semesterStart = LocalDate.of(endYear, 2, 1);
                semesterEnd = LocalDate.of(endYear, 6, 30);
            }

            LocalDate now = LocalDate.now();

            if (now.isBefore(semesterStart)) {
                return 0.0; // Chưa bắt đầu
            } else if (now.isAfter(semesterEnd)) {
                return 100.0; // Đã kết thúc
            } else {
                // Tính phần trăm dựa trên thời gian đã trải qua
                long totalDays = semesterStart.until(semesterEnd).getDays();
                long passedDays = semesterStart.until(now).getDays();
                return Math.min(100.0, (double) passedDays / totalDays * 100);
            }

        } catch (Exception e) {
            log.error("Error calculating semester progress: {}", e.getMessage());
            return 50.0; // Giá trị mặc định
        }
    }

    /**
     * Tính tỷ lệ điểm danh trung bình của cả lớp
     */
    private double calculateAverageAttendanceRateForClass(String maLhp) {
        try {
            List<DangKyHocDTO> dangKyList = dangKyHocService.getByMaLhp(maLhp);
            if (dangKyList.isEmpty()) return 0.0;

            double totalRate = 0.0;
            int studentCount = 0;

            for (DangKyHocDTO dangKy : dangKyList) {
                // Lấy tất cả điểm danh của sinh viên trong lớp này
                List<DiemDanhDTO> studentAttendance = getStudentAttendanceInClass(dangKy.getMaSv(), maLhp);

                if (!studentAttendance.isEmpty()) {
                    long presentCount = studentAttendance.stream()
                            .filter(dd -> TrangThaiDiemDanhEnum.CO_MAT.equals(dd.getTrangThai()))
                            .count();
                    double rate = (double) presentCount / studentAttendance.size() * 100;
                    totalRate += rate;
                    studentCount++;
                }
            }

            return studentCount > 0 ? totalRate / studentCount : 0.0;

        } catch (Exception e) {
            log.error("Error calculating average attendance rate for class: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Lấy điểm danh của sinh viên trong một lớp cụ thể
     */
    private List<DiemDanhDTO> getStudentAttendanceInClass(String maSv, String maLhp) {
        try {
            // Lấy tất cả lịch học của lớp
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);
            List<DiemDanhDTO> allAttendance = new ArrayList<>();

            for (LichHocDTO lichHoc : lichHocList) {
                List<DiemDanhDTO> sessionAttendance = diemDanhService.getByMaLich(lichHoc.getMaLich())
                        .stream()
                        .filter(dd -> maSv.equals(dd.getMaSv()))
                        .collect(Collectors.toList());
                allAttendance.addAll(sessionAttendance);
            }

            return allAttendance;
        } catch (Exception e) {
            log.error("Error getting student attendance in class: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Tạo báo cáo điểm danh theo học kỳ
     */
    private List<Map<String, Object>> generateSemesterAttendanceReport(String maLhp, String hocKy, String namHoc) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // Lấy danh sách sinh viên trong lớp
            List<DangKyHocDTO> dangKyList = dangKyHocService.getByMaLhp(maLhp);

            // Lấy thông tin lớp học phần để lấy hocKy và namHoc thực tế
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);

            // Lấy tất cả lịch học của lớp này (sử dụng hocKy và namHoc từ lopHocPhan)
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);

            // Tính tổng số buổi học trong học kỳ (giả sử 16 tuần)
            Set<Integer> daysOfWeek = lichHocList.stream()
                    .map(LichHocDTO::getThu)
                    .collect(Collectors.toSet());
            int totalSessions = 16 * daysOfWeek.size(); // 16 tuần * số ngày/tuần

            for (DangKyHocDTO dangKy : dangKyList) {
                SinhVienDTO sinhVien = sinhVienService.getByMaSv(dangKy.getMaSv());
                if (sinhVien == null) continue;

                Map<String, Object> studentReport = new HashMap<>();
                studentReport.put("maSv", sinhVien.getMaSv());
                studentReport.put("hoTen", sinhVien.getHoTen());
                studentReport.put("avatarUrl", sinhVien.getHinhAnh()); // Fix: sử dụng getHinhAnh() thay vì getAvatarUrl()
                studentReport.put("totalSessions", totalSessions);

                // Đếm các loại điểm danh cho sinh viên này trong tất cả lịch học của lớp
                int presentCount = 0, absentCount = 0, lateCount = 0, excusedCount = 0;

                for (LichHocDTO lichHoc : lichHocList) {
                    List<DiemDanhDTO> attendance = diemDanhService.getByMaLich(lichHoc.getMaLich())
                            .stream()
                            .filter(dd -> sinhVien.getMaSv().equals(dd.getMaSv()))
                            .collect(Collectors.toList());

                    if (attendance.isEmpty()) {
                        // Không có bản ghi điểm danh -> coi như chưa diễn ra hoặc vắng
                        // Chỉ tính vắng nếu buổi học đã diễn ra (có điểm danh của sinh viên khác)
                        List<DiemDanhDTO> totalAttendanceForSession = diemDanhService.getByMaLich(lichHoc.getMaLich());
                        if (!totalAttendanceForSession.isEmpty()) {
                            absentCount++; // Buổi học đã diễn ra nhưng sinh viên không có điểm danh
                        }
                    } else {
                        DiemDanhDTO record = attendance.get(0);
                        switch (record.getTrangThai()) {
                            case CO_MAT:
                                presentCount++;
                                break;
                            case VANG_MAT:
                                absentCount++;
                                break;
                            case DI_TRE: // Fix: sử dụng DI_TRE thay vì DI_MUON
                                lateCount++;
                                break;
                            case VANG_CO_PHEP:
                                excusedCount++;
                                break;
                        }
                    }
                }

                studentReport.put("presentCount", presentCount);
                studentReport.put("absentCount", absentCount);
                studentReport.put("lateCount", lateCount);
                studentReport.put("excusedCount", excusedCount);

                // Tính tỷ lệ điểm danh (bao gồm cả đi muộn là có mặt)
                int actualAttendedSessions = presentCount + lateCount;
                int totalActualSessions = presentCount + absentCount + lateCount + excusedCount;
                double attendanceRate = totalActualSessions > 0 ?
                        (double) actualAttendedSessions / totalActualSessions * 100 : 0;

                studentReport.put("attendanceRate", attendanceRate);

                result.add(studentReport);
            }

            // Sắp xếp theo tỷ lệ điểm danh giảm dần
            result.sort((a, b) -> Double.compare(
                    (Double) b.get("attendanceRate"),
                    (Double) a.get("attendanceRate")
            ));

        } catch (Exception e) {
            log.error("Error generating semester attendance report: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Tạo báo cáo điểm danh theo khoảng thời gian
     */
    private Map<String, Object> generatePeriodAttendanceReport(String maLhp, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> studentReports = new ArrayList<>();

        try {
            // Lấy danh sách sinh viên
            List<DangKyHocDTO> dangKyList = dangKyHocService.getByMaLhp(maLhp);

            // Lấy điểm danh trong khoảng thời gian
            List<DiemDanhDTO> allAttendance = diemDanhService.getByLopHocPhanAndDateRange(maLhp, fromDate, toDate);

            // Đếm tổng số buổi học trong khoảng thời gian
            Set<LocalDate> uniqueDates = allAttendance.stream()
                    .map(DiemDanhDTO::getNgayDiemDanh)
                    .collect(Collectors.toSet());
            int totalSessions = uniqueDates.size();

            // Thống kê tổng quan
            Map<String, Object> summary = new HashMap<>();
            long totalDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            summary.put("totalDays", totalDays);
            summary.put("totalSessions", totalSessions);

            int totalPresent = 0, totalAbsent = 0, totalLate = 0, totalExcused = 0;

            for (DangKyHocDTO dangKy : dangKyList) {
                SinhVienDTO sinhVien = sinhVienService.getByMaSv(dangKy.getMaSv());
                if (sinhVien == null) continue;

                // Lấy điểm danh của sinh viên này
                List<DiemDanhDTO> studentAttendance = allAttendance.stream()
                        .filter(dd -> dangKy.getMaSv().equals(dd.getMaSv()))
                        .collect(Collectors.toList());

                Map<String, Object> studentReport = new HashMap<>();
                studentReport.put("maSv", sinhVien.getMaSv());
                studentReport.put("hoTen", sinhVien.getHoTen());
                studentReport.put("avatarUrl", sinhVien.getHinhAnh()); // Fix: sử dụng getHinhAnh()
                studentReport.put("totalSessions", totalSessions);

                // Đếm các loại điểm danh
                long presentCount = studentAttendance.stream().filter(dd -> TrangThaiDiemDanhEnum.CO_MAT.equals(dd.getTrangThai())).count();
                long absentCount = studentAttendance.stream().filter(dd -> TrangThaiDiemDanhEnum.VANG_MAT.equals(dd.getTrangThai())).count();
                long lateCount = studentAttendance.stream().filter(dd -> TrangThaiDiemDanhEnum.DI_TRE.equals(dd.getTrangThai())).count();
                long excusedCount = studentAttendance.stream().filter(dd -> TrangThaiDiemDanhEnum.VANG_CO_PHEP.equals(dd.getTrangThai())).count();

                // Cộng vào tổng
                totalPresent += presentCount;
                totalAbsent += absentCount;
                totalLate += lateCount;
                totalExcused += excusedCount;

                studentReport.put("presentCount", presentCount);
                studentReport.put("absentCount", absentCount);
                studentReport.put("lateCount", lateCount);
                studentReport.put("excusedCount", excusedCount);

                double attendanceRate = totalSessions > 0 ? (double) presentCount / totalSessions * 100 : 0;
                studentReport.put("attendanceRate", attendanceRate);

                studentReports.add(studentReport);
            }

            summary.put("totalPresent", totalPresent);
            summary.put("totalAbsent", totalAbsent);
            summary.put("totalLate", totalLate);
            summary.put("totalExcused", totalExcused);

            result.put("summary", summary);
            result.put("studentReports", studentReports);

        } catch (Exception e) {
            log.error("Error generating period attendance report: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Tạo file Excel báo cáo
     */
    private byte[] createExcelReport(LopHocPhanDTO lopHocPhan, List<Map<String, Object>> reportData, String title) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("lopHocPhan", lopHocPhan);
            data.put("reportData", reportData);
            data.put("title", title);
            data.put("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            return excelService.generateAttendanceReport(data);

        } catch (Exception e) {
            log.error("Error creating Excel report: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Lấy điểm danh theo lớp học phần và khoảng thời gian
     */
    private List<DiemDanhDTO> getByLopHocPhanAndDateRange(String maLhp, LocalDate fromDate, LocalDate toDate) {
        try {
            // Lấy tất cả lịch học của lớp học phần này
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);

            if (lichHocList.isEmpty()) {
                log.warn("No schedule found for class {}", maLhp);
                return new ArrayList<>();
            }

            // Lấy các mã lịch học
            List<String> maLichList = lichHocList.stream()
                    .map(LichHocDTO::getMaLich)
                    .collect(Collectors.toList());

            // Lấy điểm danh theo danh sách mã lịch và khoảng thời gian
            List<DiemDanhDTO> allAttendance = new ArrayList<>();

            for (String maLich : maLichList) {
                List<DiemDanhDTO> scheduleAttendance = diemDanhService.getByMaLich(maLich)
                        .stream()
                        .filter(dd -> {
                            LocalDate attendanceDate = dd.getNgayDiemDanh();
                            return !attendanceDate.isBefore(fromDate) && !attendanceDate.isAfter(toDate);
                        })
                        .collect(Collectors.toList());
                allAttendance.addAll(scheduleAttendance);
            }

            return allAttendance;

        } catch (Exception e) {
            log.error("Error getting attendance for class {} in date range: {}", maLhp, e.getMessage());
            return new ArrayList<>();
        }
    }
}