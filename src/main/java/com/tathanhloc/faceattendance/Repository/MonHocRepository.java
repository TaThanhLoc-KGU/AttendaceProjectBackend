package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.MonHoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonHocRepository extends JpaRepository<MonHoc, String> {
    List<MonHoc> findByIsActiveTrue();
}
