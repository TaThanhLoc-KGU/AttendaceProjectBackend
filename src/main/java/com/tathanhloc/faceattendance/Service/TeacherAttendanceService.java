package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Exception.BusinessException;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý điểm danh cho giảng viên theo weekbase schedule
 * Hỗ trợ cả lịch học cũ và week-based schedule
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAttendanceService {

    private final LichHocRepository lichHocRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final DiemDanhRepository diemDanhRepository;
    private final DangKyHocRepository dangKyHocRepository;
    private final SinhVienRepository sinhVienRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final GiangVienRepository giangVienRepository;
    private final HocKyService hocKyService;
    private final WeeklyScheduleService weeklyScheduleService;

    /**
     * Lấy lịch dạy của giảng viên theo ngày
     */
    public List<TeacherScheduleDTO> getTeacherScheduleByDate(String maGv, LocalDate date) {
        log.info("📅 Getting teacher schedule for {} on {}", maGv, date);

        List<TeacherScheduleDTO> schedules = new ArrayList<>();

        try {
            // 1. Lấy lịch học cũ (traditional schedule)
            List<LichHoc> traditionalSchedules = lichHocRepository.findByMaGvAndThu(maGv, date.getDayOfWeek().getValue());
            for (LichHoc lichHoc : traditionalSchedules) {
                if (!lichHoc.isActive()) continue;

                TeacherScheduleDTO schedule = convertToTeacherScheduleDTO(lichHoc, date);
                schedules.add(schedule);
            }

            // 2. Lấy week-based schedule instances
            List<ScheduleInstance> weekBasedSchedules = scheduleInstanceRepository
                    .findByDateAndLecturer(date, maGv);

            for (ScheduleInstance instance : weekBasedSchedules) {
                if (!instance.getIsActive()) continue;

                TeacherScheduleDTO schedule = convertToTeacherScheduleDTO(instance, date);
                schedules.add(schedule);
            }

            // 3. Sắp xếp theo tiết học
            schedules.sort(Comparator.comparing(TeacherScheduleDTO::getTietBatDau));

            log.info("✅ Found {} schedules for teacher {} on {}", schedules.size(), maGv, date);
            return schedules;

        } catch (Exception e) {
            log.error("❌ Error getting teacher schedule: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lấy lịch dạy: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách sinh viên cần điểm danh cho 1 tiết học
     */
    public TeacherAttendanceSessionDTO getAttendanceSession(String scheduleId, LocalDate date, boolean isWeekBased) {
        log.info("📋 Getting attendance session for schedule {} on {} (weekBased: {})", scheduleId, date, isWeekBased);

        try {
            TeacherAttendanceSessionDTO session = new TeacherAttendanceSessionDTO();

            if (isWeekBased) {
                // Week-based schedule
                ScheduleInstance instance = scheduleInstanceRepository.findById(scheduleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Schedule Instance", "id", scheduleId));

                session = buildSessionFromInstance(instance, date);
            } else {
                // Traditional schedule
                LichHoc lichHoc = lichHocRepository.findById(scheduleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", scheduleId));

                session = buildSessionFromLichHoc(lichHoc, date);
            }

            // Lấy danh sách sinh viên đã điểm danh
            List<DiemDanh> existingAttendance = diemDanhRepository.findByScheduleIdAndDate(scheduleId, date);
            Map<String, DiemDanh> attendanceMap = existingAttendance.stream()
                    .collect(Collectors.toMap(
                            dd -> dd.getSinhVien().getMaSv(),
                            dd -> dd,
                            (existing, replacement) -> existing
                    ));

            // Cập nhật trạng thái điểm danh cho từng sinh viên
            for (StudentAttendanceDTO student : session.getStudents()) {
                DiemDanh attendance = attendanceMap.get(student.getMaSv());
                if (attendance != null) {
                    student.setTrangThai(attendance.getTrangThai());
                    student.setThoiGianVao(attendance.getThoiGianVao());
                    student.setThoiGianRa(attendance.getThoiGianRa());
                    student.setGhiChu(attendance.getGhiChu());
                    student.setDaDigemDanh(true);
                } else {
                    student.setTrangThai(null);
                    student.setDaDigemDanh(false);
                }
            }

            // Tính thống kê
            session.calculateStatistics();

            log.info("✅ Built attendance session: {} students", session.getStudents().size());
            return session;

        } catch (Exception e) {
            log.error("❌ Error building attendance session: {}", e.getMessage(), e);
            throw new BusinessException("Không thể tải phiên điểm danh: " + e.getMessage());
        }
    }

    /**
     * Thực hiện điểm danh cho 1 sinh viên
     */
    @Transactional
    public AttendanceResultDTO markAttendance(TeacherAttendanceRequestDTO request) {
        log.info("✅ Marking attendance: {}", request);

        try {
            // 1. Validate request
            validateAttendanceRequest(request);

            // 2. Kiểm tra sinh viên có trong lớp không
            validateStudentInClass(request.getMaSv(), request.getScheduleId(), request.isWeekBased());

            // 3. Tìm hoặc tạo record điểm danh
            DiemDanh attendance = findOrCreateAttendanceRecord(request);

            // 4. Cập nhật thông tin điểm danh
            updateAttendanceRecord(attendance, request);

            // 5. Lưu vào database
            attendance = diemDanhRepository.save(attendance);

            log.info("✅ Attendance marked successfully for student {}", request.getMaSv());

            return AttendanceResultDTO.builder()
                    .success(true)
                    .message("Điểm danh thành công")
                    .studentId(request.getMaSv())
                    .attendanceId(attendance.getId())
                    .status(attendance.getTrangThai())
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error marking attendance: {}", e.getMessage(), e);

            return AttendanceResultDTO.builder()
                    .success(false)
                    .message("Lỗi điểm danh: " + e.getMessage())
                    .studentId(request.getMaSv())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Điểm danh hàng loạt
     */
    @Transactional
    public BatchAttendanceResultDTO markBatchAttendance(BatchAttendanceRequestDTO request) {
        log.info("📦 Processing batch attendance: {} records", request.getAttendanceList().size());

        List<AttendanceResultDTO> results = new ArrayList<>();

        for (TeacherAttendanceRequestDTO attendanceRequest : request.getAttendanceList()) {
            AttendanceResultDTO result = markAttendance(attendanceRequest);
            results.add(result);
        }

        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failCount = results.size() - successCount;

        log.info("✅ Batch attendance completed: {} success, {} failed", successCount, failCount);

        return BatchAttendanceResultDTO.builder()
                .totalRequests(results.size())
                .successCount((int) successCount)
                .failCount((int) failCount)
                .results(results)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Lấy thống kê điểm danh theo ngày cho giảng viên
     */
    public TeacherDailyStatsDTO getDailyAttendanceStats(String maGv, LocalDate date) {
        log.info("📊 Getting daily attendance stats for teacher {} on {}", maGv, date);

        try {
            // Lấy tất cả lịch dạy trong ngày
            List<TeacherScheduleDTO> schedules = getTeacherScheduleByDate(maGv, date);

            TeacherDailyStatsDTO stats = new TeacherDailyStatsDTO();
            stats.setDate(date);
            stats.setMaGv(maGv);
            stats.setTotalSessions(schedules.size());

            int totalStudents = 0;
            int presentCount = 0;
            int absentCount = 0;
            int lateCount = 0;
            int excusedCount = 0;

            for (TeacherScheduleDTO schedule : schedules) {
                // Lấy điểm danh cho từng tiết
                List<DiemDanh> sessionAttendance = diemDanhRepository.findByScheduleIdAndDate(
                        schedule.getScheduleId(), date);

                totalStudents += getStudentCountForSession(schedule.getScheduleId(), schedule.isWeekBased());

                for (DiemDanh attendance : sessionAttendance) {
                    switch (attendance.getTrangThai()) {
                        case CO_MAT -> presentCount++;
                        case VANG_MAT -> absentCount++;
                        case DI_TRE -> lateCount++;
                        case VANG_CO_PHEP -> excusedCount++;
                    }
                }
            }

            stats.setTotalStudents(totalStudents);
            stats.setPresentCount(presentCount);
            stats.setAbsentCount(absentCount);
            stats.setLateCount(lateCount);
            stats.setExcusedCount(excusedCount);

            // Tính phần trăm
            if (totalStudents > 0) {
                stats.setAttendanceRate((double) presentCount / totalStudents * 100);
            }

            log.info("✅ Daily stats calculated: {} sessions, {} students", schedules.size(), totalStudents);
            return stats;

        } catch (Exception e) {
            log.error("❌ Error calculating daily stats: {}", e.getMessage(), e);
            throw new BusinessException("Không thể tính thống kê: " + e.getMessage());
        }
    }

    /**
     * Export danh sách điểm danh theo ngày
     */
    public TeacherAttendanceReportDTO exportDailyAttendance(String maGv, LocalDate date) {
        log.info("📤 Exporting daily attendance for teacher {} on {}", maGv, date);

        try {
            List<TeacherScheduleDTO> schedules = getTeacherScheduleByDate(maGv, date);
            List<AttendanceRecordDTO> allRecords = new ArrayList<>();

            for (TeacherScheduleDTO schedule : schedules) {
                TeacherAttendanceSessionDTO session = getAttendanceSession(
                        schedule.getScheduleId(), date, schedule.isWeekBased());

                for (StudentAttendanceDTO student : session.getStudents()) {
                    AttendanceRecordDTO record = AttendanceRecordDTO.builder()
                            .date(date)
                            .maSv(student.getMaSv())
                            .hoTen(student.getHoTen())
                            .lop(student.getLop())
                            .monHoc(session.getTenMonHoc())
                            .maMonHoc(session.getMaMonHoc())
                            .tietBatDau(session.getTietBatDau())
                            .soTiet(session.getSoTiet())
                            .phongHoc(session.getPhongHoc())
                            .trangThai(student.getTrangThai())
                            .thoiGianVao(student.getThoiGianVao())
                            .thoiGianRa(student.getThoiGianRa())
                            .ghiChu(student.getGhiChu())
                            .build();
                    allRecords.add(record);
                }
            }

            // Lấy thông tin giảng viên
            GiangVien giangVien = giangVienRepository.findById(maGv)
                    .orElseThrow(() -> new ResourceNotFoundException("Giảng viên", "mã", maGv));

            TeacherAttendanceReportDTO report = TeacherAttendanceReportDTO.builder()
                    .date(date)
                    .maGv(maGv)
                    .tenGiangVien(giangVien.getHoTen())
                    .totalSessions(schedules.size())
                    .totalRecords(allRecords.size())
                    .attendanceRecords(allRecords)
                    .exportedAt(LocalDateTime.now())
                    .build();

            log.info("✅ Exported {} attendance records for {} sessions", allRecords.size(), schedules.size());
            return report;

        } catch (Exception e) {
            log.error("❌ Error exporting attendance: {}", e.getMessage(), e);
            throw new BusinessException("Không thể export điểm danh: " + e.getMessage());
        }
    }

    // ===== HELPER METHODS =====

    private TeacherScheduleDTO convertToTeacherScheduleDTO(LichHoc lichHoc, LocalDate date) {
        LopHocPhan lhp = lichHoc.getLopHocPhan();
        return TeacherScheduleDTO.builder()
                .scheduleId(lichHoc.getMaLich())
                .date(date)
                .isWeekBased(false)
                .thu(lichHoc.getThu())
                .tietBatDau(lichHoc.getTietBatDau())
                .soTiet(lichHoc.getSoTiet())
                .maLhp(lhp.getMaLhp())
                .tenMonHoc(lhp.getMonHoc().getTenMh())
                .maMonHoc(lhp.getMonHoc().getMaMh())
                .tenGiangVien(lhp.getGiangVien().getHoTen())
                .maGv(lhp.getGiangVien().getMaGv())
                .phongHoc(lichHoc.getPhongHoc().getMaPhong())
                .tenPhong(lichHoc.getPhongHoc().getTenPhong())
                .hocKy(lhp.getHocKy())
                .namHoc(lhp.getNamHoc())
                .build();
    }

    private TeacherScheduleDTO convertToTeacherScheduleDTO(ScheduleInstance instance, LocalDate date) {
        WeeklySchedule weeklySchedule = instance.getWeeklySchedule();
        LopHocPhan lhp = weeklySchedule.getLopHocPhan();

        return TeacherScheduleDTO.builder()
                .scheduleId(instance.getMaInstance())
                .date(date)
                .isWeekBased(true)
                .thu(weeklySchedule.getThu())
                .tietBatDau(weeklySchedule.getTietBatDau())
                .soTiet(weeklySchedule.getSoTiet())
                .maLhp(lhp.getMaLhp())
                .tenMonHoc(lhp.getMonHoc().getTenMh())
                .maMonHoc(lhp.getMonHoc().getMaMh())
                .tenGiangVien(lhp.getGiangVien().getHoTen())
                .maGv(lhp.getGiangVien().getMaGv())
                .phongHoc(weeklySchedule.getPhongHocMacDinh().getMaPhong())
                .tenPhong(weeklySchedule.getPhongHocMacDinh().getTenPhong())
                .hocKy(lhp.getHocKy())
                .namHoc(lhp.getNamHoc())
                .weekNumber(instance.getTuanHoc())
                .build();
    }

    private TeacherAttendanceSessionDTO buildSessionFromLichHoc(LichHoc lichHoc, LocalDate date) {
        LopHocPhan lhp = lichHoc.getLopHocPhan();

        // Lấy danh sách sinh viên đăng ký
        List<DangKyHoc> registrations = dangKyHocRepository.findByMaLhpAndActive(lhp.getMaLhp(), true);
        List<StudentAttendanceDTO> students = registrations.stream()
                .map(dk -> convertToStudentAttendanceDTO(dk.getSinhVien()))
                .collect(Collectors.toList());

        return TeacherAttendanceSessionDTO.builder()
                .scheduleId(lichHoc.getMaLich())
                .date(date)
                .isWeekBased(false)
                .maLhp(lhp.getMaLhp())
                .tenMonHoc(lhp.getMonHoc().getTenMh())
                .maMonHoc(lhp.getMonHoc().getMaMh())
                .tietBatDau(lichHoc.getTietBatDau())
                .soTiet(lichHoc.getSoTiet())
                .phongHoc(lichHoc.getPhongHoc().getMaPhong())
                .tenPhong(lichHoc.getPhongHoc().getTenPhong())
                .students(students)
                .totalStudents(students.size())
                .build();
    }

    private TeacherAttendanceSessionDTO buildSessionFromInstance(ScheduleInstance instance, LocalDate date) {
        WeeklySchedule weeklySchedule = instance.getWeeklySchedule();
        LopHocPhan lhp = weeklySchedule.getLopHocPhan();

        // Lấy danh sách sinh viên đăng ký
        List<DangKyHoc> registrations = dangKyHocRepository.findByMaLhpAndActive(lhp.getMaLhp(), true);
        List<StudentAttendanceDTO> students = registrations.stream()
                .map(dk -> convertToStudentAttendanceDTO(dk.getSinhVien()))
                .collect(Collectors.toList());

        return TeacherAttendanceSessionDTO.builder()
                .scheduleId(instance.getMaInstance())
                .date(date)
                .isWeekBased(true)
                .maLhp(lhp.getMaLhp())
                .tenMonHoc(lhp.getMonHoc().getTenMh())
                .maMonHoc(lhp.getMonHoc().getMaMh())
                .tietBatDau(weeklySchedule.getTietBatDau())
                .soTiet(weeklySchedule.getSoTiet())
                .phongHoc(weeklySchedule.getPhongHocMacDinh().getMaPhong())
                .tenPhong(weeklySchedule.getPhongHocMacDinh().getTenPhong())
                .weekNumber(instance.getTuanHoc())
                .students(students)
                .totalStudents(students.size())
                .build();
    }

    private StudentAttendanceDTO convertToStudentAttendanceDTO(SinhVien sinhVien) {
        return StudentAttendanceDTO.builder()
                .maSv(sinhVien.getMaSv())
                .hoTen(sinhVien.getHoTen())
                .lop(sinhVien.getLop() != null ? sinhVien.getLop().getMaLop() : null)
                .email(sinhVien.getEmail())
                .daDigemDanh(false)
                .build();
    }

    private void validateAttendanceRequest(TeacherAttendanceRequestDTO request) {
        if (request.getMaSv() == null || request.getMaSv().trim().isEmpty()) {
            throw new BusinessException("Mã sinh viên không được để trống");
        }

        if (request.getScheduleId() == null || request.getScheduleId().trim().isEmpty()) {
            throw new BusinessException("ID lịch học không được để trống");
        }

        if (request.getDate() == null) {
            throw new BusinessException("Ngày điểm danh không được để trống");
        }

        if (request.getTrangThai() == null) {
            throw new BusinessException("Trạng thái điểm danh không được để trống");
        }
    }

    private void validateStudentInClass(String maSv, String scheduleId, boolean isWeekBased) {
        String maLhp;

        if (isWeekBased) {
            ScheduleInstance instance = scheduleInstanceRepository.findById(scheduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule Instance", "id", scheduleId));
            maLhp = instance.getWeeklySchedule().getLopHocPhan().getMaLhp();
        } else {
            LichHoc lichHoc = lichHocRepository.findById(scheduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", scheduleId));
            maLhp = lichHoc.getLopHocPhan().getMaLhp();
        }

        DangKyHocId dangKyId = new DangKyHocId(maSv, maLhp);
        DangKyHoc dangKy = dangKyHocRepository.findById(dangKyId)
                .orElseThrow(() -> new BusinessException("Sinh viên chưa đăng ký lớp học phần này"));

        if (!dangKy.isActive()) {
            throw new BusinessException("Đăng ký học phần không còn hiệu lực");
        }
    }

    private DiemDanh findOrCreateAttendanceRecord(TeacherAttendanceRequestDTO request) {
        List<DiemDanh> existing = diemDanhRepository.findByScheduleIdAndDateAndStudent(
                request.getScheduleId(), request.getDate(), request.getMaSv());

        if (!existing.isEmpty()) {
            return existing.get(0); // Trả về record đầu tiên
        }

        // Tạo mới
        DiemDanh attendance = new DiemDanh();
        attendance.setNgayDiemDanh(request.getDate());
        attendance.setCreatedBy(request.getCreatedBy());

        // Set student
        SinhVien sinhVien = sinhVienRepository.findById(request.getMaSv())
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã", request.getMaSv()));
        attendance.setSinhVien(sinhVien);

        // Set schedule reference
        if (request.isWeekBased()) {
            ScheduleInstance instance = scheduleInstanceRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule Instance", "id", request.getScheduleId()));
            attendance.setScheduleInstance(instance);
        } else {
            LichHoc lichHoc = lichHocRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", request.getScheduleId()));
            attendance.setLichHoc(lichHoc);
        }

        return attendance;
    }

    private void updateAttendanceRecord(DiemDanh attendance, TeacherAttendanceRequestDTO request) {
        attendance.setTrangThai(request.getTrangThai());

        if (request.getThoiGianVao() != null) {
            attendance.setThoiGianVao(request.getThoiGianVao());
        } else if (attendance.getThoiGianVao() == null) {
            // Set default time if not provided and not already set
            attendance.setThoiGianVao(LocalDateTime.now());
        }

        if (request.getThoiGianRa() != null) {
            attendance.setThoiGianRa(request.getThoiGianRa());
        }

        if (request.getGhiChu() != null) {
            attendance.setGhiChu(request.getGhiChu());
        }
    }

    private int getStudentCountForSession(String scheduleId, boolean isWeekBased) {
        String maLhp;

        if (isWeekBased) {
            ScheduleInstance instance = scheduleInstanceRepository.findById(scheduleId).orElse(null);
            if (instance == null) return 0;
            maLhp = instance.getWeeklySchedule().getLopHocPhan().getMaLhp();
        } else {
            LichHoc lichHoc = lichHocRepository.findById(scheduleId).orElse(null);
            if (lichHoc == null) return 0;
            maLhp = lichHoc.getLopHocPhan().getMaLhp();
        }

        return dangKyHocRepository.countByMaLhpAndActive(maLhp, true);
    }
}