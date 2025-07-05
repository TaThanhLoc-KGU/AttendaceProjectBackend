package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "nganh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nganh {
    @Id
    @Column(name = "ma_nganh")
    private String maNganh;

    @Column(name = "ten_nganh")
    private String tenNganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_khoa")
    private Khoa khoa;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "nganh_monhoc",
            joinColumns = @JoinColumn(name = "ma_nganh"),
            inverseJoinColumns = @JoinColumn(name = "ma_mh")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MonHoc> monHocs;

    @Column(name = "is_active")
    private boolean isActive;

    // Custom equals and hashCode - chỉ dựa trên ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nganh nganh = (Nganh) o;
        return Objects.equals(maNganh, nganh.maNganh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maNganh);
    }

    @Override
    public String toString() {
        return "Nganh{" +
                "maNganh='" + maNganh + '\'' +
                ", tenNganh='" + tenNganh + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
