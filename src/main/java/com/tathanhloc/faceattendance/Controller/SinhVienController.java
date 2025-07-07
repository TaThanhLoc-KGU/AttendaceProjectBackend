package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.SinhVienDTO;
import com.tathanhloc.faceattendance.Service.SinhVienService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tathanhloc.faceattendance.Service.FileUploadService;
import com.tathanhloc.faceattendance.Service.FaceRecognitionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.tathanhloc.faceattendance.Service.ExcelService;
import com.tathanhloc.faceattendance.DTO.ImportResultDTO;

@RestController
@RequestMapping("/api/sinhvien")
@RequiredArgsConstructor
@Slf4j
public class SinhVienController {

    private final SinhVienService sinhVienService;
    private final FileUploadService fileUploadService;
    private final FaceRecognitionService faceRecognitionService;
    private final ExcelService excelService;

    @GetMapping
    public ResponseEntity<Page<SinhVienDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(defaultValue = "maSv") String sortBy,
                                                    @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending()
        );
        return ResponseEntity.ok(sinhVienService.getAllWithPagination(pageable));
    }


    @GetMapping("/active")
    public ResponseEntity<List<SinhVienDTO>> getAllActive() {
        log.info("Lấy danh sách sinh viên đang hoạt động");
        return ResponseEntity.ok(sinhVienService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SinhVienDTO> getById(@PathVariable String id) {
        log.info("Lấy thông tin sinh viên với ID: {}", id);
        return ResponseEntity.ok(sinhVienService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SinhVienDTO> create(@Valid @RequestBody SinhVienDTO dto) {
        log.info("Tạo sinh viên mới: {}", dto);
        return ResponseEntity.ok(sinhVienService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SinhVienDTO> update(@PathVariable String id, @Valid @RequestBody SinhVienDTO dto) {
        log.info("Cập nhật sinh viên với ID {}: {}", id, dto);
        return ResponseEntity.ok(sinhVienService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Xóa sinh viên với ID: {}", id);
        sinhVienService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        log.info("Khôi phục sinh viên với ID: {}", id);
        sinhVienService.restore(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-masv/{maSv}")
    public ResponseEntity<SinhVienDTO> getByMaSv(@PathVariable String maSv) {
        log.info("Tìm sinh viên theo mã: {}", maSv);
        return ResponseEntity.ok(sinhVienService.getByMaSv(maSv));
    }

    /**
     * API lấy tất cả embedding
     * @return Danh sách embedding
     */
    @GetMapping("/embeddings")
    public ResponseEntity<List<Map<String, Object>>> getAllEmbeddings() {
        return ResponseEntity.ok(sinhVienService.getAllEmbeddings());
    }

    /**
     * API lấy embedding của một sinh viên
     * @param maSv Mã sinh viên
     * @return Embedding của sinh viên
     */
    @GetMapping("/students/{maSv}/embedding")
    public ResponseEntity<Map<String, Object>> getEmbeddingByMaSv(@PathVariable String maSv) {
        return ResponseEntity.ok(sinhVienService.getEmbeddingByMaSv(maSv));
    }

    /**
     * API lưu embedding cho một sinh viên
     * @param maSv Mã sinh viên
     * @param requestBody Body chứa embedding
     * @return SinhVienDTO đã cập nhật
     */
    @PostMapping("/students/{maSv}/embedding")
    public ResponseEntity<SinhVienDTO> saveEmbedding(
            @PathVariable String maSv,
            @RequestBody Map<String, String> requestBody) {
        String embedding = requestBody.get("embedding");
        if (embedding == null || embedding.isEmpty()) {
            throw new RuntimeException("Embedding không được để trống");
        }
        return ResponseEntity.ok(sinhVienService.saveEmbedding(maSv, embedding));
    }

    // Thêm endpoint này vào SinhVienController.java

    @GetMapping("/all")
    public ResponseEntity<List<SinhVienDTO>> getAllStudentsNoPagination() {
        log.info("Lấy tất cả sinh viên (không phân trang)");
        return ResponseEntity.ok(sinhVienService.getAll());
    }
    /**
     * Upload ảnh đại diện sinh viên
     */
    @PostMapping("/{maSv}/upload-profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @PathVariable String maSv,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading profile image for student: {}", maSv);

            // Validate file
            if (!fileUploadService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File ảnh không hợp lệ"));
            }

            String imageUrl = fileUploadService.saveStudentProfileImage(maSv, file);

            // Cập nhật đường dẫn ảnh trong database
            SinhVienDTO sinhVien = sinhVienService.getByMaSv(maSv);
            sinhVien.setHinhAnh(imageUrl);
            sinhVienService.update(maSv, sinhVien);

            return ResponseEntity.ok(Map.of("url", imageUrl));

        } catch (Exception e) {
            log.error("Error uploading profile image for student {}: ", maSv, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tải lên ảnh: " + e.getMessage()));
        }
    }

    /**
     * Upload ảnh khuôn mặt cho trích xuất đặc trưng
     */
    @PostMapping("/{maSv}/upload-face-image")
    public ResponseEntity<Map<String, Object>> uploadFaceImage(
            @PathVariable String maSv,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading face image for student: {}", maSv);

            // Validate file
            if (!fileUploadService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File ảnh không hợp lệ"));
            }

            // Kiểm tra số lượng ảnh đã có
            int currentCount = fileUploadService.getFaceImageCount(maSv);
            if (currentCount >= 5) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Đã đạt giới hạn 5 ảnh khuôn mặt"));
            }

            String imageUrl = fileUploadService.saveFaceImage(maSv, file);

            return ResponseEntity.ok(Map.of(
                    "id", System.currentTimeMillis(), // Temporary ID
                    "url", imageUrl,
                    "filename", file.getOriginalFilename(),
                    "currentCount", currentCount + 1
            ));

        } catch (Exception e) {
            log.error("Error uploading face image for student {}: ", maSv, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tải lên ảnh khuôn mặt: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách ảnh khuôn mặt của sinh viên
     */
    @GetMapping("/{maSv}/face-images")
    public ResponseEntity<List<Map<String, Object>>> getFaceImages(@PathVariable String maSv) {
        try {
            log.info("Getting face images for student: {}", maSv);
            List<Map<String, Object>> faceImages = fileUploadService.getFaceImages(maSv);
            return ResponseEntity.ok(faceImages);
        } catch (Exception e) {
            log.error("Error loading face images for student {}: ", maSv, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xóa ảnh khuôn mặt
     */
    @DeleteMapping("/{maSv}/face-image/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFaceImage(
            @PathVariable String maSv,
            @PathVariable String filename) {
        try {
            log.info("Deleting face image for student {}: {}", maSv, filename);
            fileUploadService.deleteFaceImage(maSv, filename);

            int remainingCount = fileUploadService.getFaceImageCount(maSv);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa ảnh thành công",
                    "remainingCount", remainingCount
            ));
        } catch (Exception e) {
            log.error("Error deleting face image {}/{}: ", maSv, filename, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể xóa ảnh: " + e.getMessage()));
        }
    }

    /**
     * API trích xuất đặc trưng khuôn mặt
     */
    @PostMapping("/{maSv}/extract-features")
    public ResponseEntity<Map<String, Object>> extractFeatures(@PathVariable String maSv) {
        try {
            log.info("Extracting features for student: {}", maSv);

            // Kiểm tra sinh viên tồn tại
            SinhVienDTO sinhVien = sinhVienService.getByMaSv(maSv);

            // Kiểm tra có ảnh khuôn mặt không
            List<Map<String, Object>> faceImages = fileUploadService.getFaceImages(maSv);
            if (faceImages.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Không có ảnh khuôn mặt để trích xuất đặc trưng"));
            }

            // TODO: Gọi Python service để trích xuất đặc trưng
            // String embedding = pythonFeatureExtractionService.extractFeatures(maSv);

            // Tạm thời trả về mock data
            String mockEmbedding = "mock_embedding_" + System.currentTimeMillis();
            sinhVienService.saveEmbedding(maSv, mockEmbedding);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Trích xuất đặc trưng thành công",
                    "faceCount", faceImages.size()
            ));

        } catch (Exception e) {
            log.error("Error extracting features for student {}: ", maSv, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể trích xuất đặc trưng: " + e.getMessage()));
        }
    }

    /**
     * Import sinh viên từ Excel
     */
    @PostMapping("/import-excel")
    public ResponseEntity<ImportResultDTO> importFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean createAccounts) {
        try {
            log.info("Importing students from Excel file: {}", file.getOriginalFilename());

            ImportResultDTO result = excelService.importStudentsFromExcel(file, createAccounts);

            if (result.getFailedCount() == 0) {
                return ResponseEntity.ok(result);
            } else if (result.getSuccessCount() > 0) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("Error importing from Excel: ", e);
            ImportResultDTO errorResult = new ImportResultDTO();
            errorResult.setSuccessCount(0);
            errorResult.setFailedCount(1);
            errorResult.setTotalProcessed(1);
            errorResult.setErrors(List.of("Không thể xử lý file Excel: " + e.getMessage()));
            errorResult.setTimestamp(java.time.LocalDateTime.now());
            errorResult.setStatus("ERROR");

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }


    /**
     * Export danh sách sinh viên ra Excel
     */
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String classFilter,
            @RequestParam(required = false) String status,
            HttpServletResponse response) {
        try {
            log.info("Exporting students to Excel");

            byte[] excelData = excelService.exportStudentsToExcel(search, classFilter, status);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "danh-sach-sinh-vien.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("Error exporting to Excel: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            log.info("Generating Excel import template");

            byte[] template = excelService.generateImportTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "student-import-template.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(template);

        } catch (Exception e) {
            log.error("Error generating template: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy thống kê sinh viên
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            log.info("Getting student statistics");

            List<SinhVienDTO> allStudents = sinhVienService.getAll();

            long totalStudents = allStudents.size();
            long activeStudents = allStudents.stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                    .count();
            long studentsWithPhoto = allStudents.stream()
                    .filter(s -> s.getHinhAnh() != null && !s.getHinhAnh().isEmpty())
                    .count();
            long studentsWithEmbedding = allStudents.stream()
                    .filter(s -> s.getEmbedding() != null && !s.getEmbedding().isEmpty())
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", totalStudents);
            stats.put("activeStudents", activeStudents);
            stats.put("studentsWithPhoto", studentsWithPhoto);
            stats.put("studentsWithEmbedding", studentsWithEmbedding);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting statistics: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tìm kiếm sinh viên với filter nâng cao
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SinhVienDTO>> searchWithFilters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String classFilter,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "maSv") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        try {
            log.info("Searching students with filters - search: {}, class: {}, status: {}",
                    search, classFilter, status);

            // TODO: Implement advanced search logic with filters
            // For now, use existing pagination endpoint
            return getAll(page, size, sortBy, direction);

        } catch (Exception e) {
            log.error("Error searching students: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Bulk operations - Xóa nhiều sinh viên
     */
    @PostMapping("/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDelete(@RequestBody List<String> studentIds) {
        try {
            log.info("Bulk deleting students: {}", studentIds);

            int deletedCount = 0;
            List<String> errors = new ArrayList<>();

            for (String maSv : studentIds) {
                try {
                    sinhVienService.softDelete(maSv);
                    // Xóa thư mục sinh viên
                    fileUploadService.deleteStudentDirectory(maSv);
                    deletedCount++;
                } catch (Exception e) {
                    errors.add("Không thể xóa sinh viên " + maSv + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "deletedCount", deletedCount,
                    "totalRequested", studentIds.size(),
                    "errors", errors,
                    "message", "Đã xóa " + deletedCount + "/" + studentIds.size() + " sinh viên"
            ));

        } catch (Exception e) {
            log.error("Error bulk deleting students: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể thực hiện xóa hàng loạt: " + e.getMessage()));
        }
    }

    /**
     * Bulk operations - Cập nhật trạng thái nhiều sinh viên
     */
    @PostMapping("/bulk-update-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> studentIds = (List<String>) request.get("studentIds");
            Boolean isActive = (Boolean) request.get("isActive");

            log.info("Bulk updating status for students: {} to {}", studentIds, isActive);

            int updatedCount = 0;
            List<String> errors = new ArrayList<>();

            for (String maSv : studentIds) {
                try {
                    SinhVienDTO sinhVien = sinhVienService.getByMaSv(maSv);
                    sinhVien.setIsActive(isActive);
                    sinhVienService.update(maSv, sinhVien);
                    updatedCount++;
                } catch (Exception e) {
                    errors.add("Không thể cập nhật sinh viên " + maSv + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "updatedCount", updatedCount,
                    "totalRequested", studentIds.size(),
                    "errors", errors,
                    "message", "Đã cập nhật " + updatedCount + "/" + studentIds.size() + " sinh viên"
            ));

        } catch (Exception e) {
            log.error("Error bulk updating status: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể thực hiện cập nhật hàng loạt: " + e.getMessage()));
        }


    }

    /**
     * Kiểm tra trạng thái Python environment
     */
    @GetMapping("/check-python-environment")
    public ResponseEntity<Map<String, Object>> checkPythonEnvironment() {
        try {
            log.info("Checking Python environment");
            Map<String, Object> status = faceRecognitionService.checkPythonEnvironment();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error checking Python environment: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể kiểm tra Python environment: " + e.getMessage()));
        }
    }

    /**
     * Khởi tạo Python scripts
     */
    @PostMapping("/initialize-python-scripts")
    public ResponseEntity<Map<String, Object>> initializePythonScripts() {
        try {
            log.info("Initializing Python scripts");
            faceRecognitionService.initializePythonScripts();
            return ResponseEntity.ok(Map.of(
                    "message", "Python scripts đã được khởi tạo thành công"
            ));
        } catch (Exception e) {
            log.error("Error initializing Python scripts: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể khởi tạo Python scripts: " + e.getMessage()));
        }
    }

    /**
     * Lấy tất cả sinh viên đang hoạt động (không phân trang)
     */
    @GetMapping("/active/all")
    public ResponseEntity<List<SinhVienDTO>> getAllActiveStudents() {
        log.info("Lấy tất cả sinh viên đang hoạt động (không phân trang)");
        return ResponseEntity.ok(sinhVienService.getAllActive());
    }
}

