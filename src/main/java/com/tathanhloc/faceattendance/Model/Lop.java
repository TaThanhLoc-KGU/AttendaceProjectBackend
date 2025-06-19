package com.tathanhloc.faceattendance.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lop")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lop {
    @Id
    @Column(name = "ma_lop")
    private String maLop;

    @Column(name = "ten_lop")
    private String tenLop;

    @ManyToOne
    @JoinColumn(name = "ma_nganh")
    private Nganh nganh;

    @ManyToOne
    @JoinColumn(name = "ma_khoahoc")
    private KhoaHoc khoaHoc;

    @ManyToOne
    @JoinColumn(name = "ma_khoa")
    private Khoa maKhoa;

    @Column(name = "is_active")
    private boolean isActive;
}
