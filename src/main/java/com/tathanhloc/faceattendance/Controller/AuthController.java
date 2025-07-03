package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.JwtAuthResponse;
import com.tathanhloc.faceattendance.DTO.TaiKhoanDTO;
import com.tathanhloc.faceattendance.DTO.TokenRefreshRequest;
import com.tathanhloc.faceattendance.DTO.TokenRefreshResponse;
import com.tathanhloc.faceattendance.DTO.UserProfileDTO;
import com.tathanhloc.faceattendance.Exception.TokenRefreshException;
import com.tathanhloc.faceattendance.Model.LoginRequest;
import com.tathanhloc.faceattendance.Model.RefreshToken;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Security.JwtTokenProvider;
import com.tathanhloc.faceattendance.Service.RefreshTokenService;
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
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("=== LOGIN ATTEMPT ===");
        log.info("Username: {}", request.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for user: {}", request.getUsername());

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            TaiKhoan user = userDetails.getTaiKhoan();

            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);
            log.info("JWT token generated for user: {}", user.getUsername());

            // Create refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
            log.info("Refresh token created for user: {}", user.getUsername());

            // Convert to DTO
            TaiKhoanDTO userDTO = taiKhoanService.convertToDTO(user);

            // Create response
            JwtAuthResponse response = new JwtAuthResponse(jwt, refreshToken.getToken(), userDTO);

            log.info("Login successful for user: {} with role: {}", user.getUsername(), user.getVaiTro());
            return ResponseEntity.ok(response);

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

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        log.info("Refresh token request received");

        try {
            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getTaiKhoan)
                    .map(user -> {
                        String token = tokenProvider.generateTokenFromUsername(user.getUsername());
                        log.info("New token generated for user: {}", user.getUsername());
                        return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken, "Bearer"));
                    })
                    .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                            "Refresh token không tồn tại trong hệ thống!"));
        } catch (Exception e) {
            log.error("Refresh token failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Refresh token không hợp lệ"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestParam Long userId) {
        try {
            refreshTokenService.deleteByUserId(userId);
            log.info("User logged out: {}", userId);
            return ResponseEntity.ok(createSuccessResponse("Đăng xuất thành công!"));
        } catch (Exception e) {
            log.error("Logout failed for user: {}", userId, e);
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

            UserProfileDTO profile = UserProfileDTO.builder()
                    .id(tk.getId())
                    .username(tk.getUsername())
                    .vaiTro(tk.getVaiTro().name())
                    .isActive(tk.getIsActive())
                    .hoTen(hoTen)
                    .maSo(maSo)
                    .email(email)
                    .build();

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

            String hoTen = null, maSo = null, email = null;
            if (updated.getSinhVien() != null) {
                hoTen = updated.getSinhVien().getHoTen();
                maSo = updated.getSinhVien().getMaSv();
                email = updated.getSinhVien().getEmail();
            } else if (updated.getGiangVien() != null) {
                hoTen = updated.getGiangVien().getHoTen();
                maSo = updated.getGiangVien().getMaGv();
                email = updated.getGiangVien().getEmail();
            }

            UserProfileDTO profile = UserProfileDTO.builder()
                    .id(updated.getId())
                    .username(updated.getUsername())
                    .vaiTro(updated.getVaiTro().name())
                    .isActive(updated.getIsActive())
                    .hoTen(hoTen)
                    .maSo(maSo)
                    .email(email)
                    .build();

            log.info("Password changed successfully for user: {}", userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Password change failed for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi đổi mật khẩu"));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token không hợp lệ"));
            }

            String token = authHeader.substring(7);
            boolean isValid = tokenProvider.validateToken(token);

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token không hợp lệ hoặc đã hết hạn"));
            }

            String username = tokenProvider.getUsernameFromToken(token);
            return ResponseEntity.ok(createSuccessResponse("Token hợp lệ cho người dùng: " + username));
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Lỗi xác thực token"));
        }
    }

    // Helper methods
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