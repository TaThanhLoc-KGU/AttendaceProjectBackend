package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatsDTO {
    private long totalSessions;
    private long totalStudents;
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long excusedCount;
    private double attendanceRate;
}