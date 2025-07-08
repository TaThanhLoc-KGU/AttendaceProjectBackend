package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    /**
     * Lấy lịch học theo học kỳ
     */
    @GetMapping("/semester/{maHocKy}")
    public ResponseEntity<List<LichHocDTO>> getScheduleBySemester(@PathVariable String maHocKy) {
        log.info("API: Lấy lịch học theo học kỳ {}", maHocKy);
        return ResponseEntity.ok(lichHocService.getScheduleBySemester(maHocKy));
    }

    /**
     * Lấy lịch học học kỳ hiện tại
     */
    @GetMapping("/current-semester")
    public ResponseEntity<List<LichHocDTO>> getCurrentSemesterSchedule() {
        log.info("API: Lấy lịch học học kỳ hiện tại");
        return ResponseEntity.ok(lichHocService.getCurrentSemesterSchedule());
    }

    /**
     * Lấy lịch học dạng calendar view theo học kỳ
     */
    @GetMapping("/semester/{maHocKy}/calendar")
    public ResponseEntity<Map<String, Object>> getCalendarViewBySemester(
            @PathVariable String maHocKy,
            @RequestParam(required = false) String maGv,
            @RequestParam(required = false) String maSv,
            @RequestParam(required = false) String maPhong) {

        log.info("API: Lấy calendar view theo học kỳ {} với filters - GV: {}, SV: {}, Phòng: {}",
                maHocKy, maGv, maSv, maPhong);

        Map<String, Object> calendar = lichHocService.getCalendarViewBySemester(maHocKy, maGv, maSv, maPhong);
        return ResponseEntity.ok(calendar);
    }

    /**
     * Lấy lịch học hiện tại dạng calendar view
     */
    @GetMapping("/current-calendar")
    public ResponseEntity<Map<String, Object>> getCurrentCalendarView(
            @RequestParam(required = false) String maGv,
            @RequestParam(required = false) String maSv,
            @RequestParam(required = false) String maPhong) {

        log.info("API: Lấy calendar view hiện tại với filters - GV: {}, SV: {}, Phòng: {}",
                maGv, maSv, maPhong);

        Map<String, Object> calendar = lichHocService.getCurrentCalendarView(maGv, maSv, maPhong);
        return ResponseEntity.ok(calendar);
    }

    /**
     * Lấy lịch học tuần hiện tại
     */
    @GetMapping("/current-week")
    public ResponseEntity<Map<String, Object>> getCurrentWeekSchedule(
            @RequestParam(required = false) String maGv,
            @RequestParam(required = false) String maSv,
            @RequestParam(required = false) String maPhong) {

        log.info("API: Lấy lịch học tuần hiện tại với filters - GV: {}, SV: {}, Phòng: {}",
                maGv, maSv, maPhong);

        Map<String, Object> weekSchedule = lichHocService.getCurrentWeekSchedule(maGv, maSv, maPhong);
        return ResponseEntity.ok(weekSchedule);
    }

    /**
     * Lấy lịch học hôm nay
     */
    @GetMapping("/today")
    public ResponseEntity<List<LichHocDTO>> getTodaySchedule(
            @RequestParam(required = false) String maGv,
            @RequestParam(required = false) String maSv,
            @RequestParam(required = false) String maPhong) {

        log.info("API: Lấy lịch học hôm nay với filters - GV: {}, SV: {}, Phòng: {}",
                maGv, maSv, maPhong);

        List<LichHocDTO> todaySchedule = lichHocService.getTodaySchedule(maGv, maSv, maPhong);
        return ResponseEntity.ok(todaySchedule);
    }

    /**
     * Kiểm tra xung đột lịch học trong học kỳ
     */
    @PostMapping("/semester/check-conflicts")
    public ResponseEntity<Map<String, Object>> checkConflictsInSemester(@RequestBody LichHocDTO dto) {
        log.info("API: Kiểm tra xung đột lịch học trong học kỳ {}", dto.getHocKy());

        Map<String, Object> conflicts = lichHocService.checkConflictsInSemester(dto);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * Lấy thống kê lịch học theo học kỳ
     */
    @GetMapping("/semester/{maHocKy}/statistics")
    public ResponseEntity<Map<String, Object>> getSemesterStatistics(@PathVariable String maHocKy) {
        log.info("API: Lấy thống kê lịch học theo học kỳ {}", maHocKy);

        Map<String, Object> stats = lichHocService.getSemesterStatistics(maHocKy);
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê tổng quan cho tất cả học kỳ
     */
    @GetMapping("/statistics/overview")
    public ResponseEntity<Map<String, Object>> getOverallStatistics() {
        log.info("API: Lấy thống kê tổng quan lịch học");

        Map<String, Object> stats = lichHocService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}