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
public class GiangVienDTO {
    private String maGv;
    private String hoTen;
    private String email;
    private Boolean isActive;
    private String maKhoa;
}
