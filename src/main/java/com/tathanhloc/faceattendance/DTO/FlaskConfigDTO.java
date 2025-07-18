package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaskConfigDTO {
    private String backendApiUrl;
    private String websocketUrl;
    private RecognitionSettingsDTO recognitionSettings;
    private SystemStatusDTO systemStatus;
    private Integer maxConcurrentStreams;
    private Integer frameProcessingInterval;
}