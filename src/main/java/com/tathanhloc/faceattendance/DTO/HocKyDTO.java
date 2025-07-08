package com.tathanhloc.faceattendance.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKyDTO {
    private String maHocKy;

    @NotBlank(message = "Tên học kỳ không được để trống")
    private String tenHocKy;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate ngayKetThuc;

    private String moTa;
    private Boolean isActive;
    private Boolean isCurrent;

    // Thông tin năm học liên quan
    private String maNamHoc;
    private String tenNamHoc;
    private Integer thuTu; // Thứ tự trong năm học

    // Computed fields
    private String trangThai;
    private Integer soNgayConLai;
    private Integer tongSoNgay;
    private Double tiLePhanTram;
}