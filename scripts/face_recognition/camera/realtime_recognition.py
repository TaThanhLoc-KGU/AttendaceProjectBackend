import cv2
import numpy as np
import asyncio
import time
from typing import Dict, List, Optional, Tuple
from loguru import logger
from datetime import datetime, timedelta
import pickle

from ..config.settings import settings
from ..models.face_encoder import face_encoder
from ..services.backend_api import BackendAPIService
from ..utils.image_processing import ImageProcessor
from .camera_manager import CameraManager


class RealtimeRecognitionService:
    """
    Service nhận diện khuôn mặt real-time và điểm danh tự động
    """

    def __init__(self):
        self.camera_manager = CameraManager()
        self.backend_api = BackendAPIService()
        self.image_processor = ImageProcessor()

        # Recognition state
        self.is_running = False
        self.student_embeddings = {}
        self.last_recognition_time = {}
        self.attendance_cooldown = {}

        # Statistics
        self.stats = {
            'total_recognitions': 0,
            'successful_attendance': 0,
            'failed_attempts': 0,
            'start_time': None
        }

    async def start_recognition(self, camera_config: Dict) -> Dict:
        """
        Bắt đầu nhận diện real-time

        Args:
            camera_config: Cấu hình camera
                {
                    'camera_id': str,
                    'source': str/int,  # Camera source (0, 1, 'rtsp://...')
                    'ma_lop': str,      # Mã lớp đang học
                    'detection_area': Optional[Dict]  # Vùng detection
                }
        """
        try:
            if self.is_running:
                return {
                    'success': False,
                    'error': 'Recognition is already running'
                }

            logger.info(f"Starting real-time recognition for camera {camera_config['camera_id']}")

            # 1. Load student embeddings
            await self._load_student_embeddings()

            if not self.student_embeddings:
                return {
                    'success': False,
                    'error': 'No student embeddings found'
                }

            # 2. Initialize camera
            camera_init = await self.camera_manager.initialize_camera(camera_config)
            if not camera_init['success']:
                return camera_init

            # 3. Start recognition loop
            self.is_running = True
            self.stats['start_time'] = datetime.now()

            # Chạy recognition loop trong background
            asyncio.create_task(self._recognition_loop(camera_config))

            return {
                'success': True,
                'message': 'Real-time recognition started',
                'camera_id': camera_config['camera_id'],
                'total_students': len(self.student_embeddings),
                'timestamp': datetime.now().isoformat()
            }

        except Exception as e:
            logger.error(f"Error starting recognition: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    async def stop_recognition(self) -> Dict:
        """Dừng nhận diện real-time"""
        try:
            if not self.is_running:
                return {
                    'success': False,
                    'error': 'Recognition is not running'
                }

            logger.info("Stopping real-time recognition")

            self.is_running = False
            await self.camera_manager.release_camera()

            # Tính toán statistics
            duration = datetime.now() - self.stats['start_time']

            return {
                'success': True,
                'message': 'Real-time recognition stopped',
                'duration_seconds': duration.total_seconds(),
                'statistics': self.stats,
                'timestamp': datetime.now().isoformat()
            }

        except Exception as e:
            logger.error(f"Error stopping recognition: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    async def _recognition_loop(self, camera_config: Dict):
        """Vòng lặp nhận diện chính"""
        try:
            camera_id = camera_config['camera_id']
            ma_lop = camera_config['ma_lop']
            detection_area = camera_config.get('detection_area')

            logger.info(f"Starting recognition loop for camera {camera_id}")

            while self.is_running:
                try:
                    # 1. Capture frame from camera
                    frame_result = await self.camera_manager.capture_frame()

                    if not frame_result['success']:
                        logger.warning(f"Failed to capture frame: {frame_result.get('error')}")
                        await asyncio.sleep(1.0)
                        continue

                    frame = frame_result['frame']

                    # 2. Apply detection area if specified
                    if detection_area:
                        frame = self._apply_detection_area(frame, detection_area)

                    # 3. Detect and recognize faces
                    recognition_result = await self._recognize_faces_in_frame(
                        frame, ma_lop, camera_id
                    )

                    # 4. Display results (optional, for debugging)
                    if settings.API_RELOAD:  # Only in debug mode
                        self._draw_recognition_results(frame, recognition_result)
                        cv2.imshow(f'Recognition - Camera {camera_id}', frame)
                        if cv2.waitKey(1) & 0xFF == ord('q'):
                            break

                    # 5. Wait before next recognition
                    await asyncio.sleep(settings.RECOGNITION_INTERVAL)

                except Exception as e:
                    logger.error(f"Error in recognition loop: {e}")
                    await asyncio.sleep(1.0)

            cv2.destroyAllWindows()
            logger.info("Recognition loop ended")

        except Exception as e:
            logger.error(f"Fatal error in recognition loop: {e}")
            self.is_running = False

    async def _recognize_faces_in_frame(self, frame: np.ndarray, ma_lop: str, camera_id: str) -> Dict:
        """Nhận diện khuôn mặt trong frame"""
        try:
            # 1. Detect faces
            faces = face_encoder.detect_faces(frame)

            if not faces:
                return {'faces_detected': 0, 'recognitions': []}

            # 2. Recognize each face
            recognitions = []

            for face in faces:
                embedding = face['embedding']

                # Find best match
                best_match = self._find_best_match(embedding)

                if best_match:
                    ma_sv = best_match['ma_sv']
                    similarity = best_match['similarity']
                    confidence = face['confidence']

                    # Check cooldown
                    if self._check_attendance_cooldown(ma_sv):
                        # Record attendance
                        attendance_result = await self._record_attendance(
                            ma_sv, ma_lop, camera_id
                        )

                        recognitions.append({
                            'ma_sv': ma_sv,
                            'student_name': best_match.get('student_name', 'Unknown'),
                            'similarity': similarity,
                            'confidence': confidence,
                            'bbox': face['bbox'].tolist(),
                            'attendance_recorded': attendance_result,
                            'timestamp': datetime.now().isoformat()
                        })

                        if attendance_result:
                            self.stats['successful_attendance'] += 1
                            self.attendance_cooldown[ma_sv] = datetime.now()
                            logger.info(f"Attendance recorded for {ma_sv} (similarity: {similarity:.3f})")
                        else:
                            self.stats['failed_attempts'] += 1
                    else:
                        recognitions.append({
                            'ma_sv': ma_sv,
                            'student_name': best_match.get('student_name', 'Unknown'),
                            'similarity': similarity,
                            'confidence': confidence,
                            'bbox': face['bbox'].tolist(),
                            'attendance_recorded': False,
                            'message': 'Attendance cooldown active',
                            'timestamp': datetime.now().isoformat()
                        })

            self.stats['total_recognitions'] += len(recognitions)

            return {
                'faces_detected': len(faces),
                'recognitions': recognitions
            }

        except Exception as e:
            logger.error(f"Error recognizing faces: {e}")
            return {'faces_detected': 0, 'recognitions': [], 'error': str(e)}

    def _find_best_match(self, embedding: np.ndarray) -> Optional[Dict]:
        """Tìm sinh viên khớp nhất với embedding"""
        best_match = None
        best_similarity = 0.0

        for ma_sv, student_data in self.student_embeddings.items():
            student_embedding = student_data['embedding']
            similarity = face_encoder.compare_embeddings(embedding, student_embedding)

            if similarity > best_similarity and similarity >= settings.RECOGNITION_THRESHOLD:
                best_similarity = similarity
                best_match = {
                    'ma_sv': ma_sv,
                    'similarity': similarity,
                    'student_name': student_data.get('student_name', 'Unknown')
                }

        return best_match

    def _check_attendance_cooldown(self, ma_sv: str) -> bool:
        """Kiểm tra cooldown điểm danh"""
        if ma_sv not in self.attendance_cooldown:
            return True

        last_attendance = self.attendance_cooldown[ma_sv]
        cooldown_end = last_attendance + timedelta(seconds=settings.ATTENDANCE_COOLDOWN)

        return datetime.now() > cooldown_end

    async def _record_attendance(self, ma_sv: str, ma_lop: str, camera_id: str) -> bool:
        """Ghi nhận điểm danh"""
        try:
            return await self.backend_api.record_attendance(ma_sv, ma_lop, camera_id)
        except Exception as e:
            logger.error(f"Error recording attendance for {ma_sv}: {e}")
            return False

    async def _load_student_embeddings(self):
        """Load embeddings của tất cả sinh viên từ module data directory"""
        try:
            # 1. Load from local embedding files
            embeddings_dir = settings.EMBEDDINGS_DIR
            local_embeddings = {}

            if embeddings_dir.exists():
                for embedding_file in embeddings_dir.glob("*.pkl"):
                    try:
                        with open(embedding_file, 'rb') as f:
                            data = pickle.load(f)
                            ma_sv = data['ma_sv']
                            local_embeddings[ma_sv] = {
                                'embedding': data['embedding'],
                                'timestamp': data.get('timestamp'),
                                'source': 'local_file'
                            }
                    except Exception as e:
                        logger.warning(f"Error loading embedding file {embedding_file}: {e}")

            # 2. Load student info from backend
            students = await self.backend_api.get_active_students()

            # 3. Combine data
            for student in students:
                ma_sv = student['maSv']
                if ma_sv in local_embeddings:
                    local_embeddings[ma_sv]['student_name'] = student.get('hoTen', 'Unknown')
                    local_embeddings[ma_sv]['ma_lop'] = student.get('maLop')

            self.student_embeddings = local_embeddings
            logger.info(f"Loaded {len(self.student_embeddings)} student embeddings")

        except Exception as e:
            logger.error(f"Error loading student embeddings: {e}")
            self.student_embeddings = {}

    def _apply_detection_area(self, frame: np.ndarray, detection_area: Dict) -> np.ndarray:
        """Áp dụng vùng detection"""
        try:
            x, y, w, h = detection_area['x'], detection_area['y'], detection_area['width'], detection_area['height']
            return frame[y:y + h, x:x + w]
        except Exception as e:
            logger.warning(f"Error applying detection area: {e}")
            return frame

    def _draw_recognition_results(self, frame: np.ndarray, results: Dict):
        """Vẽ kết quả nhận diện lên frame"""
        try:
            for recognition in results.get('recognitions', []):
                bbox = recognition['bbox']
                ma_sv = recognition['ma_sv']
                similarity = recognition['similarity']

                # Draw bounding box
                x1, y1, x2, y2 = bbox
                cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)

                # Draw text
                text = f"{ma_sv} ({similarity:.3f})"
                cv2.putText(frame, text, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

        except Exception as e:
            logger.warning(f"Error drawing recognition results: {e}")

    async def get_recognition_status(self) -> Dict:
        """Lấy trạng thái nhận diện"""
        return {
            'is_running': self.is_running,
            'total_students': len(self.student_embeddings),
            'statistics': self.stats,
            'camera_status': await self.camera_manager.get_status(),
            'timestamp': datetime.now().isoformat()
        }