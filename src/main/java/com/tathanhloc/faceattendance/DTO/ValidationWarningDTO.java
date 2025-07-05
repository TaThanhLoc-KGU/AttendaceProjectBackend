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
public class ValidationWarningDTO {
    private String field;
    private String value;
    private String warningCode;
    private String message;
    private Integer rowNumber;
}