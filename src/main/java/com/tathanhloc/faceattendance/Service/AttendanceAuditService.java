// ===== AttendanceAuditService.java =====
package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.TeacherAttendanceRequestDTO;
import com.tathanhloc.faceattendance.DTO.AttendanceResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service để audit/log các thao tác điểm danh
 */
@Service
@Slf4j
public class AttendanceAuditService {

    private static final String AUDIT_LOGGER = "ATTENDANCE_AUDIT";

    /**
     * Log thao tác điểm danh
     */
    public void logAttendanceAction(TeacherAttendanceRequestDTO request, AttendanceResultDTO result, String action) {
        try {
            String logMessage = String.format(
                    "[%s] %s | Student: %s | Schedule: %s | Date: %s | Status: %s | Result: %s | User: %s",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    action,
                    request.getMaSv(),
                    request.getScheduleId(),
                    request.getDate(),
                    request.getTrangThai(),
                    result.isSuccess() ? "SUCCESS" : "FAILED",
                    request.getCreatedBy()
            );

            // Log to specific audit logger
            org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER).info(logMessage);

            // Also log details for failed operations
            if (!result.isSuccess()) {
                log.warn("❌ Attendance operation failed: {}", result.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Error logging attendance action: {}", e.getMessage());
        }
    }

    /**
     * Log batch attendance operation
     */
    public void logBatchAttendanceAction(int totalRequests, int successCount, int failCount, String user) {
        try {
            String logMessage = String.format(
                    "[%s] BATCH_ATTENDANCE | Total: %d | Success: %d | Failed: %d | User: %s",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    totalRequests,
                    successCount,
                    failCount,
                    user
            );

            org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER).info(logMessage);

        } catch (Exception e) {
            log.error("❌ Error logging batch attendance: {}", e.getMessage());
        }
    }

    /**
     * Log security events
     */
    public void logSecurityEvent(String event, String user, String details) {
        try {
            String logMessage = String.format(
                    "[%s] SECURITY_EVENT | Event: %s | User: %s | Details: %s",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    event,
                    user,
                    details
            );

            org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER).warn(logMessage);

        } catch (Exception e) {
            log.error("❌ Error logging security event: {}", e.getMessage());
        }
    }
}