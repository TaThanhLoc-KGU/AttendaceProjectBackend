package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Model.NamHoc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKyNamHocDTO {
    private Integer id;
    private String maHocKy;
    private String maNamHoc;
    private Integer thuTu;
    private Boolean isActive;

    // Thông tin mở rộng
    private String tenHocKy;
    private String tenNamHoc;
}