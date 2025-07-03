import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.DiemDanhService;
import com.tathanhloc.faceattendance.Service.LichHocService;
import com.tathanhloc.faceattendance.Service.LopHocPhanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// LecturerDashboardController.java
@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
@Slf4j
class LecturerDashboardController {

    private final LopHocPhanService lopHocPhanService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("=== LECTURER DASHBOARD ACCESS ===");
        log.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");

        if (userDetails == null) {
            log.warn("No user details found, redirecting to login");
            return "redirect:/?error=not_authenticated";
        }

        try {
            log.info("Loading lecturer dashboard for user: {}", userDetails.getUsername());

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("allSchedules", lichHocService.getAll());
            model.addAttribute("allClasses", lopHocPhanService.getAll());

            log.info("Lecturer dashboard loaded successfully");
            return "lecturer/dashboard";
        } catch (Exception e) {
            log.error("Error loading lecturer dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "lecturer/dashboard";
        }
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        model.addAttribute("schedules", lichHocService.getAll());
        return "lecturer/schedule";
    }

    @GetMapping("/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        model.addAttribute("classes", lopHocPhanService.getAll());
        return "lecturer/attendance";
    }

    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        return "lecturer/reports";
    }
}
