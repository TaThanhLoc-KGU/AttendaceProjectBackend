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
public class FaceRecognitionResultDTO {
    private Boolean success;
    private String studentId;
    private String studentName;
    private Double confidence;
    private Double distance;
    private String message;
    private LocalDateTime timestamp;
    private String imageUrl;
}