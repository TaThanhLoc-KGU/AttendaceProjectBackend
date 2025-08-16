// ===== AttendanceUtils.java =====
package com.tathanhloc.faceattendance.Util;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * Utility class cho attendance operations
 */
@UtilityClass
public class AttendanceUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Format ngày theo định dạng Việt Nam
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * Format thời gian
     */
    public static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }

    /**
     * Format ngày giờ
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    /**
     * Tính tuần trong năm theo chuẩn Việt Nam (thứ 2 là ngày đầu tuần)
     */
    public static int getWeekOfYear(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.forLanguageTag("vi-VN"));
        return date.get(weekFields.weekOfWeekBasedYear());
    }

    /**
     * Lấy ngày đầu tuần (thứ 2)
     */
    public static LocalDate getStartOfWeek(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.forLanguageTag("vi-VN"));
        return date.with(weekFields.dayOfWeek(), 1);
    }

    /**
     * Lấy ngày cuối tuần (chủ nhật)
     */
    public static LocalDate getEndOfWeek(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.forLanguageTag("vi-VN"));
        return date.with(weekFields.dayOfWeek(), 7);
    }

    /**
     * Chuyển đổi số thứ sang tên thứ
     */
    public static String getDayName(Integer dayOfWeek) {
        if (dayOfWeek == null) return "";

        return switch (dayOfWeek) {
            case 1 -> "Chủ nhật";
            case 2 -> "Thứ hai";
            case 3 -> "Thứ ba";
            case 4 -> "Thứ tư";
            case 5 -> "Thứ năm";
            case 6 -> "Thứ sáu";
            case 7 -> "Thứ bảy";
            default -> "Không xác định";
        };
    }

    /**
     * Chuyển đổi trạng thái điểm danh sang text
     */
    public static String getStatusText(TrangThaiDiemDanhEnum status) {
        if (status == null) return "Chưa điểm danh";

        return switch (status) {
            case CO_MAT -> "Có mặt";
            case VANG_MAT -> "Vắng mặt";
            case DI_TRE -> "Đi trễ";
            case VANG_CO_PHEP -> "Vắng có phép";
        };
    }

    /**
     * Lấy CSS class cho trạng thái
     */
    public static String getStatusCssClass(TrangThaiDiemDanhEnum status) {
        if (status == null) return "text-muted";

        return switch (status) {
            case CO_MAT -> "text-success";
            case VANG_MAT -> "text-danger";
            case DI_TRE -> "text-warning";
            case VANG_CO_PHEP -> "text-info";
        };
    }

    /**
     * Lấy icon cho trạng thái
     */
    public static String getStatusIcon(TrangThaiDiemDanhEnum status) {
        if (status == null) return "fa-question";

        return switch (status) {
            case CO_MAT -> "fa-check";
            case VANG_MAT -> "fa-times";
            case DI_TRE -> "fa-clock";
            case VANG_CO_PHEP -> "fa-user-clock";
        };
    }

    /**
     * Tính phần trăm điểm danh
     */
    public static double calculateAttendanceRate(long presentCount, long totalCount) {
        if (totalCount == 0) return 0.0;
        return Math.round((double) presentCount / totalCount * 10000.0) / 100.0;
    }

    /**
     * Kiểm tra có phải ngày làm việc không
     */
    public static boolean isWorkingDay(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 6; // Mon-Sat (2-7 in Vietnam format)
    }

    /**
     * Tạo mã điểm danh unique
     */
    public static String generateAttendanceCode(String scheduleId, LocalDate date, String studentId) {
        return String.format("ATT_%s_%s_%s",
                scheduleId,
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                studentId);
    }

    /**
     * Validate mã sinh viên format
     */
    public static boolean isValidStudentCode(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return false;
        }

        // Thường là số hoặc có format cụ thể (ví dụ: 2021xxxx)
        return studentCode.matches("^[0-9]{8,10}$") ||
                studentCode.matches("^[A-Z]{2}[0-9]{6,8}$");
    }

    /**
     * Tính khoảng cách thời gian
     */
    public static String getTimeDistance(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return "";

        long minutes = java.time.Duration.between(from, to).toMinutes();

        if (minutes < 60) {
            return minutes + " phút";
        } else if (minutes < 1440) { // < 24 hours
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + " giờ " + (remainingMinutes > 0 ? remainingMinutes + " phút" : "");
        } else {
            long days = minutes / 1440;
            return days + " ngày";
        }
    }

    /**
     * Tính tuổi trung bình của danh sách sinh viên
     */
    public static double calculateAverageAge(List<LocalDate> birthDates) {
        if (birthDates == null || birthDates.isEmpty()) {
            return 0.0;
        }

        LocalDate now = LocalDate.now();
        return birthDates.stream()
                .filter(birthDate -> birthDate != null)
                .mapToInt(birthDate -> now.getYear() - birthDate.getYear())
                .average()
                .orElse(0.0);
    }

    /**
     * Tạo báo cáo summary text
     */
    public static String generateSummaryText(int total, int present, int absent, int late, int excused) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Tổng: %d sinh viên", total));

        if (present > 0) {
            summary.append(String.format(" | Có mặt: %d", present));
        }
        if (absent > 0) {
            summary.append(String.format(" | Vắng: %d", absent));
        }
        if (late > 0) {
            summary.append(String.format(" | Trễ: %d", late));
        }
        if (excused > 0) {
            summary.append(String.format(" | Có phép: %d", excused));
        }

        if (total > 0) {
            double rate = calculateAttendanceRate(present, total);
            summary.append(String.format(" | Tỷ lệ: %.1f%%", rate));
        }

        return summary.toString();
    }

    /**
     * Chuyển số tiết thành thời gian học
     */
    public static String periodsToTimeRange(Integer startPeriod, Integer numberOfPeriods) {
        if (startPeriod == null || numberOfPeriods == null) {
            return "";
        }

        // Giả sử tiết 1 bắt đầu lúc 6:00, mỗi tiết 50 phút
        LocalTime startTime = LocalTime.of(6, 0).plusMinutes((startPeriod - 1) * 50);
        LocalTime endTime = startTime.plusMinutes(numberOfPeriods * 50);

        return String.format("%s - %s",
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER));
    }

    /**
     * Tạo ghi chú mặc định
     */
    public static String generateDefaultNote(TrangThaiDiemDanhEnum status, boolean isManual) {
        String method = isManual ? "thủ công" : "tự động";
        String statusText = getStatusText(status).toLowerCase();

        return String.format("Điểm danh %s - %s lúc %s",
                method, statusText, LocalDateTime.now().format(TIME_FORMATTER));
    }

    /**
     * Validate khoảng thời gian
     */
    public static boolean isValidDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return false;
        }

        return !fromDate.isAfter(toDate) &&
                !fromDate.isAfter(LocalDate.now()) &&
                !fromDate.isBefore(LocalDate.now().minusYears(1));
    }

    /**
     * Tạo export filename
     */
    public static String generateExportFilename(String prefix, LocalDate date, String extension) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        return String.format("%s_%s_%s.%s", prefix, dateStr, timestamp, extension);
    }
}