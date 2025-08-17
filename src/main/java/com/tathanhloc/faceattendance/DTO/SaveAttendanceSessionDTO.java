package com.tathanhloc.faceattendance.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Valid
public class SaveAttendanceSessionDTO {
    @NotBlank(message = "Instance ID không được để trống")
    private String instanceId;

    @NotBlank(message = "Mã lớp không được để trống")
    private String classId;

    @NotNull(message = "Ngày học không được để trống")
    private LocalDate sessionDate;

    @NotNull(message = "Tuần học không được để trống")
    private Integer week;

    @Valid
    @NotEmpty(message = "Danh sách điểm danh không được để trống")
    private List<AttendanceRecordDTO> attendances;

    @Data
    @Valid
    public static class AttendanceRecordDTO {
        @NotBlank(message = "Mã sinh viên không được để trống")
        private String studentId;

        @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(regexp = "PRESENT|ABSENT|LATE", message = "Trạng thái không hợp lệ")
        private String status;

        private String timestamp;
        private String note;
        private String recognizedAt;
    }
}