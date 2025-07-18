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
public class SystemNotificationDTO {
    private String message;
    private String type; // "INFO", "WARNING", "ERROR", "SUCCESS"
    private LocalDateTime timestamp;
    private String source; // "SYSTEM", "FLASK", "CAMERA", "USER"
    private Long relatedEntityId; // Camera ID, Student ID, etc.
}
