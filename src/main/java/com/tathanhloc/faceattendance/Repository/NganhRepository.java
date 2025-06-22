package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.Nganh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NganhRepository extends JpaRepository<Nganh, String> {
    List<Nganh> findByKhoaMaKhoa(String maKhoa);
}
