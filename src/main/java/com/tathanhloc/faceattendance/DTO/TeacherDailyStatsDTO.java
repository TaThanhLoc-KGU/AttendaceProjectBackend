// ===== TeacherDailyStatsDTO.java =====
package com.tathanhloc.faceattendance.DTO;

import lombok.*;
import java.time.LocalDate;

/**
 * DTO cho thong ke diem danh hang ngay cua giang vien
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDailyStatsDTO {
    private LocalDate date;
    private String maGv;
    private String tenGiangVien;

    // Thong ke tiet hoc
    private Integer totalSessions;      // Tong so tiet day
    private Integer completedSessions;  // So tiet da day xong
    private Integer ongoingSessions;    // So tiet dang day
    private Integer upcomingSessions;   // So tiet sap toi

    // Thong ke sinh vien
    private Integer totalStudents;      // Tong so sinh vien
    private Integer presentCount;       // So sinh vien co mat
    private Integer absentCount;        // So sinh vien vang mat
    private Integer lateCount;          // So sinh vien di tre
    private Integer excusedCount;       // So sinh vien vang co phep

    // Ti le diem danh
    private Double attendanceRate;      // Ti le diem danh chung

    /**
     * Tinh ti le hoan thanh tiet hoc
     */
    public Double getSessionCompletionRate() {
        if (totalSessions == null || totalSessions == 0) return 0.0;
        if (completedSessions == null) return 0.0;
        return (double) completedSessions / totalSessions * 100;
    }
}