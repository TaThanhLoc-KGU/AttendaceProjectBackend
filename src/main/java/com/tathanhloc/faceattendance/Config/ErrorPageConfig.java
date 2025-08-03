package com.tathanhloc.faceattendance.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
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

        // Đăng ký các error pages
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
        log.info("🔧 Configuring static resource handlers for error pages");

        // Serve error pages from static/error/
        registry.addResourceHandler("/error/**")
                .addResourceLocations("classpath:/static/error/")
                .setCachePeriod(300); // Cache for 5 minutes

        // Ensure static resources are properly served
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

        log.info("✅ Static resource handlers configured successfully");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        log.info("🔧 Configuring view controllers for error pages");

        // Direct mappings for error pages (optional, for direct access)
        registry.addViewController("/error/404").setViewName("forward:/error/404.html");
        registry.addViewController("/error/403").setViewName("forward:/error/403.html");
        registry.addViewController("/error/500").setViewName("forward:/error/500.html");
        registry.addViewController("/error/503").setViewName("forward:/error/503.html");

        log.info("✅ View controllers configured successfully");
    }

    /**
     * Bean for custom error attributes (optional)
     */
    @Bean
    public CustomErrorAttributes customErrorAttributes() {
        return new CustomErrorAttributes();
    }
}