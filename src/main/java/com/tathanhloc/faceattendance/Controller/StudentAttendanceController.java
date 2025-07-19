package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.AttendanceHistoryDTO;
import com.tathanhloc.faceattendance.DTO.AttendanceStatisticsDTO;
import com.tathanhloc.faceattendance.DTO.SubjectAttendanceDTO;
import com.tathanhloc.faceattendance.Service.StudentAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final StudentAttendanceService studentAttendanceService;

    /**
     * Lấy thống kê điểm danh của sinh viên hiện tại
     */
    @GetMapping("/attendance-statistics")
    public ResponseEntity<AttendanceStatisticsDTO> getAttendanceStatistics() {
        try {
            // Lấy mã sinh viên từ authentication context
            String maSv = getCurrentStudentId();

            AttendanceStatisticsDTO statistics = studentAttendanceService.getAttendanceStatistics(maSv);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê điểm danh: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy lịch sử điểm danh của sinh viên hiện tại
     */
    @GetMapping("/attendance-history")
    public ResponseEntity<List<AttendanceHistoryDTO>> getAttendanceHistory(
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false) String subject) {

        try {
            String maSv = getCurrentStudentId();

            List<AttendanceHistoryDTO> history = studentAttendanceService.getAttendanceHistory(maSv, limit, subject);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử điểm danh: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách môn học của sinh viên (cho dropdown filter)
     */
    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectAttendanceDTO>> getStudentSubjects() {
        try {
            String maSv = getCurrentStudentId();

            List<SubjectAttendanceDTO> subjects = studentAttendanceService.getStudentSubjects(maSv);
            return ResponseEntity.ok(subjects);

        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách môn học: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Utility method để lấy mã sinh viên từ authentication context
     */
    private String getCurrentStudentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() != null) {
            // Giả sử username chính là mã sinh viên
            return authentication.getName();
        }

        // Fallback: nếu chưa implement authentication, dùng mã cố định để test
        // TODO: Implement proper authentication
        return "20210001"; // Thay bằng logic authentication thực tế
    }
}