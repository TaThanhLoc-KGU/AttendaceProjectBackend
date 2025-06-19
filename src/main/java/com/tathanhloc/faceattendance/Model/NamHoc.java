package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "nam_hoc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NamHoc {

    @Id
    @Column(name = "ma_nam_hoc")
    private String maNamHoc;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
