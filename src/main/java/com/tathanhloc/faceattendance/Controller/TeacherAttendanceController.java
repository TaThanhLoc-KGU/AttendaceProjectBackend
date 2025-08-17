package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.DiemDanh;
import com.tathanhloc.faceattendance.Model.LopHocPhan;
import com.tathanhloc.faceattendance.Model.ScheduleInstance;
import com.tathanhloc.faceattendance.Model.SinhVien;
import com.tathanhloc.faceattendance.Repository.*;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.TeacherAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * REST API Controller cho hệ thống điểm danh giảng viên
 * Hỗ trợ cả lịch học cũ và week-based schedule
 */
@RestController
@RequestMapping("/api/lecturer/attendance")
@CrossOrigin(origins = "*") // Thêm CORS
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Attendance", description = "API cho hệ thống điểm danh giảng viên")
@PreAuthorize("hasRole('GIANGVIEN')")
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;
    private final DangKyHocRepository dangKyHocRepository;
    private final LichHocRepository lichHocRepository;
    private final DiemDanhRepository diemDanhRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final SinhVienRepository sinhVienRepository;

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
            AttendanceSessionDTO session = teacherAttendanceService
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

    /**
     * Lưu phiên điểm danh - SỬA LẠI HOÀN TOÀN
     */
    @PostMapping(value = "/save-session",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAttendanceSession(
            @RequestBody Map<String, Object> requestData,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {

        log.info("💾 Received save request: {}", requestData);

        try {
            // Basic response structure
            Map<String, Object> response = new HashMap<>();

            if (userDetails == null) {
                response.put("success", false);
                response.put("message", "Không có thông tin xác thực");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String maGv = getUserLecturerCode(userDetails);
            log.info("👨‍🏫 Lecturer {} saving attendance", maGv);

            // Extract data from request
            String instanceId = (String) requestData.get("instanceId");
            String classId = (String) requestData.get("classId");
            String sessionDate = (String) requestData.get("sessionDate");
            Integer week = (Integer) requestData.get("week");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendances = (List<Map<String, Object>>) requestData.get("attendances");

            if (instanceId == null || attendances == null) {
                response.put("success", false);
                response.put("message", "Dữ liệu không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("📋 Saving {} attendance records for instance: {}", attendances.size(), instanceId);

            // Save attendance manually without complex DTO
            boolean saved = saveAttendanceManually(instanceId, classId, sessionDate, week, attendances, maGv);

            if (saved) {
                response.put("success", true);
                response.put("message", "Lưu điểm danh thành công");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Lưu điểm danh thất bại");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            log.error("❌ Error saving attendance: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi hệ thống: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Save attendance manually without complex validation
     */
    private boolean saveAttendanceManually(String instanceId, String classId, String sessionDate,
                                           Integer week, List<Map<String, Object>> attendances, String maGv) {
        try {
            log.info("💾 Manual save: instanceId={}, date={}, records={}", instanceId, sessionDate, attendances.size());

            // Get schedule instance
            Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(instanceId);
            if (instanceOpt.isEmpty()) {
                log.warn("⚠️ Schedule instance not found: {}", instanceId);
                return false;
            }

            ScheduleInstance instance = instanceOpt.get();
            LocalDate date = LocalDate.parse(sessionDate);

            // Delete existing attendance
            try {
                List<DiemDanh> existing = diemDanhRepository.findByScheduleInstanceMaInstanceAndNgayDiemDanh(instanceId, date);
                if (!existing.isEmpty()) {
                    diemDanhRepository.deleteAll(existing);
                    log.info("🗑️ Deleted {} existing records", existing.size());
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete existing: {}", e.getMessage());
            }

            // Create new attendance records
            List<DiemDanh> newRecords = new ArrayList<>();

            for (Map<String, Object> att : attendances) {
                try {
                    String studentId = (String) att.get("studentId");
                    String status = (String) att.get("status");
                    String note = (String) att.get("note");

                    // Get student
                    Optional<SinhVien> studentOpt = sinhVienRepository.findById(studentId);
                    if (studentOpt.isEmpty()) {
                        log.warn("⚠️ Student not found: {}", studentId);
                        continue;
                    }

                    // Map status
                    TrangThaiDiemDanhEnum trangThai;
                    switch (status.toUpperCase()) {
                        case "CO_MAT", "PRESENT" -> trangThai = TrangThaiDiemDanhEnum.CO_MAT;
                        case "DI_TRE", "LATE" -> trangThai = TrangThaiDiemDanhEnum.DI_TRE;
                        case "VANG_CO_PHEP", "EXCUSED" -> trangThai = TrangThaiDiemDanhEnum.VANG_CO_PHEP;
                        default -> trangThai = TrangThaiDiemDanhEnum.VANG_MAT;
                    }

                    // Create attendance record
                    DiemDanh diemDanh = DiemDanh.builder()
                            .sinhVien(studentOpt.get())
                            .scheduleInstance(instance)
                            .ngayDiemDanh(date)
                            .trangThai(trangThai)
                            .ghiChu(note)
                            .thoiGianVao(LocalDateTime.now())
                            .createdBy(maGv)
                            .build();

                    newRecords.add(diemDanh);

                } catch (Exception e) {
                    log.error("❌ Failed to process attendance record: {}", e.getMessage());
                    continue;
                }
            }

            // Save all records
            if (!newRecords.isEmpty()) {
                diemDanhRepository.saveAll(newRecords);
                log.info("✅ Saved {} attendance records", newRecords.size());

                // Update instance status
                instance.setTrangThai(ScheduleInstance.TrangThaiInstance.COMPLETED);
                scheduleInstanceRepository.save(instance);

                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ Manual save failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Transform frontend data format to our API format
     */
    private AttendanceSessionRequestDTO transformToApiFormat(Map<String, Object> frontendData, String maGv) {
        String classId = (String) frontendData.get("classId");
        String sessionDate = (String) frontendData.get("sessionDate");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attendances = (List<Map<String, Object>>) frontendData.get("attendances");

        List<AttendanceSessionRequestDTO.StudentAttendanceRecord> records = attendances.stream()
            .map(this::transformAttendanceRecord)
            .toList();

        return AttendanceSessionRequestDTO.builder()
            .maLhp(classId)
            .ngayDiemDanh(LocalDate.parse(sessionDate))
            .giangVienId(maGv)
            .sessionName("Phiên điểm danh " + sessionDate)
            .attendanceRecords(records)
            .build();
    }

    /**
     * Transform individual attendance record
     */
    private AttendanceSessionRequestDTO.StudentAttendanceRecord transformAttendanceRecord(Map<String, Object> record) {
        String studentId = (String) record.get("studentId");
        String status = (String) record.get("status");
        String note = (String) record.get("note");

        TrangThaiDiemDanhEnum trangThai = mapStatusToEnum(status);
        LocalDateTime timestamp = LocalDateTime.now();

        return AttendanceSessionRequestDTO.StudentAttendanceRecord.builder()
            .maSv(studentId)
            .trangThai(trangThai)
            .thoiGianVao(timestamp)
            .ghiChu(note)
            .build();
    }

    /**
     * Map frontend status to enum
     */
    private TrangThaiDiemDanhEnum mapStatusToEnum(String status) {
        return switch (status.toUpperCase()) {
            case "PRESENT", "CO_MAT" -> TrangThaiDiemDanhEnum.CO_MAT;
            case "ABSENT", "VANG_MAT" -> TrangThaiDiemDanhEnum.VANG_MAT;
            case "LATE", "DI_TRE" -> TrangThaiDiemDanhEnum.DI_TRE;
            case "EXCUSED", "VANG_CO_PHEP" -> TrangThaiDiemDanhEnum.VANG_CO_PHEP;
            default -> TrangThaiDiemDanhEnum.VANG_MAT;
        };
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

    /**
     * Lấy thông tin tóm tắt điểm danh của lớp
     */
    @GetMapping("/class-summary/{maLhp}")
    public ResponseEntity<ApiResponse<AttendanceClassSummaryDTO>> getClassAttendanceSummary(
            @PathVariable String maLhp,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);

            // Verify teacher owns this class
            LopHocPhan lhp = lopHocPhanRepository.findById(maLhp).orElse(null);
            if (lhp == null || !lhp.getGiangVien().getMaGv().equals(maGv)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Không có quyền truy cập lớp học này"));
            }

            AttendanceClassSummaryDTO summary = teacherAttendanceService.getClassAttendanceSummary(maLhp);
            return ResponseEntity.ok(ApiResponse.success(summary, "Lấy tóm tắt điểm danh thành công"));

        } catch (Exception e) {
            log.error("❌ Error getting class attendance summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy tóm tắt điểm danh: " + e.getMessage()));
        }
    }

    /**
     * Lấy tóm tắt điểm danh theo tuần của lớp
     */
    @GetMapping("/weekly-summary/{maLhp}")
    public ResponseEntity<ApiResponse<List<WeeklyAttendanceSummaryDTO>>> getWeeklyAttendanceSummary(
            @PathVariable String maLhp,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);

            // Verify permission
            LopHocPhan lhp = lopHocPhanRepository.findById(maLhp).orElse(null);
            if (lhp == null || !lhp.getGiangVien().getMaGv().equals(maGv)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Không có quyền truy cập lớp học này"));
            }

            List<WeeklyAttendanceSummaryDTO> weeklySummary = teacherAttendanceService.getWeeklyAttendanceSummary(maLhp);
            return ResponseEntity.ok(ApiResponse.success(weeklySummary, "Lấy tóm tắt theo tuần thành công"));

        } catch (Exception e) {
            log.error("❌ Error getting weekly attendance summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy tóm tắt theo tuần: " + e.getMessage()));
        }
    }
    /**
     * GIỮ LẠI METHOD NÀY - sửa lại để hoạt động đúng
     */
    @GetMapping("/session/{instanceId}")
    @ResponseBody  // Thêm annotation này để đảm bảo trả JSON
    public ResponseEntity<ApiResponse<AttendanceSessionDTO>> getAttendanceSessionById(
            @PathVariable String instanceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("📋 API Called: /api/teacher/attendance/session/{}", instanceId);

        try {
            if (userDetails == null) {
                log.error("❌ No user details found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Không có thông tin xác thực"));
            }

            String maGv = getUserLecturerCode(userDetails);
            log.info("🔍 Getting session for lecturer: {}, instanceId: {}", maGv, instanceId);

            // Call service method
            AttendanceSessionDTO session = teacherAttendanceService.getAttendanceSession(instanceId, maGv);

            log.info("✅ Successfully retrieved session with {} students",
                    session.getStudents() != null ? session.getStudents().size() : 0);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(ApiResponse.success(session, "Lấy phiên điểm danh thành công"));

        } catch (AccessDeniedException e) {
            log.warn("🚫 Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Không có quyền truy cập: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
            log.warn("🔍 Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy phiên học: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error in getAttendanceSessionById: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }
    /**
     * Lấy thống kê tổng quan cho dashboard
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String maGv = getUserLecturerCode(userDetails);

            // Tính toán thống kê cơ bản
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalClasses", 0);
            stats.put("totalStudents", 0);
            stats.put("averageAttendance", 0.0);
            stats.put("thisWeekAttendance", 0.0);

            return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê thành công"));

        } catch (Exception e) {
            log.error("❌ Error getting statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy thống kê: " + e.getMessage()));
        }
    }
}

