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
public class LiveRecognitionDTO {
    private Long cameraId;
    private String maSv;
    private String hoTen;
    private Double confidence;
    private BoundingBoxDTO boundingBox;
    private LocalDateTime recognitionTime;
    private String roiType; // "IN" hoặc "OUT"
    private Boolean isInROI;
    private String embeddings; // Face embeddings
}