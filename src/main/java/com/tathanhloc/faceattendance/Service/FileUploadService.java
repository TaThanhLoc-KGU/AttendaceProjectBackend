package com.tathanhloc.faceattendance.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:5242880}") // 5MB
    private long maxFileSize;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    /**
     * Tạo thư mục cho sinh viên
     */
    public void createStudentDirectory(String maSv) {
        try {
            Path studentDir = getStudentDirectory(maSv);
            Path facesDir = studentDir.resolve("faces");

            Files.createDirectories(studentDir);
            Files.createDirectories(facesDir);

            log.info("Created directories for student: {}", maSv);
        } catch (IOException e) {
            log.error("Error creating directories for student {}: ", maSv, e);
            throw new RuntimeException("Không thể tạo thư mục cho sinh viên", e);
        }
    }

    /**
     * Xóa thư mục và tất cả file của sinh viên
     */
    public void deleteStudentDirectory(String maSv) {
        try {
            Path studentDir = getStudentDirectory(maSv);
            if (Files.exists(studentDir)) {
                deleteDirectoryRecursively(studentDir);
                log.info("Deleted directory for student: {}", maSv);
            }
        } catch (IOException e) {
            log.error("Error deleting directory for student {}: ", maSv, e);
        }
    }

    /**
     * Lưu ảnh đại diện sinh viên
     */
    public String saveStudentProfileImage(String maSv, MultipartFile file) {
        try {
            validateImageFile(file);

            Path studentDir = getStudentDirectory(maSv);
            Files.createDirectories(studentDir);

            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = "profile" + fileExtension;
            Path filePath = studentDir.resolve(fileName);

            // Xóa ảnh cũ nếu có
            deleteExistingProfileImage(studentDir);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "/uploads/students/" + maSv + "/" + fileName;
            log.info("Saved profile image for student {}: {}", maSv, relativePath);

            return relativePath;

        } catch (IOException e) {
            log.error("Error saving profile image for student {}: ", maSv, e);
            throw new RuntimeException("Không thể lưu ảnh đại diện", e);
        }
    }

    /**
     * Lưu ảnh khuôn mặt để trích xuất đặc trưng
     */
    public String saveFaceImage(String maSv, MultipartFile file) {
        try {
            validateImageFile(file);

            Path facesDir = getStudentDirectory(maSv).resolve("faces");
            Files.createDirectories(facesDir);

            // Đếm số ảnh hiện tại
            int currentCount = getFaceImageCount(maSv);
            if (currentCount >= 5) {
                throw new RuntimeException("Đã đạt giới hạn 5 ảnh khuôn mặt");
            }

            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = "face_" + (currentCount + 1) + fileExtension;
            Path filePath = facesDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "/uploads/students/" + maSv + "/faces/" + fileName;
            log.info("Saved face image for student {}: {}", maSv, relativePath);

            return relativePath;

        } catch (IOException e) {
            log.error("Error saving face image for student {}: ", maSv, e);
            throw new RuntimeException("Không thể lưu ảnh khuôn mặt", e);
        }
    }

    /**
     * Lấy danh sách ảnh khuôn mặt của sinh viên
     */
    public List<Map<String, Object>> getFaceImages(String maSv) {
        List<Map<String, Object>> images = new ArrayList<>();

        try {
            Path facesDir = getStudentDirectory(maSv).resolve("faces");
            if (!Files.exists(facesDir)) {
                return images;
            }

            try (Stream<Path> files = Files.list(facesDir)) {
                files.filter(Files::isRegularFile)
                        .filter(this::isImageFile)
                        .sorted()
                        .forEach(file -> {
                            Map<String, Object> imageInfo = new HashMap<>();
                            imageInfo.put("id", file.getFileName().toString());
                            imageInfo.put("url", "/uploads/students/" + maSv + "/faces/" + file.getFileName());
                            imageInfo.put("filename", file.getFileName().toString());
                            imageInfo.put("size", getFileSize(file));
                            images.add(imageInfo);
                        });
            }

        } catch (IOException e) {
            log.error("Error loading face images for student {}: ", maSv, e);
        }

        return images;
    }

    /**
     * Đếm số ảnh khuôn mặt hiện tại
     */
    public int getFaceImageCount(String maSv) {
        try {
            Path facesDir = getStudentDirectory(maSv).resolve("faces");
            if (!Files.exists(facesDir)) {
                return 0;
            }

            try (Stream<Path> files = Files.list(facesDir)) {
                return (int) files.filter(Files::isRegularFile)
                        .filter(this::isImageFile)
                        .count();
            }

        } catch (IOException e) {
            log.error("Error counting face images for student {}: ", maSv, e);
            return 0;
        }
    }

    /**
     * Xóa ảnh khuôn mặt
     */
    public void deleteFaceImage(String maSv, String filename) {
        try {
            Path filePath = getStudentDirectory(maSv).resolve("faces").resolve(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted face image: {}/{}", maSv, filename);
            }
        } catch (IOException e) {
            log.error("Error deleting face image {}/{}: ", maSv, filename, e);
            throw new RuntimeException("Không thể xóa ảnh khuôn mặt", e);
        }
    }

    /**
     * Lưu file embedding
     */
    public void saveEmbeddingFile(String maSv, String embeddingData) {
        try {
            Path studentDir = getStudentDirectory(maSv);
            Files.createDirectories(studentDir);

            Path embeddingFile = studentDir.resolve("embeddings.json");

            Map<String, Object> embeddingInfo = new HashMap<>();
            embeddingInfo.put("maSv", maSv);
            embeddingInfo.put("embedding", embeddingData);
            embeddingInfo.put("createdAt", LocalDateTime.now().toString());
            embeddingInfo.put("faceCount", getFaceImageCount(maSv));

            String jsonData = convertToJson(embeddingInfo);
            Files.write(embeddingFile, jsonData.getBytes());

            log.info("Saved embedding file for student: {}", maSv);

        } catch (IOException e) {
            log.error("Error saving embedding file for student {}: ", maSv, e);
            throw new RuntimeException("Không thể lưu file embedding", e);
        }
    }

    /**
     * Kiểm tra file có phải ảnh hợp lệ không
     */
    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return false;
        }

        return file.getSize() <= maxFileSize;
    }

    /**
     * Validate file ảnh và throw exception nếu không hợp lệ
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Kích thước file vượt quá giới hạn cho phép");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ");
        }
    }

    /**
     * Lấy đường dẫn thư mục của sinh viên
     */
    private Path getStudentDirectory(String maSv) {
        return Paths.get(uploadDir, "students", maSv);
    }

    /**
     * Lấy extension của file
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * Kiểm tra file có phải ảnh không
     */
    private boolean isImageFile(Path file) {
        try {
            String contentType = Files.probeContentType(file);
            return contentType != null && contentType.startsWith("image/");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Lấy kích thước file
     */
    private long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Xóa ảnh đại diện cũ
     */
    private void deleteExistingProfileImage(Path studentDir) throws IOException {
        try (Stream<Path> files = Files.list(studentDir)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().startsWith("profile"))
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            log.warn("Could not delete old profile image: {}", file);
                        }
                    });
        }
    }

    /**
     * Xóa thư mục đệ quy
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Convert object to JSON string (simplified)
     */
    private String convertToJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}