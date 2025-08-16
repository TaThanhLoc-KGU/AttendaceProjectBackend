// ===== AttendanceException.java =====
package com.tathanhloc.faceattendance.Exception;

/**
 * Custom exception cho attendance operations
 */
public class AttendanceException extends RuntimeException {

    private final AttendanceErrorType type;

    public AttendanceException(AttendanceErrorType type, String message) {
        super(message);
        this.type = type;
    }

    public AttendanceException(AttendanceErrorType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public AttendanceErrorType getType() {
        return type;
    }

    public enum AttendanceErrorType {
        DUPLICATE_ATTENDANCE,
        INVALID_TIME_WINDOW,
        STUDENT_NOT_REGISTERED,
        SCHEDULE_NOT_FOUND,
        INVALID_STATUS,
        BATCH_SIZE_EXCEEDED,
        PERMISSION_DENIED
    }
}