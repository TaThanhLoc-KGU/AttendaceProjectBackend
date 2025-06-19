package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganhMonHocId implements Serializable {
    @Column(name = "ma_nganh")
    private String maNganh;

    @Column(name = "ma_mh")
    private String maMh;
}
