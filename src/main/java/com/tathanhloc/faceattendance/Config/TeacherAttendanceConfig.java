// ===== TeacherAttendanceConfig.java =====
package com.tathanhloc.faceattendance.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

/**
 * Cấu hình cho hệ thống điểm danh giảng viên
 */
@Configuration
@ConfigurationProperties(prefix = "teacher.attendance")
@Data
public class TeacherAttendanceConfig {

    /**
     * Thời gian cho phép điểm danh trước giờ học (phút)
     */
    private Integer allowAttendanceBeforeClass = 60;

    /**
     * Thời gian cho phép điểm danh sau giờ học (phút)
     */
    private Integer allowAttendanceAfterClass = 30;

    /**
     * Thời gian tối đa cho phép điểm danh muộn (phút)
     */
    private Integer maxLateAttendanceMinutes = 15;

    /**
     * Có tự động đánh dấu vắng mặt sau khi hết thời gian không
     */
    private Boolean autoMarkAbsentAfterTime = false;

    /**
     * Thời gian bắt đầu tiết 1 (mặc định 6:00)
     */
    private LocalTime firstPeriodStartTime = LocalTime.of(6, 0);

    /**
     * Thời lượng mỗi tiết học (phút)
     */
    private Integer periodDurationMinutes = 50;

    /**
     * Thời gian nghỉ giữa các tiết (phút)
     */
    private Integer breakBetweenPeriods = 0;

    /**
     * Số tiết tối đa trong 1 ngày
     */
    private Integer maxPeriodsPerDay = 12;

    /**
     * Có cho phép điểm danh hàng loạt không
     */
    private Boolean allowBatchAttendance = true;

    /**
     * Số lượng tối đa sinh viên trong 1 batch
     */
    private Integer maxStudentsPerBatch = 100;

    /**
     * Có log tất cả thao tác điểm danh không
     */
    private Boolean logAllAttendanceActions = true;

    /**
     * Có gửi notification khi điểm danh không
     */
    private Boolean sendAttendanceNotifications = false;

    /**
     * Email template cho notification
     */
    private String notificationEmailTemplate = "attendance-notification";

    /**
     * Tính thời gian bắt đầu của tiết học
     */
    public LocalTime calculatePeriodStartTime(Integer period) {
        if (period == null || period < 1) {
            return firstPeriodStartTime;
        }

        long totalMinutes = (long) (period - 1) * (periodDurationMinutes + breakBetweenPeriods);
        return firstPeriodStartTime.plusMinutes(totalMinutes);
    }

    /**
     * Tính thời gian kết thúc của tiết học
     */
    public LocalTime calculatePeriodEndTime(Integer period) {
        LocalTime startTime = calculatePeriodStartTime(period);
        return startTime.plusMinutes(periodDurationMinutes);
    }

    /**
     * Kiểm tra có trong thời gian cho phép điểm danh không
     */
    public boolean isWithinAttendanceWindow(Integer period, LocalTime currentTime) {
        LocalTime startTime = calculatePeriodStartTime(period);
        LocalTime endTime = calculatePeriodEndTime(period);

        LocalTime allowedStartTime = startTime.minusMinutes(allowAttendanceBeforeClass);
        LocalTime allowedEndTime = endTime.plusMinutes(allowAttendanceAfterClass);

        return !currentTime.isBefore(allowedStartTime) && !currentTime.isAfter(allowedEndTime);
    }

    /**
     * Kiểm tra có bị muộn không
     */
    public boolean isLateAttendance(Integer period, LocalTime attendanceTime) {
        LocalTime startTime = calculatePeriodStartTime(period);
        LocalTime lateThreshold = startTime.plusMinutes(maxLateAttendanceMinutes);

        return attendanceTime.isAfter(startTime) && attendanceTime.isBefore(lateThreshold);
    }

    /**
     * Kiểm tra có quá muộn không (không được điểm danh)
     */
    public boolean isTooLateForAttendance(Integer period, LocalTime attendanceTime) {
        LocalTime startTime = calculatePeriodStartTime(period);
        LocalTime tooLateThreshold = startTime.plusMinutes(maxLateAttendanceMinutes);

        return attendanceTime.isAfter(tooLateThreshold);
    }
}