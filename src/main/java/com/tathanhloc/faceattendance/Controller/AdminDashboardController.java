package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final SinhVienService sinhVienService;
    private final GiangVienService giangVienService;
    private final LopService lopService;
    private final DiemDanhService diemDanhService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            HttpServletRequest request,
                            Model model) {

        log.info("=== ADMIN DASHBOARD ACCESS ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request Method: {}", request.getMethod());
        log.info("Authorization Header: {}", request.getHeader("Authorization"));

        // Debug authentication context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Security Context Authentication: {}", auth);
        log.info("Is Authenticated: {}", auth != null && auth.isAuthenticated());
        log.info("Principal Type: {}", auth != null ? auth.getPrincipal().getClass().getSimpleName() : "null");
        log.info("Authorities: {}", auth != null ? auth.getAuthorities() : "null");

        log.info("UserDetails parameter: {}", userDetails);

        if (userDetails == null) {
            log.error("❌ UserDetails is null - authentication failed");

            // Debug thêm về security context
            if (auth != null) {
                log.error("Auth exists but userDetails is null - Principal: {}", auth.getPrincipal());
                log.error("Auth class: {}", auth.getClass().getSimpleName());
            }

            return "redirect:/?error=not_authenticated";
        }

        try {
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
    public String khoaManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("Admin khoa management access: {}", userDetails != null ? userDetails.getUsername() : "null");
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/khoa";
    }

    @GetMapping("/nganh")
    public String nganhManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/nganh";
    }

    @GetMapping("/monhoc")
    public String monhocManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/monhoc";
    }

    @GetMapping("/giangvien")
    public String giangvienManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/giangvien";
    }

    @GetMapping("/sinhvien")
    public String sinhvienManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/sinhvien";
    }

    @GetMapping("/lop")
    public String lopManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/lop";
    }

    @GetMapping("/camera")
    public String cameraManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/camera";
    }

    @GetMapping("/diemdanh")
    public String diemDanhReports(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/diemdanh";
    }

    @GetMapping("/system")
    public String systemSettings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "admin/system";
    }
}