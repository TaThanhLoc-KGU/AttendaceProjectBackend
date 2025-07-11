package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Model.SystemLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogDTO {

    private Long id;
    private SystemLog.LogLevel logLevel;
    private String module;
    private String action;
    private String message;
    private String userId;
    private String userName;
    private String ipAddress;
    private String userAgent;
    private String requestUrl;
    private String requestMethod;
    private String sessionId;
    private String entityType;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String errorDetails;
    private Long durationMs;
    private String status;
    private LocalDateTime createdAt;

    // Display fields
    private String logLevelDisplay;
    private String statusDisplay;
    private String durationDisplay;
    private String timeAgo;
    private String shortMessage;

    // Helper methods
    public String getLogLevelColor() {
        if (logLevel == null) return "secondary";

        switch (logLevel) {
            case ERROR:
            case FATAL:
                return "danger";
            case WARN:
                return "warning";
            case INFO:
                return "info";
            case DEBUG:
                return "secondary";
            case TRACE:
                return "light";
            default:
                return "primary";
        }
    }

    public String getStatusColor() {
        if (status == null) return "secondary";

        switch (status.toUpperCase()) {
            case "SUCCESS":
                return "success";
            case "FAILED":
                return "danger";
            case "WARNING":
                return "warning";
            default:
                return "secondary";
        }
    }

    public String getModuleIcon() {
        if (module == null) return "fas fa-cog";

        switch (module.toLowerCase()) {
            case "authentication":
            case "auth":
                return "fas fa-sign-in-alt";
            case "user":
            case "taikhoan":
                return "fas fa-user";
            case "student":
            case "sinhvien":
                return "fas fa-user-graduate";
            case "teacher":
            case "giangvien":
                return "fas fa-chalkboard-teacher";
            case "attendance":
            case "diemdanh":
                return "fas fa-check-circle";
            case "camera":
                return "fas fa-video";
            case "system":
                return "fas fa-server";
            case "database":
                return "fas fa-database";
            case "api":
                return "fas fa-plug";
            default:
                return "fas fa-cog";
        }
    }
}