import cv2
import asyncio
import threading
from typing import Dict, Optional, Union
from loguru import logger
from datetime import datetime

from ..config.settings import settings


class CameraManager:
    """
    Quản lý camera cho nhận diện khuôn mặt real-time
    Hỗ trợ webcam và IP camera
    """

    def __init__(self):
        self.cap = None
        self.is_initialized = False
        self.current_config = None
        self.frame_lock = threading.Lock()
        self.last_frame = None
        self.last_capture_time = None

    async def initialize_camera(self, camera_config: Dict) -> Dict:
        """
        Khởi tạo camera

        Args:
            camera_config: Cấu hình camera
                {
                    'camera_id': str,
                    'source': str/int,  # 0, 1, 'rtsp://...'
                    'width': int (optional),
                    'height': int (optional),
                    'fps': int (optional)
                }
        """
        try:
            if self.is_initialized:
                await self.release_camera()

            camera_source = camera_config['source']

            # Convert string numbers to int
            if isinstance(camera_source, str) and camera_source.isdigit():
                camera_source = int(camera_source)

            logger.info(f"Initializing camera: {camera_source}")

            # Initialize VideoCapture
            self.cap = cv2.VideoCapture(camera_source)

            if not self.cap.isOpened():
                return {
                    'success': False,
                    'error': f"Cannot open camera: {camera_source}"
                }

            # Set camera properties
            width = camera_config.get('width', settings.CAMERA_WIDTH)
            height = camera_config.get('height', settings.CAMERA_HEIGHT)
            fps = camera_config.get('fps', settings.CAMERA_FPS)

            self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)
            self.cap.set(cv2.CAP_PROP_FPS, fps)

            # Test capture
            ret, frame = self.cap.read()
            if not ret:
                self.cap.release()
                return {
                    'success': False,
                    'error': "Cannot capture frame from camera"
                }

            # Get actual properties
            actual_width = int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            actual_height = int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            actual_fps = int(self.cap.get(cv2.CAP_PROP_FPS))

            self.is_initialized = True
            self.current_config = camera_config
            self.last_frame = frame
            self.last_capture_time = datetime.now()

            logger.success(f"Camera initialized successfully: {actual_width}x{actual_height}@{actual_fps}fps")

            return {
                'success': True,
                'camera_id': camera_config['camera_id'],
                'source': camera_source,
                'resolution': {
                    'width': actual_width,
                    'height': actual_height
                },
                'fps': actual_fps,
                'timestamp': self.last_capture_time.isoformat()
            }

        except Exception as e:
            logger.error(f"Error initializing camera: {e}")
            if self.cap:
                self.cap.release()
            return {
                'success': False,
                'error': str(e)
            }

    async def capture_frame(self) -> Dict:
        """
        Capture frame từ camera

        Returns:
            Dict chứa frame và metadata
        """
        try:
            if not self.is_initialized or not self.cap:
                return {
                    'success': False,
                    'error': 'Camera not initialized'
                }

            with self.frame_lock:
                ret, frame = self.cap.read()

                if not ret:
                    logger.warning("Failed to capture frame")
                    return {
                        'success': False,
                        'error': 'Failed to capture frame'
                    }

                self.last_frame = frame.copy()
                self.last_capture_time = datetime.now()

            return {
                'success': True,
                'frame': frame,
                'shape': frame.shape,
                'timestamp': self.last_capture_time.isoformat()
            }

        except Exception as e:
            logger.error(f"Error capturing frame: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    async def get_current_frame(self) -> Optional[Dict]:
        """Lấy frame hiện tại mà không capture mới"""
        try:
            if not self.is_initialized or self.last_frame is None:
                return None

            with self.frame_lock:
                frame = self.last_frame.copy()
                timestamp = self.last_capture_time

            return {
                'success': True,
                'frame': frame,
                'shape': frame.shape,
                'timestamp': timestamp.isoformat() if timestamp else None
            }

        except Exception as e:
            logger.error(f"Error getting current frame: {e}")
            return None

    async def release_camera(self):
        """Giải phóng camera"""
        try:
            if self.cap:
                logger.info("Releasing camera")
                self.cap.release()
                self.cap = None

            self.is_initialized = False
            self.current_config = None
            self.last_frame = None
            self.last_capture_time = None

            logger.info("Camera released successfully")

        except Exception as e:
            logger.error(f"Error releasing camera: {e}")

    async def get_status(self) -> Dict:
        """Lấy trạng thái camera"""
        try:
            if not self.is_initialized:
                return {
                    'initialized': False,
                    'camera_id': None,
                    'source': None
                }

            # Test camera by capturing a frame
            test_result = await self.capture_frame()
            is_working = test_result['success']

            status = {
                'initialized': True,
                'working': is_working,
                'camera_id': self.current_config.get('camera_id') if self.current_config else None,
                'source': self.current_config.get('source') if self.current_config else None,
                'last_capture': self.last_capture_time.isoformat() if self.last_capture_time else None
            }

            if self.cap:
                try:
                    status.update({
                        'width': int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH)),
                        'height': int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT)),
                        'fps': int(self.cap.get(cv2.CAP_PROP_FPS))
                    })
                except:
                    pass

            return status

        except Exception as e:
            logger.error(f"Error getting camera status: {e}")
            return {
                'initialized': False,
                'error': str(e)
            }

    async def test_camera_source(self, source: Union[str, int]) -> Dict:
        """
        Test camera source mà không khởi tạo chính thức

        Args:
            source: Camera source để test

        Returns:
            Dict kết quả test
        """
        try:
            # Convert string numbers to int
            if isinstance(source, str) and source.isdigit():
                source = int(source)

            logger.info(f"Testing camera source: {source}")

            # Test capture
            test_cap = cv2.VideoCapture(source)

            if not test_cap.isOpened():
                return {
                    'success': False,
                    'source': source,
                    'error': 'Cannot open camera source'
                }

            # Try to capture a frame
            ret, frame = test_cap.read()

            if not ret or frame is None:
                test_cap.release()
                return {
                    'success': False,
                    'source': source,
                    'error': 'Cannot capture frame'
                }

            # Get camera properties
            width = int(test_cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            height = int(test_cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            fps = int(test_cap.get(cv2.CAP_PROP_FPS))

            test_cap.release()

            return {
                'success': True,
                'source': source,
                'resolution': {
                    'width': width,
                    'height': height
                },
                'fps': fps,
                'frame_shape': frame.shape
            }

        except Exception as e:
            logger.error(f"Error testing camera source {source}: {e}")
            return {
                'success': False,
                'source': source,
                'error': str(e)
            }

    async def list_available_cameras(self) -> Dict:
        """Liệt kê các camera có sẵn"""
        try:
            available_cameras = []

            # Test webcams (0-4)
            for i in range(5):
                result = await self.test_camera_source(i)
                if result['success']:
                    available_cameras.append({
                        'type': 'webcam',
                        'source': i,
                        'name': f'Webcam {i}',
                        **result
                    })

            return {
                'success': True,
                'total': len(available_cameras),
                'cameras': available_cameras
            }

        except Exception as e:
            logger.error(f"Error listing available cameras: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    def __del__(self):
        """Cleanup when object is destroyed"""
        try:
            if hasattr(self, 'cap') and self.cap:
                self.cap.release()
        except:
            pass