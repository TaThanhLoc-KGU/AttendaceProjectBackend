package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.Camera;
import com.tathanhloc.faceattendance.Model.LichHoc;
import com.tathanhloc.faceattendance.Model.PhongHoc;
import com.tathanhloc.faceattendance.Repository.CameraRepository;
import com.tathanhloc.faceattendance.Repository.LichHocRepository;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/diemdanh")
@RequiredArgsConstructor
public class DiemDanhController {

    private final DiemDanhService diemDanhService;
    private final CameraRepository cameraRepository;
    private final LichHocRepository lichHocRepository;

    @GetMapping
    public List<DiemDanhDTO> getAll() {
        return diemDanhService.getAll();
    }

    @GetMapping("/{id}")
    public DiemDanhDTO getById(@PathVariable Long id) {
        return diemDanhService.getById(id);
    }

    @PostMapping
    public DiemDanhDTO create(@RequestBody DiemDanhDTO dto) {
        return diemDanhService.create(dto);
    }

    @PutMapping("/{id}")
    public DiemDanhDTO update(@PathVariable Long id, @RequestBody DiemDanhDTO dto) {
        return diemDanhService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        diemDanhService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-masv/{maSv}")
    public ResponseEntity<List<DiemDanhDTO>> getByMaSv(@PathVariable String maSv) {
        return ResponseEntity.ok(diemDanhService.getByMaSv(maSv));
    }

    @GetMapping("/by-malich/{maLich}")
    public ResponseEntity<List<DiemDanhDTO>> getByMaLich(
            @PathVariable String maLich,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date != null) {
            List<DiemDanhDTO> attendances = diemDanhService.getByMaLichAndDate(maLich, date);
            return ResponseEntity.ok(attendances);
        } else {
            return ResponseEntity.ok(diemDanhService.getByMaLich(maLich));
        }
    }

    @GetMapping("/today/count")
    public ResponseEntity<Long> countTodayDiemDanh() {
        long count = diemDanhService.countTodayDiemDanh(); // Assuming this method exists in DiemDanhService
        return ResponseEntity.ok(count);
    }


    @PostMapping("/camera-attendance")
    public ResponseEntity<?> recordAttendanceFromCamera(@RequestBody Map<String, Object> attendanceData) {
        try {
            String studentId = (String) attendanceData.get("studentId");
            Long cameraId = Long.valueOf(attendanceData.get("cameraId").toString());

            DiemDanhDTO result = diemDanhService.recordAttendanceFromCamera(studentId, cameraId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Điểm danh thành công",
                    "data", result
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    /**
     * Lấy thống kê điểm danh tổng quan
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = diemDanhService.getAttendanceStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting attendance statistics:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thống kê theo khoảng thời gian
     */
    @GetMapping("/statistics/date-range")
    public ResponseEntity<Map<String, Object>> getStatisticsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            Map<String, Object> stats = diemDanhService.getAttendanceStatisticsByDateRange(fromDate, toDate);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting attendance statistics by date range:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy lịch sử điểm danh gần nhất
     */
    @GetMapping("/recent-history")
    public ResponseEntity<List<Map<String, Object>>> getRecentHistory(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Map<String, Object>> history = diemDanhService.getRecentAttendanceHistory(limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting recent attendance history:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy báo cáo điểm danh theo bộ lọc
     */
    @PostMapping("/filtered-report")
    public ResponseEntity<List<Map<String, Object>>> getFilteredReport(
            @RequestBody Map<String, Object> filters) {
        try {
            LocalDate fromDate = LocalDate.parse((String) filters.get("dateFrom"));
            LocalDate toDate = LocalDate.parse((String) filters.get("dateTo"));
            String subjectCode = (String) filters.get("subject");
            String lecturerCode = (String) filters.get("lecturer");
            String classCode = (String) filters.get("class");

            List<Map<String, Object>> report = diemDanhService.getFilteredAttendanceReport(
                    fromDate, toDate, subjectCode, lecturerCode, classCode);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error getting filtered attendance report:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thống kê theo học kỳ
     */
    @GetMapping("/statistics/semester")
    public ResponseEntity<Map<String, Object>> getStatisticsBySemester(
            @RequestParam String semesterCode,
            @RequestParam String yearCode) {
        try {
            Map<String, Object> stats = diemDanhService.getAttendanceStatisticsBySemester(semesterCode, yearCode);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting attendance statistics by semester:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Xuất báo cáo điểm danh ra Excel
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestBody Map<String, Object> filters) {
        try {
            LocalDate fromDate = LocalDate.parse((String) filters.get("dateFrom"));
            LocalDate toDate = LocalDate.parse((String) filters.get("dateTo"));
            String subjectCode = (String) filters.get("subject");
            String lecturerCode = (String) filters.get("lecturer");
            String classCode = (String) filters.get("class");

            byte[] excelData = diemDanhService.exportAttendanceReport(
                    fromDate, toDate, subjectCode, lecturerCode, classCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "bao-cao-diem-danh-" + fromDate + "-" + toDate + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            log.error("Error exporting attendance report:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
// Thêm vào DiemDanhController.java

    /**
     * API điểm danh thủ công
     */
    @PostMapping("/manual")
    public ResponseEntity<List<DiemDanhDTO>> createManualAttendance(
            @RequestParam String maLich,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayDiemDanh,
            @RequestBody List<ManualAttendanceRequest> requests) {

        List<DiemDanhDTO> results = diemDanhService.createBulkManualAttendance(maLich, ngayDiemDanh, requests);
        return ResponseEntity.ok(results);
    }

    /**
     * API lấy thống kê điểm danh theo lớp
     */
    @GetMapping("/stats/class/{maLhp}")
    public ResponseEntity<AttendanceStatsDTO> getAttendanceStatsByClass(@PathVariable String maLhp) {
        AttendanceStatsDTO stats = diemDanhService.getAttendanceStatsByClass(maLhp);
        return ResponseEntity.ok(stats);
    }

    /**
     * API lấy tỷ lệ điểm danh từng sinh viên
     */
    @GetMapping("/stats/students/{maLhp}")
    public ResponseEntity<List<StudentAttendanceStatsDTO>> getStudentAttendanceByClass(@PathVariable String maLhp) {
        List<StudentAttendanceStatsDTO> stats = diemDanhService.getStudentAttendanceByClass(maLhp);
        return ResponseEntity.ok(stats);
    }

    /**
     * API lấy điểm danh theo lớp và ngày
     */
    @GetMapping("/class/{maLhp}")
    public ResponseEntity<List<DiemDanhDTO>> getAttendanceByClassAndDate(
            @PathVariable String maLhp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {

        List<DiemDanhDTO> attendances = diemDanhService.getByClassAndDate(maLhp, ngay);
        return ResponseEntity.ok(attendances);
    }
    /**
     * API debug: Kiểm tra lịch học tại phòng
     * GET /api/diemdanh/debug/room/{maPhong}
     */
    @GetMapping("/debug/room/{maPhong}")
    public ResponseEntity<?> debugScheduleAtRoom(@PathVariable String maPhong) {
        try {
            Map<String, Object> debugInfo = diemDanhService.getScheduleDebugInfo(maPhong);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thông tin debug lịch học tại phòng " + maPhong,
                    "data", debugInfo
            ));
        } catch (Exception e) {
            log.error("Error getting debug info for room {}: {}", maPhong, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thông tin debug: " + e.getMessage()
            ));
        }
    }
    /**
     * API test điểm danh cho camera (có log chi tiết)
     * POST /api/diemdanh/test-camera-attendance
     */
    @PostMapping("/test-camera-attendance")
    public ResponseEntity<?> testCameraAttendance(@RequestBody Map<String, Object> testData) {
        try {
            String studentId = (String) testData.get("studentId");
            Long cameraId = Long.valueOf(testData.get("cameraId").toString());

            log.info("🧪 TEST ATTENDANCE - Student: {}, Camera: {}", studentId, cameraId);

            // Thêm thông tin debug trước khi gọi recordAttendanceFromCamera
            Optional<Camera> cameraOpt = cameraRepository.findById(cameraId);
            if (cameraOpt.isPresent()) {
                Camera camera = cameraOpt.get();
                String maPhong = camera.getMaPhong() != null ? camera.getMaPhong().getMaPhong() : null;

                if (maPhong != null) {
                    Map<String, Object> debugInfo = diemDanhService.getScheduleDebugInfo(maPhong);
                    log.info("🔍 Debug info cho phòng {}: {}", maPhong, debugInfo);
                }
            }

            DiemDanhDTO result = diemDanhService.recordAttendanceFromCamera(studentId, cameraId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test điểm danh thành công",
                    "data", result
            ));

        } catch (Exception e) {
            log.error("❌ TEST ATTENDANCE FAILED: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test điểm danh thất bại: " + e.getMessage(),
                    "errorDetails", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * API lấy tất cả lịch học hôm nay của phòng
     * GET /api/diemdanh/room/{maPhong}/today-schedules
     */
    @GetMapping("/room/{maPhong}/today-schedules")
    public ResponseEntity<?> getTodaySchedulesByRoom(@PathVariable String maPhong) {
        try {
            LocalDate today = LocalDate.now();
            int dayOfWeek = today.getDayOfWeek().getValue();

            List<LichHoc> schedules = lichHocRepository
                    .findByPhongHocMaPhongAndThuAndIsActiveTrue(maPhong, dayOfWeek);

            List<Map<String, Object>> scheduleInfo = schedules.stream().map(lichHoc -> {
                LocalTime startTime = LocalTime.of(7, 0).plusMinutes((lichHoc.getTietBatDau() - 1) * 50);
                LocalTime endTime = startTime.plusMinutes(lichHoc.getSoTiet() * 50);

                Map<String, Object> info = new HashMap<>();
                info.put("maLich", lichHoc.getMaLich());
                info.put("maLhp", lichHoc.getLopHocPhan() != null ? lichHoc.getLopHocPhan().getMaLhp() : "N/A");
                info.put("tietBatDau", lichHoc.getTietBatDau());
                info.put("soTiet", lichHoc.getSoTiet());
                info.put("thoiGianHoc", startTime + " - " + endTime);
                info.put("thoiGianChoPhepDiemDanh",
                        startTime.minusMinutes(60) + " - " + endTime.plusMinutes(30));
                info.put("isActive", lichHoc.isActive());
                return info;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lịch học hôm nay tại phòng " + maPhong,
                    "data", Map.of(
                            "date", today.toString(),
                            "dayOfWeek", dayOfWeek,
                            "roomCode", maPhong,
                            "totalSchedules", schedules.size(),
                            "schedules", scheduleInfo
                    )
            ));

        } catch (Exception e) {
            log.error("Error getting today schedules for room {}: {}", maPhong, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy lịch học: " + e.getMessage()
            ));
        }
    }
    /**
     * API kiểm tra camera và phòng học
     * GET /api/diemdanh/camera/{cameraId}/info
     */
    @GetMapping("/camera/{cameraId}/info")
    public ResponseEntity<?> getCameraInfo(@PathVariable Long cameraId) {
        try {
            Optional<Camera> cameraOpt = cameraRepository.findById(cameraId);

            if (cameraOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Camera không tồn tại với ID: " + cameraId
                ));
            }

            Camera camera = cameraOpt.get();
            PhongHoc phong = camera.getMaPhong();

            Map<String, Object> cameraInfo = new HashMap<>();
            cameraInfo.put("cameraId", camera.getId());
            cameraInfo.put("tenCamera", camera.getTenCamera());
            cameraInfo.put("ipAddress", camera.getIpAddress());
            cameraInfo.put("isActive", camera.getIsActive());

            if (phong != null) {
                cameraInfo.put("maPhong", phong.getMaPhong());
                cameraInfo.put("tenPhong", phong.getTenPhong());
                cameraInfo.put("loaiPhong", phong.getLoaiPhong());
                cameraInfo.put("sucChua", phong.getSucChua());

                // Lấy thêm thông tin lịch học hôm nay
                Map<String, Object> debugInfo = diemDanhService.getScheduleDebugInfo(phong.getMaPhong());
                cameraInfo.put("todaySchedules", debugInfo);
            } else {
                cameraInfo.put("warning", "Camera chưa được gán phòng học");
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thông tin camera " + cameraId,
                    "data", cameraInfo
            ));

        } catch (Exception e) {
            log.error("Error getting camera info {}: {}", cameraId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thông tin camera: " + e.getMessage()
            ));
        }
    }

    /**
     * API lưu phiên điểm danh - cho phép lưu nhiều bản ghi điểm danh cùng lúc
     * POST /api/diemdanh/save-session
     */
    @PostMapping("/save-session")
    public ResponseEntity<Map<String, Object>> saveAttendanceSession(@RequestBody AttendanceSessionRequestDTO request) {
        try {
            log.info("Saving attendance session for class: {}, date: {}", request.getMaLhp(), request.getNgayDiemDanh());

            Map<String, Object> result = diemDanhService.saveAttendanceSession(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lưu phiên điểm danh thành công",
                    "data", result
            ));

        } catch (Exception e) {
            log.error("Error saving attendance session:", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lưu phiên điểm danh: " + e.getMessage()
            ));
        }
    }
}
