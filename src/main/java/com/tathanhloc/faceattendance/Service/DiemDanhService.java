package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
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
    private final DangKyHocService dangKyHocService;
    private final LichHocService lichHocService;
    private final SinhVienService sinhVienService;

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


    /**
     * Lấy điểm danh theo lớp học phần và ngày
     */
    public List<DiemDanhDTO> getByClassAndDate(String maLhp, LocalDate ngay) {
        List<DiemDanh> attendances = diemDanhRepository.findByLichHoc_LopHocPhan_MaLhpAndNgayDiemDanh(maLhp, ngay);
        return attendances.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Tạo điểm danh thủ công cho nhiều sinh viên
     */
    @Transactional
    public List<DiemDanhDTO> createBulkManualAttendance(String maLich, LocalDate ngayDiemDanh,
                                                        List<ManualAttendanceRequest> requests) {
        log.info("Tạo điểm danh thủ công cho lịch {} ngày {}", maLich, ngayDiemDanh);

        // Kiểm tra lịch học tồn tại
        LichHoc lichHoc = lichHocRepository.findById(maLich)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", maLich));

        List<DiemDanhDTO> results = new ArrayList<>();

        for (ManualAttendanceRequest request : requests) {
            try {
                // Kiểm tra xem đã có điểm danh chưa
                List<DiemDanh> existing = diemDanhRepository.findByLichHocMaLichAndSinhVienMaSvAndNgayDiemDanh(
                        maLich, request.getMaSv(), ngayDiemDanh);

                DiemDanh diemDanh;
                if (!existing.isEmpty()) {
                    // Cập nhật điểm danh hiện có
                    diemDanh = existing.get(0);
                    diemDanh.setTrangThai(request.getTrangThai());
                    diemDanh.setThoiGianVao(request.getThoiGianVao());
                    diemDanh.setThoiGianRa(request.getThoiGianRa());
                } else {
                    // Tạo mới điểm danh
                    DiemDanhDTO dto = DiemDanhDTO.builder()
                            .maLich(maLich)
                            .maSv(request.getMaSv())
                            .ngayDiemDanh(ngayDiemDanh)
                            .trangThai(request.getTrangThai())
                            .thoiGianVao(request.getThoiGianVao())
                            .thoiGianRa(request.getThoiGianRa())
                            .build();

                    diemDanh = toEntity(dto);
                }

                results.add(toDTO(diemDanhRepository.save(diemDanh)));

            } catch (Exception e) {
                log.error("Lỗi tạo điểm danh cho sinh viên {}: {}", request.getMaSv(), e.getMessage());
            }
        }

        return results;
    }

    /**
     * Tính tỷ lệ điểm danh theo lớp học phần
     */
    public AttendanceStatsDTO getAttendanceStatsByClass(String maLhp) {
        log.info("Tính tỷ lệ điểm danh cho lớp {}", maLhp);

        // Đếm tổng số buổi học
        long totalSessions = lichHocRepository.countByLopHocPhanMaLhp(maLhp);

        // Đếm tổng số sinh viên đăng ký
        long totalStudents = dangKyHocRepository.countByLopHocPhanMaLhp(maLhp);

        // Đếm điểm danh theo trạng thái
        long presentCount = diemDanhRepository.countByLichHoc_LopHocPhan_MaLhpAndTrangThai(
                maLhp, TrangThaiDiemDanhEnum.CO_MAT);
        long absentCount = diemDanhRepository.countByLichHoc_LopHocPhan_MaLhpAndTrangThai(
                maLhp, TrangThaiDiemDanhEnum.VANG_MAT);
        long lateCount = diemDanhRepository.countByLichHoc_LopHocPhan_MaLhpAndTrangThai(
                maLhp, TrangThaiDiemDanhEnum.DI_TRE);
        long excusedCount = diemDanhRepository.countByLichHoc_LopHocPhan_MaLhpAndTrangThai(
                maLhp, TrangThaiDiemDanhEnum.VANG_CO_PHEP);

        long totalAttendanceRecords = presentCount + absentCount + lateCount + excusedCount;

        double attendanceRate = totalAttendanceRecords > 0 ?
                (double) (presentCount + lateCount) / totalAttendanceRecords * 100 : 0;

        return AttendanceStatsDTO.builder()
                .totalSessions(totalSessions)
                .totalStudents(totalStudents)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .attendanceRate(attendanceRate)
                .build();
    }

    /**
     * Lấy tỷ lệ điểm danh của từng sinh viên trong lớp
     */
    public List<StudentAttendanceDTO> getStudentAttendanceByClass(String maLhp) {
        // Lấy danh sách sinh viên đăng ký lớp
        List<DangKyHocDTO> registrations = dangKyHocService.getByMaLhp(maLhp);

        return registrations.stream().map(reg -> {
            String maSv = reg.getMaSv();

            // Đếm điểm danh theo trạng thái cho sinh viên này
            long presentCount = diemDanhRepository.countBySinhVienMaSvAndLichHoc_LopHocPhan_MaLhpAndTrangThai(
                    maSv, maLhp, TrangThaiDiemDanhEnum.CO_MAT);
            long absentCount = diemDanhRepository.countBySinhVienMaSvAndLichHoc_LopHocPhan_MaLhpAndTrangThai(
                    maSv, maLhp, TrangThaiDiemDanhEnum.VANG_MAT);
            long lateCount = diemDanhRepository.countBySinhVienMaSvAndLichHoc_LopHocPhan_MaLhpAndTrangThai(
                    maSv, maLhp, TrangThaiDiemDanhEnum.DI_TRE);
            long excusedCount = diemDanhRepository.countBySinhVienMaSvAndLichHoc_LopHocPhan_MaLhpAndTrangThai(
                    maSv, maLhp, TrangThaiDiemDanhEnum.VANG_CO_PHEP);

            long total = presentCount + absentCount + lateCount + excusedCount;
            double attendanceRate = total > 0 ? (double) (presentCount + lateCount) / total * 100 : 0;

            return StudentAttendanceDTO.builder()
                    .maSv(maSv)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .lateCount(lateCount)
                    .excusedCount(excusedCount)
                    .attendanceRate(attendanceRate)
                    .build();
        }).collect(Collectors.toList());
    }

    public long countTodayDiemDanh() {
        return diemDanhRepository.countByNgayDiemDanh(LocalDate.now());
    }
    // Thêm vào DiemDanhService.java

    public List<DiemDanhDTO> getByMaLichAndDate(String maLich, LocalDate ngayDiemDanh) {
        List<DiemDanh> attendances = diemDanhRepository.findByLichHocMaLichAndNgayDiemDanh(maLich, ngayDiemDanh);
        return attendances.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Lấy điểm danh theo mã lịch và mã sinh viên
     */
    public List<DiemDanhDTO> getByMaLichAndMaSv(String maLich, String maSv) {
        try {
            List<DiemDanh> diemDanhList = diemDanhRepository.findByLichHocMaLichAndSinhVienMaSv(maLich, maSv);
            return diemDanhList.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting attendance by schedule and student: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy điểm danh theo sinh viên và khoảng thời gian
     */
    public List<DiemDanhDTO> getByMaSvAndDateRange(String maSv, LocalDate fromDate, LocalDate toDate) {
        try {
            List<DiemDanh> diemDanhList = diemDanhRepository.findBySinhVienMaSvAndNgayDiemDanhBetween(
                    maSv, fromDate, toDate);
            return diemDanhList.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting attendance by student and date range: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    /**
     * Đếm số lượng điểm danh theo mã lịch
     */
    public long countByMaLich(String maLich) {
        try {
            return diemDanhRepository.countByLichHocMaLich(maLich);
        } catch (Exception e) {
            log.error("Error counting attendance by schedule: {}", e.getMessage());
            return 0;
        }
    }
    // Thêm vào DiemDanhService.java

    /**
     * Lấy điểm danh theo lớp học phần và khoảng thời gian
     * @param maLhp Mã lớp học phần
     * @param fromDate Từ ngày (nullable)
     * @param toDate Đến ngày (nullable)
     * @return Danh sách điểm danh
     */
    public List<DiemDanhDTO> getByLopHocPhanAndDateRange(String maLhp, LocalDate fromDate, LocalDate toDate) {
        log.info("Getting attendance for class {} from {} to {}", maLhp, fromDate, toDate);

        try {
            // Lấy tất cả lịch học của lớp học phần này thông qua LichHocService
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);

            if (lichHocList.isEmpty()) {
                log.warn("No schedule found for class {}", maLhp);
                return new ArrayList<>();
            }

            // Lấy các mã lịch học
            List<String> maLichList = lichHocList.stream()
                    .map(LichHocDTO::getMaLich)
                    .collect(Collectors.toList());

            // Lấy điểm danh theo danh sách mã lịch và filter theo khoảng thời gian
            List<DiemDanhDTO> allAttendance = new ArrayList<>();

            for (String maLich : maLichList) {
                List<DiemDanhDTO> scheduleAttendance = getByMaLich(maLich)
                        .stream()
                        .filter(dd -> {
                            LocalDate attendanceDate = dd.getNgayDiemDanh();
                            if (fromDate != null && attendanceDate.isBefore(fromDate)) {
                                return false;
                            }
                            if (toDate != null && attendanceDate.isAfter(toDate)) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
                allAttendance.addAll(scheduleAttendance);
            }

            return allAttendance;

        } catch (Exception e) {
            log.error("Error getting attendance for class {} in date range: {}", maLhp, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy điểm danh hôm nay theo lớp học phần
     * @param maLhp Mã lớp học phần
     * @return Danh sách điểm danh hôm nay
     */
    public List<DiemDanhDTO> getTodayAttendanceByClass(String maLhp) {
        LocalDate today = LocalDate.now();
        return getByLopHocPhanAndDateRange(maLhp, today, today);
    }

    /**
     * Thống kê điểm danh theo lớp học phần
     * @param maLhp Mã lớp học phần
     * @param fromDate Từ ngày (nullable)
     * @param toDate Đến ngày (nullable)
     * @return Map chứa thống kê
     */
    public Map<String, Object> getAttendanceStatsByClass(String maLhp, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<DiemDanhDTO> attendanceList = getByLopHocPhanAndDateRange(maLhp, fromDate, toDate);

            if (attendanceList.isEmpty()) {
                // Trả về stats rỗng
                stats.put("totalRecords", 0);
                stats.put("presentCount", 0);
                stats.put("absentCount", 0);
                stats.put("lateCount", 0);
                stats.put("excusedCount", 0);
                stats.put("attendanceRate", 0.0);
                return stats;
            }

            // Đếm các loại điểm danh
            long totalRecords = attendanceList.size();
            long presentCount = attendanceList.stream().filter(dd -> TrangThaiDiemDanhEnum.CO_MAT.equals(dd.getTrangThai())).count();
            long absentCount = attendanceList.stream().filter(dd -> TrangThaiDiemDanhEnum.VANG_MAT.equals(dd.getTrangThai())).count();
            long lateCount = attendanceList.stream().filter(dd -> TrangThaiDiemDanhEnum.DI_TRE.equals(dd.getTrangThai())).count();
            long excusedCount = attendanceList.stream().filter(dd -> TrangThaiDiemDanhEnum.VANG_CO_PHEP.equals(dd.getTrangThai())).count();

            // Tính tỷ lệ
            double attendanceRate = totalRecords > 0 ? (double) presentCount / totalRecords * 100 : 0;

            stats.put("totalRecords", totalRecords);
            stats.put("presentCount", presentCount);
            stats.put("absentCount", absentCount);
            stats.put("lateCount", lateCount);
            stats.put("excusedCount", excusedCount);
            stats.put("attendanceRate", attendanceRate);

        } catch (Exception e) {
            log.error("Error getting attendance stats for class: {}", e.getMessage());
        }

        return stats;
    }

    /**
     * Lấy thống kê điểm danh theo sinh viên trong một lớp
     * @param maLhp Mã lớp học phần
     * @return Danh sách thống kê từng sinh viên
     */
    public List<Map<String, Object>> getAttendanceStatsByStudents(String maLhp) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // Lấy danh sách sinh viên trong lớp
            List<DangKyHocDTO> dangKyList = dangKyHocService.getByMaLhp(maLhp);

            for (DangKyHocDTO dangKy : dangKyList) {
                SinhVienDTO sinhVien = sinhVienService.getByMaSv(dangKy.getMaSv());
                if (sinhVien == null) continue;

                // Lấy tất cả điểm danh của sinh viên này trong lớp
                List<DiemDanhDTO> studentAttendance = getStudentAttendanceInClass(sinhVien.getMaSv(), maLhp);

                if (!studentAttendance.isEmpty()) {
                    long presentCount = studentAttendance.stream()
                            .filter(dd -> TrangThaiDiemDanhEnum.CO_MAT.equals(dd.getTrangThai()))
                            .count();

                    long absentCount = studentAttendance.stream()
                            .filter(dd -> TrangThaiDiemDanhEnum.VANG_MAT.equals(dd.getTrangThai()))
                            .count();

                    long lateCount = studentAttendance.stream()
                            .filter(dd -> TrangThaiDiemDanhEnum.DI_TRE.equals(dd.getTrangThai()))
                            .count();

                    double attendanceRate = (double) presentCount / studentAttendance.size() * 100;

                    Map<String, Object> studentStat = new HashMap<>();
                    studentStat.put("maSv", sinhVien.getMaSv());
                    studentStat.put("hoTen", sinhVien.getHoTen());
                    studentStat.put("totalSessions", studentAttendance.size());
                    studentStat.put("presentCount", presentCount);
                    studentStat.put("absentCount", absentCount);
                    studentStat.put("lateCount", lateCount);
                    studentStat.put("attendanceRate", attendanceRate);

                    result.add(studentStat);
                }
            }

            // Sắp xếp theo tỷ lệ điểm danh giảm dần
            result.sort((a, b) -> Double.compare(
                    (Double) b.get("attendanceRate"),
                    (Double) a.get("attendanceRate")
            ));

        } catch (Exception e) {
            log.error("Error getting student attendance stats: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Lấy điểm danh của sinh viên trong một lớp cụ thể
     */
    private List<DiemDanhDTO> getStudentAttendanceInClass(String maSv, String maLhp) {
        try {
            // Lấy tất cả lịch học của lớp
            List<LichHocDTO> lichHocList = lichHocService.getByLopHocPhan(maLhp);
            List<DiemDanhDTO> allAttendance = new ArrayList<>();

            for (LichHocDTO lichHoc : lichHocList) {
                List<DiemDanhDTO> sessionAttendance = getByMaLich(lichHoc.getMaLich())
                        .stream()
                        .filter(dd -> maSv.equals(dd.getMaSv()))
                        .collect(Collectors.toList());
                allAttendance.addAll(sessionAttendance);
            }

            return allAttendance;
        } catch (Exception e) {
            log.error("Error getting student attendance in class: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Tạo báo cáo xu hướng điểm danh theo thời gian
     */
    public List<Map<String, Object>> getAttendanceTrend(String maLhp, LocalDate fromDate, LocalDate toDate) {
        List<Map<String, Object>> trend = new ArrayList<>();

        try {
            // Lấy điểm danh theo từng ngày
            List<DiemDanhDTO> attendanceList = getByLopHocPhanAndDateRange(maLhp, fromDate, toDate);

            // Group by date
            Map<LocalDate, List<DiemDanhDTO>> attendanceByDate = attendanceList.stream()
                    .collect(Collectors.groupingBy(DiemDanhDTO::getNgayDiemDanh));

            // Tạo trend data
            for (Map.Entry<LocalDate, List<DiemDanhDTO>> entry : attendanceByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<DiemDanhDTO> dailyAttendance = entry.getValue();

                long presentCount = dailyAttendance.stream()
                        .filter(dd -> TrangThaiDiemDanhEnum.CO_MAT.equals(dd.getTrangThai()))
                        .count();

                long totalCount = dailyAttendance.size();
                double rate = totalCount > 0 ? (double) presentCount / totalCount * 100 : 0;

                Map<String, Object> dailyTrend = new HashMap<>();
                dailyTrend.put("date", date.toString());
                dailyTrend.put("totalStudents", totalCount);
                dailyTrend.put("presentCount", presentCount);
                dailyTrend.put("attendanceRate", rate);

                trend.add(dailyTrend);
            }

            // Sắp xếp theo ngày
            trend.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));

        } catch (Exception e) {
            log.error("Error getting attendance trend: {}", e.getMessage());
        }

        return trend;
    }

    /**
     * Lấy top học sinh có tỷ lệ điểm danh cao nhất
     */
    public List<Map<String, Object>> getTopAttendanceStudents(String maLhp, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // Sử dụng method getAttendanceStatsByStudents đã có
            result = getAttendanceStatsByStudents(maLhp);

            // Lấy top students
            return result.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting top attendance students: {}", e.getMessage());
            return result;
        }
    }
}
