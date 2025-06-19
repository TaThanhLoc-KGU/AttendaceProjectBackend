package com.tathanhloc.faceattendance.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "lophocphan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LopHocPhan {
    @Id
    @Column(name = "ma_lhp")
    private String maLhp;

    @Column(name = "hoc_ky")
    private String hocKy;

    @Column(name = "nam_hoc")
    private String namHoc;

    @Column(name = "nhom")
    private Integer nhom;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "ma_mh")
    private MonHoc monHoc;

    @ManyToOne
    @JoinColumn(name = "ma_gv")
    private GiangVien giangVien;

    @ManyToMany
    @JoinTable(
            name = "lhp_sinhvien",
            joinColumns = @JoinColumn(name = "ma_lhp"),
            inverseJoinColumns = @JoinColumn(name = "ma_sv")
    )
    private Set<SinhVien> sinhViens;
}
