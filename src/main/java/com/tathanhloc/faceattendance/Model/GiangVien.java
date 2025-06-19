package com.tathanhloc.faceattendance.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "giangvien")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiangVien {
    @Id
    @Column(name = "ma_gv")
    private String maGv;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "email")
    private String email;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "ma_khoa")
    private Khoa khoa;
}
