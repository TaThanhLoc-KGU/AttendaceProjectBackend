package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nganh_monhoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganhMonHoc {

    @EmbeddedId
    private NganhMonHocId id;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Read-only relationships for convenience (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nganh", insertable = false, updatable = false)
    private Nganh nganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_mh", insertable = false, updatable = false)
    private MonHoc monHoc;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NganhMonHoc that = (NganhMonHoc) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NganhMonHoc{" +
                "id=" + id +
                ", isActive=" + isActive +
                '}';
    }
}