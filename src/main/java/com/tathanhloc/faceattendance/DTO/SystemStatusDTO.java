package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDTO {
    private String status; // "HEALTHY", "DEGRADED", "DOWN"
    private LocalDateTime timestamp;
    private Map<String, Object> cameraStatuses;
    private Map<String, Object> serviceStatuses;
    private Long totalStudentsEnrolled;
    private Long activeAttendanceSessions;
    private Double systemLoad;
}