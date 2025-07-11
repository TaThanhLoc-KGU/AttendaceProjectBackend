package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.LopHocPhanDTO;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.LopHocPhanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API Controller cho phân quyền giảng viên
 * Cung cấp các endpoint API để frontend gọi dữ liệu
 */
@RestController
@RequestMapping("/api/lecturer")
@RequiredArgsConstructor
@Slf4j
public class LecturerApiController {

    private final LopHocPhanService lopHocPhanService;
    private final LopHocPhanRepository lopHocPhanRepository;

    /**
     * API lấy danh sách lớp học của giảng viên đang đăng nhập
     * Endpoint: GET /api/lecturer/lophoc
     */
    @GetMapping("/lophoc")
    public ResponseEntity<List<LopHocPhanDTO>> getLopHocByLecturer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search) {

        try {
            log.info("=== API GET LECTURER CLASSES ===");
            log.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");
            log.info("Filters - Semester: {}, Year: {}, Active: {}, Search: {}", semester, year, isActive, search);

            // Kiểm tra authentication
            if (userDetails == null) {
                log.warn("No user details found");
                return ResponseEntity.status(401).build();
            }

            // Kiểm tra có thông tin giảng viên không
            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                return ResponseEntity.status(403).build();
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading classes for lecturer: {}", maGv);

            List<LopHocPhanDTO> lecturerClasses;

            // Sử dụng search nếu có từ khóa
            if (search != null && !search.trim().isEmpty()) {
                lecturerClasses = lopHocPhanService.searchByGiangVienAndKeyword(maGv, search);
            } else {
                // Sử dụng method tối ưu với semester/year
                lecturerClasses = lopHocPhanService.getByGiangVienAndSemester(maGv, semester, year);
            }

            // Áp dụng filter active status nếu có
            if (isActive != null) {
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> isActive.equals(lhp.getIsActive()))
                        .collect(Collectors.toList());
            }

            log.info("✅ Found {} classes for lecturer {}", lecturerClasses.size(), maGv);
            return ResponseEntity.ok(lecturerClasses);

        } catch (Exception e) {
            log.error("❌ Error loading lecturer classes", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API lấy thống kê tổng quan của giảng viên
     * Endpoint: GET /api/lecturer/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<LecturerStatisticsDTO> getLecturerStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            log.info("=== API GET LECTURER STATISTICS ===");

            if (userDetails == null || userDetails.getTaiKhoan().getGiangVien() == null) {
                return ResponseEntity.status(403).build();
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Sử dụng các method service được tối ưu
            long totalClasses = lopHocPhanService.countByGiangVien(maGv);
            long activeClasses = lopHocPhanRepository.countByGiangVienMaGvAndIsActive(maGv, true);
            int totalStudents = lopHocPhanService.countStudentsByGiangVien(maGv);
            int uniqueSubjects = lopHocPhanService.getUniqueSubjectsByGiangVien(maGv).size();

            LecturerStatisticsDTO statistics = LecturerStatisticsDTO.builder()
                    .totalClasses((int) totalClasses)
                    .activeClasses((int) activeClasses)
                    .totalStudents(totalStudents)
                    .uniqueSubjects(uniqueSubjects)
                    .lecturerName(userDetails.getTaiKhoan().getGiangVien().getHoTen())
                    .lecturerCode(maGv)
                    .build();

            log.info("✅ Statistics calculated for lecturer {}: {} classes, {} students",
                    maGv, totalClasses, totalStudents);

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("❌ Error calculating lecturer statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * DTO class for lecturer statistics
     */
    public static class LecturerStatisticsDTO {
        private int totalClasses;
        private int activeClasses;
        private int totalStudents;
        private int uniqueSubjects;
        private String lecturerName;
        private String lecturerCode;

        // Builder pattern
        public static LecturerStatisticsDTOBuilder builder() {
            return new LecturerStatisticsDTOBuilder();
        }

        public static class LecturerStatisticsDTOBuilder {
            private int totalClasses;
            private int activeClasses;
            private int totalStudents;
            private int uniqueSubjects;
            private String lecturerName;
            private String lecturerCode;

            public LecturerStatisticsDTOBuilder totalClasses(int totalClasses) {
                this.totalClasses = totalClasses;
                return this;
            }

            public LecturerStatisticsDTOBuilder activeClasses(int activeClasses) {
                this.activeClasses = activeClasses;
                return this;
            }

            public LecturerStatisticsDTOBuilder totalStudents(int totalStudents) {
                this.totalStudents = totalStudents;
                return this;
            }

            public LecturerStatisticsDTOBuilder uniqueSubjects(int uniqueSubjects) {
                this.uniqueSubjects = uniqueSubjects;
                return this;
            }

            public LecturerStatisticsDTOBuilder lecturerName(String lecturerName) {
                this.lecturerName = lecturerName;
                return this;
            }

            public LecturerStatisticsDTOBuilder lecturerCode(String lecturerCode) {
                this.lecturerCode = lecturerCode;
                return this;
            }

            public LecturerStatisticsDTO build() {
                LecturerStatisticsDTO dto = new LecturerStatisticsDTO();
                dto.totalClasses = this.totalClasses;
                dto.activeClasses = this.activeClasses;
                dto.totalStudents = this.totalStudents;
                dto.uniqueSubjects = this.uniqueSubjects;
                dto.lecturerName = this.lecturerName;
                dto.lecturerCode = this.lecturerCode;
                return dto;
            }
        }

        // Getters and Setters
        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

        public int getActiveClasses() { return activeClasses; }
        public void setActiveClasses(int activeClasses) { this.activeClasses = activeClasses; }

        public int getTotalStudents() { return totalStudents; }
        public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

        public int getUniqueSubjects() { return uniqueSubjects; }
        public void setUniqueSubjects(int uniqueSubjects) { this.uniqueSubjects = uniqueSubjects; }

        public String getLecturerName() { return lecturerName; }
        public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }

        public String getLecturerCode() { return lecturerCode; }
        public void setLecturerCode(String lecturerCode) { this.lecturerCode = lecturerCode; }
    }
}