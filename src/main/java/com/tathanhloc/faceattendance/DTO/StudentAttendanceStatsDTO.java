// ===== StudentAttendanceStatsDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import lombok.*;

/**
 * DTO cho thong ke diem danh cua sinh vien (khac voi StudentAttendanceDTO)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceStatsDTO {
    private String maSv;
    private String hoTen;
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long excusedCount;
    private double attendanceRate;
}