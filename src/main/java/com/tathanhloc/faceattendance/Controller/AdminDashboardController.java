// AdminDashboardController.java
package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("Admin dashboard accessed by: {}", userDetails.getUsername());

        try {
            // Load dashboard statistics using existing methods
            model.addAttribute("totalStudents", sinhVienService.getAll().size());
            model.addAttribute("totalLecturers", giangVienService.getAll().size());
            model.addAttribute("totalClasses", lopService.count());
            model.addAttribute("attendanceToday", diemDanhService.countTodayDiemDanh());

            // Add user info to model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());

            return "admin/dashboard";
        } catch (Exception e) {
            log.error("Error loading admin dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard");
            return "admin/dashboard";
        }
    }

    @GetMapping("/khoa")
    public String khoaManagement(Model model) {
        return "admin/khoa";
    }

    @GetMapping("/nganh")
    public String nganhManagement(Model model) {
        return "admin/nganh";
    }

    @GetMapping("/monhoc")
    public String monhocManagement(Model model) {
        return "admin/monhoc";
    }

    @GetMapping("/giangvien")
    public String giangvienManagement(Model model) {
        return "admin/giangvien";
    }

    @GetMapping("/sinhvien")
    public String sinhvienManagement(Model model) {
        return "admin/sinhvien";
    }

    @GetMapping("/lop")
    public String lopManagement(Model model) {
        return "admin/lop";
    }

    @GetMapping("/camera")
    public String cameraManagement(Model model) {
        return "admin/camera";
    }

    @GetMapping("/diemdanh")
    public String diemDanhReports(Model model) {
        return "admin/diemdanh";
    }

    @GetMapping("/system")
    public String systemSettings(Model model) {
        return "admin/system";
    }
}
