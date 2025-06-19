package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonHocDTO {
    private String maMh;
    private String tenMh;
    private Integer soTinChi;
    private Boolean isActive;
    private Set<String> maNganhs;
}
