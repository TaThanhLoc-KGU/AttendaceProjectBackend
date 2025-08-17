package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceClassSummaryDTO {
    private String maLhp;
    private Integer totalStudents;
    private Integer totalSessions;
    private Integer completedSessions;
    private Double averageAttendanceRate;
    private Double overallAttendanceRate;
    private Integer totalPresent;
    private Integer totalAbsent;
    private Integer totalLate;
}