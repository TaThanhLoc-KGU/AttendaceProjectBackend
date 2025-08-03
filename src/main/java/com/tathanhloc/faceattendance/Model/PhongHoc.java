package com.tathanhloc.faceattendance.Model;

import com.tathanhloc.faceattendance.Enum.LoaiPhongEnum;
import com.tathanhloc.faceattendance.Enum.ThietBiEnum;
import com.tathanhloc.faceattendance.Enum.TrangThaiPhongEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "phonghoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhongHoc {
    @Id
    @Column(name = "ma_phong")
    private String maPhong;

    @Column(name = "ten_phong", nullable = false)
    private String tenPhong;

    @Column(name = "loai_phong")
    private String loaiPhong; // LECTURE, LAB, COMPUTER, CONFERENCE, OTHER

    @Column(name = "suc_chua")
    private Integer sucChua;

    @Column(name = "toa_nha")
    private String toaNha;

    @Column(name = "tang")
    private Integer tang;

    @Column(name = "trang_thai")
    private String trangThai; // AVAILABLE, OCCUPIED, MAINTENANCE, INACTIVE

    @Column(name = "vi_tri")
    private String viTri;

    @Column(name = "thiet_bi", columnDefinition = "TEXT")
    private String thietBi; // JSON string hoặc comma-separated: "PROJECTOR,COMPUTER,AC"

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
