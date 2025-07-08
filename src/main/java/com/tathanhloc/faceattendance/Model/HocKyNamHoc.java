package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hoc_ky_nam_hoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKyNamHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_hoc_ky", referencedColumnName = "ma_hoc_ky")
    private HocKy hocKy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nam_hoc", referencedColumnName = "ma_nam_hoc")
    private NamHoc namHoc;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "thu_tu")
    private Integer thuTu; // Thứ tự học kỳ trong năm: 1, 2, 3...
}