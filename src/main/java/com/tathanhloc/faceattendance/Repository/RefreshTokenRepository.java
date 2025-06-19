package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.RefreshToken;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByTaiKhoan(TaiKhoan taiKhoan);
}
