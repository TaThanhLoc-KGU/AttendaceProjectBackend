package com.tathanhloc.faceattendance.Exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;

@ControllerAdvice
@Slf4j
public class EnhancedGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Xử lý 404 cho API requests
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String requestURI = ex.getRequestURL();
        String method = ex.getHttpMethod();

        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND,
                "Endpoint không tồn tại: " + method + " " + requestURI,
                ex
        );

        log.error("404 - No handler found for {} {}", method, requestURI);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        log.error("Resource not found: {}", ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Dữ liệu không hợp lệ hoặc vi phạm ràng buộc";
        if (ex.getMessage().contains("Duplicate entry")) {
            message = "Dữ liệu đã tồn tại";
        }
        ApiError apiError = new ApiError(HttpStatus.CONFLICT, message, ex);
        log.error("Data integrity violation: {}", ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không đúng", ex);
        log.error("Bad credentials: {}", ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, "Không có quyền truy cập", ex);
        log.error("Access denied: {}", ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Lỗi xác thực dữ liệu", ex);

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            apiError.addSubError(new ApiValidationError(
                    violation.getRootBeanClass().getSimpleName(),
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue(),
                    violation.getMessage()
            ));
        }

        log.error("Validation error: {}", ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Lỗi xác thực dữ liệu", ex);

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            apiError.addSubError(new ApiValidationError(
                    error.getObjectName(),
                    error.getField(),
                    error.getRejectedValue(),
                    error.getDefaultMessage()
            ));
        }

        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            apiError.addSubError(new ApiValidationError(
                    error.getObjectName(),
                    error.getDefaultMessage()
            ));
        }

        log.error("Method argument validation failed: {}", ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAllExceptions(Exception ex) {
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", ex);
        log.error("Unhandled exception occurred", ex);
        return buildResponseEntity(apiError);
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}