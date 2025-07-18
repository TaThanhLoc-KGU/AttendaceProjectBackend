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
public class ROIUpdateEventDTO {
    private Long cameraId;
    private ROIPolygonDTO roi;
    private String roiType; // "IN" hoặc "OUT"
    private String action; // "CREATE", "UPDATE", "DELETE"
    private LocalDateTime timestamp;
    private String updatedBy;
}
