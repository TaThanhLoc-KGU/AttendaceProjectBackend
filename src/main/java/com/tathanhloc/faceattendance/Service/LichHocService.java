package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.LichHocDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.LichHoc;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Model.LopHocPhan;
import com.tathanhloc.faceattendance.Model.PhongHoc;
import com.tathanhloc.faceattendance.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LichHocService {

    private final LichHocRepository lichHocRepository;
    private final HocKyRepository hocKyRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final PhongHocRepository phongHocRepository;
    private final DangKyHocRepository dangKyHocRepository;

    // ============ BASIC CRUD OPERATIONS ============

    public List<LichHocDTO> getAll() {
        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public LichHocDTO getById(String id) {
        return lichHocRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("LichHoc not found with id: " + id));
    }

    @Transactional
    public LichHocDTO create(LichHocDTO dto) {
        log.info("Creating new schedule: {}", dto);

        // Validate data
        validateScheduleData(dto);

        // Check for conflicts
        Map<String, Object> conflictCheck = checkConflicts(dto);
        if ((Boolean) conflictCheck.get("hasConflict")) {
            throw new RuntimeException("Trùng lịch: " + conflictCheck.get("conflictDetails"));
        }


        LichHoc lh = toEntity(dto);
        lh.setActive(true);

        lh.setCreatedAt(LocalDateTime.now());
        lh.setUpdatedAt(LocalDateTime.now());
        LichHoc saved = lichHocRepository.save(lh);
        log.info("Schedule created successfully: {}", saved.getMaLich());
        return toDTO(saved);
    }

    /**
     * Update method đã sửa
     */
    @Transactional
    public LichHocDTO update(String id, LichHocDTO dto) {
        log.info("Updating schedule with ID {}: {}", id, dto);

        LichHoc existing = lichHocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "ID", id));

        // Chỉ kiểm tra trùng lịch nếu có thay đổi về thời gian, phòng hoặc giảng viên
        boolean needConflictCheck = hasSignificantChanges(existing, dto);

        if (needConflictCheck) {
            Map<String, Object> conflictResult = checkConflictsForUpdate(id, dto);
            if ((Boolean) conflictResult.get("hasConflict")) {
                @SuppressWarnings("unchecked")
                List<String> conflicts = (List<String>) conflictResult.get("conflictDetails");
                throw new IllegalArgumentException("Trùng lịch: " + conflicts);
            }
        }

        // Update fields
        updateEntityFromDTO(existing, dto);

        existing.setUpdatedAt(LocalDateTime.now());

        LichHoc saved = lichHocRepository.save(existing);
        return toDTO(saved);
    }
    /**
     * Update entity từ DTO
     */
    private void updateEntityFromDTO(LichHoc entity, LichHocDTO dto) {
        if (dto.getMaLhp() != null && !dto.getMaLhp().equals(entity.getLopHocPhan().getMaLhp())) {
            LopHocPhan lopHocPhan = lopHocPhanRepository.findById(dto.getMaLhp())
                    .orElseThrow(() -> new ResourceNotFoundException("Lớp học phần", "mã LHP", dto.getMaLhp()));
            entity.setLopHocPhan(lopHocPhan);
        }

        if (dto.getMaPhong() != null && !dto.getMaPhong().equals(entity.getPhongHoc().getMaPhong())) {
            PhongHoc phongHoc = phongHocRepository.findById(dto.getMaPhong())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng học", "mã phòng", dto.getMaPhong()));
            entity.setPhongHoc(phongHoc);
        }

        if (dto.getThu() != null) {
            entity.setThu(dto.getThu());
        }

        if (dto.getTietBatDau() != null) {
            entity.setTietBatDau(dto.getTietBatDau());
        }

        if (dto.getSoTiet() != null) {
            entity.setSoTiet(dto.getSoTiet());
        }

        // Luôn luôn set updated timestamp
        entity.setUpdatedAt(LocalDateTime.now());
    }
    /**
     * Kiểm tra xem có thay đổi đáng kể không
     */
    private boolean hasSignificantChanges(LichHoc existing, LichHocDTO dto) {
        boolean timeChanged = !Objects.equals(existing.getThu(), dto.getThu()) ||
                !Objects.equals(existing.getTietBatDau(), dto.getTietBatDau()) ||
                !Objects.equals(existing.getSoTiet(), dto.getSoTiet());

        boolean roomChanged = dto.getMaPhong() != null &&
                !Objects.equals(existing.getPhongHoc().getMaPhong(), dto.getMaPhong());

        boolean classChanged = dto.getMaLhp() != null &&
                !Objects.equals(existing.getLopHocPhan().getMaLhp(), dto.getMaLhp());

        return timeChanged || roomChanged || classChanged;
    }

    @Transactional
    public void delete(String id) {
        log.info("Deleting schedule: {}", id);
        LichHoc lichHoc = lichHocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LichHoc not found with id: " + id));

        lichHoc.setActive(false); // Soft delete
        lichHocRepository.save(lichHoc);
        log.info("Schedule deleted successfully: {}", id);
    }

    // ============ QUERY METHODS ============

    public List<LichHocDTO> getByLopHocPhan(String maLhp) {
        return lichHocRepository.findByLopHocPhanMaLhp(maLhp).stream()
                .filter(LichHoc::isActive)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LichHocDTO> getByPhongHoc(String maPhong) {
        return lichHocRepository.findByPhongHocMaPhong(maPhong).stream()
                .filter(LichHoc::isActive)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LichHocDTO> getByThu(Integer thu) {
        return lichHocRepository.findByThu(thu).stream()
                .filter(LichHoc::isActive)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LichHocDTO> getByGiangVien(String maGv) {
        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getGiangVien().getMaGv().equals(maGv))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LichHocDTO> getBySinhVien(String maSv) {
        List<String> maLhpList = dangKyHocRepository.findBySinhVien_MaSv(maSv).stream()
                .map(dk -> dk.getLopHocPhan().getMaLhp())
                .collect(Collectors.toList());

        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> maLhpList.contains(lh.getLopHocPhan().getMaLhp()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ============ SEMESTER-BASED METHODS ============

    /**
     * Lấy lịch học theo học kỳ hiện tại
     */
    public List<LichHocDTO> getCurrentSemesterSchedule() {
        HocKy currentSemester = hocKyRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Không có học kỳ hiện tại"));

        return getScheduleBySemester(currentSemester.getMaHocKy());
    }

    /**
     * Lấy lịch học theo học kỳ
     */
    public List<LichHocDTO> getScheduleBySemester(String maHocKy) {
        log.info("Getting schedule for semester: {}", maHocKy);

        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(maHocKy))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Tạo lịch học cho cả học kỳ (sắp lịch)
     */
    @Transactional
    public Map<String, Object> createSemesterSchedule(String maHocKy, List<LichHocDTO> scheduleList) {
        log.info("Creating semester schedule for: {} with {} schedules", maHocKy, scheduleList.size());

        Map<String, Object> result = new HashMap<>();
        List<LichHocDTO> successfulSchedules = new ArrayList<>();
        List<Map<String, Object>> conflicts = new ArrayList<>();

        // Validate semester exists
        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));

        for (LichHocDTO dto : scheduleList) {
            try {
                // Set semester for the schedule
                LopHocPhan lhp = lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow();
                if (!lhp.getHocKy().equals(maHocKy)) {
                    throw new RuntimeException("Lớp học phần không thuộc học kỳ này");
                }

                // Check conflicts
                Map<String, Object> conflictCheck = checkConflicts(dto);
                if ((Boolean) conflictCheck.get("hasConflict")) {
                    conflicts.add(Map.of(
                            "schedule", dto,
                            "conflicts", conflictCheck.get("conflictDetails")
                    ));
                    continue;
                }

                // Create schedule
                LichHoc entity = toEntity(dto);
                entity.setActive(true);
                LichHoc saved = lichHocRepository.save(entity);
                successfulSchedules.add(toDTO(saved));

            } catch (Exception e) {
                log.error("Error creating schedule: {}", e.getMessage());
                conflicts.add(Map.of(
                        "schedule", dto,
                        "error", e.getMessage()
                ));
            }
        }

        result.put("semester", maHocKy);
        result.put("totalSchedules", scheduleList.size());
        result.put("successfulSchedules", successfulSchedules);
        result.put("successCount", successfulSchedules.size());
        result.put("conflicts", conflicts);
        result.put("conflictCount", conflicts.size());

        log.info("Semester schedule creation completed. Success: {}, Conflicts: {}",
                successfulSchedules.size(), conflicts.size());

        return result;
    }

    /**
     * Cập nhật lịch học cho cả học kỳ
     */
    @Transactional
    public Map<String, Object> updateSemesterSchedule(String maHocKy, List<LichHocDTO> scheduleList) {
        log.info("Updating semester schedule for: {}", maHocKy);

        // Delete existing schedules for this semester
        List<LichHoc> existingSchedules = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(maHocKy))
                .collect(Collectors.toList());

        existingSchedules.forEach(schedule -> {
            schedule.setActive(false);
            lichHocRepository.save(schedule);
        });

        // Create new schedules
        return createSemesterSchedule(maHocKy, scheduleList);
    }

    // ============ CONFLICT CHECKING ============

    /**
     * Kiểm tra trùng lịch
     */
    public Map<String, Object> checkConflicts(LichHocDTO dto) {
        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        // Get semester from LHP
        LopHocPhan lhp = lopHocPhanRepository.findById(dto.getMaLhp()).orElse(null);
        if (lhp == null) {
            result.put("hasConflict", true);
            result.put("conflictDetails", "Không tìm thấy lớp học phần");
            return result;
        }

        String hocKy = lhp.getHocKy();
        int tietKetThuc = dto.getTietBatDau() + dto.getSoTiet() - 1;

        // Check room conflicts in the same semester
        List<LichHoc> roomConflicts = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(hocKy))
                .filter(lh -> lh.getPhongHoc().getMaPhong().equals(dto.getMaPhong()))
                .filter(lh -> lh.getThu().equals(dto.getThu()))
                .filter(lh -> {
                    int existingEnd = lh.getTietBatDau() + lh.getSoTiet() - 1;
                    return !(tietKetThuc < lh.getTietBatDau() || dto.getTietBatDau() > existingEnd);
                })
                .collect(Collectors.toList());

        if (!roomConflicts.isEmpty()) {
            conflicts.add("Phòng " + dto.getMaPhong() + " đã có lịch học trong thời gian này");
        }

        // Check teacher conflicts
        String maGv = lhp.getGiangVien().getMaGv();
        List<LichHoc> teacherConflicts = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(hocKy))
                .filter(lh -> lh.getLopHocPhan().getGiangVien().getMaGv().equals(maGv))
                .filter(lh -> lh.getThu().equals(dto.getThu()))
                .filter(lh -> {
                    int existingEnd = lh.getTietBatDau() + lh.getSoTiet() - 1;
                    return !(tietKetThuc < lh.getTietBatDau() || dto.getTietBatDau() > existingEnd);
                })
                .collect(Collectors.toList());

        if (!teacherConflicts.isEmpty()) {
            conflicts.add("Giảng viên đã có lịch dạy trong thời gian này");
        }

        result.put("hasConflict", !conflicts.isEmpty());
        result.put("conflictDetails", conflicts);
        result.put("conflictCount", conflicts.size());

        return result;
    }

    /**
     * Kiểm tra trùng lịch khi update
     */
    public Map<String, Object> checkConflictsForUpdate(String scheduleId, LichHocDTO dto) {
        log.info("Checking conflicts for update - Schedule ID: {}, DTO: {}", scheduleId, dto);

        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();
        boolean hasConflict = false;

        try {
            // Lấy tất cả lịch học NGOẠI TRỪ lịch đang update
            List<LichHocDTO> allSchedules = getAll().stream()
                    .filter(s -> !s.getMaLich().equals(scheduleId)) // Loại trừ chính nó
                    .filter(s -> s.getIsActive() == null || s.getIsActive()) // Chỉ lấy lịch active
                    .collect(Collectors.toList());

            // Kiểm tra trùng giảng viên
            if (dto.getMaGv() != null && dto.getThu() != null &&
                    dto.getTietBatDau() != null && dto.getSoTiet() != null) {

                for (LichHocDTO other : allSchedules) {
                    if (other.getMaGv() != null && other.getMaGv().equals(dto.getMaGv()) &&
                            other.getThu() != null && other.getThu().equals(dto.getThu()) &&
                            isTimeOverlap(dto.getTietBatDau(), dto.getSoTiet(),
                                    other.getTietBatDau(), other.getSoTiet())) {

                        conflicts.add("Giảng viên đã có lịch dạy trong thời gian này");
                        hasConflict = true;
                        break;
                    }
                }
            }

            // Kiểm tra trùng phòng học
            if (dto.getMaPhong() != null && dto.getThu() != null &&
                    dto.getTietBatDau() != null && dto.getSoTiet() != null) {

                for (LichHocDTO other : allSchedules) {
                    if (other.getMaPhong() != null && other.getMaPhong().equals(dto.getMaPhong()) &&
                            other.getThu() != null && other.getThu().equals(dto.getThu()) &&
                            isTimeOverlap(dto.getTietBatDau(), dto.getSoTiet(),
                                    other.getTietBatDau(), other.getSoTiet())) {

                        conflicts.add("Phòng học đã được sử dụng trong thời gian này");
                        hasConflict = true;
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error checking conflicts for update: {}", e.getMessage());
            conflicts.add("Lỗi khi kiểm tra trùng lịch: " + e.getMessage());
            hasConflict = true;
        }

        result.put("hasConflict", hasConflict);
        result.put("conflictDetails", conflicts);
        result.put("scheduleId", scheduleId);

        log.info("Conflict check result for update: hasConflict={}, conflicts={}", hasConflict, conflicts);
        return result;
    }

    /**
     * Kiểm tra overlap thời gian
     */
    private boolean isTimeOverlap(Integer start1, Integer duration1, Integer start2, Integer duration2) {
        if (start1 == null || duration1 == null || start2 == null || duration2 == null) {
            return false;
        }

        int end1 = start1 + duration1 - 1;
        int end2 = start2 + duration2 - 1;

        return !(end1 < start2 || end2 < start1);
    }
    /**
     * Kiểm tra trùng lịch trong học kỳ
     */
    public Map<String, Object> checkConflictsInSemester(LichHocDTO dto) {
        return checkConflicts(dto);
    }

    // ============ CALENDAR AND STATISTICS ============

    public Map<String, Object> getCalendarView(String maGv, String maSv, String maPhong, String semester, String year) {
        Map<String, Object> calendar = new HashMap<>();

        List<LichHoc> schedules;
        if (semester != null) {
            schedules = getFilteredSchedulesBySemester(semester, maGv, maSv, maPhong);
        } else {
            schedules = getFilteredSchedulesLegacy(maGv, maSv, maPhong);
        }

        List<Map<String, Object>> events = schedules.stream()
                .map(this::convertToCalendarEvent)
                .collect(Collectors.toList());

        calendar.put("events", events);
        calendar.put("totalEvents", events.size());
        calendar.put("semester", semester);

        return calendar;
    }

    public Map<String, Object> getCalendarViewBySemester(String maHocKy, String maGv, String maSv, String maPhong) {
        Map<String, Object> calendar = new HashMap<>();

        List<LichHoc> schedules = getFilteredSchedulesBySemester(maHocKy, maGv, maSv, maPhong);

        List<Map<String, Object>> events = schedules.stream()
                .map(this::convertToCalendarEvent)
                .collect(Collectors.toList());

        calendar.put("events", events);
        calendar.put("totalEvents", events.size());
        calendar.put("semester", maHocKy);

        return calendar;
    }

    public Map<String, Object> getCurrentCalendarView(String maGv, String maSv, String maPhong) {
        try {
            HocKy currentSemester = hocKyRepository.findByIsCurrentTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("Không có học kỳ hiện tại"));

            return getCalendarViewBySemester(currentSemester.getMaHocKy(), maGv, maSv, maPhong);
        } catch (Exception e) {
            log.warn("No current semester found, using legacy calendar view");
            return getCalendarView(maGv, maSv, maPhong, null, null);
        }
    }

    public Map<String, Object> getCurrentWeekSchedule(String maGv, String maSv, String maPhong) {
        Map<String, Object> weekSchedule = new HashMap<>();

        try {
            HocKy currentSemester = hocKyRepository.findByIsCurrentTrue().orElse(null);

            List<LichHoc> schedules;
            if (currentSemester != null) {
                schedules = getFilteredSchedulesBySemester(currentSemester.getMaHocKy(), maGv, maSv, maPhong);
            } else {
                schedules = getFilteredSchedulesLegacy(maGv, maSv, maPhong);
            }

            // Group by day of week
            Map<Integer, List<Map<String, Object>>> weeklySchedule = new HashMap<>();
            for (int i = 2; i <= 8; i++) { // Monday to Sunday
                weeklySchedule.put(i, new ArrayList<>());
            }

            schedules.forEach(schedule -> {
                int dayOfWeek = schedule.getThu();
                if (dayOfWeek >= 2 && dayOfWeek <= 8) {
                    weeklySchedule.get(dayOfWeek).add(convertToCalendarEvent(schedule));
                }
            });

            weekSchedule.put("weeklySchedule", weeklySchedule);
            weekSchedule.put("totalSchedules", schedules.size());
            weekSchedule.put("semester", currentSemester != null ? currentSemester.getMaHocKy() : null);

        } catch (Exception e) {
            log.error("Error getting current week schedule: {}", e.getMessage());
            weekSchedule.put("error", e.getMessage());
        }

        return weekSchedule;
    }

    public List<LichHocDTO> getTodaySchedule(String maGv, String maSv, String maPhong) {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue() + 1; // Monday = 2, Sunday = 8

        try {
            HocKy currentSemester = hocKyRepository.findByIsCurrentTrue().orElse(null);

            List<LichHoc> schedules;
            if (currentSemester != null) {
                schedules = getFilteredSchedulesBySemester(currentSemester.getMaHocKy(), maGv, maSv, maPhong);
            } else {
                schedules = getFilteredSchedulesLegacy(maGv, maSv, maPhong);
            }

            return schedules.stream()
                    .filter(lh -> lh.getThu().equals(dayOfWeek))
                    .map(this::toDTO)
                    .sorted(Comparator.comparing(LichHocDTO::getTietBatDau))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting today's schedule: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getStatistics() {
        try {
            HocKy currentSemester = hocKyRepository.findByIsCurrentTrue().orElse(null);

            List<LichHoc> allSchedules;
            if (currentSemester != null) {
                allSchedules = lichHocRepository.findAll().stream()
                        .filter(LichHoc::isActive)
                        .filter(lh -> lh.getLopHocPhan().getHocKy().equals(currentSemester.getMaHocKy()))
                        .collect(Collectors.toList());
            } else {
                allSchedules = lichHocRepository.findAll().stream()
                        .filter(LichHoc::isActive)
                        .collect(Collectors.toList());
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSchedules", allSchedules.size());
            stats.put("currentSemester", currentSemester != null ? currentSemester.getMaHocKy() : null);
            stats.put("schedulesByDay", getSchedulesByDay(allSchedules));
            stats.put("roomUtilization", getRoomUtilization(allSchedules));
            stats.put("teacherWorkload", getTeacherWorkload(allSchedules));

            return stats;

        } catch (Exception e) {
            log.error("Error getting statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    public Map<String, Object> getSemesterStatistics(String maHocKy) {
        List<LichHoc> semesterSchedules = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(maHocKy))
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("semester", maHocKy);
        stats.put("totalSchedules", semesterSchedules.size());
        stats.put("schedulesByDay", getSchedulesByDay(semesterSchedules));
        stats.put("roomUtilization", getRoomUtilization(semesterSchedules));
        stats.put("teacherWorkload", getTeacherWorkload(semesterSchedules));

        return stats;
    }

    // ============ HELPER METHODS ============

    private void validateScheduleData(LichHocDTO dto) {
        if (dto.getThu() < 2 || dto.getThu() > 8) {
            throw new IllegalArgumentException("Thứ phải từ 2 đến 8 (Thứ 2 đến Chủ nhật)");
        }

        if (dto.getTietBatDau() < 1 || dto.getTietBatDau() > 12) {
            throw new IllegalArgumentException("Tiết bắt đầu phải từ 1 đến 12");
        }

        if (dto.getSoTiet() < 1 || dto.getSoTiet() > 6) {
            throw new IllegalArgumentException("Số tiết phải từ 1 đến 6");
        }

        if (dto.getTietBatDau() + dto.getSoTiet() - 1 > 12) {
            throw new IllegalArgumentException("Lịch học vượt quá số tiết trong ngày (tối đa 12 tiết)");
        }
    }

    private List<LichHoc> getFilteredSchedulesBySemester(String maHocKy, String maGv, String maSv, String maPhong) {
        List<LichHoc> schedules = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(maHocKy))
                .collect(Collectors.toList());

        if (maGv != null) {
            schedules = schedules.stream()
                    .filter(lh -> lh.getLopHocPhan().getGiangVien().getMaGv().equals(maGv))
                    .collect(Collectors.toList());
        }

        if (maSv != null) {
            List<String> studentLhpList = dangKyHocRepository.findBySinhVien_MaSv(maSv).stream()
                    .map(dk -> dk.getLopHocPhan().getMaLhp())
                    .collect(Collectors.toList());

            schedules = schedules.stream()
                    .filter(lh -> studentLhpList.contains(lh.getLopHocPhan().getMaLhp()))
                    .collect(Collectors.toList());
        }

        if (maPhong != null) {
            schedules = schedules.stream()
                    .filter(lh -> lh.getPhongHoc().getMaPhong().equals(maPhong))
                    .collect(Collectors.toList());
        }

        return schedules;
    }

    private List<LichHoc> getFilteredSchedulesLegacy(String maGv, String maSv, String maPhong) {
        List<LichHoc> schedules = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .collect(Collectors.toList());

        if (maGv != null) {
            schedules = schedules.stream()
                    .filter(lh -> lh.getLopHocPhan().getGiangVien().getMaGv().equals(maGv))
                    .collect(Collectors.toList());
        }

        if (maSv != null) {
            List<String> studentLhpList = dangKyHocRepository.findBySinhVien_MaSv(maSv).stream()
                    .map(dk -> dk.getLopHocPhan().getMaLhp())
                    .collect(Collectors.toList());

            schedules = schedules.stream()
                    .filter(lh -> studentLhpList.contains(lh.getLopHocPhan().getMaLhp()))
                    .collect(Collectors.toList());
        }

        if (maPhong != null) {
            schedules = schedules.stream()
                    .filter(lh -> lh.getPhongHoc().getMaPhong().equals(maPhong))
                    .collect(Collectors.toList());
        }

        return schedules;
    }

    private Map<String, Object> convertToCalendarEvent(LichHoc lichHoc) {
        Map<String, Object> event = new HashMap<>();

        LopHocPhan lhp = lichHoc.getLopHocPhan();
        PhongHoc phong = lichHoc.getPhongHoc();

        event.put("id", lichHoc.getMaLich());
        event.put("title", lhp.getMonHoc().getTenMh() + " - " + lhp.getMaLhp());
        event.put("dayOfWeek", lichHoc.getThu());
        event.put("startPeriod", lichHoc.getTietBatDau());
        event.put("endPeriod", lichHoc.getTietBatDau() + lichHoc.getSoTiet() - 1);
        event.put("room", phong.getMaPhong());
        event.put("teacher", lhp.getGiangVien().getHoTen());
        event.put("semester", lhp.getHocKy());
        event.put("academicYear", lhp.getNamHoc());

        return event;
    }

    private Map<Integer, Long> getSchedulesByDay(List<LichHoc> schedules) {
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        LichHoc::getThu,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getRoomUtilization(List<LichHoc> schedules) {
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        lh -> lh.getPhongHoc().getMaPhong(),
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getTeacherWorkload(List<LichHoc> schedules) {
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        lh -> lh.getLopHocPhan().getGiangVien().getMaGv(),
                        Collectors.summingLong(lh -> lh.getSoTiet().longValue())
                ));
    }

    private LichHoc toEntity(LichHocDTO dto) {
        LichHoc entity = new LichHoc();
        entity.setMaLich(dto.getMaLich());
        entity.setThu(dto.getThu());
        entity.setTietBatDau(dto.getTietBatDau());
        entity.setSoTiet(dto.getSoTiet());
        entity.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        // Set relationships
        entity.setLopHocPhan(lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow());
        entity.setPhongHoc(phongHocRepository.findById(dto.getMaPhong()).orElseThrow());

        return entity;
    }

    private LichHocDTO toDTO(LichHoc entity) {
        LopHocPhan lhp = entity.getLopHocPhan();
        PhongHoc phong = entity.getPhongHoc();

        return LichHocDTO.builder()
                .maLich(entity.getMaLich())
                .thu(entity.getThu())
                .tietBatDau(entity.getTietBatDau())
                .soTiet(entity.getSoTiet())
                .maLhp(lhp.getMaLhp())
                .maPhong(phong.getMaPhong())
                .isActive(entity.isActive())
                // Additional fields for display
                .tenMonHoc(lhp.getMonHoc().getTenMh())
                .tenGiangVien(lhp.getGiangVien().getHoTen())
                .maGv(lhp.getGiangVien().getMaGv())
                .tenPhong(phong.getTenPhong())
                .hocKy(lhp.getHocKy())
                .namHoc(lhp.getNamHoc())
                .nhom(lhp.getNhom())
                .build();
    }
}