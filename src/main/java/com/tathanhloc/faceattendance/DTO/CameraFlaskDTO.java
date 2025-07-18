package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraFlaskDTO {
    private Long id;
    private String tenCamera;
    private String rtspUrl;
    private String maPhong;
    private String tenPhong;
    private String loaiPhong;
    private ROIPolygonDTO vungIn;
    private ROIPolygonDTO vungOut;
    private Boolean isActive;
    private String hlsUrl;
    private ScheduleFlaskDTO currentSchedule;
}