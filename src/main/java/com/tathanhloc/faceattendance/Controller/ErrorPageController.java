package com.tathanhloc.faceattendance.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/error-pages")
public class ErrorPageController {

    @RequestMapping(value = "/404", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> get404Page() {
        try {
            // Trong thực tế, bạn có thể đọc file từ classpath
            // Ở đây tôi redirect đến static file
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Location", "/error/404.html")
                    .build();
        } catch (Exception e) {
            log.error("Error serving 404 page via API", e);
            return ResponseEntity.internalServerError()
                    .body("<h1>Error loading 404 page</h1>");
        }
    }

    @RequestMapping(value = "/403", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> get403Page() {
        try {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("Location", "/error/403.html")
                    .build();
        } catch (Exception e) {
            log.error("Error serving 403 page via API", e);
            return ResponseEntity.internalServerError()
                    .body("<h1>Error loading 403 page</h1>");
        }
    }

    @RequestMapping(value = "/500", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> get500Page() {
        try {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Location", "/error/500.html")
                    .build();
        } catch (Exception e) {
            log.error("Error serving 500 page via API", e);
            return ResponseEntity.internalServerError()
                    .body("<h1>Error loading 500 page</h1>");
        }
    }

    @RequestMapping(value = "/503", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> get503Page() {
        try {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Location", "/error/503.html")
                    .build();
        } catch (Exception e) {
            log.error("Error serving 503 page via API", e);
            return ResponseEntity.internalServerError()
                    .body("<h1>Error loading 503 page</h1>");
        }
    }
}