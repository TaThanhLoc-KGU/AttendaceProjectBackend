package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Template lịch học theo tuần - Định nghĩa pattern lặp lại
 * VD: Môn Toán học thứ 2 tiết 1-2, từ tuần 1 đến tuần 15
 */
@Entity
@Table(name = "weekly_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySchedule {

    @Id
    @Column(name = "ma_template")
    private String maTemplate;

    // ===== LIÊN KẾT VỚI LỚP HỌC PHẦN =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_lhp", nullable = false)
    private LopHocPhan lopHocPhan;

    // ===== THÔNG TIN THỜI GIAN =====
    @NotNull(message = "Thứ không được để trống")
    @Min(value = 2, message = "Thứ phải từ 2 (Thứ hai)")
    @Max(value = 8, message = "Thứ không được quá 8 (Chủ nhật)")
    @Column(name = "thu")
    private Integer thu;

    @NotNull(message = "Tiết bắt đầu không được để trống")
    @Min(value = 1, message = "Tiết bắt đầu phải từ 1")
    @Max(value = 12, message = "Tiết bắt đầu không được quá 12")
    @Column(name = "tiet_bat_dau")
    private Integer tietBatDau;

    @NotNull(message = "Số tiết không được để trống")
    @Min(value = 1, message = "Số tiết phải ít nhất 1")
    @Max(value = 6, message = "Số tiết không được quá 6")
    @Column(name = "so_tiet")
    private Integer soTiet;

    // ===== PHẠM VI ÁP DỤNG =====
    @NotNull(message = "Tuần bắt đầu không được để trống")
    @Min(value = 1, message = "Tuần bắt đầu phải từ 1")
    @Column(name = "tuan_bat_dau")
    private Integer tuanBatDau;

    @NotNull(message = "Tuần kết thúc không được để trống")
    @Min(value = 1, message = "Tuần kết thúc phải từ 1")
    @Column(name = "tuan_ket_thuc")
    private Integer tuanKetThuc;

    // ===== PHÒNG HỌC MẶC ĐỊNH =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong_mac_dinh")
    private PhongHoc phongHocMacDinh;

    // ===== METADATA =====
    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    @Column(name = "mo_ta")
    private String moTa;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_lich")
    private LoaiLich loaiLich;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiTemplate trangThai;

    // ===== AUDIT FIELDS =====
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== RELATIONSHIPS =====
    @OneToMany(mappedBy = "weeklySchedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScheduleInstance> scheduleInstances;

    // ===== ENUMS =====
    public enum LoaiLich {
        LY_THUYET("Lý thuyết"),
        THUC_HANH("Thực hành"),
        SEMINAR("Seminar"),
        THI_CUOI_KY("Thi cuối kỳ"),
        THI_GIUA_KY("Thi giữa kỳ"),
        BAO_CAO("Báo cáo"),
        KHAC("Khác");

        private final String displayName;

        LoaiLich(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TrangThaiTemplate {
        DRAFT("Bản nháp"),
        ACTIVE("Đang áp dụng"),
        PAUSED("Tạm dừng"),
        COMPLETED("Đã hoàn thành"),
        CANCELLED("Đã hủy");

        private final String displayName;

        TrangThaiTemplate(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ===== BUSINESS METHODS =====

    /**
     * Tính tổng số tuần áp dụng
     */
    public Integer getTongSoTuan() {
        if (tuanBatDau == null || tuanKetThuc == null) return 0;
        return tuanKetThuc - tuanBatDau + 1;
    }

    /**
     * Tính tiết kết thúc
     */
    public Integer getTietKetThuc() {
        if (tietBatDau == null || soTiet == null) return null;
        return tietBatDau + soTiet - 1;
    }

    /**
     * Kiểm tra template có hợp lệ không
     */
    public boolean isValidTemplate() {
        return tuanBatDau != null && tuanKetThuc != null &&
                tuanBatDau <= tuanKetThuc &&
                thu != null && thu >= 2 && thu <= 8 &&
                tietBatDau != null && tietBatDau >= 1 && tietBatDau <= 12 &&
                soTiet != null && soTiet >= 1 && soTiet <= 6 &&
                getTietKetThuc() <= 12;
    }

    /**
     * Kiểm tra tuần có nằm trong phạm vi không
     */
    public boolean isApplicableForWeek(Integer tuanHoc) {
        if (tuanHoc == null || tuanBatDau == null || tuanKetThuc == null) return false;
        return tuanHoc >= tuanBatDau && tuanHoc <= tuanKetThuc;
    }

    /**
     * Tạo mã template tự động
     */
    public static String generateTemplateId(String maLhp, Integer thu, Integer tietBatDau) {
        return String.format("WT_%s_T%d_P%d_%d",
                maLhp, thu, tietBatDau, System.currentTimeMillis() % 10000);
    }

    /**
     * Validation constraints
     */
    @AssertTrue(message = "Tuần kết thúc phải lớn hơn hoặc bằng tuần bắt đầu")
    public boolean isValidWeekRange() {
        return tuanKetThuc == null || tuanBatDau == null || tuanKetThuc >= tuanBatDau;
    }

    @AssertTrue(message = "Tiết kết thúc không được vượt quá 12")
    public boolean isValidPeriodRange() {
        return getTietKetThuc() == null || getTietKetThuc() <= 12;
    }
}