package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.LichHocDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.LichHoc;
import com.tathanhloc.faceattendance.Repository.LichHocRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.PhongHocRepository;
import com.tathanhloc.faceattendance.Repository.DangKyHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LichHocService {

    private final LichHocRepository lichHocRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final PhongHocRepository phongHocRepository;
    private final DangKyHocRepository dangKyHocRepository;

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

    // ============ SCHEDULING METHODS ============

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
        // Lấy danh sách lớp học phần mà sinh viên đăng ký
        List<String> maLhpList = dangKyHocRepository.findBySinhVien_MaSv(maSv).stream()
                .map(dk -> dk.getLopHocPhan().getMaLhp())
                .collect(Collectors.toList());

        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> maLhpList.contains(lh.getLopHocPhan().getMaLhp()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ============ CONFLICT CHECKING ============

    /**
     * Kiểm tra trùng lịch cho lịch học mới
     */
    public Map<String, Object> checkConflicts(LichHocDTO dto) {
        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        // 1. Kiểm tra trùng phòng học
        List<LichHoc> roomConflicts = findRoomConflicts(dto);
        if (!roomConflicts.isEmpty()) {
            conflicts.add("Phòng học đã được sử dụng trong khung giờ này");
        }

        // 2. Kiểm tra trùng giảng viên
        List<LichHoc> teacherConflicts = findTeacherConflicts(dto);
        if (!teacherConflicts.isEmpty()) {
            conflicts.add("Giảng viên đã có lịch dạy trong khung giờ này");
        }

        // 3. Kiểm tra trùng sinh viên
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

    /**
     * Kiểm tra trùng lịch khi cập nhật (loại trừ chính nó)
     */
    public Map<String, Object> checkConflictsForUpdate(String excludeId, LichHocDTO dto) {
        Map<String, Object> result = checkConflicts(dto);

        // Loại bỏ chính lịch học đang cập nhật khỏi danh sách conflict
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
        // Lấy danh sách sinh viên trong lớp học phần
        List<String> studentIds = dangKyHocRepository.findByLopHocPhan_MaLhp(dto.getMaLhp()).stream()
                .map(dk -> dk.getSinhVien().getMaSv())
                .collect(Collectors.toList());

        return lichHocRepository.findAll().stream()
                .filter(LichHoc::isActive)
                .filter(lh -> lh.getThu().equals(dto.getThu()))
                .filter(lh -> isTimeOverlap(lh.getTietBatDau(), lh.getSoTiet(), dto.getTietBatDau(), dto.getSoTiet()))
                .filter(lh -> hasCommonStudents(lh.getLopHocPhan().getMaLhp(), studentIds))
                .collect(Collectors.toList());
    }

    private boolean hasCommonStudents(String maLhp, List<String> studentIds) {
        List<String> classStudents = dangKyHocRepository.findByLopHocPhan_MaLhp(maLhp).stream()
                .map(dk -> dk.getSinhVien().getMaSv())
                .collect(Collectors.toList());

        return studentIds.stream().anyMatch(classStudents::contains);
    }

    private boolean isTimeOverlap(Integer start1, Integer duration1, Integer start2, Integer duration2) {
        Integer end1 = start1 + duration1 - 1;
        Integer end2 = start2 + duration2 - 1;

        return !(end1 < start2 || end2 < start1);
    }

    // ============ CALENDAR VIEW ============

    public Map<String, Object> getCalendarView(String maGv, String maSv, String maPhong, String semester, String year) {
        List<LichHocDTO> schedules;

        if (maGv != null) {
            schedules = getByGiangVien(maGv);
        } else if (maSv != null) {
            schedules = getBySinhVien(maSv);
        } else if (maPhong != null) {
            schedules = getByPhongHoc(maPhong);
        } else {
            schedules = getAll();
        }

        // Tổ chức dữ liệu theo tuần
        Map<Integer, List<LichHocDTO>> weeklySchedule = new HashMap<>();
        for (int i = 2; i <= 8; i++) { // Thứ 2 đến Chủ nhật
            weeklySchedule.put(i, new ArrayList<>());
        }

        for (LichHocDTO schedule : schedules) {
            weeklySchedule.get(schedule.getThu()).add(schedule);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("weeklySchedule", weeklySchedule);
        result.put("totalSchedules", schedules.size());

        return result;
    }

    // ============ STATISTICS ============

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

    // ============ DTO CONVERSION ============

    private LichHocDTO toDTO(LichHoc lh) {
        return LichHocDTO.builder()
                .maLich(lh.getMaLich())
                .thu(lh.getThu())
                .tietBatDau(lh.getTietBatDau())
                .soTiet(lh.getSoTiet())
                .maLhp(lh.getLopHocPhan().getMaLhp())
                .maPhong(lh.getPhongHoc().getMaPhong())
                .isActive(lh.isActive())
                // Thêm thông tin mở rộng để hiển thị
                .tenMonHoc(lh.getLopHocPhan().getMonHoc().getTenMh())
                .tenGiangVien(lh.getLopHocPhan().getGiangVien().getHoTen())
                .tenPhong(lh.getPhongHoc().getTenPhong())
                .nhom(lh.getLopHocPhan().getNhom())
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
}