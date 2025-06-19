package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.LopHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LopHocPhanRepository extends JpaRepository<LopHocPhan, String> {
    List<LopHocPhan> findByMonHocMaMh(String maMh);
    List<LopHocPhan> findByGiangVienMaGv(String maGv);
}
