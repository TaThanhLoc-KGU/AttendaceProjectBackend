package com.tathanhloc.faceattendance.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "success", required = false) String success,
                        Model model,
                        @Autowired CsrfToken csrfToken) {
        model.addAttribute("pageTitle", "Đăng nhập");
        model.addAttribute("_csrf", csrfToken); // Explicitly add CSRF token to model

        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        }

        if (success != null) {
            switch (success) {
                case "password-reset":
                    model.addAttribute("successMessage", "Mật khẩu mới đã được gửi đến email của bạn.");
                    break;
                case "password-changed":
                    model.addAttribute("successMessage", "Mật khẩu đã được thay đổi thành công. Vui lòng đăng nhập lại.");
                    break;
                default:
                    model.addAttribute("successMessage", "Thao tác thành công.");
            }
        }

        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("pageTitle", "Quên mật khẩu");
        return "auth/forgot-password";
    }

    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("pageTitle", "Đổi mật khẩu");

        // Set breadcrumb
        model.addAttribute("breadcrumbs", java.util.Arrays.asList(
                new BreadcrumbItem("Trang chủ", "/dashboard"),
                new BreadcrumbItem("Đổi mật khẩu", null)
        ));

        return "auth/change-password";
    }

    // Inner class for breadcrumb items
    public static class BreadcrumbItem {
        private String name;
        private String url;

        public BreadcrumbItem(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}