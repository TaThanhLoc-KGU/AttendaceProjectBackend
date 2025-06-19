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
public class LichHocDTO {
    private String maLich;
    private Integer thu;
    private Integer tietBatDau;
    private Integer soTiet;
    private String maLhp;
    private String maPhong;
    private Boolean isActive;
}
