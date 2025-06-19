package com.tathanhloc.faceattendance.Util;

import com.tathanhloc.faceattendance.Model.SystemLog;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AutoLogUtil {

    private final SystemLogRepository logRepo;

    public void log(TaiKhoan taiKhoan, String action, String ipAddress) {
        SystemLog log = SystemLog.builder()
                .taiKhoan(taiKhoan)
                .action(action)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        logRepo.save(log);
    }
}
