package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "nganh")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nganh {
    @Id
    @Column(name = "ma_nganh")
    private String maNganh;

    @Column(name = "ten_nganh")
    private String tenNganh;

    @ManyToOne
    @JoinColumn(name = "ma_khoa")
    private Khoa khoa;

    @ManyToMany
    @JoinTable(
            name = "nganh_monhoc",
            joinColumns = @JoinColumn(name = "ma_nganh"),
            inverseJoinColumns = @JoinColumn(name = "ma_mh")
    )
    private Set<MonHoc> monHocs;

    @Column(name = "is_active")
    private boolean isActive;

}
