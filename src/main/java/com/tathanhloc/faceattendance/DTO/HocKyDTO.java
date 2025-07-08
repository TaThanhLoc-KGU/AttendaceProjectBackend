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

    // Computed fields
    private String trangThai; // "Chưa bắt đầu", "Đang diễn ra", "Đã kết thúc"
    private Integer soNgayConLai;
    private Integer tongSoNgay;
    private Double tiLePhanTram; // Tiến độ học kỳ (%)
}