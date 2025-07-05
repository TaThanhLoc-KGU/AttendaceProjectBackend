package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "monhoc")
@Getter
@Setter
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

    @ManyToMany(mappedBy = "monHocs", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Nganh> nganhs;

    // Custom equals and hashCode - chỉ dựa trên ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonHoc monHoc = (MonHoc) o;
        return Objects.equals(maMh, monHoc.maMh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maMh);
    }

    @Override
    public String toString() {
        return "MonHoc{" +
                "maMh='" + maMh + '\'' +
                ", tenMh='" + tenMh + '\'' +
                ", soTinChi=" + soTinChi +
                ", isActive=" + isActive +
                '}';
    }
}
