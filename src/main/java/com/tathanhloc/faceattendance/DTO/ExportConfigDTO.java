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
public class ExportConfigDTO {
    private String format; // excel, pdf, csv
    private List<String> columns;
    private SearchCriteriaDTO criteria;
    private Boolean includeImages;
    private Boolean includeStatistics;
    private String fileName;
    private String templateType;
}