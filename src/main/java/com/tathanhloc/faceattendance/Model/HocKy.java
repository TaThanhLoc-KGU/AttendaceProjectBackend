package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "hoc_ky")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKy {
    @Id
    @Column(name = "ma_hoc_ky")
    private String maHocKy;

    @NotBlank(message = "Tên học kỳ không được để trống")
    @Column(name = "ten_hoc_ky")
    private String tenHocKy;

    // ===== KEEP EXISTING DATE FIELDS (for backward compatibility) =====
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    // ===== NEW WEEK-BASED FIELDS =====
    @Min(value = 1, message = "Tuần bắt đầu phải từ 1 trở lên")
    @Max(value = 52, message = "Tuần bắt đầu không được quá 52")
    @Column(name = "tuan_bat_dau")
    private Integer tuanBatDau;

    @Min(value = 4, message = "Số tuần học tối thiểu là 4 tuần")
    @Max(value = 20, message = "Số tuần học tối đa là 20 tuần")
    @Column(name = "so_tuan_hoc")
    private Integer soTuanHoc;

    @Column(name = "ngay_bat_dau_tuan_1")
    private LocalDate ngayBatDauTuan1; // Reference date for week calculations

    // ===== METADATA =====
    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "loai_hoc_ky")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoaiHocKy loaiHocKy = LoaiHocKy.CHINH_QUY;

    // ===== RELATIONSHIPS (KEEP EXISTING) =====
    @OneToMany(mappedBy = "hocKy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HocKyNamHoc> hocKyNamHocs;

    // ===== VALIDATION METHODS =====
    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu")
    public boolean isValidDateRange() {
        return ngayKetThuc == null || ngayBatDau == null || ngayKetThuc.isAfter(ngayBatDau);
    }

    @AssertTrue(message = "Tuần kết thúc không được vượt quá 52")
    public boolean isValidWeekRange() {
        if (tuanBatDau == null || soTuanHoc == null) return true;
        return (tuanBatDau + soTuanHoc - 1) <= 52;
    }

    // ===== EXISTING HELPER METHODS (KEEP COMPATIBILITY) =====
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

    // ===== NEW WEEK-BASED COMPUTED METHODS =====

    /**
     * Tính ngày bắt đầu thực tế từ week-based config (nếu có)
     * Fallback về ngayBatDau nếu không có week config
     */
    public LocalDate getNgayBatDauThucTe() {
        if (tuanBatDau != null && ngayBatDauTuan1 != null) {
            return ngayBatDauTuan1.plusWeeks(tuanBatDau - 1);
        }
        return ngayBatDau; // Fallback
    }

    /**
     * Tính ngày kết thúc thực tế từ week-based config (nếu có)
     * Fallback về ngayKetThuc nếu không có week config
     */
    public LocalDate getNgayKetThucThucTe() {
        if (tuanBatDau != null && soTuanHoc != null && ngayBatDauTuan1 != null) {
            return getNgayBatDauThucTe().plusWeeks(soTuanHoc - 1).plusDays(6);
        }
        return ngayKetThuc; // Fallback
    }

    /**
     * Kiểm tra có sử dụng week-based configuration không
     */
    public boolean isWeekBasedConfig() {
        return tuanBatDau != null && soTuanHoc != null && ngayBatDauTuan1 != null;
    }

    /**
     * Sync ngày từ week-based config sang date fields
     */
    public void syncDatesFromWeekConfig() {
        if (isWeekBasedConfig()) {
            this.ngayBatDau = getNgayBatDauThucTe();
            this.ngayKetThuc = getNgayKetThucThucTe();
        }
    }

    /**
     * Tính tuần kết thúc
     */
    public Integer getTuanKetThuc() {
        if (tuanBatDau == null || soTuanHoc == null) return null;
        return tuanBatDau + soTuanHoc - 1;
    }

    /**
     * Tính tuần hiện tại trong học kỳ (1-based)
     */
    public Integer getTuanHienTai() {
        if (!isOngoing()) return null;

        LocalDate ngayBatDauActual = isWeekBasedConfig() ? getNgayBatDauThucTe() : ngayBatDau;
        if (ngayBatDauActual == null) return null;

        LocalDate now = LocalDate.now();
        long soNgayDaQua = java.time.temporal.ChronoUnit.DAYS.between(ngayBatDauActual, now);
        return (int) (soNgayDaQua / 7) + 1;
    }

    /**
     * Tính tiến độ học kỳ theo %
     */
    public Double getTienDoPercent() {
        if (!isOngoing()) {
            if (isFinished()) return 100.0;
            if (isUpcoming()) return 0.0;
        }

        LocalDate ngayBatDauActual = isWeekBasedConfig() ? getNgayBatDauThucTe() : ngayBatDau;
        LocalDate ngayKetThucActual = isWeekBasedConfig() ? getNgayKetThucThucTe() : ngayKetThuc;

        if (ngayBatDauActual == null || ngayKetThucActual == null) return 0.0;

        LocalDate now = LocalDate.now();
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(ngayBatDauActual, ngayKetThucActual);
        long passedDays = java.time.temporal.ChronoUnit.DAYS.between(ngayBatDauActual, now);

        return Math.min(100.0, Math.max(0.0, (passedDays * 100.0) / totalDays));
    }

    // ===== ENUM DEFINITIONS =====
    public enum LoaiHocKy {
        CHINH_QUY("Chính quy"),
        HE("Hè"),
        TAP_TRUNG("Tập trung"),
        DAC_BIET("Đặc biệt");

        private final String displayName;

        LoaiHocKy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}