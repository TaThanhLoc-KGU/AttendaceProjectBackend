package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.SinhVien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SinhVienRepository extends JpaRepository<SinhVien, String> {
    List<SinhVien> findByLopMaLop(String maLop);
    Optional<SinhVien> findByEmail(String email);
    Optional<SinhVien> findByMaSv(String maSv);

}
