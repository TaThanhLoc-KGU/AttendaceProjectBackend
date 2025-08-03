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
import java.util.List;

@Entity
@Table(name = "nam_hoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NamHoc {
    @Id
    @Column(name = "ma_nam_hoc")
    private String maNamHoc;

    @NotBlank(message = "Tên năm học không được để trống")
    @Column(name = "ten_nam_hoc")
    private String tenNamHoc;

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

    // Quan hệ với HocKy thông qua bảng trung gian
    @OneToMany(mappedBy = "namHoc", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HocKyNamHoc> hocKyNamHocs;

    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu ít nhất 6 tháng")
    public boolean isValidDateRange() {
        if (ngayKetThuc == null || ngayBatDau == null) return true;
        return ngayKetThuc.isAfter(ngayBatDau.plusMonths(6));
    }

    // Helper methods...
    public boolean isOngoing() {
        if (ngayBatDau == null || ngayKetThuc == null) return false;
        LocalDate now = LocalDate.now();
        return !now.isBefore(ngayBatDau) && !now.isAfter(ngayKetThuc);
    }

    public boolean isFinished() {
        if (ngayKetThuc == null) return false;
        return LocalDate.now().isAfter(ngayKetThuc);
    }

    public boolean isUpcoming() {
        if (ngayBatDau == null) return false;
        return LocalDate.now().isBefore(ngayBatDau);
    }

    public int getStartYear() {
        return ngayBatDau != null ? ngayBatDau.getYear() : 0;
    }

    public int getEndYear() {
        return ngayKetThuc != null ? ngayKetThuc.getYear() : 0;
    }
}