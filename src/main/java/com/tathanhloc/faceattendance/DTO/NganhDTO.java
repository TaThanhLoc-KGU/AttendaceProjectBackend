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
    private String tenKhoa; // Add khoa name for easier display
    private Boolean isActive;
    private Long soSinhVien; // Add student count


    // Constructor cho các trường hợp không cần tenKhoa và soSinhVien
    public NganhDTO(String maNganh, String tenNganh, String maKhoa, Boolean isActive) {
        this.maNganh = maNganh;
        this.tenNganh = tenNganh;
        this.maKhoa = maKhoa;
        this.isActive = isActive;
        this.soSinhVien = 0L; // Default value
    }
}
