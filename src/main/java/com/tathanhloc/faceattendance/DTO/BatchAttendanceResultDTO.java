
// ===== BatchAttendanceResultDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho ket qua diem danh hang loat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAttendanceResultDTO {
    private Integer totalRequests;
    private Integer successCount;
    private Integer failCount;
    private List<AttendanceResultDTO> results;
    private LocalDateTime processedAt;

    /**
     * Ti le thanh cong
     */
    public Double getSuccessRate() {
        if (totalRequests == null || totalRequests == 0) return 0.0;
        if (successCount == null) return 0.0;
        return (double) successCount / totalRequests * 100;
    }
}
