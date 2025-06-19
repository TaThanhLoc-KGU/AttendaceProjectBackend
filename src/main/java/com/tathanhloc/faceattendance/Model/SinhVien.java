package com.tathanhloc.faceattendance.Model;

import com.tathanhloc.faceattendance.Enum.GioiTinhEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sinhvien")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinhVien {
    @Id
    @NotBlank(message = "Mã sinh viên không được để trống")
    @Size(max = 20, message = "Mã sinh viên không được vượt quá 20 ký tự")
    @Column(name = "ma_sv")
    private String maSv;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    @Column(name = "ho_ten")
    private String hoTen;

    @Enumerated(EnumType.STRING)
    @Column(name = "gioi_tinh")
    private GioiTinhEnum gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Email(message = "Email không hợp lệ")
    @Column(name = "email")
    private String email;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "embedding")
    private String embedding;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Column(name = "is_active")
    private Boolean isActive;

    @NotNull(message = "Lớp không được để trống")
    @ManyToOne
    @JoinColumn(name = "ma_lop")
    private Lop lop;
}
