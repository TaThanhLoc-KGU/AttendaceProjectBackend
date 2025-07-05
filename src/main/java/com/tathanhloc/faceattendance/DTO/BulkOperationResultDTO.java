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
public class BulkOperationResultDTO {
    private String operation;
    private Integer totalRequested;
    private Integer successCount;
    private Integer failedCount;
    private List<String> successIds;
    private List<String> failedIds;
    private List<String> errors;
    private LocalDateTime timestamp;
}