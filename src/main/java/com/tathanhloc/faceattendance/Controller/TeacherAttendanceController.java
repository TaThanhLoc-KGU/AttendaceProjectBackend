package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.TeacherAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho hệ thống điểm danh giảng viên
 * Hỗ trợ cả lịch học cũ và week-based schedule
 */
@RestController
@RequestMapping("/api/teacher/attendance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Attendance", description = "API cho hệ thống điểm danh giảng viên")
@PreAuthorize("hasRole('LECTURER')")
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;

    /**
     * Lấy lịch dạy của giảng viên theo ngày
     */
    @Operation(summary = "Lấy lịch dạy theo ngày", description = "Lấy tất cả tiết dạy của giảng viên trong một ngày")
    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<TeacherScheduleDTO>>> getTeacherSchedule(
            @Parameter(description = "Ngày cần xem lịch (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);
            List<TeacherScheduleDTO> schedules = teacherAttendanceService.getTeacherScheduleByDate(maGv, date);

            return ResponseEntity.ok(ApiResponse.success(schedules, "Lấy lịch dạy thành công"));

        } catch (Exception e) {
            log.error("❌ Error getting teacher schedule: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy lịch dạy: " + e.getMessage()));
        }
    }

    /**
     * Lấy phiên điểm danh cho 1 tiết học
     */
    @Operation(summary = "Lấy phiên điểm danh", description = "Lấy danh sách sinh viên và trạng thái điểm danh cho 1 tiết học")
    @GetMapping("/session/{scheduleId}")
    public ResponseEntity<ApiResponse<TeacherAttendanceSessionDTO>> getAttendanceSession(
            @Parameter(description = "ID của lịch học hoặc schedule instance")
            @PathVariable String scheduleId,
            @Parameter(description = "Ngày học (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Có phải week-based schedule không")
            @RequestParam(defaultValue = "false") boolean isWeekBased,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            // Verify lecturer ownership (could add additional security check here)
            String maGv = getUserLecturerCode(userDetails);

            TeacherAttendanceSessionDTO session = teacherAttendanceService
                    .getAttendanceSession(scheduleId, date, isWeekBased);

            return ResponseEntity.ok(ApiResponse.success(session, "Lấy phiên điểm danh thành công"));

        } catch (Exception e) {
            log.error("❌ Error getting attendance session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy phiên điểm danh: " + e.getMessage()));
        }
    }

    /**
     * Điểm danh cho 1 sinh viên
     */
    @Operation(summary = "Điểm danh sinh viên", description = "Thực hiện điểm danh cho 1 sinh viên trong tiết học")
    @PostMapping("/mark")
    public ResponseEntity<ApiResponse<AttendanceResultDTO>> markAttendance(
            @Valid @RequestBody TeacherAttendanceRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            // Set created by from authenticated user
            request.setCreatedBy(userDetails.getUsername());

            AttendanceResultDTO result = teacherAttendanceService.markAttendance(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result, "Điểm danh thành công"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.getMessage(), result));
            }

        } catch (Exception e) {
            log.error("❌ Error marking attendance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi điểm danh: " + e.getMessage()));
        }
    }

    /**
     * Điểm danh hàng loạt
     */
    @Operation(summary = "Điểm danh hàng loạt", description = "Thực hiện điểm danh cho nhiều sinh viên cùng lúc")
    @PostMapping("/batch-mark")
    public ResponseEntity<ApiResponse<BatchAttendanceResultDTO>> markBatchAttendance(
            @Valid @RequestBody BatchAttendanceRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            // Set created by for all requests
            String createdBy = userDetails.getUsername();
            request.getAttendanceList().forEach(req -> req.setCreatedBy(createdBy));

            BatchAttendanceResultDTO result = teacherAttendanceService.markBatchAttendance(request);

            return ResponseEntity.ok(ApiResponse.success(result,
                    String.format("Hoàn thành điểm danh: %d thành công, %d thất bại",
                            result.getSuccessCount(), result.getFailCount())));

        } catch (Exception e) {
            log.error("❌ Error marking batch attendance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi điểm danh hàng loạt: " + e.getMessage()));
        }
    }

    /**
     * Điểm danh nhanh - tất cả có mặt
     */
    @Operation(summary = "Điểm danh nhanh - tất cả có mặt",
            description = "Đánh dấu tất cả sinh viên trong tiết học là có mặt")
    @PostMapping("/mark-all-present")
    public ResponseEntity<ApiResponse<BatchAttendanceResultDTO>> markAllPresent(
            @Parameter(description = "ID lịch học")
            @RequestParam String scheduleId,
            @Parameter(description = "Ngày học")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Week-based schedule")
            @RequestParam(defaultValue = "false") boolean isWeekBased,
            @Parameter(description = "Ghi chú")
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            // Lấy danh sách sinh viên
            TeacherAttendanceSessionDTO session = teacherAttendanceService
                    .getAttendanceSession(scheduleId, date, isWeekBased);

            // Tạo batch request cho tất cả sinh viên
            BatchAttendanceRequestDTO batchRequest = new BatchAttendanceRequestDTO();
            List<TeacherAttendanceRequestDTO> attendanceList = session.getStudents().stream()
                    .map(student -> TeacherAttendanceRequestDTO.builder()
                            .maSv(student.getMaSv())
                            .scheduleId(scheduleId)
                            .date(date)
                            .isWeekBased(isWeekBased)
                            .trangThai(TrangThaiDiemDanhEnum.CO_MAT)
                            .thoiGianVao(LocalDateTime.now())
                            .ghiChu(note != null ? note : "Điểm danh tất cả có mặt")
                            .createdBy(userDetails.getUsername())
                            .build())
                    .toList();

            batchRequest.setAttendanceList(attendanceList);

            BatchAttendanceResultDTO result = teacherAttendanceService.markBatchAttendance(batchRequest);

            return ResponseEntity.ok(ApiResponse.success(result,
                    String.format("Đã điểm danh tất cả có mặt: %d sinh viên", result.getSuccessCount())));

        } catch (Exception e) {
            log.error("❌ Error marking all present: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi điểm danh tất cả có mặt: " + e.getMessage()));
        }
    }

    /**
     * Lấy thống kê điểm danh theo ngày
     */
    @Operation(summary = "Thống kê điểm danh theo ngày",
            description = "Lấy thống kê tổng quan điểm danh của giảng viên trong 1 ngày")
    @GetMapping("/daily-stats")
    public ResponseEntity<ApiResponse<TeacherDailyStatsDTO>> getDailyStats(
            @Parameter(description = "Ngày cần thống kê")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);
            TeacherDailyStatsDTO stats = teacherAttendanceService.getDailyAttendanceStats(maGv, date);

            return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê thành công"));

        } catch (Exception e) {
            log.error("❌ Error getting daily stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy thống kê: " + e.getMessage()));
        }
    }

    /**
     * Export báo cáo điểm danh theo ngày
     */
    @Operation(summary = "Export báo cáo điểm danh",
            description = "Xuất báo cáo chi tiết điểm danh theo ngày")
    @GetMapping("/export-daily")
    public ResponseEntity<ApiResponse<TeacherAttendanceReportDTO>> exportDailyReport(
            @Parameter(description = "Ngày cần xuất báo cáo")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);
            TeacherAttendanceReportDTO report = teacherAttendanceService.exportDailyAttendance(maGv, date);

            return ResponseEntity.ok(ApiResponse.success(report, "Export báo cáo thành công"));

        } catch (Exception e) {
            log.error("❌ Error exporting daily report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi export báo cáo: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật trạng thái điểm danh của sinh viên
     */
    @Operation(summary = "Cập nhật trạng thái điểm danh",
            description = "Thay đổi trạng thái điểm danh đã có của sinh viên")
    @PutMapping("/update-status")
    public ResponseEntity<ApiResponse<AttendanceResultDTO>> updateAttendanceStatus(
            @Parameter(description = "Mã sinh viên")
            @RequestParam String maSv,
            @Parameter(description = "ID lịch học")
            @RequestParam String scheduleId,
            @Parameter(description = "Ngày học")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Trạng thái mới")
            @RequestParam TrangThaiDiemDanhEnum trangThai,
            @Parameter(description = "Week-based schedule")
            @RequestParam(defaultValue = "false") boolean isWeekBased,
            @Parameter(description = "Ghi chú")
            @RequestParam(required = false) String ghiChu,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            TeacherAttendanceRequestDTO request = TeacherAttendanceRequestDTO.builder()
                    .maSv(maSv)
                    .scheduleId(scheduleId)
                    .date(date)
                    .isWeekBased(isWeekBased)
                    .trangThai(trangThai)
                    .ghiChu(ghiChu)
                    .createdBy(userDetails.getUsername())
                    .build();

            AttendanceResultDTO result = teacherAttendanceService.markAttendance(request);

            return ResponseEntity.ok(ApiResponse.success(result, "Cập nhật trạng thái thành công"));

        } catch (Exception e) {
            log.error("❌ Error updating attendance status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi cập nhật trạng thái: " + e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử điểm danh của sinh viên trong lớp
     */
    @Operation(summary = "Lịch sử điểm danh sinh viên",
            description = "Xem lịch sử điểm danh của 1 sinh viên trong lớp học phần")
    @GetMapping("/student-history/{maSv}")
    public ResponseEntity<ApiResponse<List<AttendanceRecordDTO>>> getStudentAttendanceHistory(
            @Parameter(description = "Mã sinh viên")
            @PathVariable String maSv,
            @Parameter(description = "Mã lớp học phần")
            @RequestParam String maLhp,
            @Parameter(description = "Từ ngày")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Đến ngày")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            // TODO: Implement student attendance history
            // This would require additional repository methods

            return ResponseEntity.ok(ApiResponse.success(List.of(), "Tính năng đang phát triển"));

        } catch (Exception e) {
            log.error("❌ Error getting student history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy lịch sử: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra quyền điểm danh (xem giảng viên có được phép điểm danh lớp này không)
     */
    @Operation(summary = "Kiểm tra quyền điểm danh",
            description = "Xác thực giảng viên có quyền điểm danh lớp này không")
    @GetMapping("/check-permission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAttendancePermission(
            @Parameter(description = "ID lịch học")
            @RequestParam String scheduleId,
            @Parameter(description = "Week-based schedule")
            @RequestParam(defaultValue = "false") boolean isWeekBased,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);

            // TODO: Implement permission check logic
            Map<String, Object> result = Map.of(
                    "hasPermission", true,
                    "lecturerCode", maGv,
                    "scheduleId", scheduleId,
                    "isWeekBased", isWeekBased,
                    "message", "Có quyền điểm danh"
            );

            return ResponseEntity.ok(ApiResponse.success(result, "Kiểm tra quyền thành công"));

        } catch (Exception e) {
            log.error("❌ Error checking permission: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi kiểm tra quyền: " + e.getMessage()));
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Lấy mã giảng viên từ user details
     */
    private String getUserLecturerCode(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getTaiKhoan().getGiangVien() == null) {
            throw new RuntimeException("Tài khoản không có thông tin giảng viên");
        }
        return userDetails.getTaiKhoan().getGiangVien().getMaGv();
    }

    /**
     * Exception handler cho controller này
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("❌ Unhandled exception in TeacherAttendanceController: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống: " + e.getMessage()));
    }
}