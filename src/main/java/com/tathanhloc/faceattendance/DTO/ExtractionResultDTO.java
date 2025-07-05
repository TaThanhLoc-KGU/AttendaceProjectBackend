package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResultDTO {
    private Boolean success;
    private String studentId;
    private String embeddingData;
    private Integer facesProcessed;
    private Integer embeddingSize;
    private Double qualityScore;
    private Double processingTime;
    private List<String> outputFiles;
    private String errorMessage;
    private LocalDateTime timestamp;
}