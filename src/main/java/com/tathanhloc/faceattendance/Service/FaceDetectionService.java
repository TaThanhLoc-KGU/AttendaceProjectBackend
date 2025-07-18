package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.Camera;
import com.tathanhloc.faceattendance.Model.SinhVien;
import com.tathanhloc.faceattendance.Repository.CameraRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceDetectionService {

    private final SinhVienRepository sinhVienRepository;
    private final CameraRepository cameraRepository;
    private final WebSocketService webSocketService;
    private final RealtimeAttendanceService realtimeAttendanceService;

    /**
     * Xử lý face detection results từ Flask
     */
    public List<LiveRecognitionDTO> processFaceDetection(FaceDetectionDTO detection) {
        log.info("Processing face detection from camera: {}", detection.getCameraId());

        List<LiveRecognitionDTO> recognitions = new ArrayList<>();

        try {
            // Verify camera exists và active
            Camera camera = cameraRepository.findById(detection.getCameraId())
                    .orElseThrow(() -> new RuntimeException("Camera not found: " + detection.getCameraId()));

            if (!camera.getIsActive()) {
                log.warn("Received detection from inactive camera: {}", detection.getCameraId());
                return recognitions;
            }

            // Parse ROI data
            ROIPolygonDTO roiIn = parseROI(camera.getVungIn());
            ROIPolygonDTO roiOut = parseROI(camera.getVungOut());

            // Process each detected face
            for (BoundingBoxDTO face : detection.getDetectedFaces()) {
                LiveRecognitionDTO recognition = processSingleFace(detection, face, camera, roiIn, roiOut);
                if (recognition != null) {
                    recognitions.add(recognition);

                    // Send real-time update
                    webSocketService.broadcastFaceDetection(recognition);
                }
            }

            log.info("Processed {} faces from camera {}", recognitions.size(), detection.getCameraId());

        } catch (Exception e) {
            log.error("Error processing face detection: ", e);
        }

        return recognitions;
    }

    /**
     * Xử lý một khuôn mặt được detect
     */
    private LiveRecognitionDTO processSingleFace(FaceDetectionDTO detection, BoundingBoxDTO face,
                                                 Camera camera, ROIPolygonDTO roiIn, ROIPolygonDTO roiOut) {
        try {
            // Identify person từ embedding
            String maSv = identifyPersonFromEmbedding(face.getEmbedding());

            if (maSv == null) {
                log.debug("Unknown person detected on camera {}", detection.getCameraId());
                return null;
            }

            // Get student info
            Optional<SinhVien> sinhVienOpt = sinhVienRepository.findById(maSv);
            if (sinhVienOpt.isEmpty()) {
                log.warn("Student not found: {}", maSv);
                return null;
            }

            SinhVien sinhVien = sinhVienOpt.get();

            // Check ROI
            String roiType = determineROIType(face, roiIn, roiOut);
            Boolean isInROI = roiType != null;

            LiveRecognitionDTO recognition = LiveRecognitionDTO.builder()
                    .cameraId(detection.getCameraId())
                    .maSv(maSv)
                    .hoTen(sinhVien.getHoTen())
                    .confidence(face.getConfidence())
                    .boundingBox(face)
                    .recognitionTime(detection.getTimestamp())
                    .roiType(roiType)
                    .isInROI(isInROI)
                    .embeddings(face.getEmbedding())
                    .build();

            // Process attendance if person is in ROI
            if (isInROI) {
                realtimeAttendanceService.processLiveRecognition(recognition);
            }

            return recognition;

        } catch (Exception e) {
            log.error("Error processing single face: ", e);
            return null;
        }
    }

    /**
     * Nhận diện người từ face embedding
     */
    private String identifyPersonFromEmbedding(String embeddingBase64) {
        try {
            if (embeddingBase64 == null || embeddingBase64.isEmpty()) {
                return null;
            }

            // Decode embedding
            byte[] embeddingBytes = Base64.getDecoder().decode(embeddingBase64);

            // TODO: Implement embedding comparison logic
            // So sánh với embeddings trong database
            // Trả về maSv của người có embedding gần nhất (threshold > 0.6)

            // Tạm thời mock logic
            List<SinhVien> allStudents = sinhVienRepository.findAll();
            for (SinhVien sv : allStudents) {
                if (sv.getEmbedding() != null && !sv.getEmbedding().isEmpty()) {
                    // TODO: Implement cosine similarity comparison
                    // double similarity = calculateCosineSimilarity(embeddingBytes, sv.getEmbedding());
                    // if (similarity > 0.6) return sv.getMaSv();
                }
            }

            return null; // Unknown person

        } catch (Exception e) {
            log.error("Error identifying person from embedding: ", e);
            return null;
        }
    }

    /**
     * Xác định loại ROI (IN/OUT) mà khuôn mặt nằm trong
     */
    private String determineROIType(BoundingBoxDTO face, ROIPolygonDTO roiIn, ROIPolygonDTO roiOut) {
        // Calculate face center point
        PointDTO faceCenter = PointDTO.builder()
                .x(face.getX() + face.getWidth() / 2)
                .y(face.getY() + face.getHeight() / 2)
                .build();

        // Check if in ROI IN
        if (roiIn != null && isPointInPolygon(faceCenter, roiIn.getPoints())) {
            return "IN";
        }

        // Check if in ROI OUT
        if (roiOut != null && isPointInPolygon(faceCenter, roiOut.getPoints())) {
            return "OUT";
        }

        return null; // Not in any ROI
    }

    /**
     * Parse ROI string từ database thành ROIPolygonDTO
     */
    private ROIPolygonDTO parseROI(String roiJson) {
        try {
            if (roiJson == null || roiJson.isEmpty()) {
                return null;
            }

            // TODO: Parse JSON string thành ROIPolygonDTO
            // ObjectMapper mapper = new ObjectMapper();
            // return mapper.readValue(roiJson, ROIPolygonDTO.class);

            return null; // Implement JSON parsing

        } catch (Exception e) {
            log.error("Error parsing ROI: ", e);
            return null;
        }
    }

    /**
     * Kiểm tra một điểm có nằm trong polygon không (Ray casting algorithm)
     */
    private boolean isPointInPolygon(PointDTO point, List<PointDTO> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        boolean inside = false;
        int j = polygon.size() - 1;

        for (int i = 0; i < polygon.size(); i++) {
            if (((polygon.get(i).getY() > point.getY()) != (polygon.get(j).getY() > point.getY())) &&
                    (point.getX() < (polygon.get(j).getX() - polygon.get(i).getX()) *
                            (point.getY() - polygon.get(i).getY()) / (polygon.get(j).getY() - polygon.get(i).getY()) +
                            polygon.get(i).getX())) {
                inside = !inside;
            }
            j = i;
        }

        return inside;
    }
}