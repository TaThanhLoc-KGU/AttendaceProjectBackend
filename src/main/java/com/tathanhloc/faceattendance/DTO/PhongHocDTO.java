package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.LoaiPhongEnum;
import com.tathanhloc.faceattendance.Enum.ThietBiEnum;
import com.tathanhloc.faceattendance.Enum.TrangThaiPhongEnum;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhongHocDTO {
    private String maPhong;
    private String tenPhong;
    private String loaiPhong;
    private Integer sucChua;
    private String toaNha;
    private Integer tang;
    private String trangThai;
    private String viTri;
    private String thietBi; // Comma-separated string
    private String moTa;
    private Boolean isActive;

    // Display fields for frontend
    private String loaiPhongDisplay;
    private String trangThaiDisplay;
    private String thietBiDisplay;
}