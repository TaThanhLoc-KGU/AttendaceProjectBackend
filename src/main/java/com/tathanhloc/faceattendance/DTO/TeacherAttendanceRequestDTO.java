// ===== TeacherAttendanceRequestDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO cho request diem danh tu giang vien
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAttendanceRequestDTO {
    @NotBlank(message = "Ma sinh vien khong duoc trong")
    private String maSv;

    @NotBlank(message = "ID lich hoc khong duoc trong")
    private String scheduleId;

    @NotNull(message = "Ngay diem danh khong duoc trong")
    private LocalDate date;

    private boolean isWeekBased;

    @NotNull(message = "Trang thai diem danh khong duoc trong")
    private TrangThaiDiemDanhEnum trangThai;

    private LocalDateTime thoiGianVao;
    private LocalDateTime thoiGianRa;
    private String ghiChu;
    private String createdBy;

    // Validation metadata
    private Boolean isManualEntry;  // Diem danh thu cong hay tu dong
    private String attendanceMethod; // "MANUAL", "CAMERA", "QR_CODE", etc.
}