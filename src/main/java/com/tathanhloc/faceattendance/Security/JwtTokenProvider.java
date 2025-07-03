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

    @Value("${app.jwt.expiration:86400000}") // 24 hours default
    private int jwtExpirationInMs;

    public JwtTokenProvider(SecretKey jwtSigningKey) {
        this.jwtSigningKey = jwtSigningKey;
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userDetails.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        log.debug("Generating JWT token for user: {} with expiry: {}", username, expiryDate);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSigningKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(authToken);

            log.debug("JWT token is valid");
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token format");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        } catch (SignatureException ex) {
            log.error("JWT signature validation failed");
        } catch (Exception ex) {
            log.error("JWT token validation failed: {}", ex.getMessage());
        }
        return false;
    }
}