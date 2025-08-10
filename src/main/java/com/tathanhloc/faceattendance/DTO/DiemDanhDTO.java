package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemDanhDTO {

    private Long id;

    // ===== THÔNG TIN SINH VIÊN =====
    private String maSv;
    private String hoTen;

    // ===== THÔNG TIN ĐIỂM DANH =====
    private LocalDate ngayDiemDanh;
    private TrangThaiDiemDanhEnum trangThai;
    private LocalDateTime thoiGianVao;  // ← SỬA THÀNH LocalDateTime
    private LocalDateTime thoiGianRa;   // ← SỬA THÀNH LocalDateTime
    private String ghiChu;

    // ===== LEGACY SCHEDULE FIELDS =====
    private String maLich;
    private String maLhp;  // ← THÊM FIELD NÀY

    // ===== WEEK-BASED SCHEDULE FIELDS =====
    private String maInstance;
    private Integer tuanHoc;
    private Boolean isWeekBased;

    // ===== COMPUTED FIELDS =====
    private String scheduleType;
    private String scheduleDisplayName;
    private String tenMonHoc;      // ← THÊM FIELD NÀY
    private String tenGiangVien;   // ← THÊM FIELD NÀY
    private String tenPhong;       // ← THÊM FIELD NÀY

    // ===== AUDIT FIELDS =====
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== HELPER METHODS =====

    /**
     * Get actual schedule ID (either maLich or maInstance)
     */
    public String getActualScheduleId() {
        return isWeekBased != null && isWeekBased ? maInstance : maLich;
    }

    /**
     * Get display name for schedule type
     */
    public String getScheduleTypeDisplay() {
        if (Boolean.TRUE.equals(isWeekBased)) {
            return "Lịch theo tuần";
        } else {
            return "Lịch truyền thống";
        }
    }

    /**
     * Check if attendance is completed
     */
    public boolean isCompleted() {
        return thoiGianVao != null ||
                trangThai == TrangThaiDiemDanhEnum.CO_MAT ||
                trangThai == TrangThaiDiemDanhEnum.DI_TRE;
    }

    /**
     * Get status display in Vietnamese
     */
    public String getTrangThaiDisplay() {
        if (trangThai == null) return "Chưa xác định";

        switch (trangThai) {
            case CO_MAT:
                return "Có mặt";
            case VANG_MAT:
                return "Vắng mặt";
            case DI_TRE:
                return "Muộn";
            case VANG_CO_PHEP:
                return "Vắng có phép";
            default:
                return trangThai.toString();
        }
    }
}
