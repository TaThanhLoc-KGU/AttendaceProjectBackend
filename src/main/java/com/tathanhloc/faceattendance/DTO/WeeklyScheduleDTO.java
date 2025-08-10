package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Model.WeeklySchedule;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho WeeklySchedule Template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyScheduleDTO {

    private String maTemplate;

    // ===== THÔNG TIN LỚP HỌC PHẦN =====
    @NotBlank(message = "Mã lớp học phần không được để trống")
    private String maLhp;

    // Thông tin bổ sung từ LopHocPhan
    private String tenMonHoc;
    private String maMh;
    private String tenGiangVien;
    private String maGv;
    private String hocKy;
    private String namHoc;
    private Integer nhom;

    // ===== THÔNG TIN THỜI GIAN =====
    @NotNull(message = "Thứ không được để trống")
    @Min(value = 2, message = "Thứ phải từ 2 (Thứ hai)")
    @Max(value = 8, message = "Thứ không được quá 8 (Chủ nhật)")
    private Integer thu;

    @NotNull(message = "Tiết bắt đầu không được để trống")
    @Min(value = 1, message = "Tiết bắt đầu phải từ 1")
    @Max(value = 12, message = "Tiết bắt đầu không được quá 12")
    private Integer tietBatDau;

    @NotNull(message = "Số tiết không được để trống")
    @Min(value = 1, message = "Số tiết phải ít nhất 1")
    @Max(value = 6, message = "Số tiết không được quá 6")
    private Integer soTiet;

    // ===== PHẠM VI ÁP DỤNG =====
    @NotNull(message = "Tuần bắt đầu không được để trống")
    @Min(value = 1, message = "Tuần bắt đầu phải từ 1")
    private Integer tuanBatDau;

    @NotNull(message = "Tuần kết thúc không được để trống")
    @Min(value = 1, message = "Tuần kết thúc phải từ 1")
    private Integer tuanKetThuc;

    // ===== PHÒNG HỌC =====
    private String maPhongMacDinh;
    private String tenPhongMacDinh;

    // ===== METADATA =====
    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String moTa;

    private Boolean isActive;
    private WeeklySchedule.LoaiLich loaiLich;
    private WeeklySchedule.TrangThaiTemplate trangThai;

    // ===== AUDIT =====
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== COMPUTED FIELDS =====
    private Integer tongSoTuan;
    private Integer tietKetThuc;
    private String thoiGianHienThi; // "Thứ 2, tiết 1-2"
    private String phamViTuan; // "Tuần 1-15"
    private Integer soInstanceDaTao;
    private Integer soInstanceHoanThanh;

    // ===== RELATIONSHIPS =====
    private List<ScheduleInstanceDTO> instances;

    /**
     * Convert entity to DTO
     */
    public static WeeklyScheduleDTO fromEntity(WeeklySchedule entity) {
        if (entity == null) return null;

        WeeklyScheduleDTOBuilder builder = WeeklyScheduleDTO.builder()
                .maTemplate(entity.getMaTemplate())
                .thu(entity.getThu())
                .tietBatDau(entity.getTietBatDau())
                .soTiet(entity.getSoTiet())
                .tuanBatDau(entity.getTuanBatDau())
                .tuanKetThuc(entity.getTuanKetThuc())
                .moTa(entity.getMoTa())
                .isActive(entity.getIsActive())
                .loaiLich(entity.getLoaiLich())
                .trangThai(entity.getTrangThai())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tongSoTuan(entity.getTongSoTuan())
                .tietKetThuc(entity.getTietKetThuc());

        // Thông tin LopHocPhan
        if (entity.getLopHocPhan() != null) {
            var lhp = entity.getLopHocPhan();
            builder.maLhp(lhp.getMaLhp())
                    .hocKy(lhp.getHocKy())
                    .namHoc(lhp.getNamHoc())
                    .nhom(lhp.getNhom());

            if (lhp.getMonHoc() != null) {
                builder.maMh(lhp.getMonHoc().getMaMh())
                        .tenMonHoc(lhp.getMonHoc().getTenMh());
            }

            if (lhp.getGiangVien() != null) {
                builder.maGv(lhp.getGiangVien().getMaGv())
                        .tenGiangVien(lhp.getGiangVien().getHoTen());
            }
        }

        // Thông tin PhongHoc
        if (entity.getPhongHocMacDinh() != null) {
            builder.maPhongMacDinh(entity.getPhongHocMacDinh().getMaPhong())
                    .tenPhongMacDinh(entity.getPhongHocMacDinh().getTenPhong());
        }

        WeeklyScheduleDTO dto = builder.build();

        // Computed fields
        dto.setThoiGianHienThi(dto.buildThoiGianHienThi());
        dto.setPhamViTuan(dto.buildPhamViTuan());

        return dto;
    }

    // ===== HELPER METHODS =====
    private String buildThoiGianHienThi() {
        if (thu == null || tietBatDau == null || soTiet == null) return "";

        String[] thuNames = {"", "", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
        String thuName = (thu >= 2 && thu <= 8) ? thuNames[thu] : "Thứ " + thu;

        if (soTiet == 1) {
            return String.format("%s, tiết %d", thuName, tietBatDau);
        } else {
            return String.format("%s, tiết %d-%d", thuName, tietBatDau, tietBatDau + soTiet - 1);
        }
    }

    private String buildPhamViTuan() {
        if (tuanBatDau == null || tuanKetThuc == null) return "";

        if (tuanBatDau.equals(tuanKetThuc)) {
            return "Tuần " + tuanBatDau;
        } else {
            return String.format("Tuần %d-%d", tuanBatDau, tuanKetThuc);
        }
    }

    public boolean isValidTemplate() {
        return tuanBatDau != null && tuanKetThuc != null &&
                tuanBatDau <= tuanKetThuc &&
                thu != null && thu >= 2 && thu <= 8 &&
                tietBatDau != null && tietBatDau >= 1 && tietBatDau <= 12 &&
                soTiet != null && soTiet >= 1 && soTiet <= 6 &&
                (tietBatDau + soTiet - 1) <= 12;
    }
}