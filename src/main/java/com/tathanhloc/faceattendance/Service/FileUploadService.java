package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.Model.FaceImage;
import com.tathanhloc.faceattendance.Model.SinhVien;
import com.tathanhloc.faceattendance.Repository.FaceImageRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    private final FaceImageRepository faceImageRepository;
    private final SinhVienRepository sinhVienRepository;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // ================== FILE VALIDATION ==================

    /**
     * Validate image file
     */
    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File size exceeds limit: {} bytes", file.getSize());
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    // ================== PROFILE IMAGE METHODS ==================

    /**
     * Save student profile image
     */
    @Transactional
    public String saveStudentProfileImage(String maSv, MultipartFile file) throws IOException {
        String filename = "profile_" + maSv + "_" + System.currentTimeMillis() + "." + getFileExtension(file.getOriginalFilename());
        Path uploadPath = Paths.get(uploadDir, "students", maSv, "profile");

        Files.createDirectories(uploadPath);

        // Delete old profile images if exists
        deleteOldProfileImages(uploadPath);

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = urlPrefix + "/students/" + maSv + "/profile/" + filename;

        // Update student profile
        SinhVien sinhVien = sinhVienRepository.findById(maSv)
                .orElseThrow(() -> new RuntimeException("Sinh viên không tồn tại"));
        sinhVien.setHinhAnh(imageUrl);
        sinhVienRepository.save(sinhVien);

        log.info("Saved profile image for student {}: {}", maSv, filename);
        return imageUrl;
    }

    /**
     * Delete student profile image
     */
    @Transactional
    public void deleteStudentProfileImage(String maSv) throws IOException {
        SinhVien sinhVien = sinhVienRepository.findById(maSv)
                .orElseThrow(() -> new RuntimeException("Sinh viên không tồn tại"));

        if (sinhVien.getHinhAnh() != null && !sinhVien.getHinhAnh().isEmpty()) {
            // Extract filename from URL
            String imageUrl = sinhVien.getHinhAnh();
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            // Delete physical file
            Path filePath = Paths.get(uploadDir, "students", maSv, "profile", filename);
            Files.deleteIfExists(filePath);

            // Update database
            sinhVien.setHinhAnh(null);
            sinhVienRepository.save(sinhVien);

            log.info("Deleted profile image for student {}", maSv);
        }
    }

    /**
     * Delete old profile images in directory
     */
    private void deleteOldProfileImages(Path profileDir) throws IOException {
        if (Files.exists(profileDir)) {
            Files.list(profileDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            log.warn("Failed to delete old profile image: {}", file, e);
                        }
                    });
        }
    }

    // ================== FACE IMAGE METHODS ==================

    /**
     * Save face image with slot index
     */
    @Transactional
    public String saveFaceImage(String maSv, MultipartFile file, Integer slotIndex) throws IOException {
        // Validate slot before saving
        validateSlotAvailability(maSv, slotIndex);

        SinhVien sinhVien = sinhVienRepository.findById(maSv)
                .orElseThrow(() -> new RuntimeException("Sinh viên không tồn tại"));

        // Check if slot is occupied and delete existing image
        Optional<FaceImage> existingImage = faceImageRepository.findByMaSvAndSlotIndex(maSv, slotIndex);
        if (existingImage.isPresent()) {
            // Delete existing image in this slot
            deleteFaceImageEntity(existingImage.get());
        }

        // Generate filename
        String filename = "face_" + maSv + "_slot" + slotIndex + "_" + System.currentTimeMillis() + "." + getFileExtension(file.getOriginalFilename());
        Path uploadPath = Paths.get(uploadDir, "students", maSv, "faces");

        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = urlPrefix + "/students/" + maSv + "/faces/" + filename;

        // Save to database
        FaceImage faceImage = FaceImage.builder()
                .filename(filename)
                .filePath(filePath.toString())
                .slotIndex(slotIndex)
                .sinhVien(sinhVien)
                .isActive(true)
                .build();

        faceImageRepository.save(faceImage);

        log.info("Saved face image for student {} at slot {}: {}", maSv, slotIndex, filename);
        return imageUrl;
    }

    /**
     * Save face image without slot index (backward compatibility)
     * Will find first empty slot automatically
     */
    @Transactional
    public String saveFaceImage(String maSv, MultipartFile file) throws IOException {
        // Find first empty slot
        Integer emptySlot = findFirstEmptySlot(maSv);

        if (emptySlot == null) {
            throw new RuntimeException("Đã đạt giới hạn 5 ảnh khuôn mặt");
        }

        return saveFaceImage(maSv, file, emptySlot);
    }

    /**
     * Save face image và trả về đầy đủ thông tin
     */
    @Transactional
    public Map<String, Object> saveFaceImageWithDetails(String maSv, MultipartFile file, Integer slotIndex) throws IOException {
        String imageUrl = saveFaceImage(maSv, file, slotIndex);
        FaceImage savedImage = getFaceImageBySlot(maSv, slotIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("url", imageUrl);
        if (savedImage != null) {
            result.put("id", savedImage.getId());
            result.put("filename", savedImage.getFilename());
            result.put("slotIndex", savedImage.getSlotIndex());
            result.put("filePath", savedImage.getFilePath());
            result.put("createdAt", savedImage.getCreatedAt());
        }

        return result;
    }

    /**
     * Get face images with slot information
     */
    public List<Map<String, Object>> getFaceImages(String maSv) {
        List<FaceImage> faceImages = faceImageRepository.findByMaSvAndActive(maSv);

        return faceImages.stream()
                .sorted(Comparator.comparing(FaceImage::getSlotIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(image -> {
                    Map<String, Object> imageMap = new HashMap<>();
                    imageMap.put("id", image.getId());
                    imageMap.put("filename", image.getFilename());
                    imageMap.put("url", urlPrefix + "/students/" + maSv + "/faces/" + image.getFilename());
                    imageMap.put("slotIndex", image.getSlotIndex());
                    imageMap.put("createdAt", image.getCreatedAt());
                    return imageMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get face images with slot-based mapping
     */
    public Map<String, Object> getFaceImagesWithSlotMapping(String maSv) {
        List<FaceImage> faceImageEntities = getFaceImagesEntities(maSv);

        // Create slot-based mapping
        Map<Integer, Map<String, Object>> slotMapping = new HashMap<>();
        List<Map<String, Object>> imagesList = new ArrayList<>();

        for (FaceImage img : faceImageEntities) {
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("id", img.getId());
            imageData.put("filename", img.getFilename());
            imageData.put("url", urlPrefix + "/students/" + maSv + "/faces/" + img.getFilename());
            imageData.put("slotIndex", img.getSlotIndex());
            imageData.put("createdAt", img.getCreatedAt());

            // Add to slot mapping
            if (img.getSlotIndex() != null) {
                slotMapping.put(img.getSlotIndex(), imageData);
            }

            // Add to list (for backward compatibility)
            imagesList.add(imageData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("images", imagesList);
        result.put("slots", slotMapping);
        result.put("count", faceImageEntities.size());
        result.put("maxCount", 5);

        return result;
    }

    /**
     * Delete face image by filename
     */
    @Transactional
    public void deleteFaceImage(String maSv, String filename) throws IOException {
        FaceImage faceImage = faceImageRepository.findByMaSvAndFilename(maSv, filename)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));

        deleteFaceImageEntity(faceImage);
    }

    /**
     * Delete face image by ID (more secure)
     */
    @Transactional
    public void deleteFaceImageById(String maSv, Long imageId) throws IOException {
        FaceImage faceImage = faceImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));

        // Check ownership
        if (!faceImage.getSinhVien().getMaSv().equals(maSv)) {
            throw new RuntimeException("Không có quyền xóa ảnh này");
        }

        deleteFaceImageEntity(faceImage);
    }

    /**
     * Delete face image entity and file
     */
    private void deleteFaceImageEntity(FaceImage faceImage) throws IOException {
        // Soft delete in database
        faceImage.setIsActive(false);
        faceImageRepository.save(faceImage);

        // Delete physical file
        Path filePath = Paths.get(faceImage.getFilePath());
        Files.deleteIfExists(filePath);

        log.info("Deleted face image: {}", faceImage.getFilename());
    }

    // ================== SLOT MANAGEMENT METHODS ==================

    /**
     * Tìm slot trống đầu tiên
     */
    public Integer findFirstEmptySlot(String maSv) {
        List<Integer> usedSlots = faceImageRepository.findUsedSlotsByMaSv(maSv);

        for (int i = 0; i < 5; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }

        return null; // Không có slot trống
    }

    /**
     * Validate slot availability
     */
    public void validateSlotAvailability(String maSv, Integer slotIndex) {
        if (slotIndex < 0 || slotIndex > 4) {
            throw new IllegalArgumentException("Slot index phải từ 0 đến 4");
        }

        if (!isSlotEmpty(maSv, slotIndex)) {
            log.warn("Slot {} for student {} is already occupied, will replace existing image", slotIndex, maSv);
            // Note: We don't throw exception here, just log warning
            // The existing image will be replaced in saveFaceImage method
        }
    }

    /**
     * Check if slot is empty
     */
    public boolean isSlotEmpty(String maSv, Integer slotIndex) {
        return faceImageRepository.isSlotEmpty(maSv, slotIndex);
    }

    /**
     * Get face image count
     */
    public int getFaceImageCount(String maSv) {
        return faceImageRepository.countActiveByMaSv(maSv);
    }

    /**
     * Get FaceImage entity by slot
     */
    public FaceImage getFaceImageBySlot(String maSv, Integer slotIndex) {
        return faceImageRepository.findByMaSvAndSlotIndex(maSv, slotIndex)
                .orElse(null);
    }

    /**
     * Get list of FaceImage entities
     */
    public List<FaceImage> getFaceImagesEntities(String maSv) {
        return faceImageRepository.findByMaSvAndActive(maSv);
    }

    // ================== DIRECTORY MANAGEMENT METHODS ==================

    /**
     * Create student directory structure
     */
    public void createStudentDirectory(String maSv) throws IOException {
        Path studentDir = Paths.get(uploadDir, "students", maSv);
        Path profileDir = studentDir.resolve("profile");
        Path facesDir = studentDir.resolve("faces");
        Path embeddingDir = studentDir.resolve("embeddings");

        Files.createDirectories(profileDir);
        Files.createDirectories(facesDir);
        Files.createDirectories(embeddingDir);

        log.info("Created directory structure for student: {}", maSv);
    }

    /**
     * Delete entire student directory
     */
    public void deleteStudentDirectory(String maSv) throws IOException {
        Path studentDir = Paths.get(uploadDir, "students", maSv);

        if (Files.exists(studentDir)) {
            // Delete all files and subdirectories recursively
            Files.walk(studentDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Failed to delete: {}", path, e);
                        }
                    });

            log.info("Deleted directory structure for student: {}", maSv);
        }
    }

    // ================== EMBEDDING METHODS ==================

    /**
     * Save embedding file for face recognition
     */
    public void saveEmbeddingFile(String maSv, String embeddingData) throws IOException {
        Path embeddingPath = Paths.get(uploadDir, "students", maSv, "embeddings");
        Files.createDirectories(embeddingPath);

        String filename = "embedding_" + System.currentTimeMillis() + ".txt";
        Path filePath = embeddingPath.resolve(filename);

        Files.writeString(filePath, embeddingData);

        log.info("Saved embedding file for student {}: {}", maSv, filename);
    }

    // ================== UTILITY METHODS ==================

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * Clean up orphaned files (files without database records)
     * This is a maintenance method
     */
    @Transactional
    public void cleanupOrphanedFiles() throws IOException {
        Path studentsDir = Paths.get(uploadDir, "students");

        if (!Files.exists(studentsDir)) {
            return;
        }

        Files.list(studentsDir).forEach(studentDir -> {
            try {
                String maSv = studentDir.getFileName().toString();

                // Check if student exists
                if (!sinhVienRepository.existsById(maSv)) {
                    deleteStudentDirectory(maSv);
                    log.warn("Deleted orphaned directory for non-existent student: {}", maSv);
                } else {
                    // Clean up face images
                    Path facesDir = studentDir.resolve("faces");
                    if (Files.exists(facesDir)) {
                        List<FaceImage> dbImages = faceImageRepository.findByMaSvAndActive(maSv);
                        Set<String> dbFilenames = dbImages.stream()
                                .map(FaceImage::getFilename)
                                .collect(Collectors.toSet());

                        Files.list(facesDir).forEach(file -> {
                            String filename = file.getFileName().toString();
                            if (!dbFilenames.contains(filename)) {
                                try {
                                    Files.delete(file);
                                    log.info("Deleted orphaned face image: {}", file);
                                } catch (IOException e) {
                                    log.error("Failed to delete orphaned file: {}", file, e);
                                }
                            }
                        });
                    }
                }
            } catch (IOException e) {
                log.error("Error processing student directory: {}", studentDir, e);
            }
        });
    }

    /**
     * Get upload statistics
     */
    public Map<String, Object> getUploadStatistics(String maSv) {
        Map<String, Object> stats = new HashMap<>();

        // Face images stats
        int faceImageCount = getFaceImageCount(maSv);
        List<Integer> usedSlots = faceImageRepository.findUsedSlotsByMaSv(maSv);

        stats.put("faceImageCount", faceImageCount);
        stats.put("maxFaceImages", 5);
        stats.put("remainingSlots", 5 - faceImageCount);
        stats.put("usedSlots", usedSlots);
        stats.put("canUploadMore", faceImageCount < 5);

        // Profile image stats
        try {
            SinhVien sinhVien = sinhVienRepository.findById(maSv).orElse(null);
            stats.put("hasProfileImage", sinhVien != null && sinhVien.getHinhAnh() != null && !sinhVien.getHinhAnh().isEmpty());
        } catch (Exception e) {
            stats.put("hasProfileImage", false);
        }

        return stats;
    }
}