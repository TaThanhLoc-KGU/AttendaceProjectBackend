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
public class SearchCriteriaDTO {
    private String searchTerm;
    private String classFilter;
    private String statusFilter;
    private String genderFilter;
    private String departmentFilter;
    private String majorFilter;
    private Integer minAge;
    private Integer maxAge;
    private Boolean hasPhoto;
    private Boolean hasEmbedding;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
}