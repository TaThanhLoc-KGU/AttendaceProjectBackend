package com.tathanhloc.faceattendance.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NamHocDTO {
    private String maNamHoc;

    @NotBlank(message = "Tên năm học không được để trống")
    private String tenNamHoc;

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
    private Double tiLePhanTram; // Tiến độ năm học (%)
    private Integer startYear;
    private Integer endYear;

    // Related data
    private List<HocKyDTO> danhSachHocKy; // Các học kỳ trong năm học này
    private Integer soHocKy;
}