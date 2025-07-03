import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.DangKyHocService;
import com.tathanhloc.faceattendance.Service.DiemDanhService;
import com.tathanhloc.faceattendance.Service.LichHocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// StudentDashboardController.java
@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
class StudentDashboardController {

    private final DangKyHocService dangKyHocService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("=== STUDENT DASHBOARD ACCESS ===");
        log.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");

        if (userDetails == null) {
            log.warn("No user details found, redirecting to login");
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Check if user has student profile
            if (userDetails.getTaiKhoan().getSinhVien() == null) {
                log.error("User has no student profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
                return "student/dashboard";
            }

            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            log.info("Loading student dashboard for student: {}", maSv);

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myRegistrations", dangKyHocService.getByMaSv(maSv));
            model.addAttribute("allSchedules", lichHocService.getAll());
            model.addAttribute("myAttendance", diemDanhService.getByMaSv(maSv));

            log.info("Student dashboard loaded successfully");
            return "student/dashboard";
        } catch (Exception e) {
            log.error("Error loading student dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "student/dashboard";
        }
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        model.addAttribute("schedules", lichHocService.getAll());
        return "student/schedule";
    }

    @GetMapping("/attendance")
    public String attendanceHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
            return "student/attendance";
        }

        String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
        model.addAttribute("attendanceHistory", diemDanhService.getByMaSv(maSv));
        return "student/attendance";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
            return "student/profile";
        }

        model.addAttribute("student", userDetails.getTaiKhoan().getSinhVien());
        return "student/profile";
    }
}