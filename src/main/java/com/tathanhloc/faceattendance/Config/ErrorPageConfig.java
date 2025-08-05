package com.tathanhloc.faceattendance.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for Error Pages in Face Attendance System
 */
@Configuration
@Slf4j
public class ErrorPageConfig implements ErrorPageRegistrar, WebMvcConfigurer {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        log.info("🔧 Registering custom error pages for Face Attendance System");

        // Đăng ký các error pages - TẤT CẢ ĐỀU POINT VỀ /error
        registry.addErrorPages(
                // 4xx Client Errors
                new ErrorPage(HttpStatus.BAD_REQUEST, "/error"),
                new ErrorPage(HttpStatus.UNAUTHORIZED, "/error"),
                new ErrorPage(HttpStatus.FORBIDDEN, "/error"),
                new ErrorPage(HttpStatus.NOT_FOUND, "/error"),
                new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/error"),

                // 5xx Server Errors
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error"),
                new ErrorPage(HttpStatus.BAD_GATEWAY, "/error"),
                new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/error"),
                new ErrorPage(HttpStatus.GATEWAY_TIMEOUT, "/error"),

                // General exception handling
                new ErrorPage(Exception.class, "/error")
        );

        log.info("✅ Error pages registered successfully");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("🔧 Configuring static resource handlers");

        // Serve static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // Cache for 1 hour

        // CSS, JS, Images
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(7200); // Cache images for 2 hours

        // Add favicon handling
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // Cache for 24 hours

        log.info("✅ Static resource handlers configured successfully");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        log.info("🔧 Configuring view controllers");

        // No direct error page mappings - let CustomErrorController handle them

        log.info("✅ View controllers configured successfully");
    }
}