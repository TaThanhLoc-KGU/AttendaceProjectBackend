package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBoxDTO {
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Double confidence;
    private String embedding; // Base64 encoded face embedding
    private Integer age;
    private String gender;
}