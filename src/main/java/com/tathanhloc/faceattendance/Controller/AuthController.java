package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.UserProfileDTO;
import com.tathanhloc.faceattendance.Model.LoginRequest;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.TaiKhoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TaiKhoanService taiKhoanService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("=== LOGIN ATTEMPT ===");
        log.info("Username: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for user: {}", request.getUsername());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            TaiKhoan user = userDetails.getTaiKhoan();

            UserProfileDTO profile = buildUserProfileDTO(user);
            log.info("Login successful for user: {} with role: {}", user.getUsername(), user.getVaiTro());
            return ResponseEntity.ok(profile);

        } catch (BadCredentialsException e) {
            log.error("Bad credentials for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Tên đăng nhập hoặc mật khẩu không chính xác"));

        } catch (DisabledException e) {
            log.error("Account disabled for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Tài khoản đã bị vô hiệu hóa"));

        } catch (LockedException e) {
            log.error("Account locked for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Tài khoản đã bị khóa"));

        } catch (Exception e) {
            log.error("Login failed for user: {} with error: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đã xảy ra lỗi trong quá trình xác thực"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        try {
            SecurityContextHolder.clearContext();
            log.info("User logged out");
            return ResponseEntity.ok(createSuccessResponse("Đăng xuất thành công!"));
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đăng xuất thất bại"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("No authenticated user found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Không có người dùng đăng nhập"));
        }

        try {
            TaiKhoan tk = userDetails.getTaiKhoan();
            log.info("Current user info requested: {}", tk.getUsername());
            UserProfileDTO profile = buildUserProfileDTO(tk);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error getting current user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi lấy thông tin người dùng"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String username) {
        try {
            log.info("Password reset request for username: {}", username);
            taiKhoanService.resetPassword(username);
            return ResponseEntity.ok("Mật khẩu mới đã được tạo và gửi đến email (nếu có)");
        } catch (Exception e) {
            log.error("Password reset failed for username: {}", username, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể đặt lại mật khẩu: " + e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Không có người dùng đăng nhập"));
        }

        try {
            log.info("Password change request for user: {}", userDetails.getUsername());

            if (!passwordEncoder.matches(oldPassword, userDetails.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Mật khẩu cũ không đúng"));
            }

            TaiKhoan updated = taiKhoanService.changePassword(userDetails.getUsername(), newPassword);
            UserProfileDTO profile = buildUserProfileDTO(updated);

            log.info("Password changed successfully for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Password change failed for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi đổi mật khẩu"));
        }
    }

    // Helper methods
    private UserProfileDTO buildUserProfileDTO(TaiKhoan tk) {
        String hoTen = null;
        String maSo = null;
        String email = null;

        if (tk.getSinhVien() != null) {
            hoTen = tk.getSinhVien().getHoTen();
            maSo = tk.getSinhVien().getMaSv();
            email = tk.getSinhVien().getEmail();
        } else if (tk.getGiangVien() != null) {
            hoTen = tk.getGiangVien().getHoTen();
            maSo = tk.getGiangVien().getMaGv();
            email = tk.getGiangVien().getEmail();
        }

        return UserProfileDTO.builder()
                .id(tk.getId())
                .username(tk.getUsername())
                .vaiTro(tk.getVaiTro().name())
                .isActive(tk.getIsActive())
                .hoTen(hoTen)
                .maSo(maSo)
                .email(email)
                .build();
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message);
    }

    private Object createSuccessResponse(String message) {
        return new SuccessResponse(message);
    }

    // Response classes
    public static class ErrorResponse {
        public String message;
        public boolean success = false;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    public static class SuccessResponse {
        public String message;
        public boolean success = true;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}