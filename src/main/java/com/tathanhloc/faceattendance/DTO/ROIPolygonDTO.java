package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ROIPolygonDTO {
    private List<PointDTO> points;
    private String type; // "IN" hoặc "OUT"
    private String description;
}