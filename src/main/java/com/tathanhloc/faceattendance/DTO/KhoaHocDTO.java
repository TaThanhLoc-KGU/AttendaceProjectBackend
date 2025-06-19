package com.tathanhloc.faceattendance.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhoaHocDTO {
    private String maKhoahoc;
    private String tenKhoahoc;
    private Integer namBatDau;
    private Integer namKetThuc;
    private Boolean isActive;
}
