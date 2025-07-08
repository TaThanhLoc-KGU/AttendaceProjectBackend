package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.DTO.NamHocDTO;
import com.tathanhloc.faceattendance.Service.NamHocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/namhoc")
@RequiredArgsConstructor
@Slf4j
public class NamHocController {

    private final NamHocService namHocService;

    /**
     * Lấy tất cả năm học đang hoạt động
     */
    @GetMapping
    public ResponseEntity<List<NamHocDTO>> getAll() {
        log.info("Lấy danh sách tất cả năm học");
        return ResponseEntity.ok(namHocService.getAll());
    }

    /**
     * Lấy tất cả năm học (bao gồm cả đã xóa mềm)
     */
    @GetMapping("/all")
    public ResponseEntity<List<NamHocDTO>> getAllIncludeInactive() {
        log.info("Lấy danh sách tất cả năm học (bao gồm inactive)");
        return ResponseEntity.ok(namHocService.getAllIncludeInactive());
    }

    /**
     * Lấy năm học theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NamHocDTO> getById(@PathVariable String id) {
        log.info("Lấy năm học với ID: {}", id);
        return ResponseEntity.ok(namHocService.getById(id));
    }

    /**
     * Tạo năm học mới
     */
    @PostMapping
    public ResponseEntity<NamHocDTO> create(@Valid @RequestBody NamHocDTO dto) {
        log.info("Tạo năm học mới: {}", dto.getMaNamHoc());
        try {
            NamHocDTO created = namHocService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Lỗi khi tạo năm học: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Tạo năm học mới với học kỳ mặc định
     */
    @PostMapping("/with-semesters")
    public ResponseEntity<NamHocDTO> createWithDefaultSemesters(@Valid @RequestBody NamHocDTO dto) {
        log.info("Tạo năm học mới với học kỳ mặc định: {}", dto.getMaNamHoc());
        try {
            NamHocDTO created = namHocService.createWithDefaultSemesters(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Lỗi khi tạo năm học với học kỳ mặc định: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Cập nhật năm học
     */
    @PutMapping("/{id}")
    public ResponseEntity<NamHocDTO> update(@PathVariable String id, @Valid @RequestBody NamHocDTO dto) {
        log.info("Cập nhật năm học với ID {}: {}", id, dto.getMaNamHoc());
        try {
            return ResponseEntity.ok(namHocService.update(id, dto));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật năm học: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa mềm năm học (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        log.info("Xóa mềm năm học với ID: {}", id);
        namHocService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Khôi phục năm học đã xóa mềm
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<NamHocDTO> restore(@PathVariable String id) {
        log.info("Khôi phục năm học với ID: {}", id);
        return ResponseEntity.ok(namHocService.restore(id));
    }

    // ============ SPECIAL ENDPOINTS ============

    /**
     * Lấy năm học hiện tại
     */
    @GetMapping("/current")
    public ResponseEntity<NamHocDTO> getCurrentAcademicYear() {
        log.info("Lấy năm học hiện tại");
        Optional<NamHocDTO> current = namHocService.getCurrentAcademicYear();
        return current.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy các năm học đang diễn ra
     */
    @GetMapping("/ongoing")
    public ResponseEntity<List<NamHocDTO>> getOngoingAcademicYears() {
        log.info("Lấy các năm học đang diễn ra");
        return ResponseEntity.ok(namHocService.getOngoingAcademicYears());
    }

    /**
     * Lấy các năm học sắp tới
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<NamHocDTO>> getUpcomingAcademicYears() {
        log.info("Lấy các năm học sắp tới");
        return ResponseEntity.ok(namHocService.getUpcomingAcademicYears());
    }

    /**
     * Lấy các năm học đã kết thúc
     */
    @GetMapping("/finished")
    public ResponseEntity<List<NamHocDTO>> getFinishedAcademicYears() {
        log.info("Lấy các năm học đã kết thúc");
        return ResponseEntity.ok(namHocService.getFinishedAcademicYears());
    }

    /**
     * Đặt năm học làm hiện tại
     */
    @PutMapping("/{id}/set-current")
    public ResponseEntity<NamHocDTO> setAsCurrent(@PathVariable String id) {
        log.info("Đặt năm học {} làm hiện tại", id);
        return ResponseEntity.ok(namHocService.setAsCurrent(id));
    }

    /**
     * Lấy thống kê năm học
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Lấy thống kê năm học");

        List<NamHocDTO> all = namHocService.getAll();
        List<NamHocDTO> ongoing = namHocService.getOngoingAcademicYears();
        List<NamHocDTO> upcoming = namHocService.getUpcomingAcademicYears();
        List<NamHocDTO> finished = namHocService.getFinishedAcademicYears();
        Optional<NamHocDTO> current = namHocService.getCurrentAcademicYear();

        Map<String, Object> stats = Map.of(
                "totalAcademicYears", all.size(),
                "ongoingAcademicYears", ongoing.size(),
                "upcomingAcademicYears", upcoming.size(),
                "finishedAcademicYears", finished.size(),
                "hasCurrent", current.isPresent(),
                "currentAcademicYear", current.orElse(null)
        );

        return ResponseEntity.ok(stats);
    }
    /**
     * Tạo học kỳ mặc định cho năm học
     */
    @PostMapping("/{maNamHoc}/create-semesters")
    public ResponseEntity<Map<String, Object>> createSemestersForYear(@PathVariable String maNamHoc) {
        log.info("API call: Create semesters for academic year: {}", maNamHoc);

        try {
            Map<String, Object> result = namHocService.createSemestersForYear(maNamHoc);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error in createSemestersForYear API: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("maNamHoc", maNamHoc);
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Lấy danh sách học kỳ theo năm học
     */
    @GetMapping("/{maNamHoc}/semesters")
    public ResponseEntity<List<HocKyDTO>> getSemestersByYear(@PathVariable String maNamHoc) {
        log.info("API call: Get semesters for academic year: {}", maNamHoc);

        try {
            List<HocKyDTO> semesters = namHocService.getSemestersByYear(maNamHoc);
            return ResponseEntity.ok(semesters);

        } catch (Exception e) {
            log.error("❌ Error getting semesters for year: {}", maNamHoc, e);
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    /**
     * Xóa tất cả học kỳ của năm học
     */
    @DeleteMapping("/{maNamHoc}/semesters")
    public ResponseEntity<Map<String, Object>> deleteSemestersOfYear(@PathVariable String maNamHoc) {
        log.info("API call: Delete semesters for academic year: {}", maNamHoc);

        try {
            namHocService.deleteSemestersOfYear(maNamHoc);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đã xóa thành công tất cả học kỳ của năm học " + maNamHoc);
            response.put("maNamHoc", maNamHoc);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error deleting semesters for year: {}", maNamHoc, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("maNamHoc", maNamHoc);
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Kiểm tra năm học đã có học kỳ chưa
     */
    @GetMapping("/{maNamHoc}/has-semesters")
    public ResponseEntity<Map<String, Object>> checkHasSemesters(@PathVariable String maNamHoc) {
        log.debug("API call: Check if academic year has semesters: {}", maNamHoc);

        try {
            List<HocKyDTO> semesters = namHocService.getSemestersByYear(maNamHoc);

            Map<String, Object> response = new HashMap<>();
            response.put("maNamHoc", maNamHoc);
            response.put("hasSemesters", !semesters.isEmpty());
            response.put("semesterCount", semesters.size());
            response.put("semesters", semesters);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error checking semesters for year: {}", maNamHoc, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("maNamHoc", maNamHoc);
            errorResponse.put("hasSemesters", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}