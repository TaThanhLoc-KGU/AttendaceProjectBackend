package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flask")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5000") // Flask default port
public class FlaskIntegrationController {

    private final CameraService cameraService;
    private final FaceDetectionService faceDetectionService;
    private final RealtimeAttendanceService realtimeAttendanceService;
    private final FlaskConfigurationService flaskConfigurationService;
    private final DiemDanhService diemDanhService;

    /**
     * Lấy danh sách cameras active cho Flask
     * GET /api/flask/cameras/active
     */
    @GetMapping("/cameras/active")
    public ResponseEntity<List<CameraFlaskDTO>> getActiveCamerasForFlask() {
        try {
            log.info("Flask requesting active cameras");

            List<CameraDTO> activeCameras = cameraService.getAll().stream()
                    .filter(CameraDTO::getActive)
                    .collect(Collectors.toList());

            List<CameraFlaskDTO> flaskCameras = activeCameras.stream()
                    .map(this::convertToCameraFlaskDTO)
                    .collect(Collectors.toList());

            log.info("Returning {} active cameras to Flask", flaskCameras.size());
            return ResponseEntity.ok(flaskCameras);

        } catch (Exception e) {
            log.error("Error getting active cameras for Flask: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy thông tin camera cụ thể với lịch học hiện tại
     * GET /api/flask/cameras/{id}
     */
    @GetMapping("/cameras/{id}")
    public ResponseEntity<CameraFlaskDTO> getCameraWithSchedule(@PathVariable Long id) {
        try {
            log.info("Flask requesting camera info: {}", id);

            CameraDTO camera = cameraService.getById(id);
            if (camera == null || !camera.getActive()) {
                return ResponseEntity.notFound().build();
            }

            CameraFlaskDTO flaskCamera = convertToCameraFlaskDTO(camera);

            // Add current schedule
            ScheduleFlaskDTO currentSchedule = realtimeAttendanceService.getCurrentScheduleForCamera(id);
            flaskCamera.setCurrentSchedule(currentSchedule);

            return ResponseEntity.ok(flaskCamera);

        } catch (Exception e) {
            log.error("Error getting camera {} for Flask: ", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy lịch học hiện tại cho camera
     * GET /api/flask/cameras/{id}/current-schedule
     */
    @GetMapping("/cameras/{id}/current-schedule")
    public ResponseEntity<ScheduleFlaskDTO> getCurrentSchedule(@PathVariable Long id) {
        try {
            log.info("Flask requesting current schedule for camera: {}", id);

            ScheduleFlaskDTO schedule = realtimeAttendanceService.getCurrentScheduleForCamera(id);

            if (schedule == null) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(schedule);

        } catch (Exception e) {
            log.error("Error getting current schedule for camera {}: ", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xử lý face detections từ Flask
     * POST /api/flask/face-detections
     */
    @PostMapping("/face-detections")
    public ResponseEntity<Map<String, Object>> processFaceDetections(@RequestBody FaceDetectionDTO detection) {
        try {
            log.info("Received face detection from Flask - Camera: {}, Faces: {}",
                    detection.getCameraId(), detection.getDetectedFaces().size());

            List<LiveRecognitionDTO> recognitions = faceDetectionService.processFaceDetection(detection);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "processedFaces", recognitions.size(),
                    "recognitions", recognitions,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing face detections from Flask: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Tạo attendance từ Flask
     * POST /api/flask/attendance
     */
    @PostMapping("/attendance")
    public ResponseEntity<Map<String, Object>> createAttendanceFromFlask(@RequestBody AttendanceFlaskDTO attendanceData) {
        try {
            log.info("Creating attendance from Flask - Student: {}, Camera: {}",
                    attendanceData.getMaSv(), attendanceData.getCameraId());

            // Convert và tạo attendance
            DiemDanhDTO dto = DiemDanhDTO.builder()
                    .maSv(attendanceData.getMaSv())
                    .maLich(attendanceData.getMaLich())
                    .ngayDiemDanh(attendanceData.getDetectionTime().toLocalDate())
                    .thoiGianVao(attendanceData.getDetectionTime().toLocalTime())
                    .trangThai(com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum.CO_MAT)
                    .build();

            DiemDanhDTO createdAttendance = diemDanhService.create(dto);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "attendanceId", createdAttendance.getId(),
                    "message", "Attendance created successfully",
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating attendance from Flask: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Lấy Flask configuration
     * GET /api/flask/config
     */
    @GetMapping("/config")
    public ResponseEntity<FlaskConfigDTO> getFlaskConfig() {
        try {
            log.info("Flask requesting configuration");

            FlaskConfigDTO config = flaskConfigurationService.getFlaskConfiguration();
            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("Error getting Flask configuration: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy live attendance cho phòng học
     * GET /api/flask/live-attendance/{roomId}
     */
    @GetMapping("/live-attendance/{roomId}")
    public ResponseEntity<List<DiemDanhDTO>> getLiveAttendance(@PathVariable String roomId) {
        try {
            log.info("Flask requesting live attendance for room: {}", roomId);

            // TODO: Implement logic to get current session attendance for room
            // For now, return empty list
            return ResponseEntity.ok(List.of());

        } catch (Exception e) {
            log.error("Error getting live attendance for room {}: ", roomId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Test endpoint cho Flask
     * GET /api/flask/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testFlaskConnection() {
        Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Flask integration API is working",
                "timestamp", System.currentTimeMillis(),
                "server", "Spring Boot Backend"
        );

        log.info("Flask test endpoint called");
        return ResponseEntity.ok(response);
    }

    // Helper method
    private CameraFlaskDTO convertToCameraFlaskDTO(CameraDTO camera) {
        return CameraFlaskDTO.builder()
                .id(camera.getId())
                .tenCamera(camera.getTenCamera())
                .rtspUrl(camera.getIpAddress()) // RTSP URL stored in ipAddress field
                .maPhong(camera.getMaPhong())
                .tenPhong(camera.getMaPhong()) // Nếu có trong DTO
                .isActive(camera.getActive())
                .hlsUrl("/streams/camera_" + camera.getId() + "/playlist.m3u8")
                .build();
    }
}