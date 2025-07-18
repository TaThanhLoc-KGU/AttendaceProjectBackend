package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Model.Camera;
import com.tathanhloc.faceattendance.Model.LichHoc;
import com.tathanhloc.faceattendance.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeAttendanceService {

    private final DiemDanhService diemDanhService;
    private final LichHocRepository lichHocRepository;
    private final CameraRepository cameraRepository;
    private final WebSocketService webSocketService;
    private final DangKyHocRepository dangKyHocRepository;

    // Track active attendance sessions
    private final Map<String, Set<String>> activeAttendanceSessions = new ConcurrentHashMap<>();
    private final Map<String, LocalTime> sessionStartTimes = new ConcurrentHashMap<>();

    /**
     * Xử lý live recognition và tạo attendance
     */
    @Transactional
    public void processLiveRecognition(LiveRecognitionDTO recognition) {
        try {
            log.info("Processing live recognition for student: {} on camera: {}",
                    recognition.getMaSv(), recognition.getCameraId());

            // Find current schedule
            ScheduleFlaskDTO currentSchedule = getCurrentScheduleForCamera(recognition.getCameraId());
            if (currentSchedule == null) {
                log.debug("No active schedule for camera: {}", recognition.getCameraId());
                return;
            }

            // Check if student is enrolled in this class
            if (!isStudentEnrolledInClass(recognition.getMaSv(), currentSchedule.getMaLhp())) {
                log.debug("Student {} not enrolled in class {}", recognition.getMaSv(), currentSchedule.getMaLhp());
                return;
            }

            // Check if already marked attendance in this session
            String sessionKey = currentSchedule.getMaLich() + "_" + LocalDate.now();
            if (isAlreadyMarkedToday(recognition.getMaSv(), sessionKey)) {
                log.debug("Student {} already marked attendance for session {}", recognition.getMaSv(), sessionKey);
                return;
            }

            // Create attendance record
            AttendanceFlaskDTO attendanceData = AttendanceFlaskDTO.builder()
                    .cameraId(recognition.getCameraId())
                    .maSv(recognition.getMaSv())
                    .maLich(currentSchedule.getMaLich())
                    .detectionTime(recognition.getRecognitionTime())
                    .confidence(recognition.getConfidence())
                    .attendanceType("CHECK_IN")
                    .recognitionData(buildRecognitionData(recognition))
                    .build();

            // Create attendance via existing service
            DiemDanhDTO attendance = createAttendanceFromFlask(attendanceData);

            // Track in session
            activeAttendanceSessions.computeIfAbsent(sessionKey, k -> new HashSet<>()).add(recognition.getMaSv());

            // Broadcast attendance update
            webSocketService.broadcastAttendanceUpdate(attendance);

            log.info("Created attendance for student {} in schedule {}", recognition.getMaSv(), currentSchedule.getMaLich());

        } catch (Exception e) {
            log.error("Error processing live recognition: ", e);
        }
    }

    /**
     * Lấy lịch học hiện tại cho camera
     */
    public ScheduleFlaskDTO getCurrentScheduleForCamera(Long cameraId) {
        try {
            Camera camera = cameraRepository.findById(cameraId).orElse(null);
            if (camera == null || camera.getMaPhong() == null) {
                return null;
            }

            String maPhong = camera.getMaPhong().getMaPhong();
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Mon, 7=Sun

            // Find current schedule
            List<LichHoc> schedules = lichHocRepository.findByPhongHocMaPhongAndThuAndIsActiveTrue(maPhong, dayOfWeek);

            for (LichHoc lichHoc : schedules) {
                if (isTimeInSchedule(lichHoc, now)) {
                    return convertToScheduleFlaskDTO(lichHoc);
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting current schedule for camera: {}", cameraId, e);
            return null;
        }
    }

    /**
     * Bắt đầu session điểm danh cho lịch học
     */
    public void startAttendanceSession(String maLich, Long cameraId) {
        String sessionKey = maLich + "_" + LocalDate.now();
        activeAttendanceSessions.put(sessionKey, new HashSet<>());
        sessionStartTimes.put(sessionKey, LocalTime.now());

        log.info("Started attendance session: {} for camera: {}", sessionKey, cameraId);
    }

    /**
     * Kết thúc session điểm danh
     */
    public void endAttendanceSession(String maLich) {
        String sessionKey = maLich + "_" + LocalDate.now();
        Set<String> attendedStudents = activeAttendanceSessions.remove(sessionKey);
        sessionStartTimes.remove(sessionKey);

        log.info("Ended attendance session: {}. Total attended: {}",
                sessionKey, attendedStudents != null ? attendedStudents.size() : 0);
    }

    /**
     * Lấy danh sách điểm danh hiện tại cho session
     */
    public List<DiemDanhDTO> getCurrentSessionAttendance(String maLich) {
        try {
            LocalDate today = LocalDate.now();
            return diemDanhService.getByLichHocAndDate(maLich, today);
        } catch (Exception e) {
            log.error("Error getting current session attendance: ", e);
            return new ArrayList<>();
        }
    }

    // Helper methods
    private boolean isTimeInSchedule(LichHoc lichHoc, LocalTime currentTime) {
        LocalTime startTime = LocalTime.of(7, 0).plusMinutes((lichHoc.getTietBatDau() - 1) * 50);
        LocalTime endTime = startTime.plusMinutes(lichHoc.getSoTiet() * 50);
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

    private boolean isStudentEnrolledInClass(String maSv, String maLhp) {
        return dangKyHocRepository.existsByIdMaSvAndIdMaLhpAndIsActiveTrue(maSv, maLhp);
    }

    private boolean isAlreadyMarkedToday(String maSv, String sessionKey) {
        Set<String> attendedStudents = activeAttendanceSessions.get(sessionKey);
        return attendedStudents != null && attendedStudents.contains(maSv);
    }

    private ScheduleFlaskDTO convertToScheduleFlaskDTO(LichHoc lichHoc) {
        LocalTime startTime = LocalTime.of(7, 0).plusMinutes((lichHoc.getTietBatDau() - 1) * 50);
        LocalTime endTime = startTime.plusMinutes(lichHoc.getSoTiet() * 50);

        return ScheduleFlaskDTO.builder()
                .maLich(lichHoc.getMaLich())
                .maLhp(lichHoc.getLopHocPhan().getMaLhp())
                .tenLhp(lichHoc.getLopHocPhan().getClass().getName())
                .maPhong(lichHoc.getPhongHoc().getMaPhong())
                .tenPhong(lichHoc.getPhongHoc().getTenPhong())
                .thu(lichHoc.getThu())
                .tietBatDau(lichHoc.getTietBatDau())
                .soTiet(lichHoc.getSoTiet())
                .thoiGianBatDau(startTime)
                .thoiGianKetThuc(endTime)
                .build();
    }

    private String buildRecognitionData(LiveRecognitionDTO recognition) {
        // Convert recognition data to JSON string
        return String.format("{\"confidence\": %.2f, \"roiType\": \"%s\", \"timestamp\": \"%s\"}",
                recognition.getConfidence(), recognition.getRoiType(), recognition.getRecognitionTime());
    }

    private DiemDanhDTO createAttendanceFromFlask(AttendanceFlaskDTO attendanceData) {
        // Convert to DiemDanhDTO format expected by existing service
        DiemDanhDTO dto = DiemDanhDTO.builder()
                .maSv(attendanceData.getMaSv())
                .maLich(attendanceData.getMaLich())
                .ngayDiemDanh(attendanceData.getDetectionTime().toLocalDate())
                .thoiGianVao(attendanceData.getDetectionTime().toLocalTime())
                .trangThai(TrangThaiDiemDanhEnum.CO_MAT)
                .build();

        return diemDanhService.create(dto);
    }
}