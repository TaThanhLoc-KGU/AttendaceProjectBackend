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
public class CameraStatusEventDTO {
    private Long cameraId;
    private String status; // "ONLINE", "OFFLINE", "ERROR", "STREAMING"
    private String message;
    private LocalDateTime timestamp;
    private String tenCamera;
    private String maPhong;
}