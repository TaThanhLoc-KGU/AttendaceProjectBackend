package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogDTO {
    private Long id;
    private String action;
    private String ipAddress;
    private LocalDateTime createdAt;
    private Long userId;
}
