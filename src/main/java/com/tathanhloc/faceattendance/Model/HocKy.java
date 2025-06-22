package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "hoc_ky")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKy {

    @Id
    @Column(name = "ma_hoc_ky")
    private String maHocKy;

    @Column(name = "ten_hoc_ky")
    private String tenHocKy;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
