package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlaskConfigurationService {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.websocket.endpoint:/ws}")
    private String websocketEndpoint;

    private final CameraService cameraService;
    private final SinhVienService sinhVienService;

    /**
     * Lấy configuration cho Flask app
     */
    public FlaskConfigDTO getFlaskConfiguration() {
        try {
            String backendUrl = "http://localhost:" + serverPort;
            String websocketUrl = "ws://localhost:" + serverPort + websocketEndpoint;

            RecognitionSettingsDTO recognitionSettings = RecognitionSettingsDTO.builder()
                    .recognitionThreshold(0.6)
                    .detectionThreshold(0.5)
                    .minFaceSize(40)
                    .maxFaceSize(1920)
                    .enableAgeGenderDetection(true)
                    .trackingBufferSize(30)
                    .build();

            SystemStatusDTO systemStatus = getSystemStatus();

            return FlaskConfigDTO.builder()
                    .backendApiUrl(backendUrl + "/api")
                    .websocketUrl(websocketUrl)
                    .recognitionSettings(recognitionSettings)
                    .systemStatus(systemStatus)
                    .maxConcurrentStreams(4)
                    .frameProcessingInterval(3) // Process every 3rd frame
                    .build();

        } catch (Exception e) {
            log.error("Error getting Flask configuration: ", e);
            throw new RuntimeException("Cannot get Flask configuration", e);
        }
    }

    /**
     * Lấy system status
     */
    public SystemStatusDTO getSystemStatus() {
        try {
            Map<String, Object> cameraStatuses = new HashMap<>();
            Map<String, Object> serviceStatuses = new HashMap<>();

            // Camera statuses
            cameraService.getAll().forEach(camera -> {
                cameraStatuses.put("camera_" + camera.getId(),
                        Map.of("active", camera.getActive(), "name", camera.getTenCamera()));
            });

            // Service statuses
            serviceStatuses.put("database", "HEALTHY");
            serviceStatuses.put("face_recognition", "HEALTHY");
            serviceStatuses.put("websocket", "HEALTHY");

            Long totalStudents = (long) sinhVienService.getAll().size();

            return SystemStatusDTO.builder()
                    .status("HEALTHY")
                    .timestamp(LocalDateTime.now())
                    .cameraStatuses(cameraStatuses)
                    .serviceStatuses(serviceStatuses)
                    .totalStudentsEnrolled(totalStudents)
                    .activeAttendanceSessions(0L) // TODO: Calculate active sessions
                    .systemLoad(0.5) // TODO: Calculate actual system load
                    .build();

        } catch (Exception e) {
            log.error("Error getting system status: ", e);
            return SystemStatusDTO.builder()
                    .status("DEGRADED")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}