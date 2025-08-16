// ===== AttendanceRecordDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO cho ban ghi diem danh (dung cho export/report)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordDTO {
    private LocalDate date;
    private String maSv;
    private String hoTen;
    private String lop;
    private String monHoc;
    private String maMonHoc;
    private Integer tietBatDau;
    private Integer soTiet;
    private String phongHoc;
    private TrangThaiDiemDanhEnum trangThai;
    private LocalDateTime thoiGianVao;
    private LocalDateTime thoiGianRa;
    private String ghiChu;

    /**
     * Chuyen trang thai thanh text
     */
    public String getTrangThaiText() {
        if (trangThai == null) return "Chua diem danh";

        return switch (trangThai) {
            case CO_MAT -> "Co mat";
            case VANG_MAT -> "Vang mat";
            case DI_TRE -> "Di tre";
            case VANG_CO_PHEP -> "Vang co phep";
        };
    }
}