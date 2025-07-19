package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectAttendanceDTO {
    private String maMh;
    private String tenMh;
    private String maLhp;
    private Integer nhom;
    private Long totalClasses;
    private Long present;
    private Long absent;
    private Double attendanceRate;
}