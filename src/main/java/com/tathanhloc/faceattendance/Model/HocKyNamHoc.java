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

    @ManyToOne
    @JoinColumn(name = "ma_hoc_ky")
    private HocKy hocKy;

    @ManyToOne
    @JoinColumn(name = "ma_nam_hoc")
    private NamHoc namHoc;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
