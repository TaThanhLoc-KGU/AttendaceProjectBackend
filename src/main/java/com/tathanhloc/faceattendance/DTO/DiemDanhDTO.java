package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemDanhDTO {
    private Long id;
    private LocalDate ngayDiemDanh;
    private TrangThaiDiemDanhEnum trangThai;
    private LocalTime thoiGianVao;
    private LocalTime thoiGianRa;
    private String maLich;
    private String maSv;
}
