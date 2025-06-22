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
        log.info("Đăng nhập với username: {}", request.getUsername());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            TaiKhoan user = userDetails.getTaiKhoan();
            String jwt = tokenProvider.generateToken(auth);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            TaiKhoanDTO userDTO = taiKhoanService.convertToDTO(user);

            return ResponseEntity.ok(new JwtAuthResponse(jwt, refreshToken.getToken(), userDTO));
        } catch (BadCredentialsException e) {
            log.error("Đăng nhập thất bại: Thông tin đăng nhập không chính xác", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Tên đăng nhập hoặc mật khẩu không chính xác");
        } catch (DisabledException e) {
            log.error("Đăng nhập thất bại: Tài khoản bị vô hiệu hóa", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Tài khoản đã bị vô hiệu hóa");
        } catch (LockedException e) {
            log.error("Đăng nhập thất bại: Tài khoản bị khóa", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Tài khoản đã bị khóa");
        } catch (Exception e) {
            log.error("Đăng nhập thất bại: Lỗi không xác định", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi trong quá trình xác thực");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getTaiKhoan)
                .map(user -> {
                    String token = tokenProvider.generateTokenFromUsername(user.getUsername());
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken, "Bearer"));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token không tồn tại trong hệ thống!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestParam Long userId) {
        refreshTokenService.deleteByUserId(userId);
        return ResponseEntity.ok("Đăng xuất thành công!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Không có người dùng đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Không có người dùng đăng nhập");
        }

        log.info("Lấy thông tin người dùng hiện tại: {}", userDetails.getUsername());

        TaiKhoan tk = userDetails.getTaiKhoan();

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

        return ResponseEntity.ok(UserProfileDTO.builder()
                .id(tk.getId())
                .username(tk.getUsername())
                .vaiTro(tk.getVaiTro().getValue())
                .isActive(tk.getIsActive())
                .hoTen(hoTen)
                .maSo(maSo)
                .email(email)
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String username) {
        log.info("Yêu cầu đặt lại mật khẩu cho username: {}", username);
        taiKhoanService.resetPassword(username);
        return ResponseEntity.ok("Mật khẩu mới đã được tạo và gửi đến email (nếu có)");
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        if (userDetails == null) {
            log.warn("Không có người dùng đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Không có người dùng đăng nhập");
        }

        log.info("Đổi mật khẩu cho người dùng: {}", userDetails.getUsername());

        if (!passwordEncoder.matches(oldPassword, userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Mật khẩu cũ không đúng");
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

        return ResponseEntity.ok(UserProfileDTO.builder()
                .id(updated.getId())
                .username(updated.getUsername())
                .vaiTro(updated.getVaiTro().getValue())
                .isActive(updated.getIsActive())
                .hoTen(hoTen)
                .maSo(maSo)
                .email(email)
                .build());
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ");
            }

            String token = authHeader.substring(7);
            boolean isValid = tokenProvider.validateToken(token);

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc đã hết hạn");
            }

            String username = tokenProvider.getUsernameFromToken(token);
            return ResponseEntity.ok("Token hợp lệ cho người dùng: " + username);
        } catch (Exception e) {
            log.error("Lỗi khi xác thực token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Lỗi xác thực token");
        }
    }
}
