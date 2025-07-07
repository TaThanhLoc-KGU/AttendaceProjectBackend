package com.tathanhloc.faceattendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PythonFeatureExtractionService {

    @Value("${python.script.path:scripts/face_recognition/face_feature_extractor.py}")
    private String pythonScriptPath;

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${feature.extraction.timeout:300}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Trích xuất đặc trưng khuôn mặt cho một sinh viên
     */
    public CompletableFuture<Map<String, Object>> extractFeaturesAsync(String maSv) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractFeatures(maSv);
            } catch (Exception e) {
                log.error("Error in async feature extraction for student {}: ", maSv, e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                return errorResult;
            }
        });
    }

    /**
     * Trích xuất đặc trưng khuôn mặt cho một sinh viên (synchronous)
     */
    public Map<String, Object> extractFeatures(String maSv) throws Exception {
        log.info("Starting feature extraction for student: {}", maSv);

        // Kiểm tra file Python script tồn tại
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists()) {
            throw new RuntimeException("Python script not found: " + pythonScriptPath);
        }

        // Tạo ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                scriptFile.getAbsolutePath(),
                "--student-id", maSv,
                "--single-student"
        );

        // Set environment variables
        Map<String, String> env = processBuilder.environment();
        env.put("PYTHONPATH", System.getProperty("user.dir"));
        env.put("PYTHONIOENCODING", "utf-8");

        // Set working directory
        processBuilder.directory(new File(System.getProperty("user.dir")));
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            // Khởi chạy process
            process = processBuilder.start();

            // Đọc output
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Python output: {}", line);
                }
            }

            // Chờ process hoàn thành với timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Feature extraction timed out after " + timeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            log.info("Python script finished with exit code: {}", exitCode);
            log.debug("Python script output: {}", outputStr);

            // Parse kết quả
            Map<String, Object> result = parseExtractionResult(outputStr, exitCode);
            result.put("student_id", maSv);
            result.put("exit_code", exitCode);

            return result;

        } catch (Exception e) {
            log.error("Error executing Python script for student {}: ", maSv, e);
            throw new RuntimeException("Feature extraction failed: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Parse kết quả từ Python script
     */
    private Map<String, Object> parseExtractionResult(String output, int exitCode) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tìm JSON result trong output
            String[] lines = output.split("\n");
            String jsonResult = null;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("{") && line.contains("\"success\"")) {
                    jsonResult = line;
                    break;
                }
            }

            if (jsonResult != null) {
                // Parse JSON result
                Map<String, Object> jsonMap = objectMapper.readValue(jsonResult, Map.class);
                result.putAll(jsonMap);
            } else {
                // Fallback parsing
                result.put("success", exitCode == 0);
                result.put("message", exitCode == 0 ? "Feature extraction completed" : "Feature extraction failed");
                result.put("raw_output", output);

                // Tìm thông tin cơ bản trong output
                if (output.contains("✅")) {
                    result.put("success", true);
                    result.put("status", "success");
                } else if (output.contains("❌") || output.contains("💥")) {
                    result.put("success", false);
                    result.put("status", "failed");
                }
            }

        } catch (Exception e) {
            log.error("Error parsing Python script output: ", e);
            result.put("success", false);
            result.put("error", "Failed to parse extraction result");
            result.put("raw_output", output);
        }

        return result;
    }

    /**
     * Trích xuất đặc trưng cho tất cả sinh viên
     */
    public CompletableFuture<Map<String, Object>> extractAllFeaturesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractAllFeatures();
            } catch (Exception e) {
                log.error("Error in async batch feature extraction: ", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                return errorResult;
            }
        });
    }

    /**
     * Trích xuất đặc trưng cho tất cả sinh viên (synchronous)
     */
    public Map<String, Object> extractAllFeatures() throws Exception {
        log.info("Starting batch feature extraction for all students");

        // Kiểm tra file Python script tồn tại
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists()) {
            throw new RuntimeException("Python script not found: " + pythonScriptPath);
        }

        // Tạo ProcessBuilder cho batch processing
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                scriptFile.getAbsolutePath(),
                "--batch-mode"
        );

        // Set environment variables
        Map<String, String> env = processBuilder.environment();
        env.put("PYTHONPATH", System.getProperty("user.dir"));
        env.put("PYTHONIOENCODING", "utf-8");

        // Set working directory
        processBuilder.directory(new File(System.getProperty("user.dir")));
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            // Khởi chạy process
            process = processBuilder.start();

            // Đọc output
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.info("Batch extraction: {}", line);
                }
            }

            // Chờ process hoàn thành với timeout dài hơn cho batch
            boolean finished = process.waitFor(timeoutSeconds * 5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Batch feature extraction timed out");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            log.info("Batch extraction finished with exit code: {}", exitCode);

            // Parse kết quả batch
            Map<String, Object> result = parseExtractionResult(outputStr, exitCode);
            result.put("batch_mode", true);
            result.put("exit_code", exitCode);

            return result;

        } catch (Exception e) {
            log.error("Error executing batch feature extraction: ", e);
            throw new RuntimeException("Batch feature extraction failed: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Kiểm tra Python environment
     */
    public Map<String, Object> checkPythonEnvironment() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Kiểm tra Python executable
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "--version");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                result.put("python_version", version);
                result.put("python_executable", pythonExecutable);
            }

            process.waitFor(5, TimeUnit.SECONDS);
            result.put("python_available", process.exitValue() == 0);

        } catch (Exception e) {
            result.put("python_available", false);
            result.put("error", e.getMessage());
        }

        // Kiểm tra script file
        File scriptFile = new File(pythonScriptPath);
        result.put("script_path", pythonScriptPath);
        result.put("script_exists", scriptFile.exists());

        return result;
    }
}