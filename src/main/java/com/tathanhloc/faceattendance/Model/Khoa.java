package com.tathanhloc.faceattendance.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "khoa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Khoa {
    @Id
    @Column(name = "ma_khoa")
    private String maKhoa;

    @Column(name = "ten_khoa")
    private String tenKhoa;

    @Column(name = "is_active")
    private Boolean isActive;
}
