package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final SinhVienService sinhVienService;
    private final GiangVienService giangVienService;
    private final LopService lopService;
    private final DiemDanhService diemDanhService;

    @GetMapping(value ={"/dashboard","/dashboard.html"})
    public String dashboard(Authentication authentication, Model model) {
        log.info("=== ADMIN DASHBOARD ACCESS ===");
        log.info("Authentication: {}", authentication);
        log.info("Is Authenticated: {}", authentication != null && authentication.isAuthenticated());

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("❌ No authentication found");
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Get user details from authentication
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            log.info("✅ Admin dashboard access granted for user: {}", userDetails.getUsername());
            log.info("User role: {}", userDetails.getTaiKhoan().getVaiTro());

            // Load dashboard statistics
            int totalStudents = sinhVienService.getAll().size();
            int totalLecturers = giangVienService.getAll().size();
            long totalClasses = lopService.count();
            long attendanceToday = diemDanhService.countTodayDiemDanh();

            log.info("Dashboard stats loaded - Students: {}, Lecturers: {}, Classes: {}, Attendance: {}",
                    totalStudents, totalLecturers, totalClasses, attendanceToday);

            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalLecturers", totalLecturers);
            model.addAttribute("totalClasses", totalClasses);
            model.addAttribute("attendanceToday", attendanceToday);
            model.addAttribute("currentUser", userDetails.getTaiKhoan());

            log.info("✅ Admin dashboard loaded successfully");
            return "admin/dashboard";

        } catch (Exception e) {
            log.error("❌ Error loading admin dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "admin/dashboard";
        }
    }

    @GetMapping("/khoa")
    public String khoaManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        log.info("Admin khoa management access: {}", userDetails.getUsername());
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/khoa";
    }

    @GetMapping("/nganh")
    public String nganhManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/nganh";
    }

    @GetMapping("/monhoc")
    public String monhocManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/monhoc";
    }

    @GetMapping("/giangvien")
    public String giangvienManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/giangvien";
    }

    @GetMapping("/sinhvien")
    public String sinhvienManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/sinhvien";
    }

    @GetMapping("/lop")
    public String lopManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/lop";
    }

    @GetMapping("/camera")
    public String cameraManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/camera";
    }

    @GetMapping("/diemdanh")
    public String diemDanhReports(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "baocao-diemdanh";
    }

    @GetMapping("/system")
    public String systemSettings(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/system";
    }
    @GetMapping("/taikhoan")
    public String taikhoanManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        log.info("Admin taikhoan management access: {}", userDetails.getUsername());
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/taikhoan";
    }

    @GetMapping("/namhoc")
    public String namhocManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/namhoc";
    }

    @GetMapping("/hocky")
    public String hockyManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/hocky";
    }

    @GetMapping("/lophocphan")
    public String lophocphanManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/lophocphan";
    }

    @GetMapping("/lichhoc")
    public String lichhocManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/lichhoc";
    }


    @GetMapping("/baocao-diemdanh")
    public String baocaoDiemdanhManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/baocao-diemdanh";
    }

    @GetMapping("/phonghoc")
    public String phonghocManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/phonghoc";
    }

    @GetMapping("/cauhinh-hocky")
    public String cauhinhHockyManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/cauhinh-hocky";

    }

    @GetMapping("/logs")
    public String logManagement(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "admin/logs";
    }
}