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
import org.springframework.security.access.AccessDeniedException;
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
    private final DiemDanhService diemDanhService;

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

            // 3. Sắp xếp theo thời gian
            schedules.sort(Comparator.comparing(TeacherScheduleDTO::getTietBatDau));

            log.info("✅ Found {} schedules for lecturer {} on {}", schedules.size(), maGv, date);
            return schedules;

        } catch (Exception e) {
            log.error("❌ Error getting teacher schedule: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lấy lịch dạy: " + e.getMessage());
        }
    }

    /**
     * Sửa method này để hoạt động đúng
     */
    public AttendanceSessionDTO getAttendanceSession(String scheduleId, String maGv) {
        log.info("📋 Service: Getting attendance session for scheduleId: {}, maGv: {}", scheduleId, maGv);

        try {
            // 1. Verify permission first
            boolean hasPermission = verifySessionPermission(scheduleId, maGv);
            if (!hasPermission) {
                throw new AccessDeniedException("Không có quyền truy cập phiên điểm danh này");
            }

            // 2. Get session info
            AttendanceSessionDTO sessionInfo = getSessionInfo(scheduleId);
            log.debug("Session info: {}", sessionInfo);

            // 3. Get students list
            List<AttendanceSessionDTO.StudentAttendanceDTO> students = getStudentsForAttendanceSession(scheduleId);
            log.debug("Found {} students", students.size());

            // 4. Get existing attendance
            Map<String, DiemDanh> existingAttendance = getExistingAttendance(scheduleId, sessionInfo.getSessionDate());
            log.debug("Found {} existing attendance records", existingAttendance.size());

            // 5. Merge data
            List<AttendanceSessionDTO.StudentAttendanceDTO> mergedStudents = students.stream()
                    .map(student -> {
                        DiemDanh attendance = existingAttendance.get(student.getMaSv());
                        if (attendance != null) {
                            return AttendanceSessionDTO.StudentAttendanceDTO.builder()
                                    .maSv(student.getMaSv())
                                    .hoTen(student.getHoTen())
                                    .email(student.getEmail())
                                    .trangThai(attendance.getTrangThai().name())
                                    .ghiChu(attendance.getGhiChu())
                                    .build();
                        }
                        return student; // Return original if no attendance found
                    }).collect(Collectors.toList());

            // 6. Calculate summary
            AttendanceSessionDTO.AttendanceSummaryDTO summary = calculateSummary(mergedStudents);

            // 7. Build final result
            AttendanceSessionDTO result = AttendanceSessionDTO.builder()
                    .instanceId(sessionInfo.getInstanceId())
                    .maLhp(sessionInfo.getMaLhp())
                    .sessionDate(sessionInfo.getSessionDate())
                    .week(sessionInfo.getWeek())
                    .phongHoc(sessionInfo.getPhongHoc())
                    .tietBatDau(sessionInfo.getTietBatDau())
                    .tietKetThuc(sessionInfo.getTietKetThuc())
                    .trangThai(sessionInfo.getTrangThai())
                    .students(mergedStudents)
                    .summary(summary)
                    .build();

            log.info("✅ Service: Successfully built attendance session with {} students", mergedStudents.size());
            return result;

        } catch (Exception e) {
            log.error("❌ Service error in getAttendanceSession: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lấy phiên điểm danh: " + e.getMessage());
        }
    }

    private List<AttendanceSessionDTO.StudentAttendanceDTO> getStudentsForAttendanceSession(String scheduleId) {
        try {
            String maLhp = getMaLhpFromSchedule(scheduleId);
            log.debug("Getting students for maLhp: {}", maLhp);

            // Get enrolled students
            List<DangKyHoc> registrations;
            try {
                registrations = dangKyHocRepository.findByLopHocPhanMaLhp(maLhp);
            } catch (Exception e) {
                log.warn("findByLopHocPhanMaLhp failed, trying alternative method: {}", e.getMessage());
                registrations = dangKyHocRepository.findAll().stream()
                        .filter(dk -> dk.getLopHocPhan() != null && maLhp.equals(dk.getLopHocPhan().getMaLhp()))
                        .collect(Collectors.toList());
            }

            // Filter active registrations
                List<DangKyHoc> activeRegistrations = registrations.stream()
                    .filter(DangKyHoc::isActive)
                    .collect(Collectors.toList());

            log.debug("Found {} active student registrations", activeRegistrations.size());

            return activeRegistrations.stream()
                    .map(dk -> AttendanceSessionDTO.StudentAttendanceDTO.builder()
                            .maSv(dk.getSinhVien().getMaSv())
                            .hoTen(dk.getSinhVien().getHoTen())
                            .email(dk.getSinhVien().getEmail())
                            .trangThai("CHUA_DIEM_DANH")
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting students for attendance session: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Điểm danh cho sinh viên
     */
    @Transactional
    public AttendanceResultDTO markAttendance(TeacherAttendanceRequestDTO request) {
        log.info("✅ Marking attendance for student: {} in schedule: {}",
                request.getMaSv(), request.getScheduleId());

        try {
            // Validate request
            validateAttendanceRequest(request);

            // Lấy sinh viên
            SinhVien sinhVien = sinhVienRepository.findById(request.getMaSv())
                    .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã", request.getMaSv()));

            // Tạo hoặc cập nhật điểm danh
            DiemDanh diemDanh = createOrUpdateAttendance(sinhVien, request);

            // Lưu điểm danh
            diemDanh = diemDanhRepository.save(diemDanh);

            return AttendanceResultDTO.builder()
                    .success(true)
                    .studentId(sinhVien.getMaSv())
                    .status(diemDanh.getTrangThai())
                    .timestamp(diemDanh.getThoiGianVao())
                    .message("Điểm danh thành công")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error marking attendance: {}", e.getMessage(), e);
            throw new BusinessException("Không thể điểm danh: " + e.getMessage());
        }
    }

    @Transactional
    public void saveAttendanceSession(SaveAttendanceSessionDTO saveRequest, String maGv) {
        log.info("💾 Service: Saving attendance session: {} for lecturer: {}",
                saveRequest.getInstanceId(), maGv);

        try {
            String instanceId = saveRequest.getInstanceId();

            // Verify permission
            if (!verifySessionPermission(instanceId, maGv)) {
                throw new AccessDeniedException("Không có quyền điểm danh cho phiên này");
            }

            // Get schedule instance để verify tồn tại
            Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(instanceId);
            if (instanceOpt.isEmpty()) {
                throw new ResourceNotFoundException("Không tìm thấy phiên học: " + instanceId);
            }

            ScheduleInstance instance = instanceOpt.get();

            // Xóa điểm danh cũ nếu có
            log.debug("🗑️ Deleting existing attendance for instance: {}", instanceId);
            try {
                List<DiemDanh> existingAttendance = diemDanhRepository
                        .findByScheduleInstanceMaInstanceAndNgayDiemDanh(instanceId, saveRequest.getSessionDate());
                if (!existingAttendance.isEmpty()) {
                    diemDanhRepository.deleteAll(existingAttendance);
                    log.debug("🗑️ Deleted {} existing attendance records", existingAttendance.size());
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete existing attendance: {}", e.getMessage());
            }

            // Tạo điểm danh mới
            List<DiemDanh> newAttendanceRecords = new ArrayList<>();

            for (SaveAttendanceSessionDTO.AttendanceRecordDTO att : saveRequest.getAttendances()) {
                try {
                    // Lấy sinh viên
                    SinhVien sinhVien = sinhVienRepository.findById(att.getStudentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã", att.getStudentId()));

                    // Tạo điểm danh
                    DiemDanh diemDanh = DiemDanh.builder()
                            .sinhVien(sinhVien)
                            .scheduleInstance(instance)
                            .ngayDiemDanh(saveRequest.getSessionDate())
                            .trangThai(TrangThaiDiemDanhEnum.valueOf(att.getStatus()))
                            .ghiChu(att.getNote())
                            .createdBy(maGv)
                            .build();

                    // Set thời gian nếu có
                    if (att.getTimestamp() != null && !att.getTimestamp().isEmpty()) {
                        try {
                            LocalDateTime timestamp = LocalDateTime.parse(att.getTimestamp().replace("Z", ""));
                            diemDanh.setThoiGianVao(timestamp);
                        } catch (Exception e) {
                            log.warn("⚠️ Failed to parse timestamp {}: {}", att.getTimestamp(), e.getMessage());
                            diemDanh.setThoiGianVao(LocalDateTime.now());
                        }
                    } else {
                        diemDanh.setThoiGianVao(LocalDateTime.now());
                    }

                    newAttendanceRecords.add(diemDanh);

                } catch (Exception e) {
                    log.error("❌ Failed to create attendance for student {}: {}",
                            att.getStudentId(), e.getMessage());
                    // Continue with other students
                }
            }

            // Lưu tất cả
            if (!newAttendanceRecords.isEmpty()) {
                diemDanhRepository.saveAll(newAttendanceRecords);
                log.info("✅ Saved {} attendance records for session: {}",
                        newAttendanceRecords.size(), instanceId);
            }

            // Cập nhật trạng thái schedule instance
            instance.setTrangThai(ScheduleInstance.TrangThaiInstance.COMPLETED);
            scheduleInstanceRepository.save(instance);

            log.info("✅ Service: Successfully saved attendance session");

        } catch (Exception e) {
            log.error("❌ Service error saving attendance session: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lưu phiên điểm danh: " + e.getMessage());
        }
    }

    /**
     * Lấy tóm tắt điểm danh theo tuần
     */
    public List<WeeklyAttendanceSummaryDTO> getWeeklyAttendanceSummary(String maLhp) {
        log.info("📊 Getting weekly attendance summary for class: {}", maLhp);

        try {
            List<WeeklyAttendanceSummaryDTO> weeklySummary = new ArrayList<>();

            // Lấy tất cả điểm danh của lớp học phần
            List<DiemDanh> allAttendance = diemDanhRepository.findByLopHocPhanAllTypes(maLhp);

            // Group by week
            Map<Integer, List<DiemDanh>> attendanceByWeek = allAttendance.stream()
                    .collect(Collectors.groupingBy(this::getWeekNumber));

            // Tạo summary cho mỗi tuần
            for (Map.Entry<Integer, List<DiemDanh>> entry : attendanceByWeek.entrySet()) {
                Integer week = entry.getKey();
                List<DiemDanh> weekAttendance = entry.getValue();

                WeeklyAttendanceSummaryDTO summary = calculateWeeklySummary(week, weekAttendance);
                weeklySummary.add(summary);
            }

            weeklySummary.sort(Comparator.comparing(WeeklyAttendanceSummaryDTO::getWeek));
            return weeklySummary;

        } catch (Exception e) {
            log.error("❌ Error getting weekly summary: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lấy tóm tắt theo tuần: " + e.getMessage());
        }
    }

    /**
     * Export báo cáo điểm danh
     */
    public TeacherAttendanceReportDTO exportAttendanceReport(String maGv, LocalDate date) {
        log.info("📤 Exporting attendance report for lecturer: {} on date: {}", maGv, date);

        try {
            List<TeacherScheduleDTO> schedules = getTeacherScheduleByDate(maGv, date);
            List<AttendanceRecordDTO> allRecords = new ArrayList<>();

            for (TeacherScheduleDTO schedule : schedules) {
                List<DiemDanh> attendanceRecords = diemDanhRepository
                        .findByScheduleIdAndDate(schedule.getScheduleId(), date);

                for (DiemDanh attendance : attendanceRecords) {
                    AttendanceRecordDTO record = AttendanceRecordDTO.builder()
                            .date(attendance.getNgayDiemDanh())
                            .maSv(attendance.getSinhVien().getMaSv())
                            .hoTen(attendance.getSinhVien().getHoTen())
                            .lop(attendance.getSinhVien().getLop() != null ?
                                    attendance.getSinhVien().getLop().getMaLop() : "")
                            .monHoc(schedule.getTenMonHoc())
                            .maMonHoc(schedule.getMaMonHoc())
                            .tietBatDau(schedule.getTietBatDau())
                            .soTiet(schedule.getSoTiet())
                            .phongHoc(schedule.getTenPhong())
                            .trangThai(attendance.getTrangThai())
                            .thoiGianVao(attendance.getThoiGianVao())
                            .thoiGianRa(attendance.getThoiGianRa())
                            .ghiChu(attendance.getGhiChu())
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

            log.info("✅ Exported {} attendance records for {} sessions",
                    allRecords.size(), schedules.size());
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
                .tenPhong(lichHoc.getPhongHoc() != null ? lichHoc.getPhongHoc().getTenPhong() : "")
                .phongHoc(lichHoc.getPhongHoc() != null ? lichHoc.getPhongHoc().getMaPhong() : "")
                .hocKy(lhp.getHocKy())
                .namHoc(lhp.getNamHoc())
                .build();
    }

    private TeacherScheduleDTO convertToTeacherScheduleDTO(ScheduleInstance instance, LocalDate date) {
        WeeklySchedule template = instance.getWeeklySchedule();
        LopHocPhan lhp = template.getLopHocPhan();

        // Lấy phòng học thực tế
        PhongHoc phongThucTe = instance.getPhongHocThucTe();
        String tenPhong = phongThucTe != null ? phongThucTe.getTenPhong() : "";
        String maPhong = phongThucTe != null ? phongThucTe.getMaPhong() : "";

        return TeacherScheduleDTO.builder()
                .scheduleId(instance.getMaInstance())
                .date(date)
                .isWeekBased(true)
                .weekNumber(instance.getTuanHoc())
                .thu(template.getThu())
                .tietBatDau(instance.getTietBatDauThucTe())
                .soTiet(instance.getSoTietThucTe())
                .maLhp(lhp.getMaLhp())
                .tenMonHoc(lhp.getMonHoc().getTenMh())
                .maMonHoc(lhp.getMonHoc().getMaMh())
                .tenGiangVien(lhp.getGiangVien().getHoTen())
                .maGv(lhp.getGiangVien().getMaGv())
                .tenPhong(tenPhong)
                .phongHoc(maPhong)
                .hocKy(lhp.getHocKy())
                .namHoc(lhp.getNamHoc())
                .build();
    }

    private AttendanceSessionDTO getSessionInfo(String scheduleId) {
        log.debug("Getting session info for scheduleId: {}", scheduleId);

        // Try ScheduleInstance first (week-based)
        try {
            Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(scheduleId);
            if (instanceOpt.isPresent()) {
                ScheduleInstance instance = instanceOpt.get();
                WeeklySchedule template = instance.getWeeklySchedule();
                LopHocPhan lhp = template.getLopHocPhan();

                PhongHoc phongThucTe = instance.getPhongHocThucTe();
                String tenPhong = phongThucTe != null ? phongThucTe.getTenPhong() : "";

                return AttendanceSessionDTO.builder()
                        .instanceId(scheduleId)
                        .maLhp(lhp.getMaLhp())
                        .sessionDate(instance.getNgayCuThe())
                        .week(instance.getTuanHoc())
                        .phongHoc(tenPhong)
                        .tietBatDau(instance.getTietBatDauThucTe())
                        .tietKetThuc(instance.getTietKetThucThucTe())
                        .trangThai(instance.getTrangThai().name())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to get ScheduleInstance: {}", e.getMessage());
        }

        // Try LichHoc (legacy)
        try {
            Optional<LichHoc> lichHocOpt = lichHocRepository.findById(scheduleId);
            if (lichHocOpt.isPresent()) {
                LichHoc lichHoc = lichHocOpt.get();
                LopHocPhan lhp = lichHoc.getLopHocPhan();

                return AttendanceSessionDTO.builder()
                        .instanceId(scheduleId)
                        .maLhp(lhp.getMaLhp())
                        .sessionDate(LocalDate.now()) // Current date for legacy
                        .phongHoc(lichHoc.getPhongHoc() != null ? lichHoc.getPhongHoc().getTenPhong() : "")
                        .tietBatDau(lichHoc.getTietBatDau())
                        .tietKetThuc(lichHoc.getTietBatDau() + lichHoc.getSoTiet() - 1)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to get LichHoc: {}", e.getMessage());
        }

        throw new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId);
    }

    private List<AttendanceSessionDTO.StudentAttendanceDTO> getStudentsForSession(String scheduleId){
        String maLhp = getMaLhpFromSchedule(scheduleId);

        // Lấy danh sách sinh viên đã đăng ký
        List<DangKyHoc> registrations = dangKyHocRepository.findByLopHocPhanMaLhp(maLhp)
                .stream()
                .filter(dk -> Boolean.TRUE.equals(dk.isActive()))
                .collect(Collectors.toList());

        return registrations.stream()
                .map(dk -> AttendanceSessionDTO.StudentAttendanceDTO.builder()
                        .maSv(dk.getSinhVien().getMaSv())
                        .hoTen(dk.getSinhVien().getHoTen())
                        .email(dk.getSinhVien().getEmail())
                        .trangThai("CHUA_DIEM_DANH")
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, DiemDanh> getExistingAttendance(String scheduleId, LocalDate date) {
        try {
            log.debug("Getting existing attendance for scheduleId: {} on date: {}", scheduleId, date);

            List<DiemDanh> attendanceList;

            // Try specific repository methods first
            try {
                attendanceList = diemDanhRepository.findByScheduleIdAndDate(scheduleId, date);
            } catch (Exception e) {
                log.warn("findByScheduleIdAndDate failed, trying alternative methods: {}", e.getMessage());

                // Fallback: try ScheduleInstance specific
                try {
                    attendanceList = diemDanhRepository.findByScheduleInstanceMaInstanceAndNgayDiemDanh(scheduleId, date);
                } catch (Exception e2) {
                    log.warn("ScheduleInstance method failed, returning empty list: {}", e2.getMessage());
                    attendanceList = new ArrayList<>();
                }
            }

            log.debug("Found {} existing attendance records", attendanceList.size());

            return attendanceList.stream()
                    .collect(Collectors.toMap(
                            dd -> dd.getSinhVien().getMaSv(),
                            dd -> dd,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Error getting existing attendance: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private AttendanceSessionDTO.AttendanceSummaryDTO calculateSummary(List<AttendanceSessionDTO.StudentAttendanceDTO> students) {
        int presentCount = (int) students.stream()
                .filter(s -> "CO_MAT".equals(s.getTrangThai()) || "PRESENT".equals(s.getTrangThai()))
                .count();
        int absentCount = (int) students.stream()
                .filter(s -> "VANG_MAT".equals(s.getTrangThai()) || "ABSENT".equals(s.getTrangThai()))
                .count();
        int lateCount = (int) students.stream()
                .filter(s -> "DI_TRE".equals(s.getTrangThai()) || "LATE".equals(s.getTrangThai()))
                .count();

        double attendanceRate = students.size() > 0 ?
                (double) (presentCount + lateCount) / students.size() * 100 : 0;

        return AttendanceSessionDTO.AttendanceSummaryDTO.builder()
                .totalStudents(students.size())
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .attendanceRate(attendanceRate)
                .build();
    }

    private void validateAttendanceRequest(TeacherAttendanceRequestDTO request) {
        if (request.getMaSv() == null || request.getMaSv().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống");
        }
        if (request.getScheduleId() == null || request.getScheduleId().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã lịch học không được để trống");
        }
        if (request.getTrangThai() == null) {
            throw new IllegalArgumentException("Trạng thái điểm danh không được để trống");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Ngày điểm danh không được để trống");
        }
    }

    private DiemDanh createOrUpdateAttendance(SinhVien sinhVien, TeacherAttendanceRequestDTO request) {
        // Tìm điểm danh đã có
        List<DiemDanh> existingAttendance = diemDanhRepository.findByScheduleIdAndDateAndStudent(
                request.getScheduleId(), request.getDate(), sinhVien.getMaSv());

        DiemDanh diemDanh;
        if (!existingAttendance.isEmpty()) {
            // Cập nhật điểm danh đã có
            diemDanh = existingAttendance.get(0);
            diemDanh.setTrangThai(request.getTrangThai());
            diemDanh.setGhiChu(request.getGhiChu());
        } else {
            // Tạo điểm danh mới
            diemDanh = DiemDanh.builder()
                    .sinhVien(sinhVien)
                    .ngayDiemDanh(request.getDate())
                    .trangThai(request.getTrangThai())
                    .ghiChu(request.getGhiChu())
                    .createdBy(request.getCreatedBy())
                    .build();

            // Set schedule reference
            setScheduleReference(diemDanh, request.getScheduleId());
        }

        // Set thời gian vào/ra nếu có
        if (request.getThoiGianVao() != null) {
            diemDanh.setThoiGianVao(request.getThoiGianVao());
        }
        if (request.getThoiGianRa() != null) {
            diemDanh.setThoiGianRa(request.getThoiGianRa());
        }

        return diemDanh;
    }

    private void setScheduleReference(DiemDanh diemDanh, String scheduleId) {
        // Kiểm tra xem là ScheduleInstance hay LichHoc
        Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(scheduleId);
        if (instanceOpt.isPresent()) {
            diemDanh.setScheduleInstance(instanceOpt.get());
        } else {
            Optional<LichHoc> lichHocOpt = lichHocRepository.findById(scheduleId);
            if (lichHocOpt.isPresent()) {
                diemDanh.setLichHoc(lichHocOpt.get());
            } else {
                throw new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId);
            }
        }
    }

    private DiemDanh createAttendanceRecord(SaveAttendanceSessionDTO.AttendanceRecordDTO att,
                                            String scheduleId, SaveAttendanceSessionDTO saveRequest) {
        // Lấy sinh viên
        SinhVien sinhVien = sinhVienRepository.findById(att.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã", att.getStudentId()));

        DiemDanh diemDanh = DiemDanh.builder()
                .sinhVien(sinhVien)
                .ngayDiemDanh(saveRequest.getSessionDate())
                .trangThai(TrangThaiDiemDanhEnum.valueOf(att.getStatus()))
                .ghiChu(att.getNote())
                .createdBy("SYSTEM") // Default value since SaveAttendanceSessionDTO doesn't have lecturerId
                .build();

        // Set schedule reference
        setScheduleReference(diemDanh, scheduleId);

        return diemDanh;
    }

    private void updateScheduleInstanceStatus(String scheduleId) {
        Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(scheduleId);
        if (instanceOpt.isPresent()) {
            ScheduleInstance instance = instanceOpt.get();
            instance.setTrangThai(ScheduleInstance.TrangThaiInstance.COMPLETED);
            scheduleInstanceRepository.save(instance);
        }
    }

    public boolean verifySessionPermission(String scheduleId, String maGv) {
        // Kiểm tra ScheduleInstance
        Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(scheduleId);
        if (instanceOpt.isPresent()) {
            ScheduleInstance instance = instanceOpt.get();
            GiangVien giangVienThucTe = instance.getGiangVienThucTe();
            return giangVienThucTe != null && maGv.equals(giangVienThucTe.getMaGv());
        }

        // Kiểm tra LichHoc
        Optional<LichHoc> lichHocOpt = lichHocRepository.findById(scheduleId);
        if (lichHocOpt.isPresent()) {
            LichHoc lichHoc = lichHocOpt.get();
            return maGv.equals(lichHoc.getLopHocPhan().getGiangVien().getMaGv());
        }

        return false;
    }

    private String getMaLhpFromSchedule(String scheduleId) {
        // Thử ScheduleInstance trước
        Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(scheduleId);
        if (instanceOpt.isPresent()) {
            return instanceOpt.get().getWeeklySchedule().getLopHocPhan().getMaLhp();
        }

        // Thử LichHoc
        Optional<LichHoc> lichHocOpt = lichHocRepository.findById(scheduleId);
        if (lichHocOpt.isPresent()) {
            return lichHocOpt.get().getLopHocPhan().getMaLhp();
        }

        throw new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId);
    }

    private Integer getWeekNumber(DiemDanh diemDanh) {
        if (diemDanh.getScheduleInstance() != null) {
            return diemDanh.getScheduleInstance().getTuanHoc();
        }
        // Với lịch học cũ, tính tuần dựa trên ngày
        return calculateWeekFromDate(diemDanh.getNgayDiemDanh());
    }

    private Integer calculateWeekFromDate(LocalDate date) {
        // Logic tính tuần học dựa trên ngày (có thể cần điều chỉnh theo quy định trường)
        // Ví dụ đơn giản: tuần đầu tiên của năm học
        LocalDate startOfSemester = LocalDate.of(date.getYear(), 9, 1); // 1/9
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startOfSemester, date);
        return (int) (daysBetween / 7) + 1;
    }

    private WeeklyAttendanceSummaryDTO calculateWeeklySummary(Integer week, List<DiemDanh> weekAttendance) {
        int totalSessions = weekAttendance.size();
        int presentCount = (int) weekAttendance.stream()
                .filter(dd -> dd.getTrangThai() == TrangThaiDiemDanhEnum.CO_MAT)
                .count();
        int absentCount = (int) weekAttendance.stream()
                .filter(dd -> dd.getTrangThai() == TrangThaiDiemDanhEnum.VANG_MAT)
                .count();
        int lateCount = (int) weekAttendance.stream()
                .filter(dd -> dd.getTrangThai() == TrangThaiDiemDanhEnum.DI_TRE)
                .count();

        double attendanceRate = totalSessions > 0 ?
                (double) (presentCount + lateCount) / totalSessions * 100 : 0;

        return WeeklyAttendanceSummaryDTO.builder()
                .week(week)
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .attendanceRate(attendanceRate)
                .build();
    }

    /**
     * Overload method cho compatibility với controller
     */
    public AttendanceSessionDTO getAttendanceSession(String scheduleId, LocalDate date, boolean isWeekBased) {
        // Tìm maGv từ scheduleId
        String maGv = getMaGvFromScheduleId(scheduleId);
        return getAttendanceSession(scheduleId, maGv);
    }

    private String getMaGvFromScheduleId(String scheduleId) {
        // Thử ScheduleInstance trước
        Optional<ScheduleInstance> instanceOpt = scheduleInstanceRepository.findById(scheduleId);
        if (instanceOpt.isPresent()) {
            GiangVien gv = instanceOpt.get().getGiangVienThucTe();
            return gv != null ? gv.getMaGv() : null;
        }

        // Thử LichHoc
        Optional<LichHoc> lichHocOpt = lichHocRepository.findById(scheduleId);
        if (lichHocOpt.isPresent()) {
            return lichHocOpt.get().getLopHocPhan().getGiangVien().getMaGv();
        }

        throw new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId);
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
            List<TeacherScheduleDTO> schedules = getTeacherScheduleByDate(maGv, date);

            int totalStudents = 0;
            int presentCount = 0;
            int absentCount = 0;
            int lateCount = 0;

            for (TeacherScheduleDTO schedule : schedules) {
                List<DiemDanh> sessionAttendance = diemDanhRepository.findByScheduleIdAndDate(
                        schedule.getScheduleId(), date);

                for (DiemDanh attendance : sessionAttendance) {
                    totalStudents++;
                    switch (attendance.getTrangThai()) {
                        case CO_MAT -> presentCount++;
                        case VANG_MAT -> absentCount++;
                        case DI_TRE -> lateCount++;
                    }
                }
            }

            return TeacherDailyStatsDTO.builder()
                    .date(date)
                    .maGv(maGv)
                    .totalSessions(schedules.size())
                    .totalStudents(totalStudents)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .lateCount(lateCount)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting daily stats: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lấy thống kê: " + e.getMessage());
        }
    }

    /**
     * Export báo cáo điểm danh theo ngày
     */
    public TeacherAttendanceReportDTO exportDailyAttendance(String maGv, LocalDate date) {
        return exportAttendanceReport(maGv, date);
    }

    /**
     * Lấy tóm tắt điểm danh của lớp
     */
    public AttendanceClassSummaryDTO getClassAttendanceSummary(String maLhp) {
        log.info("📊 Getting class attendance summary for: {}", maLhp);

        try {
            List<DiemDanh> allAttendance = diemDanhRepository.findByLopHocPhanAllTypes(maLhp);

            int totalStudents = (int) dangKyHocRepository.findByLopHocPhanMaLhp(maLhp)
                    .stream().filter(dk -> Boolean.TRUE.equals(dk.isActive())).count();

            int totalSessions = allAttendance.stream()
                    .collect(Collectors.groupingBy(DiemDanh::getNgayDiemDanh))
                    .size();

            int totalPresent = (int) allAttendance.stream()
                    .filter(dd -> dd.getTrangThai() == TrangThaiDiemDanhEnum.CO_MAT)
                    .count();

            int totalAbsent = (int) allAttendance.stream()
                    .filter(dd -> dd.getTrangThai() == TrangThaiDiemDanhEnum.VANG_MAT)
                    .count();

            int totalLate = (int) allAttendance.stream()
                    .filter(dd -> dd.getTrangThai() == TrangThaiDiemDanhEnum.DI_TRE)
                    .count();

            double overallRate = allAttendance.size() > 0 ?
                    (double) (totalPresent + totalLate) / allAttendance.size() * 100 : 0;

            return AttendanceClassSummaryDTO.builder()
                    .maLhp(maLhp)
                    .totalStudents(totalStudents)
                    .totalSessions(totalSessions)
                    .averageAttendanceRate(overallRate)
                    .totalPresent(totalPresent)
                    .totalAbsent(totalAbsent)
                    .totalLate(totalLate)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting class summary: {}", e.getMessage(), e);
            throw new BusinessException("Không thể lấy tóm tắt lớp: " + e.getMessage());
        }
    }

}