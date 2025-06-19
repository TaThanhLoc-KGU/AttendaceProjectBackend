package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "camera", schema = "face_attendance")
public class Camera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "ten_camera", nullable = false)
    private String tenCamera;

    @Size(max = 255)
    @Column(name = "ip_address")
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong")
    private PhongHoc maPhong;

    @Size(max = 255)
    @Column(name = "password")
    private String password;

    @Lob
    @Column(name = "vung_in")
    private String vungIn;

    @Lob
    @Column(name = "vung_out")
    private String vungOut;

    @ColumnDefault("b'1'")
    @Column(name = "is_active")
    private Boolean isActive;

}
