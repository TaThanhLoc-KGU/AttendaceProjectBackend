package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Model.ScheduleInstance;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO cho ScheduleInstance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleInstanceDTO {

    private String maInstance;
    private String maTemplate;

    // ===== THÔNG TIN CỤ THỂ =====
    @NotNull(message = "Tuần học không được để trống")
    private Integer tuanHoc;

    @NotNull(message = "Ngày cụ thể không được để trống")
    private LocalDate ngayCuThe;

    // ===== OVERRIDE FIELDS =====
    private Integer tietBatDauOverride;
    private Integer soTietOverride;
    private String maPhongOverride;
    private String tenPhongOverride;
    private String maGvOverride;
    private String tenGvOverride;

    // ===== TRẠNG THÁI =====
    private ScheduleInstance.TrangThaiInstance trangThai;
    private String ghiChu;
    private Boolean isActive;

    // ===== COMPUTED FIELDS =====
    private Integer tietBatDauThucTe;
    private Integer soTietThucTe;
    private Integer tietKetThucThucTe;
    private String maPhongThucTe;
    private String tenPhongThucTe;
    private String tenGvThucTe;
    private String thoiGianHienThi;
    private Boolean hasOverrides;
    private Boolean canTakeAttendance;

    // ===== AUDIT =====
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     */
    public static ScheduleInstanceDTO fromEntity(ScheduleInstance entity) {
        if (entity == null) return null;

        ScheduleInstanceDTOBuilder builder = ScheduleInstanceDTO.builder()
                .maInstance(entity.getMaInstance())
                .tuanHoc(entity.getTuanHoc())
                .ngayCuThe(entity.getNgayCuThe())
                .tietBatDauOverride(entity.getTietBatDauOverride())
                .soTietOverride(entity.getSoTietOverride())
                .trangThai(entity.getTrangThai())
                .ghiChu(entity.getGhiChu())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .hasOverrides(entity.hasOverrides())
                .canTakeAttendance(entity.canTakeAttendance());

        // Template info
        if (entity.getWeeklySchedule() != null) {
            builder.maTemplate(entity.getWeeklySchedule().getMaTemplate());
        }

        // Override info
        if (entity.getPhongHocOverride() != null) {
            builder.maPhongOverride(entity.getPhongHocOverride().getMaPhong())
                    .tenPhongOverride(entity.getPhongHocOverride().getTenPhong());
        }

        if (entity.getGiangVienOverride() != null) {
            builder.maGvOverride(entity.getGiangVienOverride().getMaGv())
                    .tenGvOverride(entity.getGiangVienOverride().getHoTen());
        }

        ScheduleInstanceDTO dto = builder.build();

        // Computed fields
        dto.setTietBatDauThucTe(entity.getTietBatDauThucTe());
        dto.setSoTietThucTe(entity.getSoTietThucTe());
        dto.setTietKetThucThucTe(entity.getTietKetThucThucTe());

        if (entity.getPhongHocThucTe() != null) {
            dto.setMaPhongThucTe(entity.getPhongHocThucTe().getMaPhong());
            dto.setTenPhongThucTe(entity.getPhongHocThucTe().getTenPhong());
        }

        if (entity.getGiangVienThucTe() != null) {
            dto.setTenGvThucTe(entity.getGiangVienThucTe().getHoTen());
        }

        return dto;
    }
}