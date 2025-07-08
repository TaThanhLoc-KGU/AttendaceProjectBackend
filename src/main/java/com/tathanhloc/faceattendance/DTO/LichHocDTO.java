package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Thông tin mở rộng để hiển thị
    private String tenMonHoc;
    private String tenGiangVien;
    private String tenPhong;
    private Integer nhom;
    private String maMh;
    private String maGv;
    private String hocKy;
    private String namHoc;

    // Tính toán thời gian
    private String thoiGianBatDau; // Ví dụ: "07:00"
    private String thoiGianKetThuc; // Ví dụ: "09:30"
    private String tenThu; // Ví dụ: "Thứ 2"
}