// ===== TeacherScheduleDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import lombok.*;
import java.time.LocalDate;

/**
 * DTO cho lịch dạy của giảng viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherScheduleDTO {
    private String scheduleId;           // Ma lich hoac ma instance
    private LocalDate date;              // Ngay hoc cu the
    private boolean isWeekBased;         // Week-based schedule hay traditional

    // Thong tin lich hoc
    private Integer thu;                 // Thu trong tuan (2-8)
    private Integer tietBatDau;         // Tiet bat dau
    private Integer soTiet;             // So tiet hoc
    private Integer weekNumber;         // Tuan trong nam (chi co khi week-based)

    // Thong tin lop hoc phan
    private String maLhp;               // Ma lop hoc phan
    private String tenMonHoc;           // Ten mon hoc
    private String maMonHoc;            // Ma mon hoc
    private String tenGiangVien;        // Ten giang vien
    private String maGv;                // Ma giang vien

    // Thong tin phong hoc
    private String phongHoc;            // Ma phong
    private String tenPhong;            // Ten phong

    // Thong tin hoc ky
    private String hocKy;               // Ma hoc ky
    private String namHoc;              // Nam hoc

    // Thong tin trang thai diem danh
    private Integer totalStudents;      // Tong so sinh vien
    private Integer attendedStudents;   // So sinh vien da diem danh
    private Boolean isAttendanceStarted; // Da bat dau diem danh chua

    /**
     * Tinh thoi gian hoc (format: "07:00 - 08:30")
     */
    public String getThoiGianHoc() {
        if (tietBatDau == null || soTiet == null) return "";

        // Gio bat dau: 6:00 + (tiet - 1) * 50 phut
        int startMinutes = 6 * 60 + (tietBatDau - 1) * 50;
        int endMinutes = startMinutes + soTiet * 50;

        String startTime = String.format("%02d:%02d", startMinutes / 60, startMinutes % 60);
        String endTime = String.format("%02d:%02d", endMinutes / 60, endMinutes % 60);

        return startTime + " - " + endTime;
    }

    /**
     * Tinh ti le diem danh (%)
     */
    public Double getAttendanceRate() {
        if (totalStudents == null || totalStudents == 0) return 0.0;
        if (attendedStudents == null) return 0.0;
        return (double) attendedStudents / totalStudents * 100;
    }
}