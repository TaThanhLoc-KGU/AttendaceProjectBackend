package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for import result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDTO {
    private int totalProcessed;
    private int successCount;
    private int failedCount;
    private List<String> errors;
    private List<String> warnings;
    private LocalDateTime timestamp;
    private String status;

    public ImportResultDTO(int successCount, int failedCount, List<String> errors) {
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.totalProcessed = successCount + failedCount;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
        this.status = failedCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
    }
}
