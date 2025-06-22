package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.DangKyHoc;
import com.tathanhloc.faceattendance.Model.DangKyHocId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DangKyHocRepository extends JpaRepository<DangKyHoc, DangKyHocId> {
    List<DangKyHoc> findBySinhVien_MaSv(String maSv);
    List<DangKyHoc> findByLopHocPhan_MaLhp(String maLhp);
}
