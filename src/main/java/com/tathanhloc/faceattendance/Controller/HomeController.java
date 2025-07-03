package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@Slf4j
public class HomeController {

    @GetMapping(value = {"/", "/index", "/index.html"})
    public String index(Authentication authentication,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String message,
                        Model model) {

        log.info("Accessing index page");

        // Add error/message to model if present
        if (error != null) {
            model.addAttribute("error", getErrorMessage(error));
        }
        if (message != null) {
            model.addAttribute("message", getMessage(message));
        }

        // Check if user is authenticated
        if (isAuthenticated(authentication)) {
            String role = extractUserRole(authentication);
            String redirectUrl = getRedirectUrlByRole(role);

            log.info("Authenticated user with role: {} redirecting to: {}", role, redirectUrl);
            return "redirect:" + redirectUrl;
        }

        log.info("Unauthenticated user, showing login page");
        return "index"; // Return login page
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/?error=not_authenticated";
        }

        String role = extractUserRole(authentication);
        String redirectUrl = getRedirectUrlByRole(role);

        log.info("Dashboard access for role: {}, redirecting to: {}", role, redirectUrl);
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/admin/**")
    public String adminPages(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/?error=not_authenticated";
        }

        if (!hasRole(authentication, "ADMIN")) {
            return "redirect:/?error=access_denied";
        }

        // Let the request continue to specific admin controllers
        return null;
    }

    @GetMapping("/lecturer/**")
    public String lecturerPages(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/?error=not_authenticated";
        }

        if (!hasRole(authentication, "GIANGVIEN")) {
            return "redirect:/?error=access_denied";
        }

        return null;
    }

    @GetMapping("/student/**")
    public String studentPages(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/?error=not_authenticated";
        }

        if (!hasRole(authentication, "SINHVIEN")) {
            return "redirect:/?error=access_denied";
        }

        return null;
    }

    // Helper methods
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null &&
                authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser");
    }

    private String extractUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.replace("ROLE_", ""))
                .findFirst()
                .orElse("");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private String getRedirectUrlByRole(String role) {
        switch (role) {
            case "ADMIN":
                return "/admin/dashboard";
            case "GIANGVIEN":
                return "/lecturer/dashboard";
            case "SINHVIEN":
                return "/student/dashboard";
            default:
                log.warn("Unknown role: {}", role);
                return "/?error=invalid_role";
        }
    }

    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "invalid_role":
                return "Vai trò người dùng không hợp lệ";
            case "not_authenticated":
                return "Vui lòng đăng nhập để tiếp tục";
            case "access_denied":
                return "Bạn không có quyền truy cập trang này";
            case "session_expired":
                return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại";
            case "login_failed":
                return "Đăng nhập thất bại, vui lòng kiểm tra lại thông tin";
            default:
                return "Đã xảy ra lỗi, vui lòng thử lại";
        }
    }

    private String getMessage(String messageCode) {
        switch (messageCode) {
            case "logout_success":
                return "Đăng xuất thành công";
            case "password_reset":
                return "Mật khẩu đã được đặt lại thành công";
            case "account_created":
                return "Tài khoản đã được tạo thành công";
            default:
                return messageCode;
        }
    }
}