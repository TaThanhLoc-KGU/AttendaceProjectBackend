package com.tathanhloc.faceattendance.Model;

import com.tathanhloc.faceattendance.Enum.VaiTroEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "taikhoan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Column(name = "username")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(name = "password_hash")
    private String passwordHash;

    @NotNull(message = "Vai trò không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro")
    private VaiTroEnum vaiTro;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "ma_sv")
    private SinhVien sinhVien;

    @OneToOne
    @JoinColumn(name = "ma_gv")
    private GiangVien giangVien;
}
