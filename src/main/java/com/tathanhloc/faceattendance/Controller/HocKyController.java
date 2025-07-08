package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Service.HocKyService;
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
@RequestMapping("/api/hocky")
@RequiredArgsConstructor
@Slf4j
public class HocKyController {

    private final HocKyService hocKyService;

    /**
     * Lấy tất cả học kỳ đang hoạt động
     */
    @GetMapping
    public ResponseEntity<List<HocKyDTO>> getAll() {
        log.info("Lấy danh sách tất cả học kỳ");
        return ResponseEntity.ok(hocKyService.getAll());
    }

    /**
     * Lấy tất cả học kỳ (bao gồm cả đã xóa mềm)
     */
    @GetMapping("/all")
    public ResponseEntity<List<HocKyDTO>> getAllIncludeInactive() {
        log.info("Lấy danh sách tất cả học kỳ (bao gồm inactive)");
        return ResponseEntity.ok(hocKyService.getAllIncludeInactive());
    }

    /**
     * Lấy học kỳ theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<HocKyDTO> getById(@PathVariable String id) {
        log.info("Lấy học kỳ với ID: {}", id);
        return ResponseEntity.ok(hocKyService.getById(id));
    }

    /**
     * Tạo học kỳ mới
     */
    @PostMapping
    public ResponseEntity<HocKyDTO> create(@Valid @RequestBody HocKyDTO dto) {
        log.info("Tạo học kỳ mới: {}", dto.getMaHocKy());
        try {
            HocKyDTO created = hocKyService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Lỗi khi tạo học kỳ: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Cập nhật học kỳ
     */
    @PutMapping("/{id}")
    public ResponseEntity<HocKyDTO> update(@PathVariable String id, @Valid @RequestBody HocKyDTO dto) {
        log.info("Cập nhật học kỳ với ID {}: {}", id, dto.getMaHocKy());
        try {
            return ResponseEntity.ok(hocKyService.update(id, dto));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật học kỳ: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa mềm học kỳ (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        log.info("Xóa mềm học kỳ với ID: {}", id);
        hocKyService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Khôi phục học kỳ đã xóa mềm
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<HocKyDTO> restore(@PathVariable String id) {
        log.info("Khôi phục học kỳ với ID: {}", id);
        return ResponseEntity.ok(hocKyService.restore(id));
    }

    // ============ SPECIAL ENDPOINTS ============

    /**
     * Lấy học kỳ hiện tại
     */
    @GetMapping("/current")
    public ResponseEntity<HocKyDTO> getCurrentSemester() {
        log.info("Lấy học kỳ hiện tại");
        Optional<HocKyDTO> current = hocKyService.getCurrentSemester();
        return current.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy các học kỳ đang diễn ra
     */
    @GetMapping("/ongoing")
    public ResponseEntity<List<HocKyDTO>> getOngoingSemesters() {
        log.info("Lấy các học kỳ đang diễn ra");
        return ResponseEntity.ok(hocKyService.getOngoingSemesters());
    }

    /**
     * Lấy các học kỳ sắp tới
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<HocKyDTO>> getUpcomingSemesters() {
        log.info("Lấy các học kỳ sắp tới");
        return ResponseEntity.ok(hocKyService.getUpcomingSemesters());
    }

    /**
     * Lấy các học kỳ đã kết thúc
     */
    @GetMapping("/finished")
    public ResponseEntity<List<HocKyDTO>> getFinishedSemesters() {
        log.info("Lấy các học kỳ đã kết thúc");
        return ResponseEntity.ok(hocKyService.getFinishedSemesters());
    }

    /**
     * Đặt học kỳ làm hiện tại
     */
    @PutMapping("/{id}/set-current")
    public ResponseEntity<HocKyDTO> setAsCurrent(@PathVariable String id) {
        log.info("Đặt học kỳ {} làm hiện tại", id);
        return ResponseEntity.ok(hocKyService.setAsCurrent(id));
    }

    /**
     * Lấy thống kê học kỳ
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Lấy thống kê học kỳ");

        List<HocKyDTO> all = hocKyService.getAll();
        List<HocKyDTO> ongoing = hocKyService.getOngoingSemesters();
        List<HocKyDTO> upcoming = hocKyService.getUpcomingSemesters();
        List<HocKyDTO> finished = hocKyService.getFinishedSemesters();
        Optional<HocKyDTO> current = hocKyService.getCurrentSemester();

        Map<String, Object> stats = Map.of(
                "totalSemesters", all.size(),
                "ongoingSemesters", ongoing.size(),
                "upcomingSemesters", upcoming.size(),
                "finishedSemesters", finished.size(),
                "hasCurrent", current.isPresent(),
                "currentSemester", current.orElse(null)
        );

        return ResponseEntity.ok(stats);
    }
}