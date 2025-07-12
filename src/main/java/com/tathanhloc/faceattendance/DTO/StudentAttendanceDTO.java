package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceDTO {
    private String maSv;
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long excusedCount;
    private double attendanceRate;
}