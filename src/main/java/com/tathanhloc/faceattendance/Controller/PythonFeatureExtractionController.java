package com.tathanhloc.faceattendance.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/python")
@Slf4j
public class PythonFeatureExtractionController {

    @Value("${app.python.venv.path:}")
    private String venvPath;

    @Value("${app.python.script.path:}")
    private String scriptPath;

    @Value("${app.python.script.file:scripts/face_recognition/face_feature_extractor.py}")
    private String scriptFile;

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String[] createCommand(String pythonCommand) {
        if (isWindows()) {
            // Windows command
            String command = String.format(
                    "cd /d \"%s\" && \"%s\\Scripts\\activate.bat\" && python %s",
                    scriptPath, venvPath, scriptFile
            );
            return new String[]{"cmd", "/c", command};
        } else {
            // Linux/Unix command
            String command = String.format(
                    "cd %s && source %s/bin/activate && python %s",
                    scriptPath, venvPath, scriptFile
            );
            return new String[]{"bash", "-c", command};
        }
    }

    private String[] createTestCommand() {
        if (isWindows()) {
            String command = String.format(
                    "\"%s\\Scripts\\activate.bat\" && python -c \"import insightface, numpy, cv2; print('SUCCESS')\"",
                    venvPath
            );
            return new String[]{"cmd", "/c", command};
        } else {
            String command = String.format(
                    "source %s/bin/activate && python -c \"import insightface, numpy, cv2; print('SUCCESS')\"",
                    venvPath
            );
            return new String[]{"bash", "-c", command};
        }
    }

    /**
     * Kiểm tra môi trường Python
     */
    @GetMapping("/check-environment")
    public ResponseEntity<?> checkPythonEnvironment() {
        try {
            log.info("Checking Python environment...");
            log.info("OS: {}", System.getProperty("os.name"));
            log.info("Virtual environment path: {}", venvPath);
            log.info("Script path: {}", scriptPath);

            // Kiểm tra đường dẫn tồn tại
            if (venvPath == null || venvPath.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Virtual environment path not configured"
                ));
            }

            if (scriptPath == null || scriptPath.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Script path not configured"
                ));
            }

            // Kiểm tra venv tồn tại
            File venvDir = new File(venvPath);
            if (!venvDir.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Virtual environment not found: " + venvPath
                ));
            }

            // Kiểm tra script tồn tại
            File pythonScriptFile = new File(scriptPath, scriptFile);
            if (!pythonScriptFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script not found: " + pythonScriptFile.getAbsolutePath()
                ));
            }

            // Test Python imports
            String[] testCommand = createTestCommand();
            log.info("Executing test command: {}", String.join(" ", testCommand));

            ProcessBuilder pb = new ProcessBuilder(testCommand);
            pb.directory(new File(scriptPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Python environment OK",
                        "os", System.getProperty("os.name"),
                        "venvPath", venvPath,
                        "scriptPath", scriptPath,
                        "scriptFile", pythonScriptFile.getAbsolutePath()
                ));
            } else {
                // Read output for debugging
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python environment check failed",
                        "output", output.toString(),
                        "exitCode", finished ? process.exitValue() : -1
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
            File pythonScriptFile = new File(scriptPath, scriptFile);
            if (!pythonScriptFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script not found: " + pythonScriptFile.getAbsolutePath()
                ));
            }

            // Tạo command cho single student extraction
            String[] command;
            if (isWindows()) {
                String cmd = String.format(
                        "cd /d \"%s\" && \"%s\\Scripts\\activate.bat\" && python %s single %s",
                        scriptPath, venvPath, scriptFile, maSv
                );
                command = new String[]{"cmd", "/c", cmd};
            } else {
                String cmd = String.format(
                        "cd %s && source %s/bin/activate && python %s single %s",
                        scriptPath, venvPath, scriptFile, maSv
                );
                command = new String[]{"bash", "-c", cmd};
            }

            log.info("Executing command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
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
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script timeout after 10 minutes"
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
    @RequestMapping(value = "/extract-all", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> extractFeaturesForAll() {
        try {
            log.info("Request to extract features for all students");

            // Kiểm tra script tồn tại
            File pythonScriptFile = new File(scriptPath, scriptFile);
            if (!pythonScriptFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Python script not found: " + pythonScriptFile.getAbsolutePath()
                ));
            }

            // Tạo command cho batch extraction
            String[] command;
            if (isWindows()) {
                String cmd = String.format(
                        "cd /d \"%s\" && \"%s\\Scripts\\activate.bat\" && python %s",
                        scriptPath, venvPath, scriptFile
                );
                command = new String[]{"cmd", "/c", cmd};
            } else {
                String cmd = String.format(
                        "cd %s && source %s/bin/activate && python %s",
                        scriptPath, venvPath, scriptFile
                );
                command = new String[]{"bash", "-c", cmd};
            }

            log.info("Executing batch command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
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
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Stop functionality not implemented yet"
        ));
    }

    /**
     * Tạo wrapper script nếu chưa có
     */
    @PostMapping("/create-wrapper")
    public ResponseEntity<?> createWrapperScript() {
        try {
            Path wrapperPath = Paths.get(scriptPath, "extract_wrapper.py");

            if (!Files.exists(wrapperPath)) {
                String wrapperContent = createPythonWrapper();
                Files.write(wrapperPath, wrapperContent.getBytes());
                log.info("Created wrapper script: {}", wrapperPath);
            }

            // Tạo batch script cho Windows
            if (isWindows()) {
                Path batchPath = Paths.get(scriptPath, "run_extraction.bat");
                if (!Files.exists(batchPath)) {
                    String batchContent = createWindowsBatchScript();
                    Files.write(batchPath, batchContent.getBytes());
                    log.info("Created batch script: {}", batchPath);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Wrapper scripts created successfully"
            ));

        } catch (Exception e) {
            log.error("Error creating wrapper script: ", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error creating wrapper: " + e.getMessage()
            ));
        }
    }

    private String createPythonWrapper() {
        return String.format("""
            #!/usr/bin/env python3
            import sys
            import os
            import asyncio
            import json
            from pathlib import Path
                        
            # Add scripts directory to path
            project_root = Path(__file__).parent
            scripts_dir = project_root / 'scripts' / 'face_recognition'
            sys.path.insert(0, str(scripts_dir))
                        
            try:
                from face_feature_extractor import main
            except ImportError as e:
                print(f"ERROR: Cannot import face_feature_extractor: {e}")
                sys.exit(1)
                        
            if __name__ == "__main__":
                asyncio.run(main())
            """);
    }

    private String createWindowsBatchScript() {
        return String.format("""
            @echo off
            setlocal
                        
            set VENV_PATH=%s
            set SCRIPT_DIR=%s
                        
            if not exist "%%VENV_PATH%%" (
                echo ERROR: Virtual environment not found at %%VENV_PATH%%
                exit /b 1
            )
                        
            call "%%VENV_PATH%%\\Scripts\\activate.bat"
                        
            python -c "import insightface, numpy, cv2" >nul 2>&1
            if errorlevel 1 (
                echo ERROR: Required Python packages not installed
                call deactivate
                exit /b 1
            )
                        
            cd /d "%%SCRIPT_DIR%%"
                        
            python %s %%*
                        
            set exit_code=%%errorlevel%%
            call deactivate
            exit /b %%exit_code%%
            """, venvPath, scriptPath, scriptFile);
    }
}