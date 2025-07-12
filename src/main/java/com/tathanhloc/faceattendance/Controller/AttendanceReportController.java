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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


// src/main/java/com/tathanhloc/faceattendance/Controller/AttendanceReportController.java

@RestController
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Slf4j
public class AttendanceReportController {

    private final LopHocPhanService lopHocPhanService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;
    private final HocKyService hocKyService;
    private final NamHocService namHocService;
    private final SinhVienService sinhVienService;

    /**
     * Trang báo cáo điểm danh
     */
    @GetMapping("/baocao-diemdanh")
    public String baoCaoDiemDanh(Authentication authentication, Model model,
                                 @RequestParam(value = "class", required = false) String classId) { // Sửa ở đây
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            log.info("🔍 Loading attendance report for lecturer: {}, class: {}", maGv, classId);

            // Kiểm tra quyền truy cập nếu có classId
            if (classId != null && !classId.isEmpty()) {
                LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(classId);
                if (!maGv.equals(lopHocPhan.getMaGv())) {
                    model.addAttribute("error", "Bạn không có quyền truy cập lớp học này");
                    return "lecturer/baocao-diemdanh";
                }
                model.addAttribute("lopHocPhan", lopHocPhan);
                log.info("✅ Found class: {}", lopHocPhan.getTenMonHoc());
            }

            // Lấy danh sách lớp của giảng viên
            List<LopHocPhanDTO> myClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()) && Boolean.TRUE.equals(lhp.getIsActive()))
                    .collect(Collectors.toList());

            log.info("📚 Found {} classes for lecturer", myClasses.size());

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("selectedClass", classId);

            return "lecturer/baocao-diemdanh";

        } catch (Exception e) {
            log.error("❌ Error loading attendance report: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải báo cáo điểm danh: " + e.getMessage());
            return "lecturer/baocao-diemdanh";
        }
    }

    /**
     * API lấy thông tin báo cáo điểm danh với tính toán chính xác
     */
    @GetMapping("/api/attendance-report/{maLhp}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAttendanceReport(
            @PathVariable String maLhp,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            // Tính toán thông tin học kỳ với logic mới
            Map<String, Object> semesterInfo = calculateAccurateSemesterInfo(lopHocPhan);

            // Lấy thống kê điểm danh chi tiết
            Map<String, Object> attendanceStats = calculateDetailedAttendanceStats(maLhp, fromDate, toDate);

            // Lấy dữ liệu cho biểu đồ
            Map<String, Object> chartData = generateChartData(maLhp, fromDate, toDate);

            Map<String, Object> result = new HashMap<>();
            result.put("lopHocPhan", lopHocPhan);
            result.put("semesterInfo", semesterInfo);
            result.put("attendanceStats", attendanceStats);
            result.put("chartData", chartData);
            result.put("success", true);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error generating attendance report: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * LOGIC MỚI: Tính toán thông tin học kỳ chính xác dựa trên ngày thực tế
     */
    private Map<String, Object> calculateAccurateSemesterInfo(LopHocPhanDTO lopHocPhan) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy thông tin học kỳ và năm học
            HocKyDTO hocKy = null;
            NamHocDTO namHoc = null;

            try {
                hocKy = hocKyService.getById(lopHocPhan.getHocKy());
            } catch (Exception e) {
                log.warn("Cannot find HocKy with ID: {}", lopHocPhan.getHocKy());
            }

            try {
                namHoc = namHocService.getById(lopHocPhan.getNamHoc());
            } catch (Exception e) {
                log.warn("Cannot find NamHoc with ID: {}", lopHocPhan.getNamHoc());
            }

            LocalDate ngayBatDau = hocKy != null ? hocKy.getNgayBatDau() : null;
            LocalDate ngayKetThuc = hocKy != null ? hocKy.getNgayKetThuc() : null;

            if (ngayBatDau == null || ngayKetThuc == null) {
                log.warn("Học kỳ {} không có thông tin ngày bắt đầu/kết thúc", lopHocPhan.getHocKy());
                return getDefaultSemesterInfo(lopHocPhan);
            }

            // Tính số tuần thực tế (làm tròn lên)
            long totalDays = ChronoUnit.DAYS.between(ngayBatDau, ngayKetThuc) + 1;
            int totalWeeks = (int) Math.ceil(totalDays / 7.0);

            // Lấy lịch học của lớp
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(lopHocPhan.getMaLhp());

            // Tính số ngày học mỗi tuần (số ngày khác nhau trong tuần có lịch)
            Set<Integer> daysOfWeek = lichHocList.stream()
                    .filter(lh -> Boolean.TRUE.equals(lh.getIsActive()))
                    .map(LichHocDTO::getThu)
                    .collect(Collectors.toSet());
            int sessionsPerWeek = daysOfWeek.size();

            // Tổng số buổi học dự kiến = số tuần * số ngày học mỗi tuần
            int totalExpectedSessions = totalWeeks * sessionsPerWeek;

            // Đếm số buổi học đã thực hiện (có điểm danh)
            int completedSessions = countCompletedSessions(lopHocPhan.getMaLhp());

            // Tính tiến độ học kỳ dựa trên thời gian thực tế
            LocalDate today = LocalDate.now();
            double progressByTime = 0.0;
            int remainingDays = 0;

            if (today.isBefore(ngayBatDau)) {
                progressByTime = 0.0;
                remainingDays = (int) ChronoUnit.DAYS.between(today, ngayBatDau);
            } else if (today.isAfter(ngayKetThuc)) {
                progressByTime = 100.0;
                remainingDays = 0;
            } else {
                long daysPassed = ChronoUnit.DAYS.between(ngayBatDau, today);
                progressByTime = (double) daysPassed / totalDays * 100;
                remainingDays = (int) ChronoUnit.DAYS.between(today, ngayKetThuc);
            }

            // Tính tỷ lệ điểm danh trung bình
            double avgAttendanceRate = calculateAverageAttendanceRateForClass(lopHocPhan.getMaLhp());

            // Dự đoán số buổi học còn lại
            int remainingWeeks = Math.max(0, (int) Math.ceil(remainingDays / 7.0));
            int estimatedRemainingSessions = remainingWeeks * sessionsPerWeek;

            result.put("ngayBatDau", ngayBatDau);
            result.put("ngayKetThuc", ngayKetThuc);
            result.put("totalDays", totalDays);
            result.put("totalWeeks", totalWeeks);
            result.put("sessionsPerWeek", sessionsPerWeek);
            result.put("totalExpectedSessions", totalExpectedSessions);
            result.put("completedSessions", completedSessions);
            result.put("estimatedRemainingSessions", estimatedRemainingSessions);
            result.put("progressByTime", Math.round(progressByTime * 10.0) / 10.0);
            result.put("remainingDays", remainingDays);
            result.put("avgAttendanceRate", Math.round(avgAttendanceRate * 10.0) / 10.0);
            result.put("daysOfWeek", daysOfWeek);
            result.put("lichHocList", lichHocList);

            log.info("✅ Calculated semester info for {}: {} weeks, {} sessions/week, {} total sessions",
                    lopHocPhan.getMaLhp(), totalWeeks, sessionsPerWeek, totalExpectedSessions);

            return result;

        } catch (Exception e) {
            log.error("Error calculating semester info: {}", e.getMessage());
            return getDefaultSemesterInfo(lopHocPhan);
        }
    }

    /**
     * Đếm số buổi học đã thực hiện (có điểm danh)
     */
    private int countCompletedSessions(String maLhp) {
        try {
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);
            int completed = 0;

            for (LichHocDTO lichHoc : lichHocList) {
                List<DiemDanhDTO> attendanceList = diemDanhService.getByMaLich(lichHoc.getMaLich());
                if (!attendanceList.isEmpty()) {
                    completed++;
                }
            }

            return completed;
        } catch (Exception e) {
            log.error("Error counting completed sessions: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Fallback khi không có thông tin học kỳ
     */
    private Map<String, Object> getDefaultSemesterInfo(LopHocPhanDTO lopHocPhan) {
        Map<String, Object> result = new HashMap<>();

        // Sử dụng giá trị mặc định như cũ
        int defaultWeeks = 16;
        List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(lopHocPhan.getMaLhp());
        int sessionsPerWeek = lichHocList.stream()
                .map(LichHocDTO::getThu)
                .collect(Collectors.toSet()).size();

        result.put("totalWeeks", defaultWeeks);
        result.put("sessionsPerWeek", sessionsPerWeek);
        result.put("totalExpectedSessions", defaultWeeks * sessionsPerWeek);
        result.put("completedSessions", countCompletedSessions(lopHocPhan.getMaLhp()));
        result.put("isDefault", true);
        result.put("progressByTime", 0.0);
        result.put("remainingDays", 0);
        result.put("avgAttendanceRate", 0.0);

        return result;
    }

    /**
     * Tính tỷ lệ điểm danh trung bình của cả lớp
     */
    private double calculateAverageAttendanceRateForClass(String maLhp) {
        try {
            // Lấy danh sách sinh viên trong lớp
            List<SinhVienDTO> sinhVienList = getSinhVienByMaLhp(maLhp);
            if (sinhVienList.isEmpty()) {
                return 0.0;
            }

            // Lấy tất cả lịch học của lớp
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);
            if (lichHocList.isEmpty()) {
                return 0.0;
            }

            double totalRate = 0.0;
            int validStudents = 0;

            // Tính tỷ lệ điểm danh cho từng sinh viên
            for (SinhVienDTO sv : sinhVienList) {
                int totalSessions = 0;
                int presentSessions = 0;

                for (LichHocDTO lichHoc : lichHocList) {
                    List<DiemDanhDTO> attendanceList = diemDanhService.getByMaLich(lichHoc.getMaLich());
                    if (!attendanceList.isEmpty()) {
                        totalSessions++;

                        // Kiểm tra sinh viên có điểm danh không
                        boolean isPresent = attendanceList.stream()
                                .anyMatch(dd -> sv.getMaSv().equals(dd.getMaSv()) &&
                                        ("PRESENT".equals(dd.getTrangThai()) || "LATE".equals(dd.getTrangThai())));

                        if (isPresent) {
                            presentSessions++;
                        }
                    }
                }

                if (totalSessions > 0) {
                    double studentRate = (double) presentSessions / totalSessions * 100;
                    totalRate += studentRate;
                    validStudents++;
                }
            }

            return validStudents > 0 ? totalRate / validStudents : 0.0;

        } catch (Exception e) {
            log.error("Error calculating average attendance rate: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Tính thống kê điểm danh chi tiết
     */
    private Map<String, Object> calculateDetailedAttendanceStats(String maLhp, String fromDate, String toDate) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Lấy danh sách sinh viên
            List<SinhVienDTO> sinhVienList = getSinhVienByMaLhp(maLhp);

            // Tính thống kê cho từng sinh viên
            List<Map<String, Object>> studentStats = new ArrayList<>();

            for (SinhVienDTO sv : sinhVienList) {
                Map<String, Object> svStat = calculateStudentAttendanceStats(sv.getMaSv(), maLhp, fromDate, toDate);
                studentStats.add(svStat);
            }

            // Tính thống kê tổng quan
            double totalPresentRate = studentStats.stream()
                    .mapToDouble(s -> (Double) s.getOrDefault("presentRate", 0.0))
                    .average().orElse(0.0);

            stats.put("totalStudents", sinhVienList.size());
            stats.put("averagePresentRate", Math.round(totalPresentRate * 10.0) / 10.0);
            stats.put("studentStats", studentStats);

            return stats;

        } catch (Exception e) {
            log.error("Error calculating attendance stats: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Tính thống kê điểm danh cho một sinh viên
     */
    private Map<String, Object> calculateStudentAttendanceStats(String maSv, String maLhp, String fromDate, String toDate) {
        Map<String, Object> stats = new HashMap<>();

        try {
            SinhVienDTO sinhVien = sinhVienService.getById(maSv);
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);

            int totalSessions = 0;
            int presentCount = 0;
            int absentCount = 0;
            int lateCount = 0;
            int excusedCount = 0;

            for (LichHocDTO lichHoc : lichHocList) {
                List<DiemDanhDTO> attendanceList = diemDanhService.getByMaLich(lichHoc.getMaLich());
                if (!attendanceList.isEmpty()) {
                    totalSessions++;

                    DiemDanhDTO studentAttendance = attendanceList.stream()
                            .filter(dd -> maSv.equals(dd.getMaSv()))
                            .findFirst()
                            .orElse(null);

                    if (studentAttendance != null) {
                        switch (studentAttendance.getTrangThai()) {
                            case CO_MAT:
                                presentCount++;
                                break;
                            case VANG_MAT:
                                absentCount++;
                                break;
                            case DI_TRE:
                                lateCount++;
                                break;
                            case VANG_CO_PHEP:
                                excusedCount++;
                                break;
                        }
                    } else {
                        // Không có record = vắng mặt
                        absentCount++;
                    }
                }
            }

            double presentRate = totalSessions > 0 ? (double) (presentCount + lateCount) / totalSessions * 100 : 0.0;

            stats.put("maSv", maSv);
            stats.put("hoTen", sinhVien.getHoTen());
            stats.put("totalSessions", totalSessions);
            stats.put("presentCount", presentCount);
            stats.put("absentCount", absentCount);
            stats.put("lateCount", lateCount);
            stats.put("excusedCount", excusedCount);
            stats.put("presentRate", Math.round(presentRate * 10.0) / 10.0);

            return stats;

        } catch (Exception e) {
            log.error("Error calculating student attendance stats: {}", e.getMessage());
            stats.put("maSv", maSv);
            stats.put("error", "Không thể tính toán");
            return stats;
        }
    }

    /**
     * Tạo dữ liệu cho biểu đồ
     */
    private Map<String, Object> generateChartData(String maLhp, String fromDate, String toDate) {
        Map<String, Object> chartData = new HashMap<>();

        try {
            // Dữ liệu cho biểu đồ timeline điểm danh
            List<Map<String, Object>> timelineData = new ArrayList<>();

            // Dữ liệu cho biểu đồ tỷ lệ điểm danh theo ngày
            List<Map<String, Object>> dailyAttendanceData = new ArrayList<>();

            // Dữ liệu cho biểu đồ top sinh viên có tỷ lệ điểm danh cao/thấp
            List<Map<String, Object>> studentRankingData = new ArrayList<>();

            chartData.put("timeline", timelineData);
            chartData.put("dailyAttendance", dailyAttendanceData);
            chartData.put("studentRanking", studentRankingData);

            return chartData;

        } catch (Exception e) {
            log.error("Error generating chart data: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Helper method: Lấy sinh viên theo mã lớp học phần
     */
    private List<SinhVienDTO> getSinhVienByMaLhp(String maLhp) {
        try {
            // Nếu SinhVienService không có method getByMaLhp, ta sẽ tự implement
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);

            // Lấy danh sách mã sinh viên từ lớp học phần
            Set<String> maSvs = lopHocPhan.getMaSvs() != null ? lopHocPhan.getMaSvs() : new HashSet<>();

            // Lấy thông tin chi tiết của từng sinh viên
            List<SinhVienDTO> result = new ArrayList<>();
            for (String maSv : maSvs) {
                try {
                    SinhVienDTO sv = sinhVienService.getById(maSv);
                    result.add(sv);
                } catch (Exception e) {
                    log.warn("Cannot find student with ID: {}", maSv);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Error getting students by class: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}