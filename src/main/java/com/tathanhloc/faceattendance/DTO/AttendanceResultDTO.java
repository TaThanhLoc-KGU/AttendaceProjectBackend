// ===== AttendanceResultDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO cho ket qua diem danh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResultDTO {
    private boolean success;
    private String message;
    private String studentId;
    private Long attendanceId;
    private TrangThaiDiemDanhEnum status;
    private LocalDateTime timestamp;
    private String errorCode;
}