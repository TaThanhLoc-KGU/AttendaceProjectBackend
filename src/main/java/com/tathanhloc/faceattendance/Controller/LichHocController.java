package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/lichhoc")
@RequiredArgsConstructor
public class LichHocController {

    private final LichHocService lichHocService;
    private final ExcelService excelService;

    // ============ BASIC CRUD OPERATIONS ============

    @GetMapping
    public ResponseEntity<List<LichHocDTO>> getAll() {
        return ResponseEntity.ok(lichHocService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LichHocDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(lichHocService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LichHocDTO dto) {
        try {
            LichHocDTO created = lichHocService.create(dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo lịch học thành công!",
                    "data", created
            ));
        } catch (Exception e) {
            log.error("Error creating schedule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo lịch học: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody LichHocDTO dto) {
        try {
            LichHocDTO updated = lichHocService.update(id, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật lịch học thành công!",
                    "data", updated
            ));
        } catch (Exception e) {
            log.error("Error updating schedule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật lịch học: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            lichHocService.delete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa lịch học thành công!"
            ));
        } catch (Exception e) {
            log.error("Error deleting schedule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi xóa lịch học: " + e.getMessage()
            ));
        }
    }

    // ============ QUERY METHODS ============

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

    // ============ SEMESTER-BASED METHODS ============

    /**
     * Lấy lịch học học kỳ hiện tại
     */
    @GetMapping("/current-semester")
    public ResponseEntity<List<LichHocDTO>> getCurrentSemesterSchedule() {
        log.info("API: Lấy lịch học học kỳ hiện tại");
        return ResponseEntity.ok(lichHocService.getCurrentSemesterSchedule());
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
     * Tạo lịch học cho cả học kỳ (sắp lịch)
     */
    @PostMapping("/semester/{maHocKy}/create-all")
    public ResponseEntity<?> createSemesterSchedule(
            @PathVariable String maHocKy,
            @RequestBody List<LichHocDTO> scheduleList) {

        log.info("API: Tạo lịch học cho học kỳ {} với {} lịch", maHocKy, scheduleList.size());

        try {
            Map<String, Object> result = lichHocService.createSemesterSchedule(maHocKy, scheduleList);

            boolean hasConflicts = (Integer) result.get("conflictCount") > 0;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", hasConflicts ?
                            "Tạo lịch học hoàn tất nhưng có một số xung đột" :
                            "Tạo lịch học cho học kỳ thành công!",
                    "data", result,
                    "hasConflicts", hasConflicts
            ));

        } catch (Exception e) {
            log.error("Error creating semester schedule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo lịch học cho học kỳ: " + e.getMessage()
            ));
        }
    }

    /**
     * Cập nhật lịch học cho cả học kỳ
     */
    @PutMapping("/semester/{maHocKy}/update-all")
    public ResponseEntity<?> updateSemesterSchedule(
            @PathVariable String maHocKy,
            @RequestBody List<LichHocDTO> scheduleList) {

        log.info("API: Cập nhật lịch học cho học kỳ {} với {} lịch", maHocKy, scheduleList.size());

        try {
            Map<String, Object> result = lichHocService.updateSemesterSchedule(maHocKy, scheduleList);

            boolean hasConflicts = (Integer) result.get("conflictCount") > 0;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", hasConflicts ?
                            "Cập nhật lịch học hoàn tất nhưng có một số xung đột" :
                            "Cập nhật lịch học cho học kỳ thành công!",
                    "data", result,
                    "hasConflicts", hasConflicts
            ));

        } catch (Exception e) {
            log.error("Error updating semester schedule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật lịch học cho học kỳ: " + e.getMessage()
            ));
        }
    }

    // ============ CONFLICT CHECKING ============

    /**
     * Kiểm tra trùng lịch
     */
    @PostMapping("/check-conflicts")
    public ResponseEntity<Map<String, Object>> checkConflicts(@RequestBody LichHocDTO dto) {
        log.info("API: Kiểm tra trùng lịch cho {}", dto);
        Map<String, Object> result = lichHocService.checkConflicts(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Kiểm tra trùng lịch khi cập nhật
     */
    @PostMapping("/{id}/check-conflicts")
    public ResponseEntity<Map<String, Object>> checkConflictsForUpdate(
            @PathVariable String id,
            @RequestBody LichHocDTO dto) {

        log.info("API: Kiểm tra trùng lịch khi cập nhật {} với data {}", id, dto);
        Map<String, Object> result = lichHocService.checkConflictsForUpdate(id, dto);
        return ResponseEntity.ok(result);
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

    // ============ CALENDAR AND VIEW METHODS ============

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

        log.info("API: Lấy calendar view với filters - GV: {}, SV: {}, Phòng: {}, Học kỳ: {}, Năm: {}",
                maGv, maSv, maPhong, semester, year);

        Map<String, Object> calendar = lichHocService.getCalendarView(maGv, maSv, maPhong, semester, year);
        return ResponseEntity.ok(calendar);
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

    // ============ STATISTICS METHODS ============

    /**
     * Lấy thống kê tổng quan cho tất cả học kỳ
     */
    @GetMapping("/statistics/overview")
    public ResponseEntity<Map<String, Object>> getOverallStatistics() {
        log.info("API: Lấy thống kê tổng quan lịch học");
        Map<String, Object> stats = lichHocService.getStatistics();
        return ResponseEntity.ok(stats);
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
     * Lấy thống kê lịch học hiện tại
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("API: Lấy thống kê lịch học hiện tại");
        Map<String, Object> stats = lichHocService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    // ============ UTILITY METHODS ============

    /**
     * Lấy danh sách học kỳ có lịch học
     */
    @GetMapping("/semesters")
    public ResponseEntity<List<String>> getAvailableSemesters() {
        log.info("API: Lấy danh sách học kỳ có lịch học");

        try {
            List<String> semesters = lichHocService.getAll().stream()
                    .map(LichHocDTO::getHocKy)
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(semesters);
        } catch (Exception e) {
            log.error("Error getting available semesters: {}", e.getMessage());
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    /**
     * Lấy danh sách năm học có lịch học
     */
    @GetMapping("/academic-years")
    public ResponseEntity<List<String>> getAvailableAcademicYears() {
        log.info("API: Lấy danh sách năm học có lịch học");

        try {
            List<String> academicYears = lichHocService.getAll().stream()
                    .map(LichHocDTO::getNamHoc)
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(academicYears);
        } catch (Exception e) {
            log.error("Error getting available academic years: {}", e.getMessage());
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }
    /**
     * Export lịch học ra Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String teacher,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String subject,
            HttpServletResponse response) {
        try {
            log.info("Exporting schedules to Excel with filters - semester: {}, year: {}, teacher: {}, room: {}, subject: {}",
                    semester, year, teacher, room, subject);

            byte[] excelData = excelService.exportSchedulesToExcel(semester, year, teacher, room, subject);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            String filename = String.format("lich-hoc-%s-%s.xlsx",
                    semester != null ? semester : "all",
                    year != null ? year : "all");
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("Error exporting schedules to Excel: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export lịch học dạng POST (với filter phức tạp)
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportToExcelPost(@RequestBody Map<String, Object> filters) {
        try {
            log.info("Exporting schedules to Excel with POST filters: {}", filters);

            String semester = (String) filters.get("semester");
            String year = (String) filters.get("year");
            String teacher = (String) filters.get("teacher");
            String room = (String) filters.get("room");
            String subject = (String) filters.get("subject");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> schedules = (List<Map<String, Object>>) filters.get("schedules");

            byte[] excelData = excelService.exportSchedulesToExcelFromData(schedules, semester, year);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            String filename = String.format("lich-hoc-%s-%s.xlsx",
                    semester != null ? semester : "all",
                    year != null ? year : "all");
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("Error exporting schedules to Excel via POST: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * Chuyển phòng học cho nhiều lịch học
     */
    @PutMapping("/move-room")
    public ResponseEntity<?> moveRoom(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> scheduleIds = (List<String>) request.get("scheduleIds");
            String newRoomId = (String) request.get("newRoomId");

            if (scheduleIds == null || scheduleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Danh sách lịch học không được rỗng"
                ));
            }

            if (newRoomId == null || newRoomId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Phòng học mới không được rỗng"
                ));
            }

            log.info("API: Chuyển {} lịch học sang phòng {}", scheduleIds.size(), newRoomId);

            int successCount = 0;
            int failedCount = 0;
            List<String> errors = new ArrayList<>();

            for (String scheduleId : scheduleIds) {
                try {
                    LichHocDTO schedule = lichHocService.getById(scheduleId);
                    schedule.setMaPhong(newRoomId);
                    lichHocService.update(scheduleId, schedule);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add("Lỗi khi chuyển lịch " + scheduleId + ": " + e.getMessage());
                }
            }

            if (failedCount == 0) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", String.format("Đã chuyển %d lịch học sang phòng %s thành công!", successCount, newRoomId),
                        "successCount", successCount
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", String.format("Chuyển phòng hoàn tất với %d thành công, %d thất bại", successCount, failedCount),
                        "successCount", successCount,
                        "failedCount", failedCount,
                        "errors", errors
                ));
            }

        } catch (Exception e) {
            log.error("Error moving rooms: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi chuyển phòng: " + e.getMessage()
            ));
        }
    }
}
