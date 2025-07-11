package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "log_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LogLevel logLevel;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILED, WARNING

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Enum cho log level
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }

    // Helper methods
    public String getLogLevelColor() {
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
        switch (status != null ? status.toUpperCase() : "UNKNOWN") {
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
}