// ===== TeacherAttendanceWebController.java =====
package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.TeacherAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Web controller cho teacher attendance pages
 */
@Controller
@RequestMapping("/lecturer/attendance")
@RequiredArgsConstructor
@Slf4j
public class TeacherAttendanceWebController {

    private final TeacherAttendanceService teacherAttendanceService;

    /**
     * Trang điểm danh chính
     */
    @GetMapping
    public String attendancePage(Authentication authentication, Model model,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/attendance/index";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            LocalDate selectedDate = date != null ? date : LocalDate.now();

            // Load initial data
            var schedules = teacherAttendanceService.getTeacherScheduleByDate(maGv, selectedDate);
            var dailyStats = teacherAttendanceService.getDailyAttendanceStats(maGv, selectedDate);

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("schedules", schedules);
            model.addAttribute("dailyStats", dailyStats);
            model.addAttribute("maGv", maGv);

            log.info("✅ Attendance page loaded for lecturer {}", maGv);
            return "lecturer/attendance/index";

        } catch (Exception e) {
            log.error("❌ Error loading attendance page: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải trang điểm danh: " + e.getMessage());
            return "lecturer/attendance/index";
        }
    }

    /**
     * Trang điểm danh cho một tiết học cụ thể
     */
    @GetMapping("/session")
    public String attendanceSession(Authentication authentication, Model model,
                                    @RequestParam String scheduleId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(defaultValue = "false") boolean isWeekBased) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/attendance/session";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Load session data
            var session = teacherAttendanceService.getAttendanceSession(scheduleId, date, isWeekBased);

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("session", session);
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("date", date);
            model.addAttribute("isWeekBased", isWeekBased);
            model.addAttribute("maGv", maGv);

            log.info("✅ Attendance session loaded for schedule {}", scheduleId);
            return "lecturer/attendance/session";

        } catch (Exception e) {
            log.error("❌ Error loading attendance session: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải phiên điểm danh: " + e.getMessage());
            return "lecturer/attendance/session";
        }
    }

    /**
     * Trang thống kê điểm danh
     */
    @GetMapping("/statistics")
    public String attendanceStatistics(Authentication authentication, Model model,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/attendance/statistics";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Set default date range if not provided
            if (fromDate == null) {
                fromDate = LocalDate.now().minusWeeks(4);
            }
            if (toDate == null) {
                toDate = LocalDate.now();
            }

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("maGv", maGv);

            log.info("✅ Attendance statistics page loaded for lecturer {}", maGv);
            return "lecturer/attendance/statistics";

        } catch (Exception e) {
            log.error("❌ Error loading statistics page: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải trang thống kê: " + e.getMessage());
            return "lecturer/attendance/statistics";
        }
    }

    /**
     * Trang báo cáo điểm danh
     */
    @GetMapping("/reports")
    public String attendanceReports(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/attendance/reports";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("maGv", maGv);

            log.info("✅ Attendance reports page loaded for lecturer {}", maGv);
            return "lecturer/attendance/reports";

        } catch (Exception e) {
            log.error("❌ Error loading reports page: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải trang báo cáo: " + e.getMessage());
            return "lecturer/attendance/reports";
        }
    }
}