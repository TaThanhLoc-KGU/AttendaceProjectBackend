package com.tathanhloc.faceattendance.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceRecognitionService {

    @Value("${app.python.executable:python}")
    private String pythonExecutable;

    @Value("${app.python.script.path:scripts/face_recognition}")
    private String pythonScriptPath;

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Value("${app.face.recognition.timeout:300}") // 5 minutes
    private long processTimeout;

    private final FileUploadService fileUploadService;

    /**
     * Trích xuất đặc trưng khuôn mặt từ 5 ảnh của sinh viên
     */
    public Map<String, Object> extractFeatures(String maSv) {
        try {
            log.info("Starting feature extraction for student: {}", maSv);

            // Kiểm tra điều kiện
            validatePreconditions(maSv);

            // Chuẩn bị đường dẫn
            Path studentDir = getStudentDirectory(maSv);
            Path facesDir = studentDir.resolve("faces");
            Path outputDir = studentDir.resolve("embeddings");

            // Tạo thư mục output
            Files.createDirectories(outputDir);

            // Gọi Python script
            Map<String, Object> result = executePythonScript(maSv, facesDir, outputDir);

            // Lưu kết quả
            if (result.containsKey("success") && (Boolean) result.get("success")) {
                String embeddingData = (String) result.get("embedding");
                fileUploadService.saveEmbeddingFile(maSv, embeddingData);

                log.info("Feature extraction completed successfully for student: {}", maSv);
            }

            return result;

        } catch (Exception e) {
            log.error("Error extracting features for student {}: ", maSv, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * Kiểm tra điều kiện trước khi trích xuất
     */
    private void validatePreconditions(String maSv) {
        // Kiểm tra có đủ 5 ảnh không
        int imageCount = fileUploadService.getFaceImageCount(maSv);
        if (imageCount < 5) {
            throw new IllegalStateException("Cần đủ 5 ảnh khuôn mặt để trích xuất đặc trưng. Hiện tại có: " + imageCount);
        }

        // Kiểm tra Python script có tồn tại không
        Path scriptPath = Paths.get(pythonScriptPath, "extract_features.py");
        if (!Files.exists(scriptPath)) {
            throw new IllegalStateException("Python script không tồn tại: " + scriptPath);
        }

        // Kiểm tra thư mục faces
        Path facesDir = getStudentDirectory(maSv).resolve("faces");
        if (!Files.exists(facesDir)) {
            throw new IllegalStateException("Thư mục ảnh khuôn mặt không tồn tại");
        }
    }

    /**
     * Thực thi Python script trích xuất đặc trưng
     */
    private Map<String, Object> executePythonScript(String maSv, Path facesDir, Path outputDir) {
        try {
            // Tạo lệnh Python
            List<String> command = Arrays.asList(
                    pythonExecutable,
                    Paths.get(pythonScriptPath, "extract_features.py").toString(),
                    "--student_id", maSv,
                    "--faces_dir", facesDir.toString(),
                    "--output_dir", outputDir.toString(),
                    "--format", "json"
            );

            log.info("Executing Python command: {}", String.join(" ", command));

            // Tạo ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(pythonScriptPath));
            processBuilder.redirectErrorStream(true);

            // Thực thi
            Process process = processBuilder.start();

            // Đọc output
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }

            // Chờ process hoàn thành
            boolean finished = process.waitFor(processTimeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python script timeout sau " + processTimeout + " giây");
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                // Thành công - parse kết quả
                return parseSuccessResult(output.toString(), outputDir);
            } else {
                // Lỗi
                log.error("Python script failed with exit code {}: {}", exitCode, error.toString());
                return Map.of(
                        "success", false,
                        "error", "Script execution failed: " + error.toString(),
                        "exitCode", exitCode
                );
            }

        } catch (Exception e) {
            log.error("Error executing Python script: ", e);
            return Map.of(
                    "success", false,
                    "error", "Execution error: " + e.getMessage()
            );
        }
    }

    /**
     * Parse kết quả thành công từ Python script
     */
    private Map<String, Object> parseSuccessResult(String output, Path outputDir) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());

            // Tìm file embedding result
            Path embeddingFile = outputDir.resolve("embedding.json");
            if (Files.exists(embeddingFile)) {
                String embeddingContent = Files.readString(embeddingFile);
                result.put("embedding", embeddingContent);
            }

            // Parse các thông tin khác từ output
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("FACES_PROCESSED:")) {
                    result.put("facesProcessed", Integer.parseInt(line.split(":")[1].trim()));
                } else if (line.startsWith("EMBEDDING_SIZE:")) {
                    result.put("embeddingSize", Integer.parseInt(line.split(":")[1].trim()));
                } else if (line.startsWith("QUALITY_SCORE:")) {
                    result.put("qualityScore", Double.parseDouble(line.split(":")[1].trim()));
                } else if (line.startsWith("PROCESSING_TIME:")) {
                    result.put("processingTime", Double.parseDouble(line.split(":")[1].trim()));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Error parsing Python script result: ", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to parse result: " + e.getMessage()
            );
        }
    }

    /**
     * Kiểm tra trạng thái Python environment
     */
    public Map<String, Object> checkPythonEnvironment() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Kiểm tra Python executable
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    status.put("pythonAvailable", true);
                    status.put("pythonVersion", version);
                }
            } else {
                status.put("pythonAvailable", false);
                status.put("error", "Python not found or timeout");
            }

        } catch (Exception e) {
            status.put("pythonAvailable", false);
            status.put("error", e.getMessage());
        }

        // Kiểm tra script files
        status.put("scriptPath", pythonScriptPath);
        status.put("scriptExists", Files.exists(Paths.get(pythonScriptPath, "extract_features.py")));

        return status;
    }

    /**
     * Xóa dữ liệu embedding của sinh viên
     */
    public void deleteEmbedding(String maSv) {
        try {
            Path studentDir = getStudentDirectory(maSv);
            Path embeddingsDir = studentDir.resolve("embeddings");

            if (Files.exists(embeddingsDir)) {
                Files.walk(embeddingsDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            // Xóa embedding file trong student directory
            Files.deleteIfExists(studentDir.resolve("embeddings.json"));

            log.info("Deleted embedding data for student: {}", maSv);

        } catch (Exception e) {
            log.error("Error deleting embedding for student {}: ", maSv, e);
        }
    }

    /**
     * Lấy đường dẫn thư mục sinh viên
     */
    private Path getStudentDirectory(String maSv) {
        return Paths.get(uploadDir, "students", maSv);
    }

    /**
     * Tạo script Python cơ bản nếu chưa có
     */
    public void initializePythonScripts() {
        try {
            Path scriptDir = Paths.get(pythonScriptPath);
            Files.createDirectories(scriptDir);

            // Tạo extract_features.py cơ bản
            createExtractFeaturesScript(scriptDir);

            log.info("Python scripts initialized at: {}", scriptDir);

        } catch (Exception e) {
            log.error("Error initializing Python scripts: ", e);
        }
    }

    private void createExtractFeaturesScript(Path scriptDir) throws Exception {
        String script = """
            #!/usr/bin/env python3
            import sys
            import argparse
            import json
            import os
            import numpy as np
            
            def extract_features(student_id, faces_dir, output_dir):
                print(f"FACES_PROCESSED:5")
                print(f"EMBEDDING_SIZE:128")
                print(f"QUALITY_SCORE:0.85")
                print(f"PROCESSING_TIME:2.5")
                
                # Dummy embedding data
                embedding = np.random.rand(128).tolist()
                
                # Save embedding
                os.makedirs(output_dir, exist_ok=True)
                with open(os.path.join(output_dir, 'embedding.json'), 'w') as f:
                    json.dump({'embedding': embedding}, f)
                
                return True
            
            if __name__ == "__main__":
                parser = argparse.ArgumentParser()
                parser.add_argument("--student_id", required=True)
                parser.add_argument("--faces_dir", required=True)
                parser.add_argument("--output_dir", required=True)
                parser.add_argument("--format", default="json")
                
                args = parser.parse_args()
                
                success = extract_features(args.student_id, args.faces_dir, args.output_dir)
                sys.exit(0 if success else 1)
            """;

        Files.write(scriptDir.resolve("extract_features.py"), script.getBytes());
    }

}