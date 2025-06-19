package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lhp_sinhvien")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DangKyHoc {

    @EmbeddedId
    private DangKyHocId id;

    @ManyToOne
    @MapsId("maSv")
    @JoinColumn(name = "ma_sv")
    private SinhVien sinhVien;

    @ManyToOne
    @MapsId("maLhp")
    @JoinColumn(name = "ma_lhp")
    private LopHocPhan lopHocPhan;

    @Column(name = "is_active")
    private boolean isActive;
}
