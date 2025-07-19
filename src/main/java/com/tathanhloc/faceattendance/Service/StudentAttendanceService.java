package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Repository.DiemDanhRepository;
import com.tathanhloc.faceattendance.Repository.DangKyHocRepository;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAttendanceService {

    private final DiemDanhRepository diemDanhRepository;
    private final DangKyHocRepository dangKyHocRepository;

    /**
     * Lấy thống kê điểm danh tổng quan của sinh viên
     */
    public AttendanceStatisticsDTO getAttendanceStatistics(String maSv) {
        log.info("Lấy thống kê điểm danh cho sinh viên: {}", maSv);

        try {
            // Lấy số lần có mặt và vắng mặt
            Long presentCount = diemDanhRepository.countPresentByStudent(maSv);
            Long absentCount = diemDanhRepository.countAbsentByStudent(maSv);

            // Handle null values
            presentCount = presentCount != null ? presentCount : 0L;
            absentCount = absentCount != null ? absentCount : 0L;

            // Tổng số buổi đã điểm danh
            Long totalAttendanceRecords = presentCount + absentCount;

            // Tính tỷ lệ điểm danh
            Double attendanceRate = totalAttendanceRecords > 0 ?
                    (presentCount.doubleValue() / totalAttendanceRecords.doubleValue()) * 100 : 0.0;

            // Lấy thống kê theo môn học
            List<SubjectAttendanceDTO> subjectStats = getSubjectAttendanceStats(maSv);

            // Tạo summary
            AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                    .totalClasses(totalAttendanceRecords)
                    .present(presentCount)
                    .absent(absentCount)
                    .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                    .build();

            // Tạo chart data
            AttendanceChartDataDTO chartData = AttendanceChartDataDTO.builder()
                    .present(presentCount)
                    .absent(absentCount)
                    .build();

            return AttendanceStatisticsDTO.builder()
                    .summary(summary)
                    .bySubjects(subjectStats)
                    .chartData(chartData)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê điểm danh cho sinh viên {}: {}", maSv, e.getMessage());

            // Return empty statistics on error instead of throwing exception
            return createEmptyStatistics();
        }
    }

    /**
     * Tạo thống kê rỗng khi có lỗi
     */
    private AttendanceStatisticsDTO createEmptyStatistics() {
        AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                .totalClasses(0L)
                .present(0L)
                .absent(0L)
                .attendanceRate(0.0)
                .build();

        AttendanceChartDataDTO chartData = AttendanceChartDataDTO.builder()
                .present(0L)
                .absent(0L)
                .build();

        return AttendanceStatisticsDTO.builder()
                .summary(summary)
                .bySubjects(List.of())
                .chartData(chartData)
                .build();
    }

    /**
     * Lấy thống kê theo môn học
     */
    private List<SubjectAttendanceDTO> getSubjectAttendanceStats(String maSv) {
        try {
            List<Object[]> rawData = diemDanhRepository.getAttendanceBySubjectAndStudentRaw(maSv);

            return rawData.stream().map(row -> {
                        try {
                            String maMh = (String) row[0];
                            String tenMh = (String) row[1];
                            String maLhp = (String) row[2];
                            Integer nhom = (Integer) row[3];
                            Long totalClasses = ((Number) row[4]).longValue();
                            Long present = ((Number) row[5]).longValue();
                            Long absent = ((Number) row[6]).longValue();
                            Double rate = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;

                            return SubjectAttendanceDTO.builder()
                                    .maMh(maMh)
                                    .tenMh(tenMh)
                                    .maLhp(maLhp)
                                    .nhom(nhom)
                                    .totalClasses(totalClasses)
                                    .present(present)
                                    .absent(absent)
                                    .attendanceRate(Math.round(rate * 100.0) / 100.0)
                                    .build();
                        } catch (Exception e) {
                            log.warn("Error processing subject stats row: {}", e.getMessage());
                            return null;
                        }
                    }).filter(item -> item != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting subject attendance stats: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy lịch sử điểm danh của sinh viên
     */
    public List<AttendanceHistoryDTO> getAttendanceHistory(String maSv, Integer limit, String subjectFilter) {
        log.info("Lấy lịch sử điểm danh cho sinh viên: {}, limit: {}, subject: {}", maSv, limit, subjectFilter);

        try {
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 50);
            List<Object[]> rawData;

            if (subjectFilter != null && !subjectFilter.trim().isEmpty()) {
                rawData = diemDanhRepository.getAttendanceHistoryByStudentAndSubjectRaw(maSv, subjectFilter, pageable);
            } else {
                rawData = diemDanhRepository.getAttendanceHistoryByStudentRaw(maSv, pageable);
            }

            return rawData.stream().map(row -> {
                        try {
                            Long id = ((Number) row[0]).longValue();
                            LocalDate ngayDiemDanh = (LocalDate) row[1];
                            String maMh = (String) row[2];
                            String tenMh = (String) row[3];
                            String maLhp = (String) row[4];
                            Integer nhom = (Integer) row[5];
                            Integer tietBatDau = row[6] != null ? ((Number) row[6]).intValue() : 1;
                            Integer soTiet = row[7] != null ? ((Number) row[7]).intValue() : 1;

                            // Handle TrangThaiDiemDanhEnum properly
                            String trangThai = "Vắng mặt";
                            if (row[8] != null) {
                                if (row[8] instanceof TrangThaiDiemDanhEnum) {
                                    TrangThaiDiemDanhEnum enumValue = (TrangThaiDiemDanhEnum) row[8];
                                    trangThai = enumValue == TrangThaiDiemDanhEnum.CO_MAT ? "Có mặt" : "Vắng mặt";
                                } else {
                                    trangThai = row[8].toString();
                                }
                            }

                            LocalTime thoiGianVao = (LocalTime) row[9];
                            String ghiChu = "Có mặt".equals(trangThai) ? "Điểm danh tự động" : "Vắng mặt";

                            return AttendanceHistoryDTO.builder()
                                    .id(id)
                                    .ngayHoc(ngayDiemDanh)
                                    .maMh(maMh)
                                    .tenMh(tenMh)
                                    .maLhp(maLhp)
                                    .nhom(nhom)
                                    .tietBatDau(tietBatDau)
                                    .soTiet(soTiet)
                                    .trangThai(trangThai)
                                    .thoiGianVao(thoiGianVao)
                                    .ghiChu(ghiChu)
                                    .build();
                        } catch (Exception e) {
                            log.warn("Error processing attendance history row: {}", e.getMessage());
                            return null;
                        }
                    }).filter(item -> item != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử điểm danh cho sinh viên {}: {}", maSv, e.getMessage());
            return List.of(); // Return empty list instead of throwing exception
        }
    }

    /**
     * Lấy danh sách môn học mà sinh viên đã đăng ký (cho filter)
     */
    public List<SubjectAttendanceDTO> getStudentSubjects(String maSv) {
        return getSubjectAttendanceStats(maSv);
    }
}