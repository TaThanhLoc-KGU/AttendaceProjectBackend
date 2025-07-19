// AttendanceSummaryDTO.java
package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryDTO {
    private Long totalClasses;
    private Long present;
    private Long absent;
    private Double attendanceRate;
}
