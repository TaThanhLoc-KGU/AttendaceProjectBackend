package com.tathanhloc.faceattendance.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {
    private Long totalStudents;
    private Long activeStudents;
    private Long inactiveStudents;
    private Long studentsWithPhoto;
    private Long studentsWithoutPhoto;
    private Long studentsWithEmbedding;
    private Long studentsWithoutEmbedding;
    private LocalDateTime lastUpdated;
}