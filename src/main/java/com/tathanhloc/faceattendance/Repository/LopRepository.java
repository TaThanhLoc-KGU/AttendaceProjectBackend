package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.Lop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LopRepository extends JpaRepository<Lop, String> {
    List<Lop> findByNganhMaNganh(String maNganh);
    List<Lop> findByKhoaHocMaKhoahoc(String maKhoahoc);

    Collection<Object> findByMaLop(String maLop);

}
