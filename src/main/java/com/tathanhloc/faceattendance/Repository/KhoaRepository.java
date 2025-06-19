package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.Khoa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KhoaRepository  extends JpaRepository<Khoa, String> {
    // Custom query methods can be defined here if needed
}
