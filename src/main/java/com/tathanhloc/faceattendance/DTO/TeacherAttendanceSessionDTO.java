// ===== TeacherAttendanceSessionDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO cho phien diem danh cua giang vien
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAttendanceSessionDTO {
    private String scheduleId;
    private LocalDate date;
    private boolean isWeekBased;

    // Thong tin lop hoc
    private String maLhp;
    private String tenMonHoc;
    private String maMonHoc;
    private Integer tietBatDau;
    private Integer soTiet;
    private String phongHoc;
    private String tenPhong;
    private Integer weekNumber;

    // Danh sach sinh vien
    private List<StudentAttendanceDTO> students;
    private Integer totalStudents;

    // Thong ke diem danh
    private Integer presentCount;
    private Integer absentCount;
    private Integer lateCount;
    private Integer excusedCount;
    private Integer notMarkedCount;

    /**
     * Tinh toan thong ke tu danh sach sinh vien
     */
    public void calculateStatistics() {
        if (students == null || students.isEmpty()) {
            presentCount = absentCount = lateCount = excusedCount = notMarkedCount = 0;
            return;
        }

        presentCount = (int) students.stream()
                .filter(s -> s.getTrangThai() == TrangThaiDiemDanhEnum.CO_MAT)
                .count();

        absentCount = (int) students.stream()
                .filter(s -> s.getTrangThai() == TrangThaiDiemDanhEnum.VANG_MAT)
                .count();

        lateCount = (int) students.stream()
                .filter(s -> s.getTrangThai() == TrangThaiDiemDanhEnum.DI_TRE)
                .count();

        excusedCount = (int) students.stream()
                .filter(s -> s.getTrangThai() == TrangThaiDiemDanhEnum.VANG_CO_PHEP)
                .count();

        notMarkedCount = (int) students.stream()
                .filter(s -> s.getTrangThai() == null)
                .count();
    }

    /**
     * Ti le diem danh
     */
    public Double getAttendanceRate() {
        if (totalStudents == null || totalStudents == 0) return 0.0;
        if (presentCount == null) return 0.0;
        return (double) presentCount / totalStudents * 100;
    }
}
