package com.tathanhloc.faceattendance.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganhDTO {
    private String maNganh;
    private String tenNganh;
    private String maKhoa;
    private Boolean isActive;
}
