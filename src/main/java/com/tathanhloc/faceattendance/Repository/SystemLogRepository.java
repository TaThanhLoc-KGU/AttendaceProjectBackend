package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findByTaiKhoanId(Long userId);
}
