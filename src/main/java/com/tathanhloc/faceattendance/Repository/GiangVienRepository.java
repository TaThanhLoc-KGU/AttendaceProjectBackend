package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.GiangVien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiangVienRepository extends JpaRepository<GiangVien, String> {
    Optional<GiangVien> findByEmail(String email);
    List<GiangVien> findByKhoaMaKhoa(String maKhoa);
    List<GiangVien> findByMaGv(String maGv);
}
