package com.tathanhloc.faceattendance.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PythonFeatureExtractionService {

    @Value("${app.python.venv.path:/home/loki/Desktop/face-attendance/venv}")
    private String venvPath;

    @Value("${app.python.script.path:/home/loki/Desktop/face-attendance}")
    private String scriptPath;

    @Value("${app.uploads.path:src/main/resources/static/uploads}")
    private String uploadsPath;

    @Value("${server.port:8080}")
    private String serverPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Process currentProcess;
    private Map<String, Object> currentProgress = new HashMap<>();
    private boolean isExtracting = false;

    @PostConstruct
    public void init() {
        initializePythonScripts();
    }

    /**
     * Khởi tạo Python scripts và environment
     */
    private void initializePythonScripts() {
        try {
            Path scriptDir = Paths.get(scriptPath);

            // Tạo thư mục scripts nếu chưa có
            Files.createDirectories(scriptDir);

            // Copy script trích xuất đặc trưng vào thư mục project
            copyExtractionScript(scriptDir);

            // Tạo bash script để chạy Python
            createBashScript(scriptDir);

            log.info("Python scripts initialized successfully at: {}", scriptDir);

        } catch (Exception e) {
            log.error("Error initializing Python scripts: ", e);
        }
    }

    /**
     * Copy file Python script vào project
     */
    private void copyExtractionScript(Path scriptDir) throws IOException {
        // Script đã có sẵn, chỉ cần tạo wrapper script
        String wrapperScript = createWrapperScript();
        Path wrapperFile = scriptDir.resolve("extract_wrapper.py");
        Files.write(wrapperFile, wrapperScript.getBytes());

        log.info("Python wrapper script created: {}", wrapperFile);
    }

    /**
     * Tạo wrapper script để call script chính với các tham số phù hợp
     */
    private String createWrapperScript() {
        return String.format("""
            #!/usr/bin/env python3
            import sys
            import os
            import asyncio
            import json
            
            # Add current directory to Python path
            sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
            
            # Import script chính
            from paste import FaceFeatureExtractor, main as original_main
            
            def extract_single_student(ma_sv):
                \"\"\"Trích xuất đặc trưng cho một sinh viên\"\"\"
                import asyncio
                
                async def extract_one():
                    # Cấu hình
                    PROJECT_ROOT = "%s"
                    BACKEND_API_URL = "http://localhost:%s/api"
                    FACE_API_URL = "http://localhost:8001"
                    
                    CREDENTIALS = {
                        'username': 'admin',
                        'password': 'admin123'
                    }
                    
                    # Khởi tạo extractor
                    extractor = FaceFeatureExtractor(BACKEND_API_URL, FACE_API_URL, PROJECT_ROOT, CREDENTIALS)
                    
                    # Xử lý sinh viên
                    result = await extractor.process_student(ma_sv)
                    
                    # In kết quả dưới dạng JSON để Java đọc
                    print("RESULT_JSON_START")
                    print(json.dumps(result, ensure_ascii=False, default=str))
                    print("RESULT_JSON_END")
                    
                    return result
                
                return asyncio.run(extract_one())
            
            def extract_all_students():
                \"\"\"Trích xuất đặc trưng cho tất cả sinh viên\"\"\"
                import asyncio
                
                async def extract_all():
                    # Cấu hình
                    PROJECT_ROOT = "%s"
                    BACKEND_API_URL = "http://localhost:%s/api"
                    FACE_API_URL = "http://localhost:8001"
                    
                    CREDENTIALS = {
                        'username': 'admin',
                        'password': 'admin@123'
                    }
                    
                    # Khởi tạo extractor
                    extractor = FaceFeatureExtractor(BACKEND_API_URL, FACE_API_URL, PROJECT_ROOT, CREDENTIALS)
                    
                    # Xử lý tất cả sinh viên
                    results = await extractor.process_all_students()
                    
                    # In kết quả dưới dạng JSON để Java đọc
                    print("RESULT_JSON_START")
                    print(json.dumps(results, ensure_ascii=False, default=str))
                    print("RESULT_JSON_END")
                    
                    return results
                
                return asyncio.run(extract_all())
            
            if __name__ == "__main__":
                if len(sys.argv) < 2:
                    print("Usage: python extract_wrapper.py <command> [student_id]")
                    print("Commands: single <student_id>, all")
                    sys.exit(1)
                
                command = sys.argv[1]
                
                try:
                    if command == "single" and len(sys.argv) >= 3:
                        student_id = sys.argv[2]
                        result = extract_single_student(student_id)
                        sys.exit(0 if result.get('status') == 'success' else 1)
                    elif command == "all":
                        results = extract_all_students()
                        sys.exit(0 if results.get('success') else 1)
                    else:
                        print("Invalid command or missing student_id")
                        sys.exit(1)
                        
                except Exception as e:
                    print(f"ERROR: {str(e)}", file=sys.stderr)
                    sys.exit(1)
            """, scriptPath, serverPort, scriptPath, serverPort);
    }

    /**
     * Tạo bash script để chạy Python với venv
     */
    private void createBashScript(Path scriptDir) throws IOException {
        String bashScript = String.format("""
            #!/bin/bash
            
            # Đường dẫn tới virtual environment
            VENV_PATH="%s"
            SCRIPT_DIR="%s"
            
            # Kiểm tra venv tồn tại
            if [ ! -d "$VENV_PATH" ]; then
                echo "ERROR: Virtual environment not found at $VENV_PATH"
                exit 1
            fi
            
            # Activate virtual environment
            source "$VENV_PATH/bin/activate"
            
            # Kiểm tra Python và packages
            if ! python -c "import insightface, numpy, cv2" 2>/dev/null; then
                echo "ERROR: Required Python packages not installed"
                deactivate
                exit 1
            fi
            
            # Chuyển tới thư mục script
            cd "$SCRIPT_DIR"
            
            # Chạy Python script với các tham số
            python extract_wrapper.py "$@"
            
            # Lưu exit code
            exit_code=$?
            
            # Deactivate venv
            deactivate
            
            # Trả về exit code
            exit $exit_code
            """, venvPath, scriptPath);

        Path bashFile = scriptDir.resolve("run_extraction.sh");
        Files.write(bashFile, bashScript.getBytes());

        // Make executable
        bashFile.toFile().setExecutable(true);

        log.info("Bash script created: {}", bashFile);
    }

    /**
     * Trích xuất đặc trưng cho một sinh viên
     */
    public Map<String, Object> extractFeaturesForStudent(String maSv) {
        if (isExtracting) {
            return Map.of(
                    "status", "error",
                    "message", "Đang có quá trình trích xuất khác đang chạy"
            );
        }

        try {
            isExtracting = true;
            currentProgress.clear();
            currentProgress.put("type", "single");
            currentProgress.put("student", maSv);
            currentProgress.put("status", "running");
            currentProgress.put("startTime", System.currentTimeMillis());

            // Chạy script Python
            String result = runPythonScript("single", maSv);

            // Parse kết quả JSON
            Map<String, Object> resultMap = parseResult(result);

            currentProgress.put("status", "completed");
            currentProgress.put("endTime", System.currentTimeMillis());

            return resultMap;

        } catch (Exception e) {
            log.error("Error extracting features for student {}: {}", maSv, e.getMessage());
            currentProgress.put("status", "error");
            currentProgress.put("error", e.getMessage());

            return Map.of(
                    "status", "error",
                    "message", "Lỗi trích xuất: " + e.getMessage()
            );
        } finally {
            isExtracting = false;
        }
    }

    /**
     * Trích xuất đặc trưng cho tất cả sinh viên
     */
    public Map<String, Object> extractFeaturesForAll() {
        if (isExtracting) {
            return Map.of(
                    "status", "error",
                    "message", "Đang có quá trình trích xuất khác đang chạy"
            );
        }

        try {
            isExtracting = true;
            currentProgress.clear();
            currentProgress.put("type", "all");
            currentProgress.put("status", "running");
            currentProgress.put("startTime", System.currentTimeMillis());

            // Chạy script Python async để không block request
            CompletableFuture.runAsync(() -> {
                try {
                    String result = runPythonScript("all");
                    Map<String, Object> resultMap = parseResult(result);

                    currentProgress.put("status", "completed");
                    currentProgress.put("endTime", System.currentTimeMillis());
                    currentProgress.put("result", resultMap);

                } catch (Exception e) {
                    log.error("Error in async extraction: {}", e.getMessage());
                    currentProgress.put("status", "error");
                    currentProgress.put("error", e.getMessage());
                } finally {
                    isExtracting = false;
                }
            });

            return Map.of(
                    "status", "started",
                    "message", "Quá trình trích xuất đã bắt đầu. Sử dụng /api/python/progress để theo dõi."
            );

        } catch (Exception e) {
            log.error("Error starting extraction for all students: {}", e.getMessage());
            isExtracting = false;

            return Map.of(
                    "status", "error",
                    "message", "Lỗi khởi động trích xuất: " + e.getMessage()
            );
        }
    }

    /**
     * Chạy Python script
     */
    private String runPythonScript(String command, String... args) throws Exception {
        List<String> commandList = new ArrayList<>();
        commandList.add("bash");
        commandList.add(Paths.get(scriptPath, "run_extraction.sh").toString());
        commandList.add(command);
        commandList.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(commandList);
        pb.directory(new File(scriptPath));
        pb.redirectErrorStream(true);

        log.info("Running command: {}", String.join(" ", commandList));

        currentProcess = pb.start();

        // Đọc output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(currentProcess.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Python output: {}", line);

                // Update progress nếu có
                updateProgressFromOutput(line);
            }
        }

        // Đợi process hoàn thành
        boolean finished = currentProcess.waitFor(30, TimeUnit.MINUTES);

        if (!finished) {
            currentProcess.destroyForcibly();
            throw new RuntimeException("Python script timeout after 30 minutes");
        }

        int exitCode = currentProcess.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Python script failed with exit code: " + exitCode);
        }

        return output.toString();
    }

    /**
     * Update progress từ output của Python script
     */
    private void updateProgressFromOutput(String line) {
        // Parse progress information từ output
        if (line.contains("Bắt đầu xử lý sinh viên:")) {
            String studentId = line.split(":")[1].trim();
            currentProgress.put("currentStudent", studentId);
        } else if (line.contains("Hoàn thành xử lý. Thành công:")) {
            // Parse completion stats
            String stats = line.substring(line.indexOf("Thành công:"));
            currentProgress.put("stats", stats);
        }
    }

    /**
     * Parse kết quả JSON từ output
     */
    private Map<String, Object> parseResult(String output) {
        try {
            // Tìm JSON result trong output
            int startIndex = output.indexOf("RESULT_JSON_START");
            int endIndex = output.indexOf("RESULT_JSON_END");

            if (startIndex != -1 && endIndex != -1) {
                startIndex += "RESULT_JSON_START".length();
                String jsonResult = output.substring(startIndex, endIndex).trim();

                return objectMapper.readValue(jsonResult, Map.class);
            }

            // Fallback: return simple result
            return Map.of(
                    "status", "completed",
                    "message", "Hoàn thành trích xuất",
                    "output", output
            );

        } catch (Exception e) {
            log.error("Error parsing result JSON: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", "Lỗi parse kết quả: " + e.getMessage(),
                    "output", output
            );
        }
    }

    /**
     * Kiểm tra Python environment
     */
    public Map<String, Object> checkEnvironment() {
        try {
            String command = String.format("source %s/bin/activate && python -c \"import insightface, numpy, cv2; print('SUCCESS')\"",
                    venvPath);

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            Process process = pb.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return Map.of(
                        "status", "success",
                        "message", "Python environment OK",
                        "venvPath", venvPath,
                        "scriptPath", scriptPath
                );
            } else {
                return Map.of(
                        "status", "error",
                        "message", "Python environment not ready",
                        "venvPath", venvPath
                );
            }

        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Error checking environment: " + e.getMessage()
            );
        }
    }

    /**
     * Lấy progress hiện tại
     */
    public Map<String, Object> getProgress() {
        Map<String, Object> progress = new HashMap<>(currentProgress);
        progress.put("isRunning", isExtracting);
        progress.put("timestamp", System.currentTimeMillis());
        return progress;
    }

    /**
     * Dừng quá trình trích xuất
     */
    public Map<String, Object> stopExtraction() {
        try {
            if (currentProcess != null && currentProcess.isAlive()) {
                currentProcess.destroyForcibly();
                currentProgress.put("status", "stopped");
                currentProgress.put("endTime", System.currentTimeMillis());
                isExtracting = false;

                return Map.of(
                        "status", "success",
                        "message", "Đã dừng quá trình trích xuất"
                );
            } else {
                return Map.of(
                        "status", "info",
                        "message", "Không có quá trình nào đang chạy"
                );
            }
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Lỗi dừng extraction: " + e.getMessage()
            );
        }
    }
}