package com.tathanhloc.faceattendance.Util;

import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoLogUtil {

    private final SystemLogService systemLogService;

    public void log(TaiKhoan taiKhoan, String action, String ipAddress) {
        String userId = taiKhoan != null ? taiKhoan.getId().toString() : null;
        String userName = taiKhoan != null ? taiKhoan.getUsername() : "Unknown User";

        // Determine module based on action
        String module = determineModule(action);

        systemLogService.logUserAction(
                module,
                action,
                String.format("User action: %s", action),
                userId,
                userName
        );
    }

    public void log(TaiKhoan taiKhoan, String module, String action, String message, String ipAddress) {
        String userId = taiKhoan != null ? taiKhoan.getId().toString() : null;
        String userName = taiKhoan != null ? taiKhoan.getUsername() : "Unknown User";

        systemLogService.logUserAction(module, action, message, userId, userName);
    }

    public void logError(TaiKhoan taiKhoan, String action, String errorMessage, String ipAddress) {
        String userId = taiKhoan != null ? taiKhoan.getId().toString() : null;
        String userName = taiKhoan != null ? taiKhoan.getUsername(): "Unknown User";
        String module = determineModule(action);

        systemLogService.logError(module, action, errorMessage, null);
    }

    public void logSystemEvent(String action, String message) {
        systemLogService.logSystemEvent(action, message,
                com.tathanhloc.faceattendance.Model.SystemLog.LogLevel.INFO);
    }

    private String determineModule(String action) {
        if (action.toLowerCase().contains("login") || action.toLowerCase().contains("logout") ||
                action.toLowerCase().contains("auth")) {
            return "AUTHENTICATION";
        } else if (action.toLowerCase().contains("student") || action.toLowerCase().contains("sinhvien")) {
            return "STUDENT";
        } else if (action.toLowerCase().contains("teacher") || action.toLowerCase().contains("giangvien")) {
            return "TEACHER";
        } else if (action.toLowerCase().contains("attendance") || action.toLowerCase().contains("diemdanh")) {
            return "ATTENDANCE";
        } else if (action.toLowerCase().contains("camera")) {
            return "CAMERA";
        } else if (action.toLowerCase().contains("user") || action.toLowerCase().contains("taikhoan")) {
            return "USER";
        } else {
            return "SYSTEM";
        }
    }
}