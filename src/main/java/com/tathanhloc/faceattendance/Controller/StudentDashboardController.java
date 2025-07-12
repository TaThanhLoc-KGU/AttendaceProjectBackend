package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.SinhVienDTO;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    /**
     * Trang dashboard chính của sinh viên
     */
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
            log.info("Loading student dashboard for student: {}", maSv);

            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("myRegistrations", dangKyHocService.getByMaSv(maSv));
            model.addAttribute("allSchedules", lichHocService.getAll());
            model.addAttribute("myAttendance", diemDanhService.getByMaSv(maSv));

            log.info("Student dashboard loaded successfully");
            return "student/dashboard";
        } catch (Exception e) {
            log.error("Error loading student dashboard", e);
            model.addAttribute("error", "Không thể tải dữ liệu dashboard: " + e.getMessage());
            return "student/dashboard";
        }
    }

    /**
     * Trang lịch học của sinh viên
     */
    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/?error=not_authenticated";
        }
        model.addAttribute("schedules", lichHocService.getAll());
        return "student/schedule";
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
     * API đổi mật khẩu cho sinh viên
     */
    @PostMapping("/api/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        try {
            log.info("Password change request for student user: {}", userDetails.getUsername());

            // Validate input
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vui lòng nhập mật khẩu cũ"));
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vui lòng nhập mật khẩu mới"));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mật khẩu mới phải có ít nhất 6 ký tự"));
            }

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mật khẩu xác nhận không khớp"));
            }

            // Check old password
            if (!passwordEncoder.matches(oldPassword, userDetails.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mật khẩu cũ không đúng"));
            }

            // Change password
            taiKhoanService.changePassword(userDetails.getUsername(), newPassword);

            log.info("Password changed successfully for student user: {}", userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đổi mật khẩu thành công"
            ));

        } catch (Exception e) {
            log.error("Password change failed for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi khi đổi mật khẩu: " + e.getMessage()));
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
     * API upload ảnh khuôn mặt cho sinh viên (chính mình)
     */
    @PostMapping("/api/upload-face-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFaceImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

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

            String imageUrl = fileUploadService.saveFaceImage(maSv, file);

            return ResponseEntity.ok(Map.of(
                    "id", System.currentTimeMillis(),
                    "url", imageUrl,
                    "filename", file.getOriginalFilename(),
                    "currentCount", currentCount + 1
            ));

        } catch (Exception e) {
            log.error("Error uploading face image for student: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tải lên ảnh: " + e.getMessage()));
        }
    }

    /**
     * API lấy danh sách ảnh khuôn mặt
     */
    @GetMapping("/api/face-images")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFaceImages(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            List<Map<String, Object>> faceImages = fileUploadService.getFaceImages(maSv);
            return ResponseEntity.ok(faceImages);
        } catch (Exception e) {
            log.error("Error loading face images: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API xóa ảnh khuôn mặt
     */
    @DeleteMapping("/api/face-image/{filename}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFaceImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String filename) {

        if (userDetails == null || userDetails.getTaiKhoan().getSinhVien() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không có quyền truy cập"));
        }

        try {
            String maSv = userDetails.getTaiKhoan().getSinhVien().getMaSv();
            fileUploadService.deleteFaceImage(maSv, filename);

            int remainingCount = fileUploadService.getFaceImageCount(maSv);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa ảnh thành công",
                    "remainingCount", remainingCount
            ));
        } catch (Exception e) {
            log.error("Error deleting face image: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể xóa ảnh: " + e.getMessage()));
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
}