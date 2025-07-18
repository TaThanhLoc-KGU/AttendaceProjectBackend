package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast face detection event
     */
    public void broadcastFaceDetection(LiveRecognitionDTO recognition) {
        try {
            messagingTemplate.convertAndSend("/topic/face-detections", recognition);
            log.debug("Broadcasted face detection for student: {}", recognition.getMaSv());
        } catch (Exception e) {
            log.error("Error broadcasting face detection: ", e);
        }
    }

    /**
     * Broadcast attendance update event
     */
    public void broadcastAttendanceUpdate(DiemDanhDTO attendance) {
        try {
            messagingTemplate.convertAndSend("/topic/attendance-updates", attendance);
            log.debug("Broadcasted attendance update for student: {}", attendance.getMaSv());
        } catch (Exception e) {
            log.error("Error broadcasting attendance update: ", e);
        }
    }

    /**
     * Broadcast camera status change
     */
    public void broadcastCameraStatus(Long cameraId, String status) {
        try {
            CameraStatusEventDTO event = CameraStatusEventDTO.builder()
                    .cameraId(cameraId)
                    .status(status)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/camera-status", event);
            log.debug("Broadcasted camera status: {} for camera: {}", status, cameraId);
        } catch (Exception e) {
            log.error("Error broadcasting camera status: ", e);
        }
    }

    /**
     * Broadcast ROI update event
     */
    public void broadcastROIUpdate(Long cameraId, ROIPolygonDTO roi) {
        try {
            ROIUpdateEventDTO event = ROIUpdateEventDTO.builder()
                    .cameraId(cameraId)
                    .roi(roi)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/roi-updates", event);
            log.debug("Broadcasted ROI update for camera: {}", cameraId);
        } catch (Exception e) {
            log.error("Error broadcasting ROI update: ", e);
        }
    }

    /**
     * Send system status update
     */
    public void broadcastSystemStatus(SystemStatusDTO status) {
        try {
            messagingTemplate.convertAndSend("/topic/system-status", status);
            log.debug("Broadcasted system status: {}", status.getStatus());
        } catch (Exception e) {
            log.error("Error broadcasting system status: ", e);
        }
    }
}