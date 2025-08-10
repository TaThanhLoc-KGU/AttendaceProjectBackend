package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Instance cụ thể của lịch học cho một tuần/ngày cụ thể
 * VD: Môn Toán ngày 2024-09-02 (thứ 2 tuần 1) tiết 1-2 tại phòng A101
 */
@Entity
@Table(name = "schedule_instance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleInstance {

    @Id
    @Column(name = "ma_instance")
    private String maInstance;

    // ===== LIÊN KẾT VỚI TEMPLATE =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_template", nullable = false)
    private WeeklySchedule weeklySchedule;

    // ===== THÔNG TIN CỤ THỂ =====
    @NotNull(message = "Tuần học không được để trống")
    @Min(value = 1, message = "Tuần học phải từ 1")
    @Column(name = "tuan_hoc")
    private Integer tuanHoc;

    @NotNull(message = "Ngày cụ thể không được để trống")
    @Column(name = "ngay_cu_the")
    private LocalDate ngayCuThe;

    // ===== OVERRIDE THÔNG TIN (nếu khác với template) =====
    @Column(name = "tiet_bat_dau_override")
    private Integer tietBatDauOverride;

    @Column(name = "so_tiet_override")
    private Integer soTietOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong_override")
    private PhongHoc phongHocOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_gv_override")
    private GiangVien giangVienOverride;

    // ===== TRẠNG THÁI =====
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiInstance trangThai;

    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
    @Column(name = "ghi_chu")
    private String ghiChu;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    // ===== LIÊN KẾT VỚI ĐIỂM DANH (backwards compatibility) =====
    @Column(name = "ma_lich_cu")
    private String maLichCu; // Reference to old LichHoc if migrated

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

    // ===== ENUMS =====
    public enum TrangThaiInstance {
        SCHEDULED("Đã lên lịch"),
        CONFIRMED("Đã xác nhận"),
        IN_PROGRESS("Đang diễn ra"),
        COMPLETED("Đã hoàn thành"),
        CANCELLED("Đã hủy"),
        POSTPONED("Hoãn lại"),
        RESCHEDULED("Đổi lịch"),
        NO_SHOW("Vắng mặt");

        private final String displayName;

        TrangThaiInstance(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Lấy tiết bắt đầu thực tế (ưu tiên override)
     */
    public Integer getTietBatDauThucTe() {
        return tietBatDauOverride != null ? tietBatDauOverride :
                weeklySchedule != null ? weeklySchedule.getTietBatDau() : null;
    }

    /**
     * Lấy số tiết thực tế (ưu tiên override)
     */
    public Integer getSoTietThucTe() {
        return soTietOverride != null ? soTietOverride :
                weeklySchedule != null ? weeklySchedule.getSoTiet() : null;
    }

    /**
     * Lấy phòng học thực tế (ưu tiên override)
     */
    public PhongHoc getPhongHocThucTe() {
        return phongHocOverride != null ? phongHocOverride :
                weeklySchedule != null ? weeklySchedule.getPhongHocMacDinh() : null;
    }

    /**
     * Lấy giảng viên thực tế (ưu tiên override)
     */
    public GiangVien getGiangVienThucTe() {
        return giangVienOverride != null ? giangVienOverride :
                weeklySchedule != null && weeklySchedule.getLopHocPhan() != null ?
                        weeklySchedule.getLopHocPhan().getGiangVien() : null;
    }

    /**
     * Tính tiết kết thúc thực tế
     */
    public Integer getTietKetThucThucTe() {
        Integer start = getTietBatDauThucTe();
        Integer duration = getSoTietThucTe();
        return (start != null && duration != null) ? start + duration - 1 : null;
    }

    /**
     * Kiểm tra có thay đổi so với template không
     */
    public boolean hasOverrides() {
        return tietBatDauOverride != null || soTietOverride != null ||
                phongHocOverride != null || giangVienOverride != null;
    }

    /**
     * Kiểm tra có thể điểm danh không
     */
    public boolean canTakeAttendance() {
        return trangThai == TrangThaiInstance.SCHEDULED ||
                trangThai == TrangThaiInstance.CONFIRMED ||
                trangThai == TrangThaiInstance.IN_PROGRESS;
    }

    /**
     * Kiểm tra đã kết thúc chưa
     */
    public boolean isCompleted() {
        return trangThai == TrangThaiInstance.COMPLETED;
    }

    /**
     * Kiểm tra có bị hủy không
     */
    public boolean isCancelled() {
        return trangThai == TrangThaiInstance.CANCELLED;
    }

    /**
     * Tạo mã instance tự động
     */
    public static String generateInstanceId(String maTemplate, Integer tuanHoc, LocalDate ngayCuThe) {
        return String.format("SI_%s_W%02d_%s",
                maTemplate.replace("WT_", ""),
                tuanHoc,
                ngayCuThe.toString().replace("-", ""));
    }

    /**
     * Tạo instance từ template
     */
    public static ScheduleInstance fromTemplate(WeeklySchedule template, Integer tuanHoc, LocalDate ngayCuThe) {
        return ScheduleInstance.builder()
                .maInstance(generateInstanceId(template.getMaTemplate(), tuanHoc, ngayCuThe))
                .weeklySchedule(template)
                .tuanHoc(tuanHoc)
                .ngayCuThe(ngayCuThe)
                .trangThai(TrangThaiInstance.SCHEDULED)
                .isActive(true)
                .build();
    }

    /**
     * Validation
     */
    @AssertTrue(message = "Ngày cụ thể phải khớp với thứ trong template")
    public boolean isValidDayOfWeek() {
        if (ngayCuThe == null || weeklySchedule == null || weeklySchedule.getThu() == null) {
            return true; // Skip validation if data incomplete
        }
        int dayOfWeek = ngayCuThe.getDayOfWeek().getValue() + 1; // Monday = 2, Sunday = 8
        return dayOfWeek == weeklySchedule.getThu();
    }

    @AssertTrue(message = "Tuần học phải nằm trong phạm vi của template")
    public boolean isValidWeekNumber() {
        return weeklySchedule == null || weeklySchedule.isApplicableForWeek(tuanHoc);
    }
}