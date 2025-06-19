package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.Exception.TokenRefreshException;
import com.tathanhloc.faceattendance.Model.RefreshToken;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Repository.RefreshTokenRepository;
import com.tathanhloc.faceattendance.Repository.TaiKhoanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${app.jwt.refresh-expiration:604800000}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .taiKhoan(taiKhoanRepository.findById(userId).orElseThrow(
                        () -> new RuntimeException("Không tìm thấy tài khoản với ID: " + userId)))
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .token(UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token đã hết hạn. Vui lòng đăng nhập lại");
        }

        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByTaiKhoan(
                taiKhoanRepository.findById(userId).orElseThrow(
                        () -> new RuntimeException("Không tìm thấy tài khoản với ID: " + userId)));
    }
}
