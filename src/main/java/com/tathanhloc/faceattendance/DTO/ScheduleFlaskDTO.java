package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleFlaskDTO {
    private String maLich;
    private String maLhp;
    private String tenLhp;
    private String maPhong;
    private String tenPhong;
    private Integer thu;
    private Integer tietBatDau;
    private Integer soTiet;
    private LocalTime thoiGianBatDau;
    private LocalTime thoiGianKetThuc;
    private List<String> danhSachSinhVien;
    private String giangVien;
}
