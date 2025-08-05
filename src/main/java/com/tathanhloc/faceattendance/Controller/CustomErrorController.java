package com.tathanhloc.faceattendance.Controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔧 Custom Error Controller - Thay thế cho Spring Boot BasicErrorController
 * ✅ Xử lý cả API errors (JSON) và Web errors (HTML)
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        // Lấy thông tin lỗi
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestURI = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        Integer statusCode = status != null ? Integer.valueOf(status.toString()) : 500;

        log.error("🚨 Custom Error Handler - Status: {} | Path: {} | Message: {}",
                statusCode, requestURI, errorMessage);

        if (exception != null) {
            log.error("Exception details: ", exception);
        }

        // Kiểm tra loại request
        if (isApiRequest(request)) {
            log.info("📱 Handling as API request");
            return handleApiError(request, statusCode, requestURI, errorMessage);
        } else {
            log.info("🌐 Handling as Web request");
            return handleWebError(statusCode, requestURI, model);
        }
    }

    /**
     * 📱 Xử lý API errors - Trả về JSON
     */
    private ResponseEntity<Map<String, Object>> handleApiError(HttpServletRequest request,
                                                               Integer statusCode,
                                                               String requestURI,
                                                               String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();

        // Thông tin cơ bản
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", statusCode);
        errorResponse.put("path", requestURI);
        errorResponse.put("method", request.getMethod());

        // Thông điệp lỗi theo mã lỗi
        switch (statusCode) {
            case 400:
                errorResponse.put("error", "Bad Request");
                errorResponse.put("message", "Yêu cầu không hợp lệ");
                break;
            case 401:
                errorResponse.put("error", "Unauthorized");
                errorResponse.put("message", "Chưa được xác thực. Vui lòng đăng nhập.");
                break;
            case 403:
                errorResponse.put("error", "Forbidden");
                errorResponse.put("message", "Không có quyền truy cập tài nguyên này");
                break;
            case 404:
                errorResponse.put("error", "Not Found");
                errorResponse.put("message", "API endpoint không tồn tại: " + request.getMethod() + " " + requestURI);
                break;
            case 405:
                errorResponse.put("error", "Method Not Allowed");
                errorResponse.put("message", "Phương thức " + request.getMethod() + " không được hỗ trợ");
                break;
            case 500:
                errorResponse.put("error", "Internal Server Error");
                errorResponse.put("message", "Lỗi hệ thống. Vui lòng thử lại sau.");
                break;
            default:
                errorResponse.put("error", "Error");
                errorResponse.put("message", "Đã xảy ra lỗi: " + statusCode);
        }

        // Thêm debug info trong development
        String profile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(profile) || "development".equals(profile)) {
            Map<String, Object> debug = new HashMap<>();
            debug.put("userAgent", request.getHeader("User-Agent"));
            debug.put("remoteAddr", request.getRemoteAddr());
            debug.put("originalMessage", errorMessage);
            debug.put("acceptHeader", request.getHeader("Accept"));
            debug.put("contentType", request.getHeader("Content-Type"));
            errorResponse.put("debug", debug);
        }

        log.info("📤 API Error Response: {}", errorResponse);

        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * 🌐 Xử lý Web errors - Trả về HTML page
     */
    private String handleWebError(Integer statusCode, String requestURI, Model model) {
        // Thêm attributes cho template
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("requestURI", requestURI);
        model.addAttribute("timestamp", LocalDateTime.now());

        switch (statusCode) {
            case 403:
                log.warn("🚫 403 Forbidden - Redirecting to 403 page");
                model.addAttribute("errorTitle", "Truy cập bị từ chối");
                model.addAttribute("errorMessage", "Bạn không có quyền truy cập trang này.");
                return "error/403";

            case 404:
                log.info("🔍 404 Not Found - Redirecting to 404 page");
                model.addAttribute("errorTitle", "Trang không tồn tại");
                model.addAttribute("errorMessage", "Trang bạn đang tìm kiếm không tồn tại.");
                return "error/404";

            case 500:
                log.error("💥 500 Internal Server Error - Redirecting to 500 page");
                model.addAttribute("errorTitle", "Lỗi hệ thống");
                model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
                return "error/500";

            default:
                log.error("❓ Unhandled error {} - Redirecting to generic error page", statusCode);
                model.addAttribute("errorTitle", "Đã xảy ra lỗi");
                model.addAttribute("errorMessage", "Mã lỗi: " + statusCode);
                return "error/error";
        }
    }

    /**
     * 🔍 Kiểm tra xem đây có phải API request không
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        String requestURI = request.getRequestURI();
        String xRequestedWith = request.getHeader("X-Requested-With");

        boolean isApi = (acceptHeader != null && acceptHeader.contains("application/json")) ||
                (contentType != null && contentType.contains("application/json")) ||
                (requestURI != null && (requestURI.startsWith("/api/") || requestURI.startsWith("/rest/"))) ||
                "XMLHttpRequest".equals(xRequestedWith);

        log.debug("🔍 Request Analysis: URI={}, Accept={}, ContentType={}, IsAPI={}",
                requestURI, acceptHeader, contentType, isApi);

        return isApi;
    }
}