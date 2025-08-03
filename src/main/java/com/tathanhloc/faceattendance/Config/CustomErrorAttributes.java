package com.tathanhloc.faceattendance.Config;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Error Attributes to enhance error information
 */
@Slf4j
class CustomErrorAttributes extends org.springframework.boot.web.servlet.error.DefaultErrorAttributes {

    @Override
    public java.util.Map<String, Object> getErrorAttributes(
            org.springframework.web.context.request.WebRequest webRequest,
            org.springframework.boot.web.error.ErrorAttributeOptions options) {

        java.util.Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);

        // Add custom attributes for Face Attendance System
        errorAttributes.put("service", "Face Attendance System");
        errorAttributes.put("version", "1.0.0"); // Update with your version
        errorAttributes.put("support", "support@faceattendance.com");

        // Add request info
        String userAgent = webRequest.getHeader("User-Agent");
        if (userAgent != null) {
            errorAttributes.put("userAgent", userAgent);
        }

        // Add correlation ID for tracking
        String correlationId = java.util.UUID.randomUUID().toString().substring(0, 8);
        errorAttributes.put("correlationId", correlationId);

        // Log error for monitoring
        Integer status = (Integer) errorAttributes.get("status");
        String path = (String) errorAttributes.get("path");

        if (status != null && status >= 500) {
            log.error("Server error {} at path: {} | Correlation ID: {}", status, path, correlationId);
        } else if (status != null && status >= 400) {
            log.warn("Client error {} at path: {} | Correlation ID: {}", status, path, correlationId);
        }

        return errorAttributes;
    }
}