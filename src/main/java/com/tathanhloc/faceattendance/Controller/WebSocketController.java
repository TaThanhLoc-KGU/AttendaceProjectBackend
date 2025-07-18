package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.FaceDetectionService;
import com.tathanhloc.faceattendance.Service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final FaceDetectionService faceDetectionService;
    private final WebSocketService webSocketService;

    /**
     * Handle face detection từ Flask
     * Client gửi: /app/face-detection
     * Server broadcast: /topic/face-detections
     */
    @MessageMapping("/face-detection")
    @SendTo("/topic/face-detections")
    public LiveRecognitionDTO handleFaceDetection(FaceDetectionDTO detection) {
        try {
            log.info("Received face detection via WebSocket from camera: {}", detection.getCameraId());

            // Process detection và return first recognition (if any)
            var recognitions = faceDetectionService.processFaceDetection(detection);

            return recognitions.isEmpty() ? null : recognitions.get(0);

        } catch (Exception e) {
            log.error("Error handling face detection via WebSocket: ", e);
            return null;
        }
    }

    /**
     * Handle attendance update
     * Client gửi: /app/attendance-update
     * Server broadcast: /topic/attendance-updates
     */
    @MessageMapping("/attendance-update")
    @SendTo("/topic/attendance-updates")
    public AttendanceEventDTO handleAttendanceUpdate(DiemDanhDTO attendance) {
        try {
            log.info("Broadcasting attendance update for student: {}", attendance.getMaSv());

            return AttendanceEventDTO.builder()
                    .attendanceId(attendance.getId())
                    .maSv(attendance.getMaSv())
                    .maLich(attendance.getMaLich())
                    .timestamp(java.time.LocalDateTime.now())
                    .status(attendance.getTrangThai().name())
                    .build();

        } catch (Exception e) {
            log.error("Error handling attendance update via WebSocket: ", e);
            return null;
        }
    }

    /**
     * Handle camera status change
     * Client gửi: /app/camera-status
     * Server broadcast: /topic/camera-status
     */
    @MessageMapping("/camera-status")
    @SendTo("/topic/camera-status")
    public CameraStatusEventDTO handleCameraStatus(CameraStatusEventDTO statusEvent) {
        try {
            log.info("Broadcasting camera status: {} for camera: {}",
                    statusEvent.getStatus(), statusEvent.getCameraId());

            return statusEvent;

        } catch (Exception e) {
            log.error("Error handling camera status via WebSocket: ", e);
            return null;
        }
    }

    /**
     * Handle ROI update
     * Client gửi: /app/roi-update
     * Server broadcast: /topic/roi-updates
     */
    @MessageMapping("/roi-update")
    @SendTo("/topic/roi-updates")
    public ROIUpdateEventDTO handleROIUpdate(ROIUpdateEventDTO roiEvent) {
        try {
            log.info("Broadcasting ROI update for camera: {}", roiEvent.getCameraId());

            return roiEvent;

        } catch (Exception e) {
            log.error("Error handling ROI update via WebSocket: ", e);
            return null;
        }
    }

    /**
     * Handle system status request
     * Client gửi: /app/system-status
     * Server phản hồi riêng: /user/queue/system-status
     */
    @MessageMapping("/system-status")
    @SendToUser("/queue/system-status")
    public SystemStatusDTO handleSystemStatusRequest() {
        try {
            log.info("System status requested via WebSocket");

            // TODO: Get actual system status
            return SystemStatusDTO.builder()
                    .status("HEALTHY")
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error handling system status request via WebSocket: ", e);
            return null;
        }
    }
}