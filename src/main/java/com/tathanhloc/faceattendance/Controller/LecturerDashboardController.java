package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller xử lý các trang dashboard của giảng viên
 * Bao gồm: dashboard chính, lớp học, điểm danh, báo cáo, lịch giảng dạy
 */
@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Slf4j
public class LecturerDashboardController {

    private final LopHocPhanService lopHocPhanService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;
    private final SinhVienService sinhVienService;
    private final MonHocService monHocService;
    private final DangKyHocService dangKyHocService;

    /**
     * Trang dashboard chính của giảng viên
     */
    @GetMapping(value = {"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("=== LECTURER DASHBOARD ACCESS ===");
        log.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");

        if (userDetails == null) {
            log.warn("No user details found, redirecting to login");
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Kiểm tra user có thông tin giảng viên không
            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/dashboard";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading lecturer dashboard for lecturer: {}", maGv);

            // Lấy danh sách lớp học của giảng viên
            List<LopHocPhanDTO> myClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()) && Boolean.TRUE.equals(lhp.getIsActive()))
                    .collect(Collectors.toList());

            // Lấy lịch học hôm nay - sử dụng method có sẵn
            List<LichHocDTO> todaySchedule = lichHocService.getTodaySchedule(maGv, null, null);

            // Lấy các nhóm đang dạy (top 5 lớp gần đây)
            List<LopHocPhanDTO> recentClasses = myClasses.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            // Tính toán thống kê
            int totalClasses = myClasses.size();
            int totalStudents = 0;
            // Tính tổng số sinh viên từ tất cả lớp
            for (LopHocPhanDTO lhp : myClasses) {
                try {
                    List<DangKyHocDTO> students = dangKyHocService.getByMaLhp(lhp.getMaLhp());
                    totalStudents += students.size();
                } catch (Exception e) {
                    log.warn("Cannot get students for class {}: {}", lhp.getMaLhp(), e.getMessage());
                }
            }

            long classesToday = todaySchedule.size();
            long attendanceToday = diemDanhService.countTodayDiemDanh();

            // Thêm dữ liệu vào model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("recentClasses", recentClasses);
            model.addAttribute("todaySchedule", todaySchedule);
            model.addAttribute("totalClasses", totalClasses);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("classesToday", classesToday);
            model.addAttribute("attendanceToday", attendanceToday);

            log.info("✅ Lecturer dashboard loaded successfully for {}: {} classes, {} students, {} classes today",
                    maGv, totalClasses, totalStudents, classesToday);
            return "lecturer/dashboard";

        } catch (Exception e) {
            log.error("❌ Error loading lecturer dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "lecturer/dashboard";
        }
    }

    /**
     * Trang lịch giảng dạy
     */
    @GetMapping("/lichhoc")
    public String lichHoc(Authentication authentication, Model model,
                          @RequestParam(defaultValue = "week") String view,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/lichhoc";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            LocalDate selectedDate = date != null ? date : LocalDate.now();

            List<LichHocDTO> schedule;

            // Sử dụng các method có sẵn
            if ("day".equals(view)) {
                schedule = lichHocService.getTodaySchedule(maGv, null, null);
            } else {
                // Lấy tất cả lịch của giảng viên rồi filter
                schedule = lichHocService.getByGiangVien(maGv);
            }

            model.addAttribute("schedule", schedule);
            model.addAttribute("currentView", view);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("currentUser", userDetails.getTaiKhoan());

            return "lecturer/lichhoc";
        } catch (Exception e) {
            log.error("Error loading schedule for lecturer: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải lịch học: " + e.getMessage());
            return "lecturer/lichhoc";
        }
    }

    /**
     * Trang danh sách lớp học của giảng viên
     */
    @GetMapping("/lophoc")
    public String lophoc(Authentication authentication, Model model,
                         @RequestParam(required = false) String semester,
                         @RequestParam(required = false) String year) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Kiểm tra thông tin giảng viên
            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/lophoc";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading classes page for lecturer: {}", maGv);

            // Lấy danh sách lớp học của giảng viên này
            List<LopHocPhanDTO> lecturerClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            // Lọc theo học kỳ và năm học nếu có
            if (semester != null && !semester.isEmpty()) {
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> semester.equals(lhp.getHocKy()))
                        .collect(Collectors.toList());
            }
            if (year != null && !year.isEmpty()) {
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> year.equals(lhp.getNamHoc()))
                        .collect(Collectors.toList());
            }

            // Thêm thông tin số lượng sinh viên cho mỗi lớp
            for (LopHocPhanDTO lhp : lecturerClasses) {
                try {
                    List<DangKyHocDTO> students = dangKyHocService.getByMaLhp(lhp.getMaLhp());
                    lhp.setSoLuongSinhVien(students.size());
                } catch (Exception e) {
                    log.warn("Cannot get student count for class {}: {}", lhp.getMaLhp(), e.getMessage());
                    lhp.setSoLuongSinhVien(0);
                }
            }

            // Thêm dữ liệu vào model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lecturerClasses", lecturerClasses);
            model.addAttribute("selectedSemester", semester);
            model.addAttribute("selectedYear", year);

            log.info("✅ Classes page loaded successfully for lecturer {}: {} classes",
                    maGv, lecturerClasses.size());
            return "lecturer/lophoc";

        } catch (Exception e) {
            log.error("❌ Error loading lecturer classes page", e);
            model.addAttribute("error", "Không thể tải dữ liệu lớp học: " + e.getMessage());
            return "lecturer/lophoc";
        }
    }

    /**
     * Trang điểm danh hôm nay
     */
    @GetMapping("/diemdanh-homnay")
    public String diemDanhHomNay(Authentication authentication, Model model,
                                 @RequestParam(required = false) String classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/diemdanh-homnay";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            LocalDate today = LocalDate.now();
            log.info("Loading attendance page for lecturer: {}", maGv);

            // Lấy lịch học hôm nay của giảng viên
            List<LichHocDTO> todaySchedules = lichHocService.getTodaySchedule(maGv, null, null);

            // Lấy thông tin điểm danh cho mỗi lịch học hôm nay
            for (LichHocDTO lichHoc : todaySchedules) {
                try {
                    List<DiemDanhDTO> attendanceList = diemDanhService.getByMaLich(lichHoc.getMaLich());
                    // Có thể thêm thông tin này vào DTO hoặc xử lý riêng
                } catch (Exception e) {
                    log.warn("Cannot get attendance for schedule {}: {}", lichHoc.getMaLich(), e.getMessage());
                }
            }

            // Nếu có classId, lọc theo lớp cụ thể
            if (classId != null && !classId.isEmpty()) {
                model.addAttribute("selectedClass", classId);
            }

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("todaySchedules", todaySchedules);
            model.addAttribute("currentDate", today);

            log.info("✅ Attendance page loaded for lecturer {}", maGv);
            return "lecturer/diemdanh-homnay";

        } catch (Exception e) {
            log.error("❌ Error loading attendance page", e);
            model.addAttribute("error", "Không thể tải trang điểm danh: " + e.getMessage());
            return "lecturer/diemdanh-homnay";
        }
    }

    /**
     * Trang điểm danh thủ công
     */
    @GetMapping("/diemdanh-thucong")
    public String diemDanhThuCong(Authentication authentication, Model model,
                                  @RequestParam(required = false) String maLhp,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/diemdanh-thucong";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            LocalDate selectedDate = date != null ? date : LocalDate.now();

            // Lấy danh sách lớp học của giảng viên
            List<LopHocPhanDTO> myClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()) && Boolean.TRUE.equals(lhp.getIsActive()))
                    .collect(Collectors.toList());

            List<DangKyHocDTO> studentRegistrations = null;
            List<LichHocDTO> classSchedules = null;

            if (maLhp != null) {
                // Lấy danh sách sinh viên đăng ký lớp
                studentRegistrations = dangKyHocService.getByMaLhp(maLhp);

                // Lấy lịch học của lớp
                classSchedules = lichHocService.getByLopHocPhan(maLhp);
            }

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("studentRegistrations", studentRegistrations);
            model.addAttribute("classSchedules", classSchedules);
            model.addAttribute("selectedClass", maLhp);
            model.addAttribute("selectedDate", selectedDate);

            log.info("✅ Manual attendance page loaded for lecturer {}", maGv);
            return "lecturer/diemdanh-thucong";

        } catch (Exception e) {
            log.error("❌ Error loading manual attendance page", e);
            model.addAttribute("error", "Không thể tải trang điểm danh thủ công: " + e.getMessage());
            return "lecturer/diemdanh-thucong";
        }
    }

    /**
     * Trang báo cáo ngày học
     */
    @GetMapping("/baocao-ngay")
    public String baoCaoNgay(Authentication authentication, Model model,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) String maLhp) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/baocao-ngay";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            LocalDate selectedDate = date != null ? date : LocalDate.now();

            // Lấy danh sách lớp học của giảng viên
            List<LopHocPhanDTO> myClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()) && Boolean.TRUE.equals(lhp.getIsActive()))
                    .collect(Collectors.toList());

            // TODO: Tạo báo cáo dựa trên dữ liệu có sẵn
            // Hiện tại chưa có method báo cáo trong service, sẽ implement sau

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("selectedClass", maLhp);

            log.info("✅ Daily report loaded for lecturer {}", maGv);
            return "lecturer/baocao-ngay";
        } catch (Exception e) {
            log.error("Error loading daily report: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải báo cáo ngày học: " + e.getMessage());
            return "lecturer/baocao-ngay";
        }
    }

    /**
     * Trang báo cáo học kỳ
     */
    @GetMapping("/baocao-hocky")
    public String baoCaoHocKy(Authentication authentication, Model model,
                              @RequestParam(required = false) String semester,
                              @RequestParam(required = false) String maLhp) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/baocao-hocky";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            String selectedSemester = semester != null ? semester : "2024-2025-1"; // Default current semester

            // Lấy danh sách lớp học của giảng viên
            List<LopHocPhanDTO> myClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()) && Boolean.TRUE.equals(lhp.getIsActive()))
                    .collect(Collectors.toList());

            // TODO: Tạo báo cáo học kỳ dựa trên dữ liệu có sẵn

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("selectedSemester", selectedSemester);
            model.addAttribute("selectedClass", maLhp);

            log.info("✅ Semester report loaded for lecturer {}", maGv);
            return "lecturer/baocao-hocky";
        } catch (Exception e) {
            log.error("Error loading semester report: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải báo cáo học kỳ: " + e.getMessage());
            return "lecturer/baocao-hocky";
        }
    }

    // Giữ nguyên các method cũ để tương thích
    /**
     * Trang lịch sử điểm danh
     */
    @GetMapping("/lichsu-diemdanh")
    public String lichSuDiemDanh(Authentication authentication, Model model,
                                 @RequestParam(required = false) String classId,
                                 @RequestParam(required = false) String fromDate,
                                 @RequestParam(required = false) String toDate) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/lichsu-diemdanh";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading attendance history for lecturer: {}", maGv);

            // Lấy danh sách lớp của giảng viên để filter
            List<LopHocPhanDTO> lecturerClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lecturerClasses", lecturerClasses);
            model.addAttribute("selectedClass", classId);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);

            log.info("✅ Attendance history page loaded for lecturer {}", maGv);
            return "lecturer/lichsu-diemdanh";

        } catch (Exception e) {
            log.error("❌ Error loading attendance history", e);
            model.addAttribute("error", "Không thể tải lịch sử điểm danh: " + e.getMessage());
            return "lecturer/lichsu-diemdanh";
        }
    }

    /**
     * Trang báo cáo điểm danh (giữ nguyên method cũ)
     * Đổi URL để tránh xung đột với AttendanceReportController
     */
    @GetMapping("/baocao-diemdanh-old")
    public String baoCaoDiemDanh(Authentication authentication, Model model,
                                 @RequestParam(required = false) String classId,
                                 @RequestParam(required = false) String reportType) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/baocao-diemdanh";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading attendance reports for lecturer: {}", maGv);

            // Lấy danh sách lớp của giảng viên
            List<LopHocPhanDTO> lecturerClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lecturerClasses", lecturerClasses);
            model.addAttribute("selectedClass", classId);
            model.addAttribute("reportType", reportType);

            log.info("✅ Attendance reports page loaded for lecturer {}", maGv);
            return "lecturer/baocao-diemdanh";

        } catch (Exception e) {
            log.error("❌ Error loading attendance reports", e);
            model.addAttribute("error", "Không thể tải báo cáo điểm danh: " + e.getMessage());
            return "lecturer/baocao-diemdanh";
        }
    }

    /**
     * Trang danh sách sinh viên
     */
    @GetMapping("/danhsach-sinhvien")
    public String danhSachSinhVien(Authentication authentication, Model model,
                                   @RequestParam(required = false) String classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/danhsach-sinhvien";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading student list for lecturer: {}", maGv);

            // Lấy danh sách lớp của giảng viên
            List<LopHocPhanDTO> lecturerClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lecturerClasses", lecturerClasses);
            model.addAttribute("selectedClass", classId);

            log.info("✅ Student list page loaded for lecturer {}", maGv);
            return "lecturer/danhsach-sinhvien";

        } catch (Exception e) {
            log.error("❌ Error loading student list", e);
            model.addAttribute("error", "Không thể tải danh sách sinh viên: " + e.getMessage());
            return "lecturer/danhsach-sinhvien";
        }
    }

    /**
     * Trang xem sinh viên chưa cập nhật sinh trắc học
     */
    @GetMapping("/sinhvien-chua-capnhat")
    public String sinhVienChuaCapNhat(Authentication authentication, Model model,
                                      @RequestParam(required = false) String classId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/sinhvien-chua-capnhat";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading students without biometric data for lecturer: {}", maGv);

            // Lấy danh sách lớp của giảng viên
            List<LopHocPhanDTO> lecturerClasses = lopHocPhanService.getAll().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            // TODO: Lấy danh sách sinh viên chưa cập nhật sinh trắc học
            // List<SinhVienDTO> studentsWithoutBiometric = sinhVienService.getStudentsWithoutBiometricData(classId);

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lecturerClasses", lecturerClasses);
            model.addAttribute("selectedClass", classId);
            // model.addAttribute("studentsWithoutBiometric", studentsWithoutBiometric);

            log.info("✅ Students without biometric page loaded for lecturer {}", maGv);
            return "lecturer/sinhvien-chua-capnhat";

        } catch (Exception e) {
            log.error("❌ Error loading students without biometric", e);
            model.addAttribute("error", "Không thể tải danh sách sinh viên: " + e.getMessage());
            return "lecturer/sinhvien-chua-capnhat";
        }
    }

    /**
     * Trang thông tin cá nhân
     */
    @GetMapping("/thongtin-canhan")
    public String thongTinCaNhan(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/thongtin-canhan";
            }

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lecturer", userDetails.getTaiKhoan().getGiangVien());

            log.info("✅ Profile page loaded for lecturer");
            return "lecturer/thongtin-canhan";
        } catch (Exception e) {
            log.error("Error loading profile page: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải thông tin cá nhân: " + e.getMessage());
            return "lecturer/thongtin-canhan";
        }
    }

    /**
     * API: Lấy lịch học hôm nay
     */
    @GetMapping("/api/schedule/today")
    @ResponseBody
    public List<LichHocDTO> getTodayScheduleApi(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                return List.of();
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            return lichHocService.getTodaySchedule(maGv, null, null);
        } catch (Exception e) {
            log.error("Error getting today schedule: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * API: Lấy danh sách sinh viên theo lớp học phần
     */
    @GetMapping("/api/students/{maLhp}")
    @ResponseBody
    public List<DangKyHocDTO> getStudentsByClass(Authentication authentication, @PathVariable String maLhp) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                return List.of();
            }

            // Kiểm tra giảng viên có quyền truy cập lớp này không
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            LopHocPhanDTO lhp = lopHocPhanService.getById(maLhp);

            if (!maGv.equals(lhp.getMaGv())) {
                log.warn("Lecturer {} tried to access class {} which is not theirs", maGv, maLhp);
                return List.of();
            }

            return dangKyHocService.getByMaLhp(maLhp);
        } catch (Exception e) {
            log.error("Error getting students by class: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Trang chi tiết lớp học cụ thể
     */
    @GetMapping("/lophoc/{maLhp}")
    public String chiTietLopHoc(@PathVariable String maLhp,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        if (userDetails == null || !userDetails.isCredentialsNonExpired()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Kiểm tra thông tin giảng viên
            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/chitiet-lophoc";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading class details for lecturer {} and class {}", maGv, maLhp);

            // Lấy thông tin lớp học phần
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);

            // Kiểm tra quyền truy cập (chỉ giảng viên của lớp mới được xem)
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                log.warn("Lecturer {} tried to access class {} which is not theirs", maGv, maLhp);
                model.addAttribute("error", "Bạn không có quyền truy cập lớp học này");
                return "lecturer/chitiet-lophoc";
            }

            // Lấy danh sách sinh viên trong lớp
            List<DangKyHocDTO> danhSachDangKy = dangKyHocService.getByMaLhp(maLhp);
            List<SinhVienDTO> danhSachSinhVien = danhSachDangKy.stream()
                    .map(dk -> sinhVienService.getByMaSv(dk.getMaSv()))
                    .collect(Collectors.toList());

            // Lấy lịch học của lớp này
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);

            // Lấy thống kê điểm danh
            // TODO: Implement thống kê điểm danh theo lớp

            // Thêm vào model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lopHocPhan", lopHocPhan);
            model.addAttribute("danhSachSinhVien", danhSachSinhVien);
            model.addAttribute("lichHocList", lichHocList);
            model.addAttribute("soLuongSinhVien", danhSachSinhVien.size());

            log.info("✅ Class details loaded for {}: {} students", maLhp, danhSachSinhVien.size());
            return "lecturer/chitiet-lophoc";

        } catch (Exception e) {
            log.error("❌ Error loading class details for {}", maLhp, e);
            model.addAttribute("error", "Không thể tải thông tin lớp học: " + e.getMessage());
            return "lecturer/chitiet-lophoc";
        }
    }

    /**
     * Trang lịch giảng dạy
     */
    @GetMapping("/lich-giangday")
    public String lichGiangDay(Authentication authentication, Model model,
                               @RequestParam(required = false) String view,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate selectedDate) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/lich-giangday";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading teaching schedule for lecturer: {}", maGv);

            // Set default view
            view = view != null ? view : "table";
            selectedDate = selectedDate != null ? selectedDate : LocalDate.now();

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("currentView", view);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("maGv", maGv);

            return "lecturer/lich-giangday";

        } catch (Exception e) {
            log.error("Error loading teaching schedule: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải lịch giảng dạy: " + e.getMessage());
            return "lecturer/lich-giangday";
        }
    }
}
