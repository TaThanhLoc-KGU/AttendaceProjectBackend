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
public class FaceDetectionEventDTO {
    private Long cameraId;
    private String maSv;
    private String hoTen;
    private Double confidence;
    private BoundingBoxDTO boundingBox;
    private LocalDateTime timestamp;
    private Boolean isRecognized;
    private String roiType;
}