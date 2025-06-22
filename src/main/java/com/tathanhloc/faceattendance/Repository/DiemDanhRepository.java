package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.DiemDanh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DiemDanhRepository extends JpaRepository<DiemDanh, Long> {
    List<DiemDanh> findBySinhVienMaSv(String maSv);
    List<DiemDanh> findByLichHocMaLich(String maLich);
    long countByNgayDiemDanh(LocalDate ngayDiemDanh);
}
