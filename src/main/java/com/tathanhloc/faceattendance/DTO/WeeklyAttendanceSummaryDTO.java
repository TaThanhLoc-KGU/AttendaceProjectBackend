package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyAttendanceSummaryDTO {
    private Integer week;
    private LocalDate weekStartDate;
    private Integer totalSessions;
    private Integer completedSessions;
    private Double attendanceRate;
    private Integer presentCount;
    private Integer absentCount;
    private Integer lateCount;
}