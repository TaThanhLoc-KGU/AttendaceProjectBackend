package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.NamHocDTO;
import com.tathanhloc.faceattendance.Service.NamHocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
}