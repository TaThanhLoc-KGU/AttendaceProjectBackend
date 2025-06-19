package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {
    Optional<TaiKhoan> findByUsername(String username);
    boolean existsByUsername(String username);
}
