// ===== AttendanceValidationService.java =====
package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.Config.TeacherAttendanceConfig;
import com.tathanhloc.faceattendance.DTO.TeacherAttendanceRequestDTO;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service validation cho điểm danh
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceValidationService {

    private final TeacherAttendanceConfig config;
    private final LichHocRepository lichHocRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final DangKyHocRepository dangKyHocRepository;
    private final SinhVienRepository sinhVienRepository;
    private final DiemDanhRepository diemDanhRepository;

    /**
     * Validate toàn bộ request điểm danh
     */
    public ValidationResult validateAttendanceRequest(TeacherAttendanceRequestDTO request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // 1. Validate basic fields
            validateBasicFields(request, errors);

            // 2. Validate student exists and is registered
            validateStudentRegistration(request, errors);

            // 3. Validate schedule exists and lecturer permission
            validateScheduleAndPermission(request, errors);

            // 4. Validate attendance timing
            validateAttendanceTiming(request, warnings);

            // 5. Validate duplicate attendance
            validateDuplicateAttendance(request, warnings);

            // 6. Validate status logic
            validateStatusLogic(request, warnings);

        } catch (Exception e) {
            log.error("❌ Validation error: {}", e.getMessage(), e);
            errors.add("Lỗi hệ thống khi validate: " + e.getMessage());
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    /**
     * Validate batch attendance request
     */
    public ValidationResult validateBatchAttendanceRequest(List<TeacherAttendanceRequestDTO> requests) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (requests == null || requests.isEmpty()) {
            errors.add("Danh sách điểm danh trống");
            return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .build();
        }

        if (requests.size() > config.getMaxStudentsPerBatch()) {
            errors.add(String.format("Vượt quá số lượng sinh viên cho phép trong 1 batch (%d)",
                    config.getMaxStudentsPerBatch()));
        }

        // Validate từng request
        for (int i = 0; i < requests.size(); i++) {
            TeacherAttendanceRequestDTO request = requests.get(i);
            ValidationResult result = validateAttendanceRequest(request);

            if (!result.isValid()) {
                errors.add(String.format("Request %d: %s", i + 1, String.join(", ", result.getErrors())));
            }

            if (!result.getWarnings().isEmpty()) {
                warnings.add(String.format("Request %d: %s", i + 1, String.join(", ", result.getWarnings())));
            }
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    // ===== PRIVATE VALIDATION METHODS =====

    private void validateBasicFields(TeacherAttendanceRequestDTO request, List<String> errors) {
        if (request.getMaSv() == null || request.getMaSv().trim().isEmpty()) {
            errors.add("Mã sinh viên không được để trống");
        }

        if (request.getScheduleId() == null || request.getScheduleId().trim().isEmpty()) {
            errors.add("ID lịch học không được để trống");
        }

        if (request.getDate() == null) {
            errors.add("Ngày điểm danh không được để trống");
        }

        if (request.getTrangThai() == null) {
            errors.add("Trạng thái điểm danh không được để trống");
        }

        // Validate date not in future
        if (request.getDate() != null && request.getDate().isAfter(LocalDate.now())) {
            errors.add("Không thể điểm danh cho ngày trong tương lai");
        }

        // Validate date not too old
        if (request.getDate() != null && request.getDate().isBefore(LocalDate.now().minusDays(30))) {
            errors.add("Không thể điểm danh cho ngày quá xa trong quá khứ (>30 ngày)");
        }
    }

    private void validateStudentRegistration(TeacherAttendanceRequestDTO request, List<String> errors) {
        if (request.getMaSv() == null || request.getScheduleId() == null) {
            return; // Already handled in basic validation
        }

        try {
            // Check student exists
            boolean studentExists = sinhVienRepository.existsById(request.getMaSv());
            if (!studentExists) {
                errors.add("Sinh viên không tồn tại: " + request.getMaSv());
                return;
            }

            // Get class code from schedule
            String maLhp = getClassCodeFromSchedule(request.getScheduleId(), request.isWeekBased());
            if (maLhp == null) {
                errors.add("Không tìm thấy lớp học phần từ lịch học");
                return;
            }

            // Check student registration
            boolean isRegistered = dangKyHocRepository.existsByMaSvAndMaLhpAndActive(
                    request.getMaSv(), maLhp);

            if (!isRegistered) {
                errors.add("Sinh viên chưa đăng ký lớp học phần này hoặc đăng ký không còn hiệu lực");
            }

        } catch (Exception e) {
            log.error("Error validating student registration: {}", e.getMessage());
            errors.add("Lỗi khi kiểm tra đăng ký sinh viên");
        }
    }

    private void validateScheduleAndPermission(TeacherAttendanceRequestDTO request, List<String> errors) {
        if (request.getScheduleId() == null) {
            return;
        }

        try {
            if (request.isWeekBased()) {
                // Validate schedule instance exists
                boolean exists = scheduleInstanceRepository.existsById(request.getScheduleId());
                if (!exists) {
                    errors.add("Lịch học không tồn tại: " + request.getScheduleId());
                }
            } else {
                // Validate traditional schedule exists
                boolean exists = lichHocRepository.existsById(request.getScheduleId());
                if (!exists) {
                    errors.add("Lịch học không tồn tại: " + request.getScheduleId());
                }
            }

        } catch (Exception e) {
            log.error("Error validating schedule: {}", e.getMessage());
            errors.add("Lỗi khi kiểm tra lịch học");
        }
    }

    private void validateAttendanceTiming(TeacherAttendanceRequestDTO request, List<String> warnings) {
        if (request.getDate() == null || request.getScheduleId() == null) {
            return;
        }

        try {
            // Get period info from schedule
            Integer period = getPeriodFromSchedule(request.getScheduleId(), request.isWeekBased());
            if (period == null) {
                warnings.add("Không thể xác định tiết học để kiểm tra thời gian");
                return;
            }

            LocalTime currentTime = LocalTime.now();
            LocalDate currentDate = LocalDate.now();

            // Only check timing for current date
            if (!request.getDate().equals(currentDate)) {
                return;
            }

            // Check if within attendance window
            if (!config.isWithinAttendanceWindow(period, currentTime)) {
                warnings.add("Ngoài thời gian cho phép điểm danh cho tiết " + period);
            }

            // Check if late attendance
            if (TrangThaiDiemDanhEnum.CO_MAT.equals(request.getTrangThai()) &&
                    config.isLateAttendance(period, currentTime)) {
                warnings.add("Sinh viên đến muộn, có thể cần chuyển trạng thái thành 'Đi trễ'");
            }

        } catch (Exception e) {
            log.error("Error validating timing: {}", e.getMessage());
            warnings.add("Không thể kiểm tra thời gian điểm danh");
        }
    }

    private void validateDuplicateAttendance(TeacherAttendanceRequestDTO request, List<String> warnings) {
        if (request.getMaSv() == null || request.getScheduleId() == null || request.getDate() == null) {
            return;
        }

        try {
            List<DiemDanh> existing = diemDanhRepository.findByScheduleIdAndDateAndStudent(
                    request.getScheduleId(), request.getDate(), request.getMaSv());

            if (!existing.isEmpty()) {
                DiemDanh existingRecord = existing.get(0);
                if (!existingRecord.getTrangThai().equals(request.getTrangThai())) {
                    warnings.add(String.format("Sinh viên đã có điểm danh với trạng thái '%s', sẽ được cập nhật",
                            existingRecord.getTrangThai()));
                } else {
                    warnings.add("Sinh viên đã được điểm danh với cùng trạng thái");
                }
            }

        } catch (Exception e) {
            log.error("Error checking duplicate attendance: {}", e.getMessage());
            warnings.add("Không thể kiểm tra trùng lặp điểm danh");
        }
    }

    private void validateStatusLogic(TeacherAttendanceRequestDTO request, List<String> warnings) {
        if (request.getTrangThai() == null) {
            return;
        }

        // Validate time fields based on status
        if (TrangThaiDiemDanhEnum.CO_MAT.equals(request.getTrangThai()) ||
                TrangThaiDiemDanhEnum.DI_TRE.equals(request.getTrangThai())) {

            if (request.getThoiGianVao() == null) {
                warnings.add("Nên có thời gian vào cho trạng thái 'Có mặt' hoặc 'Đi trễ'");
            }
        }

        // Validate late status timing
        if (TrangThaiDiemDanhEnum.DI_TRE.equals(request.getTrangThai()) &&
                request.getThoiGianVao() != null) {

            try {
                Integer period = getPeriodFromSchedule(request.getScheduleId(), request.isWeekBased());
                if (period != null) {
                    LocalTime periodStart = config.calculatePeriodStartTime(period);
                    LocalTime attendanceTime = request.getThoiGianVao().toLocalTime();

                    if (!attendanceTime.isAfter(periodStart)) {
                        warnings.add("Thời gian vào không phù hợp với trạng thái 'Đi trễ'");
                    }
                }
            } catch (Exception e) {
                log.debug("Could not validate late timing: {}", e.getMessage());
            }
        }
    }

    // ===== HELPER METHODS =====

    private String getClassCodeFromSchedule(String scheduleId, boolean isWeekBased) {
        try {
            if (isWeekBased) {
                return scheduleInstanceRepository.findById(scheduleId)
                        .map(si -> si.getWeeklySchedule().getLopHocPhan().getMaLhp())
                        .orElse(null);
            } else {
                return lichHocRepository.findById(scheduleId)
                        .map(lh -> lh.getLopHocPhan().getMaLhp())
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting class code: {}", e.getMessage());
            return null;
        }
    }

    private Integer getPeriodFromSchedule(String scheduleId, boolean isWeekBased) {
        try {
            if (isWeekBased) {
                return scheduleInstanceRepository.findById(scheduleId)
                        .map(si -> si.getWeeklySchedule().getTietBatDau())
                        .orElse(null);
            } else {
                return lichHocRepository.findById(scheduleId)
                        .map(LichHoc::getTietBatDau)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting period: {}", e.getMessage());
            return null;
        }
    }

    /**
     * DTO cho kết quả validation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
    }
}