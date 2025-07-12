package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Security.CustomUserDetails;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final DangKyHocService dangKyHocService;
    private final SinhVienService sinhVienService;
    private final LichHocService lichHocService;
    private final DiemDanhService diemDanhService;

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

            // Lấy danh sách lớp học phần - sử dụng method có sẵn
            List<LopHocPhanDTO> allClasses = lopHocPhanService.getAllWithNames();

            // Lọc theo giảng viên
            List<LopHocPhanDTO> lecturerClasses = allClasses.stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            // Áp dụng filter nếu có
            if (semester != null && !semester.isEmpty()) {
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> semester.equals(lhp.getHocKy()))
                        .collect(Collectors.toList());
            }

            if (year != null && !year.isEmpty()) {
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> year.equals(lhp.getNamHoc()))
                        .collect(Collectors.toList());
            }

            if (isActive != null) {
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> isActive.equals(lhp.getIsActive()))
                        .collect(Collectors.toList());
            }

            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                lecturerClasses = lecturerClasses.stream()
                        .filter(lhp -> {
                            String searchableText = String.join(" ",
                                    lhp.getTenMonHoc() != null ? lhp.getTenMonHoc() : "",
                                    lhp.getMaMh() != null ? lhp.getMaMh() : "",
                                    lhp.getMaLhp() != null ? lhp.getMaLhp() : ""
                            ).toLowerCase();
                            return searchableText.contains(searchLower);
                        })
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

            // Lấy danh sách lớp của giảng viên
            List<LopHocPhanDTO> lecturerClasses = lopHocPhanService.getAllWithNames().stream()
                    .filter(lhp -> maGv.equals(lhp.getMaGv()))
                    .collect(Collectors.toList());

            // Tính toán thống kê
            int totalClasses = lecturerClasses.size();
            int activeClasses = (int) lecturerClasses.stream()
                    .filter(lhp -> Boolean.TRUE.equals(lhp.getIsActive()))
                    .count();
            int totalStudents = lecturerClasses.stream()
                    .mapToInt(lhp -> lhp.getSoLuongSinhVien() != null ? lhp.getSoLuongSinhVien() : 0)
                    .sum();
            int uniqueSubjects = (int) lecturerClasses.stream()
                    .map(LopHocPhanDTO::getMaMh)
                    .distinct()
                    .count();

            LecturerStatisticsDTO statistics = LecturerStatisticsDTO.builder()
                    .totalClasses(totalClasses)
                    .activeClasses(activeClasses)
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

    /**
     * Trang chi tiết lớp học cụ thể
     */
    @GetMapping("/lophoc/{maLhp}")
    public String chiTietLopHoc(@PathVariable String maLhp,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        if (userDetails == null || !userDetails.isCredentialsNonExpired()) {
            return "redirect:/?error=not_authenticated";
        }

        try {
            // Kiểm tra thông tin giảng viên
            if (userDetails.getTaiKhoan().getGiangVien() == null) {
                log.error("User has no lecturer profile: {}", userDetails.getUsername());
                model.addAttribute("error", "Tài khoản không có thông tin giảng viên");
                return "lecturer/chitiet-lophoc";
            }

            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();
            log.info("Loading class details for lecturer {} and class {}", maGv, maLhp);

            // Lấy thông tin lớp học phần
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);

            // Kiểm tra quyền truy cập (chỉ giảng viên của lớp mới được xem)
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                log.warn("Lecturer {} tried to access class {} which is not theirs", maGv, maLhp);
                model.addAttribute("error", "Bạn không có quyền truy cập lớp học này");
                return "lecturer/chitiet-lophoc";
            }

            // Lấy danh sách sinh viên trong lớp
            List<DangKyHocDTO> danhSachDangKy = dangKyHocService.getByMaLhp(maLhp);
            List<SinhVienDTO> danhSachSinhVien = danhSachDangKy.stream()
                    .map(dk -> sinhVienService.getByMaSv(dk.getMaSv()))
                    .collect(Collectors.toList());

            // Lấy lịch học của lớp này
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);

            // Thêm vào model
            model.addAttribute("currentUser", userDetails.getTaiKhoan());
            model.addAttribute("lopHocPhan", lopHocPhan);
            model.addAttribute("danhSachSinhVien", danhSachSinhVien);
            model.addAttribute("lichHocList", lichHocList);
            model.addAttribute("soLuongSinhVien", danhSachSinhVien.size());

            log.info("✅ Class details loaded for {}: {} students", maLhp, danhSachSinhVien.size());
            return "lecturer/chitiet-lophoc";

        } catch (Exception e) {
            log.error("❌ Error loading class details for {}", maLhp, e);
            model.addAttribute("error", "Không thể tải thông tin lớp học: " + e.getMessage());
            return "lecturer/chitiet-lophoc";
        }
    }
    /**
     * API lấy điểm danh theo lớp học phần
     */
    @GetMapping("/api/diemdanh/{maLhp}")
    @ResponseBody
    public ResponseEntity<List<DiemDanhDTO>> getDiemDanhByClass(
            @PathVariable String maLhp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getTaiKhoan().getGiangVien() == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            // Lấy điểm danh theo khoảng thời gian
            List<DiemDanhDTO> diemDanhList = diemDanhService.getByLopHocPhanAndDateRange(maLhp, fromDate, toDate);

            return ResponseEntity.ok(diemDanhList);

        } catch (Exception e) {
            log.error("Error getting attendance for class {}: {}", maLhp, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API lấy thống kê điểm danh theo lớp
     */
    @GetMapping("/api/diemdanh/{maLhp}/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAttendanceStats(
            @PathVariable String maLhp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getTaiKhoan().getGiangVien() == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            String maGv = userDetails.getTaiKhoan().getGiangVien().getMaGv();

            // Kiểm tra quyền truy cập
            LopHocPhanDTO lopHocPhan = lopHocPhanService.getByMaLhp(maLhp);
            if (!maGv.equals(lopHocPhan.getMaGv())) {
                return ResponseEntity.status(403).build();
            }

            // Lấy thống kê điểm danh
            Map<String, Object> stats = diemDanhService.getAttendanceStatsByClass(maLhp, fromDate, toDate);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting attendance stats for class {}: {}", maLhp, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}