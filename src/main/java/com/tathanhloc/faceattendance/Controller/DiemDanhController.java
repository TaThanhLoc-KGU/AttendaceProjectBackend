package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
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
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/diemdanh")
@RequiredArgsConstructor
public class DiemDanhController {

    private final DiemDanhService diemDanhService;

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
    public ResponseEntity<List<DiemDanhDTO>> getByMaLich(@PathVariable String maLich) {
        return ResponseEntity.ok(diemDanhService.getByMaLich(maLich));
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

}
