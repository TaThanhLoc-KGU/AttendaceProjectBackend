package com.tathanhloc.faceattendance.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class HomeController {

    @GetMapping(value = {"/", "/index", "/index.html", "/login"})
    public String index(Authentication authentication,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String message,
                        Model model) {

        log.info("=== HOME PAGE ACCESS ===");
        log.info("Authentication present: {}", authentication != null);
        log.info("Is authenticated: {}", isAuthenticated(authentication));

        // Nếu đã đăng nhập, chuyển hướng tới dashboard phù hợp
        if (isAuthenticated(authentication)) {
            String role = extractUserRole(authentication);
            String redirectUrl = getRedirectUrlByRole(role);
            log.info("User already authenticated with role: {}, redirecting to: {}", role, redirectUrl);
            return "redirect:" + redirectUrl;
        }

        // Thêm thông báo lỗi/thành công nếu có
        if (error != null) {
            model.addAttribute("error", getErrorMessage(error));
            log.info("Error message: {}", error);
        }
        if (message != null) {
            model.addAttribute("message", getMessage(message));
            log.info("Success message: {}", message);
        }

        log.info("Showing login page");
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            log.warn("Unauthenticated access to /dashboard");
            return "redirect:/?error=not_authenticated";
        }

        String role = extractUserRole(authentication);
        String redirectUrl = getRedirectUrlByRole(role);
        log.info("Dashboard redirect for role: {} -> {}", role, redirectUrl);
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/logout")
    public String logout() {
        log.info("Logout accessed");
        return "redirect:/?message=logout_success";
    }

    // Helper methods
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }

    private String extractUserRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "";
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.replace("ROLE_", ""))
                .findFirst()
                .orElse("");
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
                return "Bạn không có quyền truy cập";
            case "session_expired":
                return "Phiên đăng nhập đã hết hạn";
            case "login_failed":
                return "Đăng nhập thất bại";
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