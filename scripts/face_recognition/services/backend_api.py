import aiohttp
import asyncio
from typing import Dict, List, Optional
from loguru import logger
import json

from ..config.settings import settings


class BackendAPIService:
    """
    Service để giao tiếp với Spring Boot backend API
    """

    def __init__(self):
        self.base_url = settings.BACKEND_API_URL
        self.timeout = aiohttp.ClientTimeout(total=settings.BACKEND_API_TIMEOUT)
        self.session = None

    async def __aenter__(self):
        """Async context manager entry"""
        self.session = aiohttp.ClientSession(timeout=self.timeout)
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit"""
        if self.session:
            await self.session.close()

    async def _get_session(self):
        """Get or create aiohttp session"""
        if self.session is None or self.session.closed:
            self.session = aiohttp.ClientSession(timeout=self.timeout)
        return self.session

    async def _make_request(self, method: str, endpoint: str, **kwargs) -> Dict:
        """Thực hiện HTTP request"""
        url = f"{self.base_url}{endpoint}"
        session = await self._get_session()

        try:
            async with session.request(method, url, **kwargs) as response:
                if response.status == 200:
                    data = await response.json()
                    return {'success': True, 'data': data}
                else:
                    text = await response.text()
                    logger.error(f"API request failed: {method} {url} - {response.status}: {text}")
                    return {
                        'success': False,
                        'error': f"HTTP {response.status}: {text}",
                        'status_code': response.status
                    }

        except asyncio.TimeoutError:
            logger.error(f"API request timeout: {method} {url}")
            return {'success': False, 'error': 'Request timeout'}
        except Exception as e:
            logger.error(f"API request error: {method} {url} - {e}")
            return {'success': False, 'error': str(e)}

    async def get_student_info(self, ma_sv: str) -> Optional[Dict]:
        """
        Lấy thông tin sinh viên từ backend

        Args:
            ma_sv: Mã sinh viên

        Returns:
            Thông tin sinh viên hoặc None nếu không tìm thấy
        """
        try:
            response = await self._make_request('GET', f'/sinhvien/by-masv/{ma_sv}')

            if response['success']:
                logger.debug(f"Retrieved student info for {ma_sv}")
                return response['data']
            else:
                logger.warning(f"Student {ma_sv} not found: {response.get('error')}")
                return None

        except Exception as e:
            logger.error(f"Error getting student info for {ma_sv}: {e}")
            return None

    async def save_student_embedding(self, ma_sv: str, embedding: str) -> bool:
        """
        Lưu embedding của sinh viên vào database

        Args:
            ma_sv: Mã sinh viên
            embedding: Embedding string

        Returns:
            True nếu lưu thành công
        """
        try:
            payload = {'embedding': embedding}
            response = await self._make_request(
                'POST',
                f'/sinhvien/students/{ma_sv}/embedding',
                json=payload,
                headers={'Content-Type': 'application/json'}
            )

            if response['success']:
                logger.info(f"Successfully saved embedding for student {ma_sv}")
                return True
            else:
                logger.error(f"Failed to save embedding for {ma_sv}: {response.get('error')}")
                return False

        except Exception as e:
            logger.error(f"Error saving embedding for {ma_sv}: {e}")
            return False

    async def get_student_embedding(self, ma_sv: str) -> Optional[str]:
        """
        Lấy embedding của sinh viên từ database

        Args:
            ma_sv: Mã sinh viên

        Returns:
            Embedding string hoặc None
        """
        try:
            response = await self._make_request('GET', f'/sinhvien/students/{ma_sv}/embedding')

            if response['success']:
                embedding_data = response['data']
                return embedding_data.get('embedding')
            else:
                logger.warning(f"No embedding found for student {ma_sv}")
                return None

        except Exception as e:
            logger.error(f"Error getting embedding for {ma_sv}: {e}")
            return None

    async def get_all_student_embeddings(self) -> List[Dict]:
        """
        Lấy tất cả embeddings của sinh viên từ database

        Returns:
            Danh sách embeddings
        """
        try:
            response = await self._make_request('GET', '/sinhvien/embeddings')

            if response['success']:
                logger.info(f"Retrieved {len(response['data'])} student embeddings")
                return response['data']
            else:
                logger.error(f"Failed to get all embeddings: {response.get('error')}")
                return []

        except Exception as e:
            logger.error(f"Error getting all embeddings: {e}")
            return []

    async def get_active_students(self) -> List[Dict]:
        """
        Lấy danh sách sinh viên đang hoạt động

        Returns:
            Danh sách sinh viên
        """
        try:
            response = await self._make_request('GET', '/sinhvien/active')

            if response['success']:
                logger.info(f"Retrieved {len(response['data'])} active students")
                return response['data']
            else:
                logger.error(f"Failed to get active students: {response.get('error')}")
                return []

        except Exception as e:
            logger.error(f"Error getting active students: {e}")
            return []

    async def record_attendance(self, ma_sv: str, ma_lop: str, camera_id: str) -> bool:
        """
        Ghi nhận điểm danh cho sinh viên

        Args:
            ma_sv: Mã sinh viên
            ma_lop: Mã lớp
            camera_id: ID camera

        Returns:
            True nếu ghi nhận thành công
        """
        try:
            payload = {
                'maSv': ma_sv,
                'maLop': ma_lop,
                'cameraId': camera_id,
                'timestamp': self._get_current_timestamp()
            }

            response = await self._make_request(
                'POST',
                '/diemdanh/record',
                json=payload,
                headers={'Content-Type': 'application/json'}
            )

            if response['success']:
                logger.info(f"Successfully recorded attendance for student {ma_sv}")
                return True
            else:
                logger.error(f"Failed to record attendance for {ma_sv}: {response.get('error')}")
                return False

        except Exception as e:
            logger.error(f"Error recording attendance for {ma_sv}: {e}")
            return False

    async def get_camera_info(self, camera_id: str) -> Optional[Dict]:
        """
        Lấy thông tin camera

        Args:
            camera_id: ID camera

        Returns:
            Thông tin camera hoặc None
        """
        try:
            response = await self._make_request('GET', f'/cameras/{camera_id}')

            if response['success']:
                return response['data']
            else:
                logger.warning(f"Camera {camera_id} not found")
                return None

        except Exception as e:
            logger.error(f"Error getting camera info for {camera_id}: {e}")
            return None

    async def get_active_cameras(self) -> List[Dict]:
        """
        Lấy danh sách camera đang hoạt động

        Returns:
            Danh sách camera
        """
        try:
            response = await self._make_request('GET', '/cameras?active=true')

            if response['success']:
                cameras = [cam for cam in response['data'] if cam.get('isActive', False)]
                logger.info(f"Retrieved {len(cameras)} active cameras")
                return cameras
            else:
                logger.error(f"Failed to get active cameras: {response.get('error')}")
                return []

        except Exception as e:
            logger.error(f"Error getting active cameras: {e}")
            return []

    async def health_check(self) -> bool:
        """
        Kiểm tra sức khỏe backend API

        Returns:
            True nếu backend đang hoạt động bình thường
        """
        try:
            response = await self._make_request('GET', '/health')
            return response['success']

        except Exception as e:
            logger.error(f"Backend health check failed: {e}")
            return False

    def _get_current_timestamp(self) -> str:
        """Lấy timestamp hiện tại theo format ISO"""
        from datetime import datetime
        return datetime.now().isoformat()

    async def close(self):
        """Đóng session"""
        if self.session and not self.session.closed:
            await self.session.close()
            logger.debug("Backend API session closed")