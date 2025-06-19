package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nganh_monhoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganhMonHoc {
    @EmbeddedId
    private NganhMonHocId id;

    @ManyToOne
    @MapsId("maNganh")
    private Nganh nganh;

    @ManyToOne
    @MapsId("maMh")
    private MonHoc monHoc;

    @Column(name = "is_active")
    private boolean isActive;
}
