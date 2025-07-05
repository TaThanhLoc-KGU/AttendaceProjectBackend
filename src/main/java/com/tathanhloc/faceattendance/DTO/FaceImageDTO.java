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
public class FaceImageDTO {
    private String id;
    private String maSv;
    private String filename;
    private String url;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private Boolean isProcessed;
    private Double qualityScore;
}