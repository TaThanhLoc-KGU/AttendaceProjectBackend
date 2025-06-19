package com.tathanhloc.faceattendance.Model;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "diemdanh")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemDanh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ngày điểm danh không được để trống")
    @Column(name = "ngay_diem_danh")
    private LocalDate ngayDiemDanh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiDiemDanhEnum trangThai;

    @Column(name = "thoi_gian_vao")
    private LocalTime thoiGianVao;

    @Column(name = "thoi_gian_ra")
    private LocalTime thoiGianRa;

    @NotNull(message = "Lịch học không được để trống")
    @ManyToOne
    @JoinColumn(name = "ma_lich")
    private LichHoc lichHoc;

    @NotNull(message = "Sinh viên không được để trống")
    @ManyToOne
    @JoinColumn(name = "ma_sv")
    private SinhVien sinhVien;
}
