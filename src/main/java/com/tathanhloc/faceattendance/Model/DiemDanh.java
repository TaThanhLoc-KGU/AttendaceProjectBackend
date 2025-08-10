package com.tathanhloc.faceattendance.Model;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "diemdanh")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // ← THÊM BUILDER
public class DiemDanh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ma_sv")
    private SinhVien sinhVien;

    @Column(name = "ngay_diem_danh")
    private LocalDate ngayDiemDanh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiDiemDanhEnum trangThai;

    @Column(name = "thoi_gian_vao")
    private LocalDateTime thoiGianVao;  // ← PHẢI LÀ LocalDateTime

    @Column(name = "thoi_gian_ra")
    private LocalDateTime thoiGianRa;   // ← PHẢI LÀ LocalDateTime

    // OLD REFERENCE (keep for backward compatibility)
    @ManyToOne
    @JoinColumn(name = "ma_lich")
    private LichHoc lichHoc;

    // NEW REFERENCE (for week-based schedules)
    @ManyToOne
    @JoinColumn(name = "ma_instance")
    private ScheduleInstance scheduleInstance;

    // ← THÊM FIELD GHI CHÚ
    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods...
    public String getMaLichThucTe() {
        if (scheduleInstance != null) {
            return scheduleInstance.getMaInstance();
        }
        return lichHoc != null ? lichHoc.getMaLich() : null;
    }

    public String getMaLhpThucTe() {
        if (scheduleInstance != null && scheduleInstance.getWeeklySchedule() != null) {
            return scheduleInstance.getWeeklySchedule().getLopHocPhan().getMaLhp();
        }
        return lichHoc != null ? lichHoc.getLopHocPhan().getMaLhp() : null;
    }

    public boolean isWeekBasedSchedule() {
        return scheduleInstance != null;
    }
}
