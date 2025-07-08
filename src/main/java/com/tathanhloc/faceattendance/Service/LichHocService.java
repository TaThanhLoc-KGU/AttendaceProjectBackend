// 1. Cập nhật LichHocService.java - Thêm các method hỗ trợ học kỳ

package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.LichHocDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.LichHoc;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Repository.LichHocRepository;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.PhongHocRepository;
import com.tathanhloc.faceattendance.Repository.DangKyHocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    // ============ EXISTING METHODS FROM ORIGINAL SERVICE ============

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

    public LichHocDTO create(LichHocDTO dto) {
        LichHoc lh = toEntity(dto);
        return toDTO(lichHocRepository.save(lh));
    }

    public LichHocDTO update(String id, LichHocDTO dto) {
        LichHoc existing = lichHocRepository.findById(id).orElseThrow();
        existing.setThu(dto.getThu());
        existing.setTietBatDau(dto.getTietBatDau());
        existing.setSoTiet(dto.getSoTiet());
        existing.setPhongHoc(phongHocRepository.findById(dto.getMaPhong()).orElseThrow());
        existing.setLopHocPhan(lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow());
        return toDTO(lichHocRepository.save(existing));
    }

    public void delete(String id) {
        LichHoc lichHoc = lichHocRepository.findById(id).orElseThrow();
        lichHoc.setActive(false); // Soft delete
        lichHocRepository.save(lichHoc);
    }

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

    public Map<String, Object> getCalendarView(String maGv, String maSv, String maPhong, String semester, String year) {
        Map<String, Object> calendar = new HashMap<>();

        List<LichHoc> schedules = getFilteredSchedulesLegacy(maGv, maSv, maPhong);

        List<Map<String, Object>> events = schedules.stream()
                .map(this::convertToCalendarEvent)
                .collect(Collectors.toList());

        calendar.put("events", events);
        calendar.put("totalEvents", events.size());

        return calendar;
    }

    public Map<String, Object> getStatistics() {
        List<LichHoc> allSchedules = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSchedules", allSchedules.size());
        stats.put("schedulesByDay", getSchedulesByDay(allSchedules));
        stats.put("roomUtilization", getRoomUtilization(allSchedules));
        stats.put("teacherWorkload", getTeacherWorkload(allSchedules));

        return stats;
    }

    // ============ NEW SEMESTER-BASED METHODS ============

    /**
     * Lấy lịch học theo học kỳ hiện tại
     */
    public List<LichHocDTO> getCurrentSemesterSchedule() {
        HocKy currentSemester = hocKyRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("Không có học kỳ hiện tại"));

        return getScheduleBySemester(currentSemester.getMaHocKy());
    }

    /**
     * Lấy lịch học theo học kỳ (sử dụng hocKy và namHoc từ LopHocPhan)
     */
    public List<LichHocDTO> getScheduleBySemester(String maHocKy) {
        log.info("Lấy lịch học cho học kỳ: {}", maHocKy);

        // Tìm học kỳ để lấy thông tin
        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ: " + maHocKy));

        // Lấy lịch học thông qua LopHocPhan có hocKy tương ứng
        List<LichHoc> schedules = lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(maHocKy))
                .collect(Collectors.toList());

        return schedules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch học dạng calendar view theo học kỳ
     */
    public Map<String, Object> getCalendarViewBySemester(String maHocKy,
                                                         String maGv,
                                                         String maSv,
                                                         String maPhong) {
        log.info("Lấy calendar view cho học kỳ: {}", maHocKy);

        Map<String, Object> calendar = new HashMap<>();

        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ: " + maHocKy));

        List<LichHoc> schedules = getFilteredSchedules(maHocKy, maGv, maSv, maPhong);

        List<Map<String, Object>> events = schedules.stream()
                .map(this::convertToCalendarEvent)
                .collect(Collectors.toList());

        calendar.put("semester", Map.of(
                "maHocKy", hocKy.getMaHocKy(),
                "tenHocKy", hocKy.getTenHocKy(),
                "ngayBatDau", hocKy.getNgayBatDau(),
                "ngayKetThuc", hocKy.getNgayKetThuc()
        ));
        calendar.put("events", events);
        calendar.put("totalEvents", events.size());

        return calendar;
    }

    /**
     * Lấy lịch học hiện tại dạng calendar view
     */
    public Map<String, Object> getCurrentCalendarView(String maGv, String maSv, String maPhong) {
        HocKy currentSemester = hocKyRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("Không có học kỳ hiện tại"));

        return getCalendarViewBySemester(currentSemester.getMaHocKy(), maGv, maSv, maPhong);
    }

    /**
     * Lấy lịch học tuần hiện tại
     */
    public Map<String, Object> getCurrentWeekSchedule(String maGv, String maSv, String maPhong) {
        HocKy currentSemester = hocKyRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("Không có học kỳ hiện tại"));

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<LichHoc> schedules = getFilteredSchedules(currentSemester.getMaHocKy(), maGv, maSv, maPhong);

        Map<String, List<LichHocDTO>> weeklySchedule = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            int dayOfWeek = date.getDayOfWeek().getValue();

            List<LichHocDTO> daySchedules = schedules.stream()
                    .filter(lh -> lh.getThu() == dayOfWeek)
                    .filter(lh -> isDateInSemester(date, currentSemester))
                    .map(this::toDTO)
                    .sorted(Comparator.comparing(LichHocDTO::getTietBatDau))
                    .collect(Collectors.toList());

            weeklySchedule.put(formatDayOfWeek(dayOfWeek) + " (" + date + ")", daySchedules);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("semester", Map.of(
                "maHocKy", currentSemester.getMaHocKy(),
                "tenHocKy", currentSemester.getTenHocKy()
        ));
        result.put("weekStart", weekStart);
        result.put("weekEnd", weekEnd);
        result.put("schedule", weeklySchedule);

        return result;
    }

    /**
     * Lấy lịch học hôm nay
     */
    public List<LichHocDTO> getTodaySchedule(String maGv, String maSv, String maPhong) {
        HocKy currentSemester = hocKyRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("Không có học kỳ hiện tại"));

        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        List<LichHoc> schedules = getFilteredSchedules(currentSemester.getMaHocKy(), maGv, maSv, maPhong);

        return schedules.stream()
                .filter(lh -> lh.getThu() == dayOfWeek)
                .filter(lh -> isDateInSemester(today, currentSemester))
                .map(this::toDTO)
                .sorted(Comparator.comparing(LichHocDTO::getTietBatDau))
                .collect(Collectors.toList());
    }

    /**
     * Lấy thống kê lịch học theo học kỳ
     */
    public Map<String, Object> getSemesterStatistics(String maHocKy) {
        Map<String, Object> stats = new HashMap<>();

        List<LichHoc> schedules = getScheduleEntitiesBySemester(maHocKy);

        stats.put("totalSchedules", schedules.size());
        stats.put("totalClasses", schedules.stream()
                .map(lh -> lh.getLopHocPhan().getMaLhp())
                .distinct()
                .count());
        stats.put("totalTeachers", schedules.stream()
                .map(lh -> lh.getLopHocPhan().getGiangVien().getMaGv())
                .distinct()
                .count());
        stats.put("totalRooms", schedules.stream()
                .map(lh -> lh.getPhongHoc().getMaPhong())
                .distinct()
                .count());

        stats.put("schedulesByDay", getSchedulesByDay(schedules));
        stats.put("schedulesByPeriod", getSchedulesByPeriod(schedules));

        return stats;
    }

    // ============ CONFLICT CHECKING METHODS ============

    public Map<String, Object> checkConflicts(LichHocDTO dto) {
        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        List<LichHoc> roomConflicts = findRoomConflicts(dto);
        if (!roomConflicts.isEmpty()) {
            conflicts.add("Phòng học đã được sử dụng trong khung giờ này");
        }

        List<LichHoc> teacherConflicts = findTeacherConflicts(dto);
        if (!teacherConflicts.isEmpty()) {
            conflicts.add("Giảng viên đã có lịch dạy trong khung giờ này");
        }

        List<LichHoc> studentConflicts = findStudentConflicts(dto);
        if (!studentConflicts.isEmpty()) {
            conflicts.add("Có sinh viên đã có lịch học trong khung giờ này");
        }

        result.put("hasConflict", !conflicts.isEmpty());
        result.put("conflicts", conflicts);
        result.put("roomConflicts", roomConflicts.stream().map(this::toDTO).collect(Collectors.toList()));
        result.put("teacherConflicts", teacherConflicts.stream().map(this::toDTO).collect(Collectors.toList()));
        result.put("studentConflicts", studentConflicts.stream().map(this::toDTO).collect(Collectors.toList()));

        return result;
    }

    public Map<String, Object> checkConflictsForUpdate(String excludeId, LichHocDTO dto) {
        Map<String, Object> result = checkConflicts(dto);

        @SuppressWarnings("unchecked")
        List<LichHocDTO> roomConflicts = (List<LichHocDTO>) result.get("roomConflicts");
        @SuppressWarnings("unchecked")
        List<LichHocDTO> teacherConflicts = (List<LichHocDTO>) result.get("teacherConflicts");
        @SuppressWarnings("unchecked")
        List<LichHocDTO> studentConflicts = (List<LichHocDTO>) result.get("studentConflicts");

        roomConflicts.removeIf(lh -> lh.getMaLich().equals(excludeId));
        teacherConflicts.removeIf(lh -> lh.getMaLich().equals(excludeId));
        studentConflicts.removeIf(lh -> lh.getMaLich().equals(excludeId));

        @SuppressWarnings("unchecked")
        List<String> conflicts = (List<String>) result.get("conflicts");
        conflicts.clear();

        if (!roomConflicts.isEmpty()) {
            conflicts.add("Phòng học đã được sử dụng trong khung giờ này");
        }
        if (!teacherConflicts.isEmpty()) {
            conflicts.add("Giảng viên đã có lịch dạy trong khung giờ này");
        }
        if (!studentConflicts.isEmpty()) {
            conflicts.add("Có sinh viên đã có lịch học trong khung giờ này");
        }

        result.put("hasConflict", !conflicts.isEmpty());
        return result;
    }

    public Map<String, Object> checkConflictsInSemester(LichHocDTO dto) {
        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        if (dto.getHocKy() == null) {
            conflicts.add("Chưa chỉ định học kỳ");
            result.put("hasConflict", true);
            result.put("conflicts", conflicts);
            return result;
        }

        HocKy hocKy = hocKyRepository.findById(dto.getHocKy())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ: " + dto.getHocKy()));

        return checkConflicts(dto);
    }

    // ============ HELPER METHODS ============

    private List<LichHoc> getFilteredSchedules(String maHocKy, String maGv, String maSv, String maPhong) {
        List<LichHoc> schedules = getScheduleEntitiesBySemester(maHocKy);

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

    private List<LichHoc> getScheduleEntitiesBySemester(String maHocKy) {
        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getHocKy().equals(maHocKy))
                .collect(Collectors.toList());
    }

    private List<LichHoc> findRoomConflicts(LichHocDTO dto) {
        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getPhongHoc().getMaPhong().equals(dto.getMaPhong()))
                .filter(lh -> lh.getThu().equals(dto.getThu()))
                .filter(lh -> isTimeOverlap(lh.getTietBatDau(), lh.getSoTiet(), dto.getTietBatDau(), dto.getSoTiet()))
                .collect(Collectors.toList());
    }

    private List<LichHoc> findTeacherConflicts(LichHocDTO dto) {
        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getLopHocPhan().getGiangVien().getMaGv().equals(
                        lopHocPhanRepository.findById(dto.getMaLhp()).get().getGiangVien().getMaGv()))
                .filter(lh -> lh.getThu().equals(dto.getThu()))
                .filter(lh -> isTimeOverlap(lh.getTietBatDau(), lh.getSoTiet(), dto.getTietBatDau(), dto.getSoTiet()))
                .collect(Collectors.toList());
    }

    private List<LichHoc> findStudentConflicts(LichHocDTO dto) {
        List<String> studentMaLhpList = dangKyHocRepository.findByLopHocPhan_MaLhp(dto.getMaLhp()).stream()
                .map(dk -> dk.getSinhVien().getMaSv())
                .collect(Collectors.toList());

        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> !lh.getLopHocPhan().getMaLhp().equals(dto.getMaLhp()))
                .filter(lh -> lh.getThu().equals(dto.getThu()))
                .filter(lh -> hasStudentConflict(lh.getLopHocPhan().getMaLhp(), studentMaLhpList))
                .filter(lh -> isTimeOverlap(lh.getTietBatDau(), lh.getSoTiet(), dto.getTietBatDau(), dto.getSoTiet()))
                .collect(Collectors.toList());
    }

    private boolean hasStudentConflict(String maLhp, List<String> studentIds) {
        List<String> lhpStudents = dangKyHocRepository.findByLopHocPhan_MaLhp(maLhp).stream()
                .map(dk -> dk.getSinhVien().getMaSv())
                .collect(Collectors.toList());

        return lhpStudents.stream().anyMatch(studentIds::contains);
    }

    private boolean isTimeOverlap(int start1, int duration1, int start2, int duration2) {
        int end1 = start1 + duration1 - 1;
        int end2 = start2 + duration2 - 1;
        return !(end1 < start2 || end2 < start1);
    }

    private Map<String, Object> convertToCalendarEvent(LichHoc lichHoc) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", lichHoc.getMaLich());
        event.put("title", lichHoc.getLopHocPhan().getMonHoc().getTenMh());
        event.put("start", calculateStartTime(lichHoc.getTietBatDau()));
        event.put("end", calculateEndTime(lichHoc.getTietBatDau(), lichHoc.getSoTiet()));
        event.put("dayOfWeek", lichHoc.getThu());
        event.put("room", lichHoc.getPhongHoc().getTenPhong());
        event.put("teacher", lichHoc.getLopHocPhan().getGiangVien().getHoTen());
        event.put("className", lichHoc.getLopHocPhan().getMaLhp());
        event.put("color", getColorBySubject(lichHoc.getLopHocPhan().getMonHoc().getMaMh()));

        return event;
    }

    private boolean isDateInSemester(LocalDate date, HocKy hocKy) {
        return !date.isBefore(hocKy.getNgayBatDau()) && !date.isAfter(hocKy.getNgayKetThuc());
    }

    private String formatDayOfWeek(int dayOfWeek) {
        String[] days = {"", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? days[dayOfWeek] : "Không xác định";
    }

    private LocalTime calculateStartTime(int tietBatDau) {
        return LocalTime.of(7, 0).plusMinutes((tietBatDau - 1) * 50);
    }

    private LocalTime calculateEndTime(int tietBatDau, int soTiet) {
        return calculateStartTime(tietBatDau).plusMinutes(soTiet * 50 - 5);
    }

    private String getColorBySubject(String maMon) {
        int hash = maMon.hashCode();
        String[] colors = {"#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#34495e"};
        return colors[Math.abs(hash) % colors.length];
    }

    private Map<Integer, Long> getSchedulesByDay(List<LichHoc> schedules) {
        return schedules.stream()
                .collect(Collectors.groupingBy(LichHoc::getThu, Collectors.counting()));
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
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getSchedulesByPeriod(List<LichHoc> schedules) {
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        lh -> "Tiết " + lh.getTietBatDau() + "-" + (lh.getTietBatDau() + lh.getSoTiet() - 1),
                        Collectors.counting()
                ));
    }

    // ============ DTO CONVERSION METHODS ============

    private LichHocDTO toDTO(LichHoc lh) {
        return LichHocDTO.builder()
                .maLich(lh.getMaLich())
                .thu(lh.getThu())
                .tietBatDau(lh.getTietBatDau())
                .soTiet(lh.getSoTiet())
                .maLhp(lh.getLopHocPhan().getMaLhp())
                .maPhong(lh.getPhongHoc().getMaPhong())
                .isActive(lh.isActive())
                // Thông tin mở rộng từ existing service
                .tenMonHoc(lh.getLopHocPhan().getMonHoc().getTenMh())
                .tenGiangVien(lh.getLopHocPhan().getGiangVien().getHoTen())
                .tenPhong(lh.getPhongHoc().getTenPhong())
                .nhom(lh.getLopHocPhan().getNhom())
                .maMh(lh.getLopHocPhan().getMonHoc().getMaMh())
                .maGv(lh.getLopHocPhan().getGiangVien().getMaGv())
                .hocKy(lh.getLopHocPhan().getHocKy())
                .namHoc(lh.getLopHocPhan().getNamHoc())
                // Tính toán thời gian
                .thoiGianBatDau(calculateStartTime(lh.getTietBatDau()).toString())
                .thoiGianKetThuc(calculateEndTime(lh.getTietBatDau(), lh.getSoTiet()).toString())
                .tenThu(formatDayOfWeek(lh.getThu()))
                .build();
    }

    private LichHoc toEntity(LichHocDTO dto) {
        return LichHoc.builder()
                .maLich(dto.getMaLich())
                .thu(dto.getThu())
                .tietBatDau(dto.getTietBatDau())
                .soTiet(dto.getSoTiet())
                .lopHocPhan(lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow())
                .phongHoc(phongHocRepository.findById(dto.getMaPhong()).orElseThrow())
                .isActive(true)
                .build();
    }

    // ============ ADDITIONAL EXPORT/IMPORT METHODS (PLACEHOLDERS) ============

    public byte[] exportSemesterScheduleToExcel(String maHocKy) {
        // TODO: Implement Excel export functionality
        throw new UnsupportedOperationException("Excel export chưa được implement");
    }

    public Map<String, Object> importSemesterScheduleFromExcel(String maHocKy, byte[] fileData) {
        // TODO: Implement Excel import functionality
        throw new UnsupportedOperationException("Excel import chưa được implement");
    }

    public Map<String, Object> copySemesterSchedule(String sourceSemester, String targetSemester, boolean overwrite) {
        // TODO: Implement copy schedule functionality
        throw new UnsupportedOperationException("Copy schedule chưa được implement");
    }
}