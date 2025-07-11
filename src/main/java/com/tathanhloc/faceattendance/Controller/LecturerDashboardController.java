package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.LichHocDTO;
import com.tathanhloc.faceattendance.DTO.LopHocPhanDTO;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * Trang dashboard chính của giảng viên
     */
    @GetMapping(value = {"/dashboard", "/dashboard.html"})
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

            // Tính toán thống kê
            int totalClasses = myClasses.size();
            int totalStudents = myClasses.stream().mapToInt(LopHocPhanDTO::getSoLuongSinhVien).sum();
            long classesToday = (long) lichHocService.getTodaySchedule(maGv, null, null).size();
            long attendanceToday = diemDanhService.countTodayDiemDanh();

            // Thêm dữ liệu vào model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myClasses", myClasses);
            model.addAttribute("totalClasses", totalClasses);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("classesToday", classesToday);
            model.addAttribute("attendanceToday", attendanceToday);

            log.info("✅ Lecturer dashboard loaded successfully for {}: {} classes, {} students",
                    maGv, totalClasses, totalStudents);
            return "lecturer/dashboard";

        } catch (Exception e) {
            log.error("❌ Error loading lecturer dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "lecturer/dashboard";
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
            log.info("Loading attendance page for lecturer: {}", maGv);

            // Lấy lịch học hôm nay của giảng viên
            List<LichHocDTO> todaySchedules = lichHocService.getTodaySchedule(maGv, null, null);

            // Nếu có classId, lọc theo lớp cụ thể
            if (classId != null && !classId.isEmpty()) {
                // TODO: Lọc theo classId
                model.addAttribute("selectedClass", classId);
            }

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("todaySchedules", todaySchedules);
            model.addAttribute("currentDate", LocalDate.now());

            log.info("✅ Attendance page loaded for lecturer {}", maGv);
            return "lecturer/diemdanh-homnay";

        } catch (Exception e) {
            log.error("❌ Error loading attendance page", e);
            model.addAttribute("error", "Không thể tải trang điểm danh: " + e.getMessage());
            return "lecturer/diemdanh-homnay";
        }
    }

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
     * Trang báo cáo điểm danh
     */
    @GetMapping("/baocao-diemdanh")
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
     * Trang lịch giảng dạy
     */
    @GetMapping("/lich-giangday")
    public String lichGiangDay(Authentication authentication, Model model,
                               @RequestParam(required = false) String week,
                               @RequestParam(required = false) String month) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/lich-giangday";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading teaching schedule for lecturer: {}", maGv);

            // Lấy lịch giảng dạy của giảng viên
            List<LichHocDTO> teachingSchedules = lichHocService.getByGiangVien(maGv);

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("teachingSchedules", teachingSchedules);
            model.addAttribute("selectedWeek", week);
            model.addAttribute("selectedMonth", month);
            model.addAttribute("currentDate", LocalDate.now());

            log.info("✅ Teaching schedule loaded for lecturer {}", maGv);
            return "lecturer/lich-giangday";

        } catch (Exception e) {
            log.error("❌ Error loading teaching schedule", e);
            model.addAttribute("error", "Không thể tải lịch giảng dạy: " + e.getMessage());
            return "lecturer/lich-giangday";
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
}