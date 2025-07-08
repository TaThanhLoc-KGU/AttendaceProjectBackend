package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lichhoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichHoc {
    @Id
    @Column(name = "ma_lich")
    private String maLich;

    @Column(name = "thu")
    private Integer thu;

    @Column(name = "tiet_bat_dau")
    private Integer tietBatDau;

    @Column(name = "so_tiet")
    private Integer soTiet;

    @ManyToOne
    @JoinColumn(name = "ma_lhp")
    private LopHocPhan lopHocPhan;

    @ManyToOne
    @JoinColumn(name = "ma_phong")
    private PhongHoc phongHoc;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "created_at")
    @CreationTimestamp  // Tự động set khi tạo
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp   // Tự động set khi update
    private LocalDateTime updatedAt;

}
