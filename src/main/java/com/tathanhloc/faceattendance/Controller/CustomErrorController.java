package com.tathanhloc.faceattendance.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Error Controller cho Face Attendance System
 * Xử lý error pages và API error responses
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorPath = getErrorPath(request);
        String userAgent = request.getHeader("User-Agent");

        // Determine if this is an API request
        boolean isApiRequest = isApiRequest(request);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            log.error("Error {} occurred at path: {} | User-Agent: {}",
                    statusCode, errorPath, userAgent);

            if (isApiRequest) {
                return handleApiError(request, statusCode);
            } else {
                return handleWebError(request, statusCode);
            }
        }

        log.error("Unknown error occurred at path: {} | User-Agent: {}", errorPath, userAgent);

        if (isApiRequest) {
            return handleApiError(request, 500);
        } else {
            return handleWebError(request, 500);
        }
    }

    /**
     * Handle API error responses (JSON)
     */
    @ResponseBody
    private ResponseEntity<Map<String, Object>> handleApiError(HttpServletRequest request, Integer statusCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", statusCode);
        errorResponse.put("error", httpStatus.getReasonPhrase());
        errorResponse.put("path", getErrorPath(request));

        switch (statusCode) {
            case 400:
                errorResponse.put("message", "Yêu cầu không hợp lệ");
                break;
            case 401:
                errorResponse.put("message", "Yêu cầu xác thực");
                break;
            case 403:
                errorResponse.put("message", "Không có quyền truy cập");
                break;
            case 404:
                errorResponse.put("message", "Endpoint không tồn tại: " + request.getMethod() + " " + getErrorPath(request));
                break;
            case 405:
                errorResponse.put("message", "Phương thức HTTP không được hỗ trợ");
                break;
            case 500:
                errorResponse.put("message", "Lỗi máy chủ nội bộ");
                break;
            case 503:
                errorResponse.put("message", "Dịch vụ tạm thời không khả dụng");
                break;
            default:
                errorResponse.put("message", "Đã xảy ra lỗi: " + httpStatus.getReasonPhrase());
        }

        // Thêm thông tin debug cho development
        String profile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(profile) || "development".equals(profile)) {
            errorResponse.put("debug", Map.of(
                    "userAgent", request.getHeader("User-Agent"),
                    "method", request.getMethod(),
                    "queryString", request.getQueryString(),
                    "remoteAddr", request.getRemoteAddr()
            ));
        }

        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * Handle web error pages (HTML)
     */
    private String handleWebError(HttpServletRequest request, Integer statusCode) {
        String errorPath = getErrorPath(request);

        switch (statusCode) {
            case 403:
                log.warn("403 Forbidden access attempt to: {}", errorPath);
                return "forward:/error/403.html";

            case 404:
                log.info("404 Not Found: {}", errorPath);
                return "forward:/error/404.html";

            case 500:
                log.error("500 Internal Server Error at: {}", errorPath);
                return "forward:/error/500.html";

            case 503:
                log.warn("503 Service Unavailable at: {}", errorPath);
                return "forward:/error/503.html";

            default:
                log.error("Unhandled error {} at: {}", statusCode, errorPath);
                return "forward:/error/500.html";
        }
    }

    /**
     * Determine if request is for API (expects JSON response)
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");

        // Check if request path starts with /api
        if (requestURI != null && requestURI.startsWith("/api")) {
            return true;
        }

        // Check Accept header
        if (acceptHeader != null &&
                (acceptHeader.contains("application/json") ||
                        acceptHeader.contains("application/xml"))) {
            return true;
        }

        // Check Content-Type header
        if (contentType != null &&
                (contentType.contains("application/json") ||
                        contentType.contains("application/xml"))) {
            return true;
        }

        // Check if it's AJAX request
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(requestedWith)) {
            return true;
        }

        return false;
    }

    /**
     * Get error path from request
     */
    private String getErrorPath(HttpServletRequest request) {
        String errorPath = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorPath == null) {
            errorPath = request.getRequestURI();
        }
        return errorPath;
    }

    /**
     * Spring Boot 2.3+ requires this method
     */
    public String getErrorPath() {
        return "/error";
    }
}