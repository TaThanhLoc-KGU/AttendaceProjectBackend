package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LopHocPhanDTO {
    private String maLhp;
    private String hocKy;
    private String namHoc;
    private Integer nhom;
    private Boolean isActive;
    private String maMh;
    private String maGv;
    private Set<String> maSvs;

    private Integer soLuongSinhVien;

    // New fields for names - THÊM CÁC FIELD NÀY
    private String tenMonHoc;        // Tên môn học
    private String tenGiangVien;     // Tên giảng viên
    private Integer soTinChi;        // Số tín chỉ môn học

}
