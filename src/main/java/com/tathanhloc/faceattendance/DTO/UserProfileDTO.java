package com.tathanhloc.faceattendance.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String username;
    private String vaiTro;      // admin, giangvien, sinhvien
    private Boolean isActive;

    private String hoTen;
    private String maSo;        // maSv hoặc maGv
    private String email;
}
