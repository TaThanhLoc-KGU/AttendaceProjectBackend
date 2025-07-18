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
public class FaceDetectionDTO {
    private Long cameraId;
    private LocalDateTime timestamp;
    private String frameId;
    private List<BoundingBoxDTO> detectedFaces;
    private String imageBase64; // Optional: snapshot của frame
}