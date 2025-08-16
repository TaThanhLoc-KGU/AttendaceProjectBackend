// ===== TeacherAttendanceReportDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho bao cao diem danh cua giang vien
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAttendanceReportDTO {
    private LocalDate date;
    private String maGv;
    private String tenGiangVien;
    private Integer totalSessions;
    private Integer totalRecords;
    private List<AttendanceRecordDTO> attendanceRecords;
    private LocalDateTime exportedAt;

    /**
     * Tinh tong so sinh vien co mat
     */
    public Integer getTotalPresent() {
        if (attendanceRecords == null) return 0;
        return (int) attendanceRecords.stream()
                .filter(r -> TrangThaiDiemDanhEnum.CO_MAT.equals(r.getTrangThai()))
                .count();
    }

    /**
     * Tinh tong so sinh vien vang mat
     */
    public Integer getTotalAbsent() {
        if (attendanceRecords == null) return 0;
        return (int) attendanceRecords.stream()
                .filter(r -> TrangThaiDiemDanhEnum.VANG_MAT.equals(r.getTrangThai()))
                .count();
    }

    /**
     * Ti le diem danh chung
     */
    public Double getOverallAttendanceRate() {
        if (totalRecords == null || totalRecords == 0) return 0.0;
        return (double) getTotalPresent() / totalRecords * 100;
    }
}