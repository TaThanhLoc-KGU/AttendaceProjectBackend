// ===== StudentAttendanceDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO cho thong tin diem danh cua sinh vien
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceDTO {
    private String maSv;
    private String hoTen;
    private String lop;
    private String email;

    // Thong tin diem danh
    private TrangThaiDiemDanhEnum trangThai;
    private LocalDateTime thoiGianVao;
    private LocalDateTime thoiGianRa;
    private String ghiChu;
    private Boolean daDigemDanh;

    // Metadata
    private String avatarUrl;
    private Boolean hasWarning;  // Canh bao (vi pham gi do)
    private String warningMessage;

    /**
     * Kiem tra co di tre khong
     */
    public Boolean isDiTre() {
        return TrangThaiDiemDanhEnum.DI_TRE.equals(trangThai);
    }

    /**
     * Kiem tra co mat khong
     */
    public Boolean isCoMat() {
        return TrangThaiDiemDanhEnum.CO_MAT.equals(trangThai);
    }

    /**
     * Kiem tra vang mat
     */
    public Boolean isVangMat() {
        return TrangThaiDiemDanhEnum.VANG_MAT.equals(trangThai);
    }
}
