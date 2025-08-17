// ===== BatchAttendanceRequestDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

/**
 * DTO cho request diem danh hang loat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAttendanceRequestDTO {
    @NotEmpty(message = "Danh sach diem danh khong duoc trong")
    @Valid
    private List<TeacherAttendanceRequestDTO> attendanceList;


    private String batchNote;  // Ghi chu chung cho ca batch
    private String batchType;  // "ALL_PRESENT", "MANUAL", "IMPORT"
}
