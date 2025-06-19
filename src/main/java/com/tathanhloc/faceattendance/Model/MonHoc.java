package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "monhoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonHoc {
    @Id
    @Column(name = "ma_mh")
    private String maMh;

    @Column(name = "ten_mh")
    private String tenMh;

    @Column(name = "so_tin_chi")
    private Integer soTinChi;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToMany(mappedBy = "monHocs")
    private Set<Nganh> nganhs;
}
