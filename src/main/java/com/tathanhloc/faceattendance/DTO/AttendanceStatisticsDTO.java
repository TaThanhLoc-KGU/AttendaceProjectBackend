package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatisticsDTO {
    private AttendanceSummaryDTO summary;
    private List<SubjectAttendanceDTO> bySubjects;
    private AttendanceChartDataDTO chartData;
}

