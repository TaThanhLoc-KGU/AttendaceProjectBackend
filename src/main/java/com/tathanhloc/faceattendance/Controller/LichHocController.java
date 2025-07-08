package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lichhoc")
@RequiredArgsConstructor
public class LichHocController {

    private final LichHocService lichHocService;

    @GetMapping
    public List<LichHocDTO> getAll() {
        return lichHocService.getAll();
    }

    @GetMapping("/{id}")
    public LichHocDTO getById(@PathVariable String id) {
        return lichHocService.getById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LichHocDTO dto) {
        try {
            // Kiểm tra trùng lịch trước khi tạo
            Map<String, Object> conflictCheck = lichHocService.checkConflicts(dto);
            if ((Boolean) conflictCheck.get("hasConflict")) {
                return ResponseEntity.badRequest().body(conflictCheck);
            }

            LichHocDTO created = lichHocService.create(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Lỗi khi tạo lịch học: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody LichHocDTO dto) {
        try {
            // Kiểm tra trùng lịch trước khi cập nhật (trừ chính nó)
            Map<String, Object> conflictCheck = lichHocService.checkConflictsForUpdate(id, dto);
            if ((Boolean) conflictCheck.get("hasConflict")) {
                return ResponseEntity.badRequest().body(conflictCheck);
            }

            LichHocDTO updated = lichHocService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Lỗi khi cập nhật lịch học: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        lichHocService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ============ SCHEDULING APIs ============

    /**
     * Lấy lịch học theo lớp học phần
     */
    @GetMapping("/by-lhp/{maLhp}")
    public ResponseEntity<List<LichHocDTO>> getByLopHocPhan(@PathVariable String maLhp) {
        return ResponseEntity.ok(lichHocService.getByLopHocPhan(maLhp));
    }

    /**
     * Lấy lịch học theo phòng học
     */
    @GetMapping("/by-phong/{maPhong}")
    public ResponseEntity<List<LichHocDTO>> getByPhongHoc(@PathVariable String maPhong) {
        return ResponseEntity.ok(lichHocService.getByPhongHoc(maPhong));
    }

    /**
     * Lấy lịch học theo giảng viên
     */
    @GetMapping("/by-giangvien/{maGv}")
    public ResponseEntity<List<LichHocDTO>> getByGiangVien(@PathVariable String maGv) {
        return ResponseEntity.ok(lichHocService.getByGiangVien(maGv));
    }

    /**
     * Lấy lịch học theo sinh viên
     */
    @GetMapping("/by-sinhvien/{maSv}")
    public ResponseEntity<List<LichHocDTO>> getBySinhVien(@PathVariable String maSv) {
        return ResponseEntity.ok(lichHocService.getBySinhVien(maSv));
    }

    /**
     * Lấy lịch học theo thứ
     */
    @GetMapping("/by-thu/{thu}")
    public ResponseEntity<List<LichHocDTO>> getByThu(@PathVariable Integer thu) {
        return ResponseEntity.ok(lichHocService.getByThu(thu));
    }

    /**
     * Kiểm tra trùng lịch
     */
    @PostMapping("/check-conflicts")
    public ResponseEntity<Map<String, Object>> checkConflicts(@RequestBody LichHocDTO dto) {
        Map<String, Object> result = lichHocService.checkConflicts(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy lịch học dạng calendar view
     */
    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getCalendarView(
            @RequestParam(required = false) String maGv,
            @RequestParam(required = false) String maSv,
            @RequestParam(required = false) String maPhong,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String year) {

        Map<String, Object> calendar = lichHocService.getCalendarView(maGv, maSv, maPhong, semester, year);
        return ResponseEntity.ok(calendar);
    }

    /**
     * Lấy thống kê lịch học
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = lichHocService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}