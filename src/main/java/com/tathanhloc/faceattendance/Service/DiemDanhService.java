package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.DiemDanhDTO;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiemDanhService extends BaseService<DiemDanh, Long, DiemDanhDTO> {

    private final DiemDanhRepository diemDanhRepository;
    private final LichHocRepository lichHocRepository;
    private final SinhVienRepository sinhVienRepository;
    private final DangKyHocRepository dangKyHocRepository;
    private final CameraRepository cameraRepository;
    private final ExcelService excelService;

    @Override
    protected JpaRepository<DiemDanh, Long> getRepository() {
        return diemDanhRepository;
    }

    @Override
    protected void setActive(DiemDanh entity, boolean active) {
        // DiemDanh không có trường isActive
    }

    @Override
    protected boolean isActive(DiemDanh entity) {
        // DiemDanh không có trường isActive, luôn trả về true
        return true;
    }

    @Transactional
    public DiemDanhDTO create(DiemDanhDTO dto) {
        log.info("Tạo điểm danh mới: {}", dto);

        // Kiểm tra sinh viên có đăng ký lớp học phần không
        LichHoc lichHoc = lichHocRepository.findById(dto.getMaLich())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", dto.getMaLich()));

        String maLhp = lichHoc.getLopHocPhan().getMaLhp();
        String maSv = dto.getMaSv();

        DangKyHocId dangKyHocId = new DangKyHocId(maSv, maLhp);
        DangKyHoc dangKyHoc = dangKyHocRepository.findById(dangKyHocId)
                .orElseThrow(() -> new RuntimeException("Sinh viên chưa đăng ký lớp học phần này"));

        if (!dangKyHoc.isActive()) {
            throw new RuntimeException("Đăng ký học phần không còn hiệu lực");
        }

        DiemDanh entity = toEntity(dto);
        entity.setId(null); // auto-generated
        return toDTO(diemDanhRepository.save(entity));
    }

    @Transactional
    public DiemDanhDTO update(Long id, DiemDanhDTO dto) {
        log.info("Cập nhật điểm danh ID {}: {}", id, dto);

        DiemDanh existing = diemDanhRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm danh", "ID", id));

        existing.setNgayDiemDanh(dto.getNgayDiemDanh());
        existing.setTrangThai(dto.getTrangThai());
        existing.setThoiGianVao(dto.getThoiGianVao());
        existing.setThoiGianRa(dto.getThoiGianRa());

        if (!existing.getLichHoc().getMaLich().equals(dto.getMaLich())) {
            existing.setLichHoc(lichHocRepository.findById(dto.getMaLich())
                    .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", dto.getMaLich())));
        }

        if (!existing.getSinhVien().getMaSv().equals(dto.getMaSv())) {
            existing.setSinhVien(sinhVienRepository.findById(dto.getMaSv())
                    .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã sinh viên", dto.getMaSv())));

            // Kiểm tra sinh viên có đăng ký lớp học phần không
            String maLhp = existing.getLichHoc().getLopHocPhan().getMaLhp();
            String maSv = dto.getMaSv();

            DangKyHocId dangKyHocId = new DangKyHocId(maSv, maLhp);
            DangKyHoc dangKyHoc = dangKyHocRepository.findById(dangKyHocId)
                    .orElseThrow(() -> new RuntimeException("Sinh viên chưa đăng ký lớp học phần này"));

            if (!dangKyHoc.isActive()) {
                throw new RuntimeException("Đăng ký học phần không còn hiệu lực");
            }
        }

        return toDTO(diemDanhRepository.save(existing));
    }

    public void delete(Long id) {
        log.info("Xóa điểm danh ID: {}", id);

        if (!diemDanhRepository.existsById(id)) {
            throw new ResourceNotFoundException("Điểm danh", "ID", id);
        }
        diemDanhRepository.deleteById(id);
    }

    // Mapping
    @Override
    protected DiemDanhDTO toDTO(DiemDanh d) {
        return DiemDanhDTO.builder()
                .id(d.getId())
                .ngayDiemDanh(d.getNgayDiemDanh())
                .trangThai(d.getTrangThai())
                .thoiGianVao(d.getThoiGianVao())
                .thoiGianRa(d.getThoiGianRa())
                .maLich(d.getLichHoc().getMaLich())
                .maSv(d.getSinhVien().getMaSv())
                .build();
    }

    @Override
    protected DiemDanh toEntity(DiemDanhDTO dto) {
        LichHoc lichHoc = lichHocRepository.findById(dto.getMaLich())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", dto.getMaLich()));

        SinhVien sinhVien = sinhVienRepository.findById(dto.getMaSv())
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã sinh viên", dto.getMaSv()));

        return DiemDanh.builder()
                .id(dto.getId())
                .ngayDiemDanh(dto.getNgayDiemDanh() != null ? dto.getNgayDiemDanh() : LocalDate.now())
                .trangThai(dto.getTrangThai() != null ? dto.getTrangThai() : TrangThaiDiemDanhEnum.CO_MAT)
                .thoiGianVao(dto.getThoiGianVao())
                .thoiGianRa(dto.getThoiGianRa())
                .lichHoc(lichHoc)
                .sinhVien(sinhVien)
                .build();
    }

    public List<DiemDanhDTO> getByMaSv(String maSv) {
        log.info("Lấy danh sách điểm danh theo mã sinh viên: {}", maSv);

        if (!sinhVienRepository.existsById(maSv)) {
            throw new ResourceNotFoundException("Sinh viên", "mã sinh viên", maSv);
        }

        return diemDanhRepository.findBySinhVienMaSv(maSv).stream()
                .map(this::toDTO).toList();
    }

    public List<DiemDanhDTO> getByMaLich(String maLich) {
        log.info("Lấy danh sách điểm danh theo mã lịch: {}", maLich);

        if (!lichHocRepository.existsById(maLich)) {
            throw new ResourceNotFoundException("Lịch học", "mã lịch", maLich);
        }

        return diemDanhRepository.findByLichHocMaLich(maLich).stream()
                .map(this::toDTO).toList();
    }

    public long countTodayDiemDanh() {
            log.info("Đếm số lượng điểm danh trong ngày");
            return diemDanhRepository.countByNgayDiemDanh(LocalDate.now());
        }



    /**
     * API chính cho camera gọi - chỉ cần studentId và cameraId
     */
    @Transactional
    public DiemDanhDTO recordAttendanceFromCamera(String maSv, Long cameraId) {
        // 1. Lấy camera và phòng học
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "id", cameraId));

        if (camera.getMaPhong() == null) {
            throw new RuntimeException("Camera chưa được gán phòng học");
        }

        // 2. Tìm lịch học hiện tại ở phòng này
        String maLich = findCurrentScheduleAtRoom(camera.getMaPhong().getMaPhong());

        // 3. Tạo DTO và gọi method create() có sẵn
        DiemDanhDTO dto = DiemDanhDTO.builder()
                .maSv(maSv)
                .maLich(maLich)
                .ngayDiemDanh(LocalDate.now())
                .thoiGianVao(LocalTime.now())
                .trangThai(TrangThaiDiemDanhEnum.CO_MAT)
                .build();

        return create(dto); // Sử dụng logic create() đã có
    }

    /**
     * Tìm lịch học đang diễn ra tại phòng
     */
    private String findCurrentScheduleAtRoom(String maPhong) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Mon, 7=Sun

        // Tìm tất cả lịch học ở phòng này hôm nay
        List<LichHoc> schedules = lichHocRepository
                .findByPhongHocMaPhongAndThuAndIsActiveTrue(maPhong, dayOfWeek);

        // Tìm lịch học đang diễn ra
        for (LichHoc lichHoc : schedules) {
            if (isTimeInSchedule(lichHoc, now)) {
                return lichHoc.getMaLich();
            }
        }

        throw new RuntimeException("Không có lịch học nào đang diễn ra tại phòng này");
    }

    /**
     * Kiểm tra thời gian hiện tại có trong khung giờ học không
     */
    private boolean isTimeInSchedule(LichHoc lichHoc, LocalTime currentTime) {
        // Giả sử: Tiết 1 = 7:00, mỗi tiết 45 phút + nghỉ 5 phút
        LocalTime startTime = LocalTime.of(7, 0).plusMinutes((lichHoc.getTietBatDau() - 1) * 50);
        LocalTime endTime = startTime.plusMinutes(lichHoc.getSoTiet() * 50);

        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

    /**
     * Thống kê điểm danh tổng quan
     */
    public Map<String, Object> getAttendanceStatistics() {
        log.info("Lấy thống kê điểm danh tổng quan");

        Map<String, Object> stats = new HashMap<>();

        try {
            // Thống kê theo trạng thái
            long totalPresent = diemDanhRepository.countByTrangThai(TrangThaiDiemDanhEnum.CO_MAT);
            long totalAbsent = diemDanhRepository.countByTrangThai(TrangThaiDiemDanhEnum.VANG_MAT);
            long totalLate = diemDanhRepository.countByTrangThai(TrangThaiDiemDanhEnum.DI_TRE);
            long totalExcused = diemDanhRepository.countByTrangThai(TrangThaiDiemDanhEnum.VANG_CO_PHEP);

            stats.put("totalPresent", totalPresent);
            stats.put("totalAbsent", totalAbsent);
            stats.put("totalLate", totalLate);
            stats.put("totalExcused", totalExcused);

            // Thống kê hôm nay
            LocalDate today = LocalDate.now();
            long todayPresent = diemDanhRepository.countByNgayDiemDanhAndTrangThai(today, TrangThaiDiemDanhEnum.CO_MAT);
            long todayAbsent = diemDanhRepository.countByNgayDiemDanhAndTrangThai(today, TrangThaiDiemDanhEnum.VANG_MAT);
            long todayLate = diemDanhRepository.countByNgayDiemDanhAndTrangThai(today, TrangThaiDiemDanhEnum.DI_TRE);
            long todayClasses = diemDanhRepository.countDistinctLichHocByNgayDiemDanh(today);

            stats.put("todayPresent", todayPresent);
            stats.put("todayAbsent", todayAbsent);
            stats.put("todayLate", todayLate);
            stats.put("todayClasses", todayClasses);

        } catch (Exception e) {
            log.error("Error getting attendance statistics", e);
            // Return default values on error
            stats.put("totalPresent", 0L);
            stats.put("totalAbsent", 0L);
            stats.put("totalLate", 0L);
            stats.put("totalExcused", 0L);
            stats.put("todayPresent", 0L);
            stats.put("todayAbsent", 0L);
            stats.put("todayLate", 0L);
            stats.put("todayClasses", 0L);
        }

        return stats;
    }

    /**
     * Thống kê điểm danh theo khoảng thời gian
     */
    public Map<String, Object> getAttendanceStatisticsByDateRange(LocalDate fromDate, LocalDate toDate) {
        log.info("Lấy thống kê điểm danh từ {} đến {}", fromDate, toDate);

        Map<String, Object> stats = new HashMap<>();

        try {
            // Thống kê theo ngày - FIXED: Handle Object[]
            List<Object[]> dailyStatsRows = diemDanhRepository.findDailyAttendanceStats(fromDate, toDate);
            List<Map<String, Object>> dailyStats = new ArrayList<>();

            for (Object[] row : dailyStatsRows) {
                Map<String, Object> dailyStat = new HashMap<>();
                dailyStat.put("date", row[0]);
                dailyStat.put("present", row[1]);
                dailyStat.put("absent", row[2]);
                dailyStat.put("late", row[3]);
                dailyStat.put("excused", row[4]);
                dailyStats.add(dailyStat);
            }

            stats.put("dailyStats", dailyStats);

            // Thống kê tổng trong khoảng thời gian
            long totalPresent = diemDanhRepository.countByNgayDiemDanhBetweenAndTrangThai(
                    fromDate, toDate, TrangThaiDiemDanhEnum.CO_MAT);
            long totalAbsent = diemDanhRepository.countByNgayDiemDanhBetweenAndTrangThai(
                    fromDate, toDate, TrangThaiDiemDanhEnum.VANG_MAT);
            long totalLate = diemDanhRepository.countByNgayDiemDanhBetweenAndTrangThai(
                    fromDate, toDate, TrangThaiDiemDanhEnum.DI_TRE);

            stats.put("totalPresent", totalPresent);
            stats.put("totalAbsent", totalAbsent);
            stats.put("totalLate", totalLate);

        } catch (Exception e) {
            log.error("Error getting attendance statistics by date range", e);
            stats.put("dailyStats", new ArrayList<>());
            stats.put("totalPresent", 0L);
            stats.put("totalAbsent", 0L);
            stats.put("totalLate", 0L);
        }

        return stats;
    }

    /**
     * Lấy lịch sử điểm danh gần nhất với thông tin chi tiết
     */
    public List<Map<String, Object>> getRecentAttendanceHistory(int limit) {
        log.info("Lấy lịch sử điểm danh gần nhất, limit: {}", limit);

        try {
            // FIXED: Handle Object[] results
            List<Object[]> results = diemDanhRepository.findRecentAttendanceHistory(limit);

            return results.stream().map(row -> {
                Map<String, Object> record = new HashMap<>();
                record.put("date", row[0]);
                record.put("subjectName", row[1]);
                record.put("subjectCode", row[2]);
                record.put("className", row[3]);
                record.put("lecturerName", row[4]);
                record.put("roomName", row[5]);
                record.put("session", row[6]);
                record.put("present", row[7]);
                record.put("absent", row[8]);
                record.put("late", row[9]);
                record.put("excused", row[10]);
                record.put("totalStudents", row[11]);
                return record;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting recent attendance history", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy báo cáo điểm danh theo bộ lọc
     */
    public List<Map<String, Object>> getFilteredAttendanceReport(
            LocalDate fromDate, LocalDate toDate,
            String subjectCode, String lecturerCode, String classCode) {
        log.info("Lấy báo cáo điểm danh theo bộ lọc: từ {} đến {}, môn {}, GV {}, lớp {}",
                fromDate, toDate, subjectCode, lecturerCode, classCode);

        try {
            // FIXED: Handle Object[] results
            List<Object[]> results = diemDanhRepository.findFilteredAttendanceReport(
                    fromDate, toDate, subjectCode, lecturerCode, classCode);

            return results.stream().map(row -> {
                Map<String, Object> record = new HashMap<>();
                record.put("date", row[0]);
                record.put("subjectName", row[1]);
                record.put("subjectCode", row[2]);
                record.put("className", row[3]);
                record.put("lecturerName", row[4]);
                record.put("roomName", row[5]);
                record.put("session", row[6]);
                record.put("timeStart", row[7]);
                record.put("timeEnd", row[8]);
                record.put("present", row[9]);
                record.put("absent", row[10]);
                record.put("late", row[11]);
                record.put("excused", row[12]);
                record.put("totalStudents", row[13]);
                return record;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting filtered attendance report", e);
            return new ArrayList<>();
        }
    }

    /**
     * Thống kê điểm danh theo học kỳ
     */
    public Map<String, Object> getAttendanceStatisticsBySemester(String semesterCode, String yearCode) {
        log.info("Lấy thống kê điểm danh theo học kỳ: {}, năm: {}", semesterCode, yearCode);

        Map<String, Object> stats = new HashMap<>();

        try {
            // FIXED: Handle Object[] results for subject stats
            List<Object[]> subjectStatsRows = diemDanhRepository.findAttendanceStatsBySubject(semesterCode, yearCode);
            List<Map<String, Object>> subjectStats = new ArrayList<>();

            for (Object[] row : subjectStatsRows) {
                Map<String, Object> subjectStat = new HashMap<>();
                subjectStat.put("subjectCode", row[0]);
                subjectStat.put("subjectName", row[1]);
                subjectStat.put("present", row[2]);
                subjectStat.put("absent", row[3]);
                subjectStat.put("late", row[4]);
                subjectStat.put("excused", row[5]);
                subjectStat.put("total", row[6]);
                subjectStats.add(subjectStat);
            }
            stats.put("subjectStats", subjectStats);

            // FIXED: Handle Object[] results for lecturer stats
            List<Object[]> lecturerStatsRows = diemDanhRepository.findAttendanceStatsByLecturer(semesterCode, yearCode);
            List<Map<String, Object>> lecturerStats = new ArrayList<>();

            for (Object[] row : lecturerStatsRows) {
                Map<String, Object> lecturerStat = new HashMap<>();
                lecturerStat.put("lecturerCode", row[0]);
                lecturerStat.put("lecturerName", row[1]);
                lecturerStat.put("present", row[2]);
                lecturerStat.put("absent", row[3]);
                lecturerStat.put("late", row[4]);
                lecturerStat.put("excused", row[5]);
                lecturerStat.put("total", row[6]);
                lecturerStats.add(lecturerStat);
            }
            stats.put("lecturerStats", lecturerStats);

            // FIXED: Handle Object[] results for class stats
            List<Object[]> classStatsRows = diemDanhRepository.findAttendanceStatsByClass(semesterCode, yearCode);
            List<Map<String, Object>> classStats = new ArrayList<>();

            for (Object[] row : classStatsRows) {
                Map<String, Object> classStat = new HashMap<>();
                classStat.put("classCode", row[0]);
                classStat.put("subjectName", row[1]);
                classStat.put("lecturerName", row[2]);
                classStat.put("present", row[3]);
                classStat.put("absent", row[4]);
                classStat.put("late", row[5]);
                classStat.put("excused", row[6]);
                classStat.put("total", row[7]);
                classStats.add(classStat);
            }
            stats.put("classStats", classStats);

        } catch (Exception e) {
            log.error("Error getting attendance statistics by semester", e);
            stats.put("subjectStats", new ArrayList<>());
            stats.put("lecturerStats", new ArrayList<>());
            stats.put("classStats", new ArrayList<>());
        }

        return stats;
    }
    /**
     * Xuất báo cáo điểm danh ra Excel
     */
    public byte[] exportAttendanceReport(
            LocalDate fromDate, LocalDate toDate,
            String subjectCode, String lecturerCode, String classCode) {
        log.info("Xuất báo cáo điểm danh ra Excel");

        List<Map<String, Object>> data = getFilteredAttendanceReport(
                fromDate, toDate, subjectCode, lecturerCode, classCode);

        return excelService.exportAttendanceReport(data, fromDate, toDate);
    }

}
