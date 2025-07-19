package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceHistoryDTO {
    private Long id;
    private LocalDate ngayHoc;
    private String maMh;
    private String tenMh;
    private String maLhp;
    private Integer nhom;
    private Integer tietBatDau;
    private Integer soTiet;
    private String trangThai;
    private LocalTime thoiGianVao;
    private String ghiChu;
}