package com.tathanhloc.faceattendance.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve streams từ static directory
        registry.addResourceHandler("/streams/**")
                .addResourceLocations("classpath:/static/streams/")
                .setCachePeriod(0) // No cache for live streams
                .resourceChain(false);

        // ✅ CẤU HÌNH CHO UPLOADS - GIẢI QUYẾT VẤN ĐỀ LOAD ẢNH
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/")
                .setCachePeriod(0) // No cache trong development
                .resourceChain(false);

    }


}