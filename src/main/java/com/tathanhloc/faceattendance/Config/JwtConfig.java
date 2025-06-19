package com.tathanhloc.faceattendance.Config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecretKey jwtSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();

        // Kiểm tra độ dài khóa
        if (keyBytes.length < 32) {
            // Nếu khóa quá ngắn, sử dụng phương thức tạo khóa ngẫu nhiên an toàn
            return Jwts.SIG.HS256.key().build();
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
