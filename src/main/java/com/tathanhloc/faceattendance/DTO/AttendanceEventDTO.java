package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEventDTO {
    private Long attendanceId;
    private String maSv;
    private String hoTen;
    private String maLich;
    private String tenLhp;
    private String maPhong;
    private String status; // "CO_MAT", "VANG_MAT", "DI_TRE"
    private LocalDateTime timestamp;
    private Double confidence;
    private Long cameraId;
}