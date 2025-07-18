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
public class AttendanceFlaskDTO {
    private Long cameraId;
    private String maSv;
    private String maLich;
    private LocalDateTime detectionTime;
    private Double confidence;
    private String attendanceType; // "CHECK_IN", "CHECK_OUT"
    private String recognitionData; // JSON data từ Flask
}