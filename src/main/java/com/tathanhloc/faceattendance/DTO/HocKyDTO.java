package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Model.HocKy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKyDTO {
    private String maHocKy;

    @NotBlank(message = "Tên học kỳ không được để trống")
    private String tenHocKy;

    // ===== EXISTING DATE FIELDS (for backward compatibility) =====
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate ngayKetThuc;

    // ===== NEW WEEK-BASED FIELDS (optional) =====
    @Min(value = 1, message = "Tuần bắt đầu phải từ 1 trở lên")
    @Max(value = 52, message = "Tuần bắt đầu không được quá 52")
    private Integer tuanBatDau;

    @Min(value = 4, message = "Số tuần học tối thiểu là 4 tuần")
    @Max(value = 20, message = "Số tuần học tối đa là 20 tuần")
    private Integer soTuanHoc;

    private LocalDate ngayBatDauTuan1;

    // ===== METADATA =====
    private String moTa;
    private Boolean isActive;
    private Boolean isCurrent;
    private HocKy.LoaiHocKy loaiHocKy;

    // ===== COMPUTED FIELDS =====
    private Integer tuanKetThuc;              // Tuần cuối cùng (nếu week-based)
    private LocalDate ngayBatDauThucTe;       // Ngày bắt đầu thực tế
    private LocalDate ngayKetThucThucTe;      // Ngày kết thúc thực tế
    private String trangThai;                 // "Chưa bắt đầu", "Đang diễn ra", "Đã kết thúc"
    private Integer tuanHienTai;              // Tuần hiện tại (nếu đang diễn ra)
    private Double tiLePhanTram;             // Tiến độ theo %
    private Integer soNgayConLai;             // Số ngày còn lại
    private Integer tongSoNgay;               // Tổng số ngày của học kỳ
    private Boolean isWeekBasedConfig;        // Có sử dụng week-based config không

    // ===== ACADEMIC METRICS =====
    private Integer soBuoiHocDuKien;          // Số buổi học dự kiến
    private Integer soBuoiHocThucTe;          // Số buổi học thực tế đã diễn ra
    private Integer soLopHocPhan;             // Số lớp học phần trong học kỳ
    private Integer soSinhVienDangKy;         // Tổng số sinh viên đăng ký

    // ===== WEEK BREAKDOWN =====
    private List<TuanHocDTO> danhSachTuanHoc;  // Chi tiết từng tuần (nếu week-based)

    // ===== RELATED DATA =====
    private List<String> maNamHocList;        // Danh sách mã năm học liên kết
    private String tenNamHocChinh;            // Tên năm học chính

    // ===== VALIDATION STATUS =====
    private Boolean isValidDateRange;         // Kiểm tra phạm vi ngày hợp lệ
    private Boolean isValidWeekRange;         // Kiểm tra phạm vi tuần hợp lệ
    private Boolean hasConflicts;             // Có xung đột với học kỳ khác không
    private List<String> validationMessages; // Danh sách lỗi validation


    // Thêm vào HocKyDTO.java - sau phần fields hiện tại
    private Integer thuTu;                    // Thứ tự trong năm học
    private String maNamHoc;                  // Mã năm học (từ relationship)
    private String tenNamHoc;                 // Tên năm học (từ relationship)

    // ===== HELPER METHODS FOR FRONTEND =====

    public String getKhoangThoiGianDisplay() {
        LocalDate start = ngayBatDauThucTe != null ? ngayBatDauThucTe : ngayBatDau;
        LocalDate end = ngayKetThucThucTe != null ? ngayKetThucThucTe : ngayKetThuc;

        if (start == null || end == null) {
            return "Chưa xác định";
        }
        return String.format("%s - %s", formatDate(start), formatDate(end));
    }

    public String getTuanDisplay() {
        if (tuanBatDau == null || tuanKetThuc == null) {
            return "Theo ngày";
        }
        if (tuanBatDau.equals(tuanKetThuc)) {
            return "Tuần " + tuanBatDau;
        }
        return String.format("Tuần %d - %d", tuanBatDau, tuanKetThuc);
    }

    public String getSoTuanDisplay() {
        if (soTuanHoc == null) {
            // Tính từ ngày nếu không có week config
            if (ngayBatDau != null && ngayKetThuc != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(ngayBatDau, ngayKetThuc);
                int weeks = (int) Math.ceil(days / 7.0);
                return weeks + " tuần (tính từ ngày)";
            }
            return "Chưa xác định";
        }
        return soTuanHoc + " tuần";
    }

    public String getLoaiHocKyDisplay() {
        return loaiHocKy != null ? loaiHocKy.getDisplayName() : "Chính quy";
    }

    public String getTrangThaiColor() {
        if (trangThai == null) return "secondary";
        return switch (trangThai) {
            case "Đang diễn ra" -> "success";
            case "Chưa bắt đầu" -> "primary";
            case "Đã kết thúc" -> "secondary";
            default -> "secondary";
        };
    }

    public String getTienDoDisplay() {
        if (tiLePhanTram == null) return "0%";
        return String.format("%.1f%%", tiLePhanTram);
    }

    public String getConfigTypeDisplay() {
        return Boolean.TRUE.equals(isWeekBasedConfig) ? "Theo tuần" : "Theo ngày";
    }

    public boolean isCanEdit() {
        return !"Đã kết thúc".equals(trangThai);
    }

    public boolean isCanDelete() {
        return "Chưa bắt đầu".equals(trangThai) &&
                (soLopHocPhan == null || soLopHocPhan == 0);
    }

    public boolean isCanSetCurrent() {
        return Boolean.TRUE.equals(isActive) &&
                ("Đang diễn ra".equals(trangThai) || "Chưa bắt đầu".equals(trangThai));
    }

    public boolean isCanConvertToWeekBased() {
        return !Boolean.TRUE.equals(isWeekBasedConfig) &&
                ngayBatDau != null && ngayKetThuc != null &&
                "Chưa bắt đầu".equals(trangThai);
    }

    // ===== UTILITY METHODS =====

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    // ===== INNER DTO CLASS =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TuanHocDTO {
        private Integer soTuan;                // Số thứ tự tuần (1, 2, 3...)
        private Integer tuanTrongNam;          // Tuần trong năm (1-52)
        private LocalDate ngayBatDau;          // Thứ 2
        private LocalDate ngayKetThuc;         // Chủ nhật
        private Boolean isNghiLe;              // Có nghỉ lễ không
        private String ghiChu;                 // Ghi chú đặc biệt
        private Integer soBuoiHoc;             // Số buổi học trong tuần này
        private Boolean isHienTai;             // Có phải tuần hiện tại không

        public String getNgayDisplay() {
            if (ngayBatDau == null || ngayKetThuc == null) return "";
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM");
            return String.format("%s - %s",
                    ngayBatDau.format(formatter),
                    ngayKetThuc.format(formatter));
        }

        public String getTuanLabel() {
            return "Tuần " + soTuan;
        }

        public String getTrangThaiClass() {
            if (Boolean.TRUE.equals(isHienTai)) return "table-primary";
            if (Boolean.TRUE.equals(isNghiLe)) return "table-warning";
            return "";
        }
    }
}