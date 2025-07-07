package com.tathanhloc.faceattendance.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/python")
@Slf4j
public class PythonFeatureExtractionController {

    @Value("${app.python.venv.path:/home/loki/Desktop/face-attendance/.venv}")
    private String venvPath;

    @Value("${app.python.script.path:/home/loki/Desktop/face-attendance}")
    private String scriptPath;

    @Value("${app.python.script.file:scripts/face_recognition/face_feature_extractor.py}")
    private String scriptFile;

    /**
     * Test endpoint để kiểm tra controller hoạt động
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Python controller is working");
        response.put("venvPath", venvPath);
        response.put("scriptPath", scriptPath);
        response.put("timestamp", System.currentTimeMillis());

        log.info("Python controller test endpoint called");
        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra Python environment
     */
    @GetMapping("/check-environment")
    public ResponseEntity<?> checkPythonEnvironment() {
        try {
            log.info("Checking Python environment...");

            // Kiểm tra venv tồn tại
            File venvDir = new File(venvPath);
            if (!venvDir.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Virtual environment not found: " + venvPath
                ));
            }

            // Kiểm tra script tồn tại
            File pythonScriptFile = new File(scriptPath + "/" + scriptFile);
            if (!pythonScriptFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script not found: " + pythonScriptFile.getAbsolutePath()
                ));
            }

            // Test Python imports
            String command = String.format("source %s/bin/activate && python -c \"import insightface, numpy, cv2; print('SUCCESS')\"", venvPath);

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(new File(scriptPath));

            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Python environment OK",
                        "venvPath", venvPath,
                        "scriptPath", scriptPath,
                        "scriptFile", pythonScriptFile.getAbsolutePath()
                ));
            } else {
                // Read error output
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }

                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python environment check failed",
                        "error", errorOutput.toString()
                ));
            }

        } catch (Exception e) {
            log.error("Error checking Python environment: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error checking environment: " + e.getMessage()
            ));
        }
    }

    /**
     * Trích xuất đặc trưng cho một sinh viên
     */
    @PostMapping("/extract/{maSv}")
    public ResponseEntity<?> extractFeaturesForStudent(@PathVariable String maSv) {
        try {
            log.info("Request to extract features for student: {}", maSv);

            // Kiểm tra script tồn tại
            File pythonScriptFile = new File(scriptPath + "/" + scriptFile);
            if (!pythonScriptFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script not found: " + pythonScriptFile.getAbsolutePath()
                ));
            }

            // Tạo command để chạy Python script
            String command = String.format(
                    "cd %s && source %s/bin/activate && python %s",
                    scriptPath, venvPath, scriptFile
            );

            log.info("Executing command: {}", command);

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(new File(scriptPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Đọc output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Python output: {}", line);
            }

            // Đợi process hoàn thành
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script timeout after 30 minutes"
                ));
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            if (exitCode == 0) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Feature extraction completed for student: " + maSv,
                        "output", outputStr,
                        "exitCode", exitCode
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script failed with exit code: " + exitCode,
                        "output", outputStr,
                        "exitCode", exitCode
                ));
            }

        } catch (Exception e) {
            log.error("Error extracting features for student {}: ", maSv, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Trích xuất đặc trưng cho tất cả sinh viên
     */
    @PostMapping("/extract-all")
    public ResponseEntity<?> extractFeaturesForAll() {
        try {
            log.info("Request to extract features for all students");

            // Kiểm tra script tồn tại
            File pythonScriptFile = new File(scriptPath + "/" + scriptFile);
            if (!pythonScriptFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script not found: " + pythonScriptFile.getAbsolutePath()
                ));
            }

            // Tạo command để chạy Python script
            String command = String.format(
                    "cd %s && source %s/bin/activate && python %s",
                    scriptPath, venvPath, scriptFile
            );

            log.info("Executing batch command: {}", command);

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(new File(scriptPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Đọc output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("Python batch output: {}", line);
            }

            // Đợi process hoàn thành (timeout 30 phút)
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python batch script timeout after 30 minutes"
                ));
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            if (exitCode == 0) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Batch feature extraction completed successfully",
                        "output", outputStr,
                        "exitCode", exitCode
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python batch script failed with exit code: " + exitCode,
                        "output", outputStr,
                        "exitCode", exitCode
                ));
            }

        } catch (Exception e) {
            log.error("Error in batch feature extraction: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy progress của quá trình trích xuất (placeholder)
     */
    @GetMapping("/progress")
    public ResponseEntity<?> getExtractionProgress() {
        // Placeholder implementation
        return ResponseEntity.ok(Map.of(
                "status", "running",
                "progress", 50,
                "message", "Progress tracking not implemented yet"
        ));
    }

    /**
     * Dừng quá trình trích xuất (placeholder)
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopExtraction() {
        // Placeholder implementation
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Stop functionality not implemented yet"
        ));
    }
}