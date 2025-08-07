// THAY THẾ TOÀN BỘ WebConfig.java
package com.tathanhloc.faceattendance.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("🔧 Configuring static resource handlers");

        // ✅ CRITICAL FIX: Đảm bảo streams được serve từ file system

        // Method 1: Serve from target directory (for Maven builds)
        File targetStreamsDir = new File("target/classes/static/streams");
        if (targetStreamsDir.exists()) {
            String targetPath = targetStreamsDir.getAbsolutePath().replace("\\", "/");
            registry.addResourceHandler("/streams/**")
                    .addResourceLocations("file:///" + targetPath + "/")
                    .setCachePeriod(0)  // No cache for live streams
                    .resourceChain(false);
            log.info("✅ Added TARGET streams handler: file:///{}", targetPath);
        }

        // Method 2: Serve from src directory (for development)
        File srcStreamsDir = new File("src/main/resources/static/streams");
        if (srcStreamsDir.exists()) {
            String srcPath = srcStreamsDir.getAbsolutePath().replace("\\", "/");
            registry.addResourceHandler("/streams/**")
                    .addResourceLocations("file:///" + srcPath + "/")
                    .setCachePeriod(0)
                    .resourceChain(false);
            log.info("✅ Added SRC streams handler: file:///{}", srcPath);
        }

        // Method 3: Classpath fallback
        registry.addResourceHandler("/streams/**")
                .addResourceLocations("classpath:/static/streams/")
                .setCachePeriod(0)
                .resourceChain(false);
        log.info("✅ Added CLASSPATH streams handler");

        // ✅ UPLOADS configuration
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/")
                .setCachePeriod(0)
                .resourceChain(false);

        // ✅ STATIC resources configuration
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // ✅ CSS, JS, Images
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        log.info("✅ All resource handlers configured successfully");
    }
}