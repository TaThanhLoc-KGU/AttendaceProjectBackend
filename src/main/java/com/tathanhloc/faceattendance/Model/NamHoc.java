package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "nam_hoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NamHoc {
    @Id
    @Column(name = "ma_nam_hoc")
    private String maNamHoc; // Ví dụ: "2024-2025"

    @NotBlank(message = "Tên năm học không được để trống")
    @Column(name = "ten_nam_hoc")
    private String tenNamHoc; // Ví dụ: "Năm học 2024-2025"

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    /**
     * Validation: Ngày kết thúc phải sau ngày bắt đầu ít nhất 6 tháng
     */
    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu ít nhất 6 tháng")
    public boolean isValidDateRange() {
        if (ngayKetThuc == null || ngayBatDau == null) return true;
        return ngayKetThuc.isAfter(ngayBatDau.plusMonths(6));
    }

    /**
     * Kiểm tra năm học có đang diễn ra không
     */
    public boolean isOngoing() {
        if (ngayBatDau == null || ngayKetThuc == null) return false;
        LocalDate now = LocalDate.now();
        return !now.isBefore(ngayBatDau) && !now.isAfter(ngayKetThuc);
    }

    /**
     * Kiểm tra năm học đã kết thúc chưa
     */
    public boolean isFinished() {
        if (ngayKetThuc == null) return false;
        return LocalDate.now().isAfter(ngayKetThuc);
    }

    /**
     * Kiểm tra năm học chưa bắt đầu
     */
    public boolean isUpcoming() {
        if (ngayBatDau == null) return false;
        return LocalDate.now().isBefore(ngayBatDau);
    }

    /**
     * Lấy năm bắt đầu
     */
    public int getStartYear() {
        return ngayBatDau != null ? ngayBatDau.getYear() : 0;
    }

    /**
     * Lấy năm kết thúc
     */
    public int getEndYear() {
        return ngayKetThuc != null ? ngayKetThuc.getYear() : 0;
    }
}