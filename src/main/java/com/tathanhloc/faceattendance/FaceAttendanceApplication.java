package com.tathanhloc.faceattendance;

import com.tathanhloc.faceattendance.Enum.VaiTroEnum;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Repository.TaiKhoanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableAsync
@EnableWebSocketMessageBroker
public class FaceAttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceAttendanceApplication.class, args);
    }


    @Bean
    CommandLineRunner initAdmin(TaiKhoanRepository taiKhoanRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (taiKhoanRepository.findByUsername("admin").isEmpty()) {
                TaiKhoan admin = TaiKhoan.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin@123"))
                        .vaiTro(VaiTroEnum.ADMIN)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                taiKhoanRepository.save(admin);
                System.out.println("✅ Admin account created successfully!");
            }
        };
    }


}
