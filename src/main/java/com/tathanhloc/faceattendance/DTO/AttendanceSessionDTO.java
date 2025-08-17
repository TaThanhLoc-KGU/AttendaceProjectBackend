package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSessionDTO {
    private String instanceId;
    private String maLhp;
    private LocalDate sessionDate;
    private Integer week;
    private String phongHoc;
    private Integer tietBatDau;
    private Integer tietKetThuc;
    private String trangThai;

    private AttendanceSummaryDTO summary;
    private List<StudentAttendanceDTO> students;

    @Data
    @Builder
    public static class AttendanceSummaryDTO {
        private Integer totalStudents;
        private Integer presentCount;
        private Integer absentCount;
        private Integer lateCount;
        private Double attendanceRate;
    }

    @Data
    @Builder
    public static class StudentAttendanceDTO {
        private String maSv;
        private String hoTen;
        private String email;
        private String trangThai; // PRESENT, ABSENT, LATE
        private String thoiGianDiemDanh;
        private String ghiChu;
    }
}