package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionSettingsDTO {
    private Double recognitionThreshold;
    private Double detectionThreshold;
    private Integer minFaceSize;
    private Integer maxFaceSize;
    private Boolean enableAgeGenderDetection;
    private Integer trackingBufferSize;
}
