package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKyDTO {

    private String maHocKy;

    private String tenHocKy;

    private Boolean isActive ;
}
