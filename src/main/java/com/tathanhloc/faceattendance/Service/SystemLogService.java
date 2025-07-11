package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.SystemLogDTO;
import com.tathanhloc.faceattendance.Model.SystemLog;
import com.tathanhloc.faceattendance.Repository.SystemLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {

    private final SystemLogRepository logRepository;

    // =================== CRUD Operations ===================

    public Page<SystemLogDTO> getAllLogs(Pageable pageable) {
        Page<SystemLog> logs = logRepository.findAll(pageable);
        return logs.map(this::toDTO);
    }

    public SystemLogDTO getLogById(Long id) {
        return logRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    public Page<SystemLogDTO> searchLogs(String module, SystemLog.LogLevel logLevel, String status,
                                         String userId, LocalDateTime startTime, LocalDateTime endTime,
                                         String keyword, Pageable pageable) {
        Page<SystemLog> logs = logRepository.findWithFilters(
                module, logLevel, status, userId, startTime, endTime, keyword, pageable);
        return logs.map(this::toDTO);
    }

    // =================== Logging Methods ===================

    @Async
    public void logInfo(String module, String action, String message) {
        saveLog(SystemLog.LogLevel.INFO, module, action, message, null, null, "SUCCESS");
    }

    @Async
    public void logInfo(String module, String action, String message, String userId, String userName) {
        saveLog(SystemLog.LogLevel.INFO, module, action, message, userId, userName, "SUCCESS");
    }

    @Async
    public void logWarning(String module, String action, String message) {
        saveLog(SystemLog.LogLevel.WARN, module, action, message, null, null, "WARNING");
    }

    @Async
    public void logError(String module, String action, String message, String errorDetails) {
        SystemLog log = buildBaseLog(SystemLog.LogLevel.ERROR, module, action, message, null, null);
        log.setErrorDetails(errorDetails);
        log.setStatus("FAILED");
        logRepository.save(log);
    }

    @Async
    public void logUserAction(String module, String action, String message, String userId, String userName) {
        saveLog(SystemLog.LogLevel.INFO, module, action, message, userId, userName, "SUCCESS");
    }

    @Async
    public void logDataChange(String module, String action, String entityType, String entityId,
                              String oldValue, String newValue, String userId, String userName) {
        SystemLog log = buildBaseLog(SystemLog.LogLevel.INFO, module, action,
                String.format("Updated %s [%s]", entityType, entityId), userId, userName);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setStatus("SUCCESS");
        logRepository.save(log);
    }

    @Async
    public void logPerformance(String module, String action, String message, long durationMs) {
        SystemLog log = buildBaseLog(SystemLog.LogLevel.INFO, module, action, message, null, null);
        log.setDurationMs(durationMs);
        log.setStatus(durationMs > 5000 ? "WARNING" : "SUCCESS"); // Slow if > 5 seconds
        logRepository.save(log);
    }

    @Async
    public void logAuthentication(String action, String userId, String userName, boolean success, String details) {
        SystemLog log = buildBaseLog(
                success ? SystemLog.LogLevel.INFO : SystemLog.LogLevel.WARN,
                "AUTHENTICATION", action, details, userId, userName);
        log.setStatus(success ? "SUCCESS" : "FAILED");
        logRepository.save(log);
    }

    @Async
    public void logSystemEvent(String action, String message, SystemLog.LogLevel level) {
        saveLog(level, "SYSTEM", action, message, null, null, "SUCCESS");
    }

    // =================== Private Helper Methods ===================

    private void saveLog(SystemLog.LogLevel level, String module, String action, String message,
                         String userId, String userName, String status) {
        SystemLog log = buildBaseLog(level, module, action, message, userId, userName);
        log.setStatus(status);
        logRepository.save(log);
    }

    private SystemLog buildBaseLog(SystemLog.LogLevel level, String module, String action, String message,
                                   String userId, String userName) {
        SystemLog log = SystemLog.builder()
                .logLevel(level)
                .module(module)
                .action(action)
                .message(message)
                .userId(userId)
                .userName(userName)
                .build();

        // Get request info if available
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
                log.setRequestUrl(request.getRequestURL().toString());
                log.setRequestMethod(request.getMethod());
                log.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
            }
        } catch (Exception e) {
            // Ignore if request context is not available
        }

        return log;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    // =================== Statistics Methods ===================

    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minus(24, ChronoUnit.HOURS);
        LocalDateTime lastWeek = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime lastMonth = now.minus(30, ChronoUnit.DAYS);

        // Total counts
        stats.put("totalLogs", logRepository.count());
        stats.put("logsLast24h", logRepository.countSince(last24h));
        stats.put("logsLastWeek", logRepository.countSince(lastWeek));
        stats.put("logsLastMonth", logRepository.countSince(lastMonth));

        // Error counts
        stats.put("errorsLast24h", logRepository.countErrorsSince(last24h));
        stats.put("errorsLastWeek", logRepository.countErrorsSince(lastWeek));

        // Breakdown by level
        List<Object[]> levelStats = logRepository.countByLogLevel();
        Map<String, Long> levelCounts = levelStats.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> (Long) arr[1]
                ));
        stats.put("logLevelCounts", levelCounts);

        // Breakdown by module
        List<Object[]> moduleStats = logRepository.countByModule();
        Map<String, Long> moduleCounts = moduleStats.stream()
                .limit(10) // Top 10 modules
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
        stats.put("moduleStats", moduleCounts);

        // Breakdown by status
        List<Object[]> statusStats = logRepository.countByStatus();
        Map<String, Long> statusCounts = statusStats.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
        stats.put("statusStats", statusCounts);

        return stats;
    }

    public List<SystemLogDTO> getRecentErrors(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return logRepository.findRecentErrors(pageable)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<SystemLogDTO> getSlowOperations(long thresholdMs, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return logRepository.findSlowOperations(thresholdMs, pageable)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // =================== Maintenance Methods ===================

    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(daysToKeep, ChronoUnit.DAYS);
        logRepository.deleteOldLogs(cutoffDate);
        logInfo("SYSTEM", "LOG_CLEANUP", String.format("Cleaned up logs older than %d days", daysToKeep));
    }

    // =================== DTO Conversion ===================

    private SystemLogDTO toDTO(SystemLog log) {
        SystemLogDTO dto = SystemLogDTO.builder()
                .id(log.getId())
                .logLevel(log.getLogLevel())
                .module(log.getModule())
                .action(log.getAction())
                .message(log.getMessage())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestUrl(log.getRequestUrl())
                .requestMethod(log.getRequestMethod())
                .sessionId(log.getSessionId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .errorDetails(log.getErrorDetails())
                .durationMs(log.getDurationMs())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();

        // Add display fields
        dto.setLogLevelDisplay(log.getLogLevel().name());
        dto.setStatusDisplay(log.getStatus() != null ? log.getStatus() : "UNKNOWN");

        if (log.getDurationMs() != null) {
            dto.setDurationDisplay(formatDuration(log.getDurationMs()));
        }

        dto.setTimeAgo(getTimeAgo(log.getCreatedAt()));
        dto.setShortMessage(log.getMessage().length() > 100 ?
                log.getMessage().substring(0, 100) + "..." : log.getMessage());

        return dto;
    }

    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.2fs", durationMs / 1000.0);
        } else {
            return String.format("%.2fm", durationMs / 60000.0);
        }
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (minutes < 1440) {
            return (minutes / 60) + " giờ trước";
        } else {
            return (minutes / 1440) + " ngày trước";
        }
    }
}