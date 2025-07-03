// LecturerDashboardController.java
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
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Slf4j
public class LecturerDashboardController {

    private final LopHocPhanService lopHocPhanService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("Lecturer dashboard accessed by: {}", userDetails.getUsername());

        try {
            // Add user info to model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());

            // Load basic data using existing methods
            model.addAttribute("allSchedules", lichHocService.getAll());
            model.addAttribute("allClasses", lopHocPhanService.getAll());

            return "lecturer/dashboard";
        } catch (Exception e) {
            log.error("Error loading lecturer dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard");
            return "lecturer/dashboard";
        }
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("schedules", lichHocService.getAll());
        return "lecturer/schedule";
    }

    @GetMapping("/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("classes", lopHocPhanService.getAll());
        return "lecturer/attendance";
    }

    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return "lecturer/reports";
    }
}