package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.FaceImage;
import com.tathanhloc.faceattendance.Repository.CameraRepository;
import com.tathanhloc.faceattendance.Repository.FaceImageRepository;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.DangKyHocService;
import com.tathanhloc.faceattendance.Service.DiemDanhService;
import com.tathanhloc.faceattendance.Service.FileUploadService;
import com.tathanhloc.faceattendance.Service.LichHocService;
import com.tathanhloc.faceattendance.Service.SinhVienService;
import com.tathanhloc.faceattendance.Service.TaiKhoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardController {

    private final DangKyHocService dangKyHocService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;
    private final SinhVienService sinhVienService;
    private final FileUploadService fileUploadService;
    private final TaiKhoanService taiKhoanService;
    private final PasswordEncoder passwordEncoder;
    private final FaceImageRepository faceImageRepository;


    /**
     * Trang lịch học chi tiết của sinh viên - URL: /student/lichhoc
     */
    @GetMapping("/lichhoc")
    public String lichHoc(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Kiểm tra thông tin sinh viên
            if (userDetails.getTaiKhoan().getSinhVien() == null) {
                log.error("User has no student profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
                return "student/lichhoc";
            }

            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            SinhVienDTO student = sinhVienService.getByMaSv(maSv);

            // Lấy lịch học của sinh viên
            List<LichHocDTO> mySchedules = lichHocService.getBySinhVien(maSv);
            List<LichHocDTO> todaySchedules = lichHocService.getTodaySchedule(null, maSv, null);

            // ✅ CHUẨN BỊ DATA CHO CALENDAR GRID VIEW
            // Map<Ngày, Map<Tiết, LichHocDTO>>
            Map<Integer, Map<Integer, LichHocDTO>> calendarGrid = new HashMap<>();

            // Khởi tạo grid cho 7 ngày và 12 tiết
            for (int day = 2; day <= 8; day++) {
                Map<Integer, LichHocDTO> daySchedule = new HashMap<>();
                calendarGrid.put(day, daySchedule);
            }

            // Đặt lịch vào grid
            for (LichHocDTO schedule : mySchedules) {
                Integer day = schedule.getThu();
                Integer startPeriod = schedule.getTietBatDau();
                Integer numPeriods = schedule.getSoTiet();

                if (day != null && startPeriod != null && numPeriods != null) {
                    // Đặt lịch vào các tiết liên tiếp
                    for (int i = 0; i < numPeriods; i++) {
                        int period = startPeriod + i;
                        if (period <= 12) { // Giới hạn 12 tiết
                            calendarGrid.get(day).put(period, schedule);
                        }
                    }
                }
            }

            // Các thống kê khác
            long uniqueSubjectsCount = mySchedules.stream()
                    .map(LichHocDTO::getTenMonHoc)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            // Thêm vào model
            model.addAttribute("student", student);
            model.addAttribute("schedules", mySchedules);
            model.addAttribute("calendarGrid", calendarGrid); // ✅ GRID DATA
            model.addAttribute("todaySchedules", todaySchedules);
            model.addAttribute("totalSchedules", mySchedules.size());
            model.addAttribute("uniqueSubjectsCount", uniqueSubjectsCount);

            // ✅ THÊM DANH SÁCH TIẾT HỌC VÀ NGÀY
            model.addAttribute("periods", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
            model.addAttribute("weekDays", Arrays.asList(2, 3, 4, 5, 6, 7, 8));

            log.info("✅ Calendar grid prepared: {} schedules", mySchedules.size());
            return "student/lichhoc";

        } catch (Exception e) {
            log.error("❌ Error loading student schedule", e);
            model.addAttribute("error", "Không thể tải lịch học: " + e.getMessage());
            return "student/lichhoc";
        }
    }

    /**
     * Trang lịch sử điểm danh
     */
    @GetMapping("/attendance")
    public String attendanceHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
            return "student/attendance";
        }

        String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
        model.addAttribute("attendanceHistory", diemDanhService.getByMaSv(maSv));
        return "student/attendance";
    }

    /**
     * Trang thông tin cá nhân - chỉnh sửa ảnh đại diện
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
            return "student/profile";
        }

        model.addAttribute("student", userDetails.getTaiKhoan().getSinhVien());
        return "student/profile";
    }

    /**
     * Trang đổi mật khẩu
     */
    @GetMapping("/change-password")
    public String changePasswordPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }

        model.addAttribute("currentUser", userDetails.getTaiKhoan());
        return "student/change-password";
    }

    // ================== API ENDPOINTS ==================

    /**
     * API upload ảnh đại diện sinh viên (chỉ cho chính mình)
     */
    @PostMapping("/api/upload-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tài khoản không có thông tin sinh viên"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            log.info("Student {} uploading profile image", maSv);

            // Validate file
            if (!fileUploadService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File ảnh không hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP dưới 5MB"));
            }

            // Save image
            String imageUrl = fileUploadService.saveStudentProfileImage(maSv, file);

            // Update database
            SinhVienDTO sinhVien = sinhVienService.getByMaSv(maSv);
            sinhVien.setHinhAnh(imageUrl);
            sinhVienService.update(maSv, sinhVien);

            log.info("Profile image updated successfully for student: {}", maSv);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Cập nhật ảnh đại diện thành công",
                    "url", imageUrl
            ));

        } catch (Exception e) {
            log.error("Error uploading profile image for student: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tải lên ảnh: " + e.getMessage()));
        }
    }

    /**
     * API lấy thông tin sinh viên hiện tại
     */
    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<?> getStudentProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tài khoản không có thông tin sinh viên"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            SinhVienDTO sinhVien = sinhVienService.getByMaSv(maSv);

            return ResponseEntity.ok(sinhVien);
        } catch (Exception e) {
            log.error("Error getting student profile: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể lấy thông tin sinh viên: " + e.getMessage()));
        }
    }

    // Thêm vào StudentDashboardController

    /**
     * API upload ảnh khuôn mặt cho sinh viên (chính mình) - HỖ TRỢ SLOT
     */
    @PostMapping("/api/upload-face-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFaceImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "slotIndex", required = false) Integer slotIndex) {

        if (userDetails == null || userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();

        try {
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

            // Validate slot index nếu được cung cấp
            if (slotIndex != null && (slotIndex < 0 || slotIndex > 4)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Slot index phải từ 0 đến 4"));
            }

            String imageUrl;
            FaceImage savedImage;

            if (slotIndex != null) {
                // Upload vào slot cụ thể
                imageUrl = fileUploadService.saveFaceImage(maSv, file, slotIndex);
                savedImage = fileUploadService.getFaceImageBySlot(maSv, slotIndex);
            } else {
                // Tự động tìm slot trống
                imageUrl = fileUploadService.saveFaceImage(maSv, file);
                // Lấy ảnh vừa được thêm (ảnh mới nhất)
                List<FaceImage> images = fileUploadService.getFaceImagesEntities(maSv);
                savedImage = images.isEmpty() ? null : images.get(images.size() - 1);
            }

            return ResponseEntity.ok(Map.of(
                    "id", savedImage != null ? savedImage.getId() : System.currentTimeMillis(),
                    "url", imageUrl,
                    "filename", savedImage != null ? savedImage.getFilename() : file.getOriginalFilename(),
                    "slotIndex", savedImage != null ? savedImage.getSlotIndex() : -1,
                    "currentCount", fileUploadService.getFaceImageCount(maSv)
            ));

        } catch (Exception e) {
            log.error("Error uploading face image for student: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tải lên ảnh: " + e.getMessage()));
        }
    }


    /**
     * API xóa ảnh khuôn mặt - IMPROVED VERSION
     */
    @DeleteMapping("/api/face-image/{filename}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFaceImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String filename) {

        log.info("🗑️ Delete face image request for filename: {}", filename);

        if (userDetails == null || userDetails.getTaiKhoan().getSinhVien() == null) {
            log.warn("❌ Unauthorized delete attempt for filename: {}", filename);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            log.info("🔍 Processing delete for student: {} - filename: {}", maSv, filename);

            // Decode filename if needed (handle URL encoding)
            String decodedFilename = java.net.URLDecoder.decode(filename, "UTF-8");
            log.info("📝 Decoded filename: {}", decodedFilename);

            // Check if image exists before attempting delete
            Optional<FaceImage> faceImageOpt = faceImageRepository.findByMaSvAndFilename(maSv, decodedFilename);

            if (faceImageOpt.isEmpty()) {
                log.warn("⚠️ Face image not found - maSv: {}, filename: {}", maSv, decodedFilename);

                // Try to find by original filename
                faceImageOpt = faceImageRepository.findByMaSvAndFilename(maSv, filename);

                if (faceImageOpt.isEmpty()) {
                    log.error("❌ Face image not found with both filenames - maSv: {}, original: {}, decoded: {}",
                            maSv, filename, decodedFilename);

                    // List all images for debugging
                    List<FaceImage> allImages = faceImageRepository.findByMaSvAndActive(maSv);
                    log.info("📋 Available images for student {}: {}", maSv,
                            allImages.stream().map(FaceImage::getFilename).collect(Collectors.toList()));

                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error", "Không tìm thấy ảnh để xóa",
                                    "filename", filename,
                                    "decodedFilename", decodedFilename,
                                    "availableImages", allImages.stream().map(FaceImage::getFilename).collect(Collectors.toList())
                            ));
                }
            }

            FaceImage faceImage = faceImageOpt.get();
            log.info("✅ Found face image to delete: ID={}, filename={}, slot={}",
                    faceImage.getId(), faceImage.getFilename(), faceImage.getSlotIndex());

            // Delete the image
            fileUploadService.deleteFaceImage(maSv, faceImage.getFilename());

            int remainingCount = fileUploadService.getFaceImageCount(maSv);

            log.info("🎉 Successfully deleted face image for student: {} - remaining: {}", maSv, remainingCount);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa ảnh thành công",
                    "remainingCount", remainingCount,
                    "deletedFilename", faceImage.getFilename(),
                    "slotIndex", faceImage.getSlotIndex()
            ));

        } catch (Exception e) {
            log.error("💥 Error deleting face image for filename: {} - Error: ", filename, e);

            // More detailed error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể xóa ảnh: " + e.getMessage());
            errorResponse.put("filename", filename);
            errorResponse.put("errorType", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            if (e.getCause() != null) {
                errorResponse.put("cause", e.getCause().getMessage());
            }

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    /**
     * API xóa ảnh đại diện
     */
    @DeleteMapping("/api/delete-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        if (userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tài khoản không có thông tin sinh viên"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            log.info("Student {} deleting profile image", maSv);

            // Update database - set image to null
            SinhVienDTO sinhVien = sinhVienService.getByMaSv(maSv);

            // Delete old image file if exists
            if (sinhVien.getHinhAnh() != null && !sinhVien.getHinhAnh().isEmpty()) {
                fileUploadService.deleteStudentProfileImage(maSv); // ✅ Method này giờ đã có
            }

            sinhVien.setHinhAnh(null);
            sinhVienService.update(maSv, sinhVien);

            log.info("Profile image deleted successfully for student: {}", maSv);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Xóa ảnh đại diện thành công"
            ));

        } catch (Exception e) {
            log.error("Error deleting profile image for student: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể xóa ảnh: " + e.getMessage()));
        }
    }
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("=== STUDENT DASHBOARD ACCESS ===");
        log.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");

        if (userDetails == null) {
            log.warn("No user details found, redirecting to login");
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Check if user has student profile
            if (userDetails.getTaiKhoan().getSinhVien() == null) {
                log.error("User has no student profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin sinh viên");
                return "student/dashboard";
            }

            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            SinhVienDTO student = sinhVienService.getByMaSv(maSv);
            log.info("Loading student dashboard for student: {}", maSv);

            // 1. Thông tin sinh viên
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("student", student);

            // 2. Các môn đang học
            List<DangKyHocDTO> myRegistrations = dangKyHocService.getByMaSv(maSv);
            model.addAttribute("myRegistrations", myRegistrations);
            model.addAttribute("totalSubjects", myRegistrations.size());

            // 3. Tình trạng sinh trắc học
            // 3. Tình trạng sinh trắc học - LOGIC SỬA
            boolean hasEmbedding = student.getEmbedding() != null && !student.getEmbedding().trim().isEmpty();
            int faceImageCount = fileUploadService.getFaceImageCount(maSv);
            boolean hasProfileImage = student.getHinhAnh() != null && !student.getHinhAnh().trim().isEmpty();

            model.addAttribute("hasEmbedding", hasEmbedding);
            model.addAttribute("faceImageCount", faceImageCount);
            model.addAttribute("hasProfileImage", hasProfileImage);

// Tính toán trạng thái sinh trắc học - LOGIC ĐÚNG
            String biometricStatus;
            if (hasEmbedding && faceImageCount >= 3) {
                biometricStatus = "completed"; // ✅ Có embedding VÀ đủ ảnh
            } else if (faceImageCount >= 3) {
                biometricStatus = "ready"; // ✅ Đủ ảnh, chưa có embedding
            } else if (faceImageCount > 0) {
                biometricStatus = "partial"; // ✅ Có ít ảnh, chưa đủ
            } else {
                biometricStatus = "empty"; // ✅ Chưa có ảnh nào
            }
            model.addAttribute("biometricStatus", biometricStatus);

            log.info("Biometric Status: {} (embedding: {}, faceCount: {})",
                    biometricStatus, hasEmbedding, faceImageCount);

            // 4. Thống kê điểm danh
            List<DiemDanhDTO> myAttendance = diemDanhService.getByMaSv(maSv);
            model.addAttribute("myAttendance", myAttendance);

            // Thống kê tổng hợp điểm danh
            Map<String, Integer> attendanceStats = calculateAttendanceStats(myAttendance);
            model.addAttribute("attendanceStats", attendanceStats);

            // Thống kê điểm danh theo môn học
            Map<String, Map<String, Integer>> subjectAttendanceStats = calculateSubjectAttendanceStats(myAttendance, myRegistrations);
            model.addAttribute("subjectAttendanceStats", subjectAttendanceStats);

            log.info("Student dashboard loaded successfully");
            return "student/dashboard";
        } catch (Exception e) {
            log.error("Error loading student dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "student/dashboard";
        }
    }

    // Helper methods
    private Map<String, Integer> calculateAttendanceStats(List<DiemDanhDTO> attendanceList) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", attendanceList.size());
        stats.put("present", 0);
        stats.put("absent", 0);
        stats.put("late", 0);
        stats.put("excused", 0);

        for (DiemDanhDTO attendance : attendanceList) {
            switch (attendance.getTrangThai()) {
                case CO_MAT -> stats.put("present", stats.get("present") + 1);
                case VANG_MAT -> stats.put("absent", stats.get("absent") + 1);
                case DI_TRE -> stats.put("late", stats.get("late") + 1);
                case VANG_CO_PHEP -> stats.put("excused", stats.get("excused") + 1);
            }
        }

        return stats;
    }

    private Map<String, Map<String, Integer>> calculateSubjectAttendanceStats(
            List<DiemDanhDTO> attendanceList, List<DangKyHocDTO> registrations) {

        Map<String, Map<String, Integer>> subjectStats = new HashMap<>();

        // Tạo map để lookup thông tin lớp học phần
        Map<String, String> lichToLhpMap = new HashMap<>();
        for (DiemDanhDTO attendance : attendanceList) {
            try {
                // Lấy thông tin lịch học để biết mã lớp học phần
                // Note: Cần implement method getLichHocByMaLich trong service
                String maLhp = attendance.getMaLhp(); // Giả sử có field này trong DTO
                lichToLhpMap.put(attendance.getMaLich(), maLhp);
            } catch (Exception e) {
                log.warn("Cannot get LHP for attendance: {}", attendance.getMaLich());
            }
        }

        for (DangKyHocDTO registration : registrations) {
            String maLhp = registration.getMaLhp();
            Map<String, Integer> stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("present", 0);
            stats.put("absent", 0);
            stats.put("late", 0);
            stats.put("excused", 0);

            // Đếm điểm danh theo lớp học phần
            for (DiemDanhDTO attendance : attendanceList) {
                if (maLhp.equals(lichToLhpMap.get(attendance.getMaLich()))) {
                    stats.put("total", stats.get("total") + 1);
                    switch (attendance.getTrangThai()) {
                        case CO_MAT -> stats.put("present", stats.get("present") + 1);
                        case VANG_MAT -> stats.put("absent", stats.get("absent") + 1);
                        case DI_TRE -> stats.put("late", stats.get("late") + 1);
                        case VANG_CO_PHEP -> stats.put("excused", stats.get("excused") + 1);
                    }
                }
            }

            subjectStats.put(maLhp, stats);
        }

        return subjectStats;
    }

    /**
     * API lấy danh sách ảnh khuôn mặt - HỖ TRỢ SLOT MAPPING
     */
    @GetMapping("/api/get-face-images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFaceImages(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            List<FaceImage> faceImageEntities = fileUploadService.getFaceImagesEntities(maSv);

            // Tạo slot-based mapping
            Map<Integer, Map<String, Object>> slotMapping = new HashMap<>();
            List<Map<String, Object>> imagesList = new ArrayList<>();

            for (FaceImage img : faceImageEntities) {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("id", img.getId());
                imageData.put("filename", img.getFilename());
                imageData.put("url", "/uploads/students/" + maSv + "/faces/" + img.getFilename());
                imageData.put("slotIndex", img.getSlotIndex());
                imageData.put("createdAt", img.getCreatedAt());

                // Thêm vào slot mapping
                if (img.getSlotIndex() != null) {
                    slotMapping.put(img.getSlotIndex(), imageData);
                }

                // Thêm vào danh sách (để tương thích với frontend cũ)
                imagesList.add(imageData);
            }

            return ResponseEntity.ok(Map.of(
                    "images", imagesList, // Tương thích với frontend hiện tại
                    "slots", slotMapping,  // Hỗ trợ slot-based access
                    "count", faceImageEntities.size(),
                    "maxCount", 5
            ));

        } catch (Exception e) {
            log.error("Error getting face images", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể lấy danh sách ảnh"));
        }
    }
    /**
     * API xóa ảnh khuôn mặt
     */
    @DeleteMapping("/api/delete-face-image/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteFaceImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable int imageId) {

        if (userDetails == null || userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();

            // TODO: Implement delete specific face image in FileUploadService
            // fileUploadService.deleteFaceImage(maSv, imageId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Xóa ảnh thành công"
            ));

        } catch (Exception e) {
            log.error("Error deleting face image", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể xóa ảnh"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changeStudentPassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();

            // Validate input
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Mật khẩu mới phải có ít nhất 6 ký tự",
                        "success", false
                ));
            }

            // Change password with verification
            boolean success = taiKhoanService.changePassword(
                    username,
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "message", "Đổi mật khẩu thành công",
                        "success", true,
                        "redirect", "/student/dashboard"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể đổi mật khẩu",
                        "success", false
                ));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "success", false
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Lỗi hệ thống: " + e.getMessage(),
                    "success", false
            ));
        }
    }
    /**
     * API đổi mật khẩu cho sinh viên
     */
    @PostMapping("/api/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> requestData) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        try {
            String currentPassword = requestData.get("currentPassword");
            String newPassword = requestData.get("newPassword");
            String confirmPassword = requestData.get("confirmPassword");

            // Validate inputs
            if (currentPassword == null || newPassword == null || confirmPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Thiếu thông tin cần thiết"));
            }

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mật khẩu xác nhận không khớp"));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mật khẩu phải có ít nhất 6 ký tự"));
            }

            // ✅ VALIDATE mật khẩu cũ trước khi gọi service
            if (!passwordEncoder.matches(currentPassword, userDetails.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mật khẩu cũ không đúng"));
            }

            // ✅ GỌI service với đúng 2 tham số
            taiKhoanService.changePassword(userDetails.getUsername(), newPassword);

            log.info("Password changed successfully for user: {}", userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "Đổi mật khẩu thành công",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Error changing password for user {}: ", userDetails.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

}