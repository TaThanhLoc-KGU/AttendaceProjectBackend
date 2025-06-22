package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.HocKyNamHoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HocKyNamHocRepository extends JpaRepository<HocKyNamHoc, Integer> {
    List<HocKyNamHoc> findByHocKy_MaHocKy(String maHocKy);
    List<HocKyNamHoc> findByNamHoc_MaNamHoc(String maNamHoc);
}
