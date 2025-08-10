package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho bulk operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyScheduleBulkDTO {
    private List<String> maLhpList;
    private Integer tuanBatDau;
    private Integer tuanKetThuc;
    private List<TimetableSlotDTO> timeSlots;
    private Boolean autoAssignRooms;
    private Boolean avoidConflicts;
    private Boolean autoActivate;
    private String createdBy;
}