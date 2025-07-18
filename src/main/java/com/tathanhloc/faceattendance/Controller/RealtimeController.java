package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5000")
public class RealtimeController {

    private final RealtimeAttendanceService realtimeAttendanceService;
    private final WebSocketService webSocketService;
    private final FlaskConfigurationService flaskConfigurationService;

    /**
     * Bắt đầu session điểm danh
     * POST /api/realtime/sessions/start
     */
    @PostMapping("/sessions/start")
    public ResponseEntity<Map<String, Object>> startAttendanceSession(@RequestBody Map<String, Object> request) {
        try {
            String maLich = (String) request.get("maLich");
            Long cameraId = Long.valueOf(request.get("cameraId").toString());

            log.info("Starting attendance session - Schedule: {}, Camera: {}", maLich, cameraId);

            realtimeAttendanceService.startAttendanceSession(maLich, cameraId);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Attendance session started",
                    "maLich", maLich,
                    "cameraId", cameraId,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error starting attendance session: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Kết thúc session điểm danh
     * POST /api/realtime/sessions/end
     */
    @PostMapping("/sessions/end")
    public ResponseEntity<Map<String, Object>> endAttendanceSession(@RequestBody Map<String, String> request) {
        try {
            String maLich = request.get("maLich");

            log.info("Ending attendance session - Schedule: {}", maLich);

            realtimeAttendanceService.endAttendanceSession(maLich);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Attendance session ended",
                    "maLich", maLich,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error ending attendance session: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Lấy live stats cho phòng học
     * GET /api/realtime/stats/{roomId}
     */
    @GetMapping("/stats/{roomId}")
    public ResponseEntity<Map<String, Object>> getLiveStats(@PathVariable String roomId) {
        try {
            log.info("Getting live stats for room: {}", roomId);

            // TODO: Implement live stats calculation
            Map<String, Object> stats = Map.of(
                    "roomId", roomId,
                    "totalStudents", 0,
                    "presentStudents", 0,
                    "attendanceRate", 0.0,
                    "lastUpdate", System.currentTimeMillis()
            );

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting live stats for room {}: ", roomId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy system status
     * GET /api/realtime/system-status
     */
    @GetMapping("/system-status")
    public ResponseEntity<SystemStatusDTO> getSystemStatus() {
        try {
            log.info("Getting system status");

            SystemStatusDTO status = flaskConfigurationService.getSystemStatus();
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error getting system status: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     * GET /api/realtime/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "services", Map.of(
                        "database", "UP",
                        "websocket", "UP",
                        "face_recognition", "UP"
                )
        );

        return ResponseEntity.ok(health);
    }
}