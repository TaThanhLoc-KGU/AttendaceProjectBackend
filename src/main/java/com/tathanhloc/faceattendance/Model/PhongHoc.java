package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "ten_phong")
    private String tenPhong;

    @Column(name = "mo_ta")
    private String moTa;
}
