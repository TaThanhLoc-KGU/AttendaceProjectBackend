package com.tathanhloc.faceattendance.Security;

import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Repository.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username: {}", username);

        TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("Không tìm thấy tài khoản với username: " + username);
                });

        log.info("User found: {} with role: {} and active status: {}",
                taiKhoan.getUsername(), taiKhoan.getVaiTro(), taiKhoan.getIsActive());

        if (!taiKhoan.getIsActive()) {
            log.warn("User account is not active: {}", username);
            throw new DisabledException("Tài khoản đã bị vô hiệu hóa");
        }

        return new CustomUserDetails(taiKhoan);
    }
}