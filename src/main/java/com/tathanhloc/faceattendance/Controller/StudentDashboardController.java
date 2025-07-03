// StudentDashboardController.java
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
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardController {

    private final DangKyHocService dangKyHocService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("Student dashboard accessed by: {}", userDetails.getUsername());

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();

            // Load student-specific data using existing methods
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myRegistrations", dangKyHocService.getByMaSv(maSv));
            model.addAttribute("allSchedules", lichHocService.getAll());
            model.addAttribute("myAttendance", diemDanhService.getByMaSv(maSv));

            return "student/dashboard";
        } catch (Exception e) {
            log.error("Error loading student dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard");
            return "student/dashboard";
        }
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("schedules", lichHocService.getAll());
        return "student/schedule";
    }

    @GetMapping("/attendance")
    public String attendanceHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
        model.addAttribute("attendanceHistory", diemDanhService.getByMaSv(maSv));
        return "student/attendance";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("student", userDetails.getTaiKhoan().getSinhVien());
        return "student/profile";
    }
}