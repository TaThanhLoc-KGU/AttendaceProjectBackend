package com.tathanhloc.faceattendance.DTO;

import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSessionRequestDTO {

    // Thông tin phiên điểm danh
    private String maLhp;              // Mã lớp học phần
    private String maLich;             // Mã lịch học (optional)
    private LocalDate ngayDiemDanh;    // Ngày điểm danh
    private String sessionName;       // Tên phiên (optional)
    private String giangVienId;        // Mã giảng viên thực hiện

    // Danh sách điểm danh của sinh viên
    private List<StudentAttendanceRecord> attendanceRecords;

    // Thông tin bổ sung
    private String ghiChu;             // Ghi chú chung cho phiên
    private LocalDateTime sessionStartTime;  // Thời gian bắt đầu phiên
    private LocalDateTime sessionEndTime;    // Thời gian kết thúc phiên

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentAttendanceRecord {
        private String maSv;                           // Mã sinh viên
        private TrangThaiDiemDanhEnum trangThai;      // Trạng thái điểm danh
        private LocalDateTime thoiGianVao;            // Thời gian vào (optional)
        private LocalDateTime thoiGianRa;             // Thời gian ra (optional)
        private String ghiChu;                        // Ghi chú riêng cho sinh viên
    }
}
