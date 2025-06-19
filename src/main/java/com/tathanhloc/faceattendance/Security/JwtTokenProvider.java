package com.tathanhloc.faceattendance.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey jwtSigningKey;

    @Value("${app.jwt.expiration:86400000}")
    private int jwtExpirationInMs;

    public JwtTokenProvider(SecretKey jwtSigningKey) {
        this.jwtSigningKey = jwtSigningKey;
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .claim("sub", userDetails.getUsername())
                .claim("iat", now)
                .claim("exp", expiryDate)
                .signWith(jwtSigningKey)
                .compact();
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .claim("sub", username)
                .claim("iat", now)
                .claim("exp", expiryDate)
                .signWith(jwtSigningKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSigningKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Token JWT không hợp lệ");
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT đã hết hạn");
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT không được hỗ trợ");
        } catch (IllegalArgumentException ex) {
            log.error("Chuỗi claims JWT trống");
        } catch (SignatureException ex) {
            log.error("Xác thực chữ ký JWT thất bại");
        }
        return false;
    }
}
