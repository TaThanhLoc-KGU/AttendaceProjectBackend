package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Service.HocKyService;
import com.tathanhloc.faceattendance.Exception.BusinessException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hocky")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HocKyController {

    private final HocKyService hocKyService;

    // ===== EXISTING CRUD OPERATIONS (Keep compatibility) =====

    /**
     * Lấy danh sách tất cả học kỳ
     */
    @GetMapping("/all")
    public ResponseEntity<List<HocKyDTO>> getAllSemesters() {
        log.info("API: Lấy danh sách tất cả học kỳ");
        return ResponseEntity.ok(hocKyService.getAll());
    }

    /**
     * Lấy danh sách học kỳ đang hoạt động
     */
    @GetMapping("/active")
    public ResponseEntity<List<HocKyDTO>> getActiveSemesters() {
        log.info("API: Lấy danh sách học kỳ đang hoạt động");
        return ResponseEntity.ok(hocKyService.getAllActive());
    }

    /**
     * Lấy thông tin chi tiết một học kỳ
     */
    @GetMapping("/{maHocKy}")
    public ResponseEntity<HocKyDTO> getSemester(@PathVariable String maHocKy) {
        log.info("API: Lấy thông tin học kỳ: {}", maHocKy);
        return ResponseEntity.ok(hocKyService.getById(maHocKy));
    }

    /**
     * Lấy học kỳ hiện tại
     */
    @GetMapping("/current")
    public ResponseEntity<HocKyDTO> getCurrentSemester() {
        log.info("API: Lấy học kỳ hiện tại");
        Optional<HocKyDTO> current = hocKyService.getCurrentSemester();

        if (current.isPresent()) {
            return ResponseEntity.ok(current.get());
        } else {
            return ResponseEntity.ok().body(null);
        }
    }

    /**
     * Tạo học kỳ mới
     */
    @PostMapping
    public ResponseEntity<?> createSemester(@Valid @RequestBody HocKyDTO semesterDTO) {
        log.info("API: Tạo học kỳ mới: {}", semesterDTO.getMaHocKy());

        try {
            HocKyDTO created = hocKyService.create(semesterDTO);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo học kỳ thành công!",
                    "data", created
            ));
        } catch (BusinessException e) {
            log.error("Business error creating semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo học kỳ: " + e.getMessage()
            ));
        }
    }

    /**
     * Cập nhật học kỳ
     */
    @PutMapping("/{maHocKy}")
    public ResponseEntity<?> updateSemester(@PathVariable String maHocKy,
                                            @Valid @RequestBody HocKyDTO semesterDTO) {
        log.info("API: Cập nhật học kỳ: {}", maHocKy);

        try {
            HocKyDTO updated = hocKyService.update(maHocKy, semesterDTO);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật học kỳ thành công!",
                    "data", updated
            ));
        } catch (BusinessException e) {
            log.error("Business error updating semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error updating semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật học kỳ: " + e.getMessage()
            ));
        }
    }

    /**
     * Xóa học kỳ
     */
    @DeleteMapping("/{maHocKy}")
    public ResponseEntity<?> deleteSemester(@PathVariable String maHocKy) {
        log.info("API: Xóa học kỳ: {}", maHocKy);

        try {
            hocKyService.delete(maHocKy);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa học kỳ thành công!"
            ));
        } catch (BusinessException e) {
            log.error("Business error deleting semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error deleting semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi xóa học kỳ: " + e.getMessage()
            ));
        }
    }

    // ===== STATUS MANAGEMENT =====

    /**
     * Đặt học kỳ làm học kỳ hiện tại
     */
    @PutMapping("/{maHocKy}/set-current")
    public ResponseEntity<?> setAsCurrentSemester(@PathVariable String maHocKy) {
        log.info("API: Đặt học kỳ hiện tại: {}", maHocKy);

        try {
            HocKyDTO updated = hocKyService.setAsCurrent(maHocKy);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã đặt học kỳ hiện tại thành công!",
                    "data", updated
            ));
        } catch (BusinessException e) {
            log.error("Business error setting current semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error setting current semester: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi đặt học kỳ hiện tại: " + e.getMessage()
            ));
        }
    }

    // ===== WEEK-BASED FEATURES =====

    /**
     * Lấy danh sách tuần học của một học kỳ (chỉ cho week-based config)
     */
    @GetMapping("/{maHocKy}/weeks")
    public ResponseEntity<?> getSemesterWeeks(@PathVariable String maHocKy) {
        log.info("API: Lấy danh sách tuần học: {}", maHocKy);

        try {
            List<HocKyDTO.TuanHocDTO> weeks = hocKyService.getTuanHocList(maHocKy);

            if (weeks.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Học kỳ không có cấu hình week-based",
                        "data", weeks,
                        "isWeekBased", false
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", weeks,
                    "isWeekBased", true
            ));

        } catch (Exception e) {
            log.error("Error getting semester weeks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách tuần học: " + e.getMessage()
            ));
        }
    }

    /**
     * Chuyển đổi học kỳ sang week-based configuration
     */
    @PostMapping("/{maHocKy}/convert-to-week-based")
    public ResponseEntity<?> convertToWeekBased(@PathVariable String maHocKy,
                                                @RequestBody Map<String, Object> params) {
        log.info("API: Chuyển đổi học kỳ {} sang week-based", maHocKy);

        try {
            Integer tuanBatDau = (Integer) params.get("tuanBatDau");
            Integer soTuanHoc = (Integer) params.get("soTuanHoc");
            String ngayBatDauTuan1Str = (String) params.get("ngayBatDauTuan1");

            if (tuanBatDau == null || soTuanHoc == null || ngayBatDauTuan1Str == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu thông tin: tuanBatDau, soTuanHoc, ngayBatDauTuan1"
                ));
            }

            LocalDate ngayBatDauTuan1 = LocalDate.parse(ngayBatDauTuan1Str);

            HocKyDTO converted = hocKyService.convertToWeekBased(maHocKy, tuanBatDau, soTuanHoc, ngayBatDauTuan1);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chuyển đổi sang week-based thành công!",
                    "data", converted
            ));

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error converting to week-based: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi chuyển đổi: " + e.getMessage()
            ));
        }
    }

    /**
     * Tính toán thông tin học kỳ dựa trên input
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateSemesterInfo(@RequestBody Map<String, Object> params) {
        log.info("API: Tính toán thông tin học kỳ");

        try {
            // Support both date-based and week-based calculation
            String mode = (String) params.getOrDefault("mode", "date"); // "date" or "week"

            if ("week".equals(mode)) {
                return calculateWeekBasedInfo(params);
            } else {
                return calculateDateBasedInfo(params);
            }

        } catch (Exception e) {
            log.error("Error calculating semester info: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tính toán: " + e.getMessage()
            ));
        }
    }

    private ResponseEntity<?> calculateWeekBasedInfo(Map<String, Object> params) {
        Integer tuanBatDau = (Integer) params.get("tuanBatDau");
        Integer soTuanHoc = (Integer) params.get("soTuanHoc");
        String ngayBatDauTuan1Str = (String) params.get("ngayBatDauTuan1");

        if (tuanBatDau == null || soTuanHoc == null || ngayBatDauTuan1Str == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thiếu thông tin đầu vào cho week-based calculation"
            ));
        }

        LocalDate ngayBatDauTuan1 = LocalDate.parse(ngayBatDauTuan1Str);

        // Tính toán
        LocalDate ngayBatDauThucTe = ngayBatDauTuan1.plusWeeks(tuanBatDau - 1);
        LocalDate ngayKetThucThucTe = ngayBatDauThucTe.plusWeeks(soTuanHoc - 1).plusDays(6);
        Integer tuanKetThuc = tuanBatDau + soTuanHoc - 1;
        Integer tongSoNgay = (int) java.time.temporal.ChronoUnit.DAYS.between(ngayBatDauThucTe, ngayKetThucThucTe) + 1;
        Integer soBuoiHocDuKien = soTuanHoc * 3; // Giả sử 3 buổi/tuần

        // Validation
        boolean isValidWeekRange = tuanKetThuc <= 52;
        boolean isValidStartDate = ngayBatDauTuan1.getDayOfWeek().getValue() == 1; // Monday

        // Use HashMap instead of Map.of() to avoid parameter limit
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mode", "week");
        result.put("tuanBatDau", tuanBatDau);
        result.put("tuanKetThuc", tuanKetThuc);
        result.put("soTuanHoc", soTuanHoc);
        result.put("ngayBatDauThucTe", ngayBatDauThucTe.toString());
        result.put("ngayKetThucThucTe", ngayKetThucThucTe.toString());
        result.put("tongSoNgay", tongSoNgay);
        result.put("soBuoiHocDuKien", soBuoiHocDuKien);
        result.put("isValidWeekRange", isValidWeekRange);
        result.put("isValidStartDate", isValidStartDate);
        result.put("khoangThoiGian", ngayBatDauThucTe + " - " + ngayKetThucThucTe);

        return ResponseEntity.ok(result);
    }

    private ResponseEntity<?> calculateDateBasedInfo(Map<String, Object> params) {
        String ngayBatDauStr = (String) params.get("ngayBatDau");
        String ngayKetThucStr = (String) params.get("ngayKetThuc");

        if (ngayBatDauStr == null || ngayKetThucStr == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thiếu thông tin đầu vào cho date-based calculation"
            ));
        }

        LocalDate ngayBatDau = LocalDate.parse(ngayBatDauStr);
        LocalDate ngayKetThuc = LocalDate.parse(ngayKetThucStr);

        // Tính toán
        long tongSoNgay = java.time.temporal.ChronoUnit.DAYS.between(ngayBatDau, ngayKetThuc) + 1;
        int soTuanTuongDuong = (int) Math.ceil(tongSoNgay / 7.0);
        int soBuoiHocDuKien = soTuanTuongDuong * 3; // Giả sử 3 buổi/tuần

        // Validation
        boolean isValidDateRange = ngayKetThuc.isAfter(ngayBatDau);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "mode", "date",
                "ngayBatDau", ngayBatDau.toString(),
                "ngayKetThuc", ngayKetThuc.toString(),
                "tongSoNgay", tongSoNgay,
                "soTuanTuongDuong", soTuanTuongDuong,
                "soBuoiHocDuKien", soBuoiHocDuKien,
                "isValidDateRange", isValidDateRange,
                "khoangThoiGian", ngayBatDau + " - " + ngayKetThuc
        ));
    }

    // ===== PRESET CONFIGURATIONS =====

    /**
     * Lấy cấu hình preset cho học kỳ
     */
    @GetMapping("/presets")
    public ResponseEntity<Map<String, Object>> getSemesterPresets() {
        log.info("API: Lấy cấu hình preset");

        Map<String, Object> presets = Map.of(
                "standard", Map.of(
                        "soTuan", 15,
                        "moTa", "Học kỳ chuẩn 15 tuần",
                        "loaiHocKy", "CHINH_QUY"
                ),
                "extended", Map.of(
                        "soTuan", 18,
                        "moTa", "Học kỳ mở rộng 18 tuần",
                        "loaiHocKy", "CHINH_QUY"
                ),
                "summer", Map.of(
                        "soTuan", 8,
                        "moTa", "Học kỳ hè ngắn hạn",
                        "loaiHocKy", "HE"
                ),
                "intensive", Map.of(
                        "soTuan", 6,
                        "moTa", "Khóa học tập trung",
                        "loaiHocKy", "TAP_TRUNG"
                )
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "presets", presets
        ));
    }

    // ===== COMPATIBILITY ENDPOINTS =====

    /**
     * Endpoint tương thích cho các service khác gọi
     */
    @GetMapping("/by-code/{maHocKy}")
    public ResponseEntity<HocKyDTO> getByCode(@PathVariable String maHocKy) {
        // Alias for getSemester - for backward compatibility
        return getSemester(maHocKy);
    }

    /**
     * Lấy thông tin cơ bản học kỳ (không có computed fields)
     */
    @GetMapping("/{maHocKy}/basic")
    public ResponseEntity<HocKyDTO> getBasicInfo(@PathVariable String maHocKy) {
        log.info("API: Lấy thông tin cơ bản học kỳ: {}", maHocKy);

        try {
            HocKyDTO hocKy = hocKyService.getById(maHocKy);

            // Return basic info only
            HocKyDTO basicInfo = HocKyDTO.builder()
                    .maHocKy(hocKy.getMaHocKy())
                    .tenHocKy(hocKy.getTenHocKy())
                    .ngayBatDau(hocKy.getNgayBatDau())
                    .ngayKetThuc(hocKy.getNgayKetThuc())
                    .isActive(hocKy.getIsActive())
                    .isCurrent(hocKy.getIsCurrent())
                    .isWeekBasedConfig(hocKy.getIsWeekBasedConfig())
                    .build();

            return ResponseEntity.ok(basicInfo);

        } catch (Exception e) {
            log.error("Error getting basic semester info: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}