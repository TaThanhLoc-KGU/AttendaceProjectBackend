// ===== TeacherAttendanceExceptionHandler.java =====
package com.tathanhloc.faceattendance.Exception;

import com.tathanhloc.faceattendance.DTO.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler cho Teacher Attendance
 */
@RestControllerAdvice(basePackages = "com.tathanhloc.faceattendance.Controller")
@Slf4j
public class TeacherAttendanceExceptionHandler {

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("🔍 Validation errors: {}", errors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Dữ liệu đầu vào không hợp lệ")
                        .data(errors)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        log.warn("⚠️ Business exception: {}", ex.getMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getMessage().contains("không tồn tại")) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getMessage().contains("không có quyền")) {
            status = HttpStatus.FORBIDDEN;
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("BUSINESS_ERROR")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("🔍 Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("RESOURCE_NOT_FOUND")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle data integrity violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        log.error("❌ Data integrity violation: {}", ex.getMessage());

        String message = "Có lỗi với dữ liệu";
        if (ex.getMessage().contains("unique") || ex.getMessage().contains("duplicate")) {
            message = "Dữ liệu đã tồn tại";
        } else if (ex.getMessage().contains("foreign key")) {
            message = "Dữ liệu tham chiếu không hợp lệ";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(message)
                        .errorCode("DATA_INTEGRITY_ERROR")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle attendance specific exceptions
     */
    @ExceptionHandler(AttendanceException.class)
    public ResponseEntity<ApiResponse<Object>> handleAttendanceException(AttendanceException ex) {
        log.warn("📝 Attendance exception: {}", ex.getMessage());

        HttpStatus status = switch (ex.getType()) {
            case DUPLICATE_ATTENDANCE -> HttpStatus.CONFLICT;
            case INVALID_TIME_WINDOW -> HttpStatus.BAD_REQUEST;
            case STUDENT_NOT_REGISTERED -> HttpStatus.FORBIDDEN;
            case SCHEDULE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("ATTENDANCE_ERROR_" + ex.getType())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle security/permission exceptions
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Object>> handleSecurityException(SecurityException ex) {
        log.warn("🔒 Security exception: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.builder()
                        .success(false)
                        .message("Không có quyền thực hiện thao tác này")
                        .errorCode("SECURITY_ERROR")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("🔢 Illegal argument: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .success(false)
                        .message("Tham số không hợp lệ: " + ex.getMessage())
                        .errorCode("INVALID_ARGUMENT")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("❌ Runtime exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .success(false)
                        .message("Lỗi hệ thống: " + ex.getMessage())
                        .errorCode("RUNTIME_ERROR")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("❌ Unexpected exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .success(false)
                        .message("Đã xảy ra lỗi không mong muốn")
                        .errorCode("UNEXPECTED_ERROR")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}