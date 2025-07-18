package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send face detection event to specific room/camera subscribers
     */
    @Async
    public void sendFaceDetectionToRoom(String roomId, FaceDetectionEventDTO event) {
        try {
            String destination = "/topic/room/" + roomId + "/face-detections";
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Sent face detection event to room: {}", roomId);
        } catch (Exception e) {
            log.error("Error sending face detection event to room {}: ", roomId, e);
        }
    }

    /**
     * Send attendance event to specific room subscribers
     */
    @Async
    public void sendAttendanceToRoom(String roomId, AttendanceEventDTO event) {
        try {
            String destination = "/topic/room/" + roomId + "/attendance";
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Sent attendance event to room: {}", roomId);
        } catch (Exception e) {
            log.error("Error sending attendance event to room {}: ", roomId, e);
        }
    }

    /**
     * Send camera status to all Flask clients
     */
    @Async
    public void sendCameraStatusToFlask(CameraStatusEventDTO event) {
        try {
            messagingTemplate.convertAndSend("/topic/flask/camera-status", event);
            log.debug("Sent camera status to Flask clients: {}", event.getCameraId());
        } catch (Exception e) {
            log.error("Error sending camera status to Flask: ", e);
        }
    }

    /**
     * Send system-wide notification
     */
    @Async
    public void sendSystemNotification(String message, String type) {
        try {
            SystemNotificationDTO notification = SystemNotificationDTO.builder()
                    .message(message)
                    .type(type) // "INFO", "WARNING", "ERROR", "SUCCESS"
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/system/notifications", notification);
            log.debug("Sent system notification: {} - {}", type, message);
        } catch (Exception e) {
            log.error("Error sending system notification: ", e);
        }
    }

    /**
     * Send live statistics update
     */
    @Async
    public void sendLiveStatistics(String roomId, Map<String, Object> stats) {
        try {
            String destination = "/topic/room/" + roomId + "/statistics";
            messagingTemplate.convertAndSend(destination, stats);
            log.debug("Sent live statistics to room: {}", roomId);
        } catch (Exception e) {
            log.error("Error sending live statistics to room {}: ", roomId, e);
        }
    }
}