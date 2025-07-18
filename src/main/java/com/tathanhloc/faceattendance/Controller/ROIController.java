package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.CameraService;
import com.tathanhloc.faceattendance.Service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/roi")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5000")
public class ROIController {

    private final CameraService cameraService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    /**
     * Lấy ROI hiện tại của camera
     * GET /api/roi/camera/{cameraId}
     */
    @GetMapping("/camera/{cameraId}")
    public ResponseEntity<Map<String, ROIPolygonDTO>> getCameraROI(@PathVariable Long cameraId) {
        try {
            log.info("Getting ROI for camera: {}", cameraId);

            CameraDTO camera = cameraService.getById(cameraId);
            if (camera == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, ROIPolygonDTO> response = Map.of(
                    "roiIn", parseROIFromString(camera.getVungIn()),
                    "roiOut", parseROIFromString(camera.getVungOut())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting ROI for camera {}: ", cameraId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update ROI IN cho camera
     * POST /api/roi/camera/{cameraId}/in
     */
    @PostMapping("/camera/{cameraId}/in")
    public ResponseEntity<Map<String, Object>> updateROIIn(@PathVariable Long cameraId,
                                                           @RequestBody ROIPolygonDTO roiIn) {
        try {
            log.info("Updating ROI IN for camera: {}", cameraId);

            CameraDTO camera = cameraService.getById(cameraId);
            if (camera == null) {
                return ResponseEntity.notFound().build();
            }

            // Convert ROI to JSON string
            String roiJson = objectMapper.writeValueAsString(roiIn);
            camera.setVungIn(roiJson);

            // Update camera
            cameraService.update(cameraId, camera);

            // Broadcast ROI update
            webSocketService.broadcastROIUpdate(cameraId, roiIn);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "ROI IN updated successfully",
                    "cameraId", cameraId,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating ROI IN for camera {}: ", cameraId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Update ROI OUT cho camera
     * POST /api/roi/camera/{cameraId}/out
     */
    @PostMapping("/camera/{cameraId}/out")
    public ResponseEntity<Map<String, Object>> updateROIOut(@PathVariable Long cameraId,
                                                            @RequestBody ROIPolygonDTO roiOut) {
        try {
            log.info("Updating ROI OUT for camera: {}", cameraId);

            CameraDTO camera = cameraService.getById(cameraId);
            if (camera == null) {
                return ResponseEntity.notFound().build();
            }

            // Convert ROI to JSON string
            String roiJson = objectMapper.writeValueAsString(roiOut);
            camera.setVungOut(roiJson);

            // Update camera
            cameraService.update(cameraId, camera);

            // Broadcast ROI update
            webSocketService.broadcastROIUpdate(cameraId, roiOut);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "ROI OUT updated successfully",
                    "cameraId", cameraId,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating ROI OUT for camera {}: ", cameraId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Xóa ROI cho camera
     * DELETE /api/roi/camera/{cameraId}
     */
    @DeleteMapping("/camera/{cameraId}")
    public ResponseEntity<Map<String, Object>> clearCameraROI(@PathVariable Long cameraId,
                                                              @RequestParam(defaultValue = "both") String type) {
        try {
            log.info("Clearing ROI {} for camera: {}", type, cameraId);

            CameraDTO camera = cameraService.getById(cameraId);
            if (camera == null) {
                return ResponseEntity.notFound().build();
            }

            // Clear specified ROI type
            switch (type.toLowerCase()) {
                case "in":
                    camera.setVungIn(null);
                    break;
                case "out":
                    camera.setVungOut(null);
                    break;
                case "both":
                default:
                    camera.setVungIn(null);
                    camera.setVungOut(null);
                    break;
            }

            // Update camera
            cameraService.update(cameraId, camera);

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "ROI " + type + " cleared successfully",
                    "cameraId", cameraId,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error clearing ROI for camera {}: ", cameraId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Validate ROI polygon
     * POST /api/roi/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateROI(@RequestBody ROIPolygonDTO roi) {
        try {
            boolean isValid = validateROIPolygon(roi);

            Map<String, Object> response = Map.of(
                    "isValid", isValid,
                    "message", isValid ? "ROI is valid" : "ROI is invalid",
                    "pointCount", roi.getPoints() != null ? roi.getPoints().size() : 0,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating ROI: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "isValid", false,
                    "message", "Error validating ROI: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    // Helper methods
    private ROIPolygonDTO parseROIFromString(String roiJson) {
        try {
            if (roiJson == null || roiJson.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(roiJson, ROIPolygonDTO.class);
        } catch (Exception e) {
            log.error("Error parsing ROI JSON: ", e);
            return null;
        }
    }

    private boolean validateROIPolygon(ROIPolygonDTO roi) {
        if (roi == null || roi.getPoints() == null) {
            return false;
        }

        // Must have at least 3 points for a valid polygon
        if (roi.getPoints().size() < 3) {
            return false;
        }

        // Check if all points have valid coordinates
        return roi.getPoints().stream()
                .allMatch(point -> point.getX() != null && point.getY() != null
                        && point.getX() >= 0 && point.getY() >= 0);
    }
}
