package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.NganhMonHoc;
import com.tathanhloc.faceattendance.Model.NganhMonHocId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NganhMonHocRepository extends JpaRepository<NganhMonHoc, NganhMonHocId> {
    List<NganhMonHoc> findByNganhMaNganh(String maNganh);
    List<NganhMonHoc> findByMonHocMaMh(String maMh);


}
