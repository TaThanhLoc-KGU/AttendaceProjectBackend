package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DangKyHocDTO {
    private String maSv;
    private String maLhp;
    private Boolean isActive;
    // THÊM FIELD NÀY

    // Có thể thêm các field display khác nếu cần
    private String tenMonHoc;
    private String tenGiangVien;
    private String tenSinhVien;
    private String maMh;
    private String nhom;
}
