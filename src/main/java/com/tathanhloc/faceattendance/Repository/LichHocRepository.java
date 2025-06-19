package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.DiemDanh;
import com.tathanhloc.faceattendance.Model.LichHoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LichHocRepository extends JpaRepository<LichHoc, String> {
    List<LichHoc> findByLopHocPhanMaLhp(String maLhp);
    List<LichHoc> findByPhongHocMaPhong(String maPhong);
    List<LichHoc> findByThu(Integer thu);
    List<LichHoc> findByPhongHocMaPhongAndThuAndIsActiveTrue(String maPhong, Integer thu);

}
