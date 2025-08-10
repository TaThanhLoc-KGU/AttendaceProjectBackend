package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Model.WeeklySchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho time slot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSlotDTO {
    private Integer thu;
    private Integer tietBatDau;
    private Integer soTiet;
    private String maPhong;
    private WeeklySchedule.LoaiLich loaiLich;
}