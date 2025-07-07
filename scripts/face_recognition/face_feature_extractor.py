import os
import cv2
import numpy as np
import insightface
import requests
import json
import base64
from pathlib import Path
import logging
from typing import List, Dict, Optional, Tuple
import time
import asyncio
import aiohttp
from sklearn.preprocessing import normalize

# Cấu hình logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class FaceFeatureExtractor:
    def __init__(self, backend_api_url: str, face_api_url: str, project_root: str, credentials: Dict = None):
        """
        Khởi tạo class trích xuất đặc trưng khuôn mặt

        Args:
            backend_api_url: URL của Spring Boot backend API
            face_api_url: URL của Face Recognition service API
            project_root: Đường dẫn gốc project face-attendance
            credentials: Dict với username/password để đăng nhập (optional)
        """
        self.backend_api_url = backend_api_url.rstrip('/')
        self.face_api_url = face_api_url.rstrip('/')
        self.credentials = credentials
        self.session_cookies = None  # Lưu cookies sau khi đăng nhập

        # Đường dẫn chính xác theo cấu trúc project
        self.project_root = Path(project_root)
        self.student_base_dir = self.project_root / "src" / "main" / "resources" / "static" / "uploads" / "students"

        # Initialize InsightFace model
        self.app = insightface.app.FaceAnalysis(
            providers=['CPUExecutionProvider'],  # Hoặc 'CUDAExecutionProvider' nếu có GPU
            name='buffalo_l'  # Model chất lượng cao như backend
        )
        self.app.prepare(ctx_id=0, det_size=(640, 640))

        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }

        # Cấu hình theo backend settings
        self.detection_threshold = 0.5
        self.recognition_threshold = 0.6
        self.max_face_size = 1920
        self.min_face_size = 40
        self.required_face_images = 5  # 5 ảnh faces + 1 profile

        logger.info(f"FaceFeatureExtractor initialized")
        logger.info(f"Project root: {self.project_root}")
        logger.info(f"Student base directory: {self.student_base_dir}")

        # Nếu có credentials, sẽ đăng nhập khi cần
        if credentials:
            logger.info(f"Authentication credentials provided for user: {credentials.get('username')}")

    async def login_session(self) -> bool:
        """
        Đăng nhập để lấy session cookies

        Returns:
            True nếu đăng nhập thành công
        """
        if not self.credentials:
            logger.warning("No credentials provided for authentication")
            return False

        try:
            login_data = {
                'username': self.credentials['username'],
                'password': self.credentials['password']
            }

            async with aiohttp.ClientSession() as session:
                url = f"{self.backend_api_url}/auth/login"
                async with session.post(url, json=login_data, headers=self.headers) as response:
                    if response.status == 200:
                        # Lưu cookies từ response
                        self.session_cookies = response.cookies
                        logger.info("✅ Login successful, session established")
                        return True
                    else:
                        response_text = await response.text()
                        logger.error(f"❌ Login failed: {response.status} - {response_text}")
                        return False

        except Exception as e:
            logger.error(f"Login error: {str(e)}")
            return False

    def get_student_image_paths(self, ma_sv: str) -> Dict:
        """
        Lấy đường dẫn ảnh của sinh viên theo cấu trúc thực tế

        Args:
            ma_sv: Mã sinh viên

        Returns:
            Dictionary chứa đường dẫn các ảnh
        """
        student_dir = self.student_base_dir / ma_sv
        faces_dir = student_dir / "faces"

        result = {
            'student_dir': student_dir,
            'profile_image': None,
            'face_images': [],
            'exists': student_dir.exists()
        }

        if not student_dir.exists():
            logger.warning(f"Thư mục sinh viên không tồn tại: {student_dir}")
            return result

        # Tìm ảnh profile
        for ext in ['.jpg', '.jpeg', '.png', '.webp']:
            profile_path = student_dir / f"profile{ext}"
            if profile_path.exists():
                result['profile_image'] = profile_path
                break

        # Tìm ảnh faces
        if faces_dir.exists():
            for i in range(1, 6):  # face_1.jpg đến face_5.jpg
                for ext in ['.jpg', '.jpeg', '.png', '.webp']:
                    face_path = faces_dir / f"face_{i}{ext}"
                    if face_path.exists():
                        result['face_images'].append(face_path)
                        break

        logger.info(f"Sinh viên {ma_sv}: Profile={'✓' if result['profile_image'] else '✗'}, "
                    f"Faces={len(result['face_images'])}/5")

        return result

    def load_and_preprocess_image(self, image_path: Path) -> Optional[np.ndarray]:
        """
        Load và tiền xử lý ảnh theo chuẩn InsightFace

        Args:
            image_path: Đường dẫn đến ảnh

        Returns:
            Ảnh đã được tiền xử lý hoặc None nếu lỗi
        """
        try:
            # Đọc ảnh
            image = cv2.imread(str(image_path))
            if image is None:
                logger.warning(f"Không thể đọc ảnh: {image_path}")
                return None

            # Resize nếu ảnh quá lớn
            height, width = image.shape[:2]
            if width > self.max_face_size or height > self.max_face_size:
                scale = self.max_face_size / max(width, height)
                new_width = int(width * scale)
                new_height = int(height * scale)
                image = cv2.resize(image, (new_width, new_height))

            return image
        except Exception as e:
            logger.error(f"Lỗi khi xử lý ảnh {image_path}: {str(e)}")
            return None

    def extract_face_features(self, image_paths: List[Path]) -> Tuple[List[np.ndarray], Dict]:
        """
        Trích xuất đặc trưng khuôn mặt sử dụng InsightFace

        Args:
            image_paths: Danh sách đường dẫn ảnh

        Returns:
            Tuple của (danh sách embeddings, metadata)
        """
        all_embeddings = []
        face_metadata = {
            'total_images': len(image_paths),
            'processed_images': 0,
            'valid_faces': 0,
            'face_qualities': [],
            'detection_results': []
        }

        for i, image_path in enumerate(image_paths):
            image = self.load_and_preprocess_image(image_path)
            if image is None:
                face_metadata['detection_results'].append({
                    'image': image_path.name,
                    'status': 'failed_to_load'
                })
                continue

            try:
                # Detect faces using InsightFace
                faces = self.app.get(image)

                if not faces:
                    logger.warning(f"Không tìm thấy khuôn mặt trong ảnh: {image_path.name}")
                    face_metadata['detection_results'].append({
                        'image': image_path.name,
                        'status': 'no_face_detected'
                    })
                    continue

                # Chọn khuôn mặt tốt nhất (det_score cao nhất và kích thước lớn nhất)
                best_face = max(faces, key=lambda x: x.det_score * self._calculate_face_area(x.bbox))

                # Kiểm tra chất lượng khuôn mặt
                face_area = self._calculate_face_area(best_face.bbox)
                if face_area < self.min_face_size * self.min_face_size:
                    logger.warning(f"Khuôn mặt quá nhỏ trong ảnh: {image_path.name}")
                    face_metadata['detection_results'].append({
                        'image': image_path.name,
                        'status': 'face_too_small',
                        'area': face_area
                    })
                    continue

                # Trích xuất embedding
                embedding = best_face.normed_embedding

                # Validate embedding
                if embedding is None or len(embedding) != 512:
                    logger.warning(f"Embedding không hợp lệ cho ảnh: {image_path.name}")
                    continue

                all_embeddings.append(embedding)
                face_metadata['valid_faces'] += 1
                face_metadata['face_qualities'].append({
                    'image': image_path.name,
                    'det_score': float(best_face.det_score),
                    'face_area': face_area,
                    'age': int(best_face.age) if hasattr(best_face, 'age') else None,
                    'gender': int(best_face.gender) if hasattr(best_face, 'gender') else None
                })
                face_metadata['detection_results'].append({
                    'image': image_path.name,
                    'status': 'success',
                    'det_score': float(best_face.det_score),
                    'face_area': face_area
                })

                logger.info(f"Trích xuất thành công từ: {image_path.name} (score: {best_face.det_score:.3f})")

            except Exception as e:
                logger.error(f"Lỗi khi trích xuất từ {image_path.name}: {str(e)}")
                face_metadata['detection_results'].append({
                    'image': image_path.name,
                    'status': 'extraction_error',
                    'error': str(e)
                })
                continue

            face_metadata['processed_images'] += 1

        return all_embeddings, face_metadata

    def _calculate_face_area(self, bbox) -> float:
        """Tính diện tích khuôn mặt từ bounding box"""
        return (bbox[2] - bbox[0]) * (bbox[3] - bbox[1])

    def create_composite_embedding(self, embeddings: List[np.ndarray], method: str = "mean") -> Optional[np.ndarray]:
        """
        Tạo embedding tổng hợp từ nhiều embeddings theo backend logic

        Args:
            embeddings: Danh sách các embedding vectors
            method: Phương pháp tổng hợp ("mean", "median", "weighted_mean")

        Returns:
            Embedding tổng hợp đã được normalize
        """
        if not embeddings:
            return None

        embeddings_array = np.array(embeddings)

        if method == "mean":
            composite = np.mean(embeddings_array, axis=0)
        elif method == "median":
            composite = np.median(embeddings_array, axis=0)
        elif method == "weighted_mean":
            # Weight by quality scores if available
            composite = np.mean(embeddings_array, axis=0)
        else:
            composite = np.mean(embeddings_array, axis=0)

        # Normalize embedding như backend
        composite_normalized = normalize([composite], norm='l2')[0]

        return composite_normalized

    def validate_embeddings_quality(self, embeddings: List[np.ndarray]) -> Dict:
        """
        Kiểm tra chất lượng của các embeddings

        Args:
            embeddings: Danh sách các embedding vectors

        Returns:
            Dictionary chứa thông tin về chất lượng
        """
        if len(embeddings) < 2:
            return {
                'is_valid': len(embeddings) >= 1,
                'num_embeddings': len(embeddings),
                'avg_similarity': None,
                'min_similarity': None,
                'max_similarity': None,
                'std_similarity': None,
                'recommendation': 'Cần ít nhất 2 ảnh để đánh giá chất lượng'
            }

        # Tính cosine similarity giữa các embeddings
        similarities = []
        for i in range(len(embeddings)):
            for j in range(i + 1, len(embeddings)):
                similarity = np.dot(embeddings[i], embeddings[j])
                similarities.append(similarity)

        similarities = np.array(similarities)
        avg_similarity = np.mean(similarities)

        # Đánh giá chất lượng
        is_good_quality = avg_similarity > self.recognition_threshold and len(embeddings) >= 3

        quality_info = {
            'is_valid': avg_similarity > 0.4,  # Threshold thấp hơn để accept
            'is_good_quality': is_good_quality,
            'num_embeddings': len(embeddings),
            'avg_similarity': float(avg_similarity),
            'min_similarity': float(np.min(similarities)),
            'max_similarity': float(np.max(similarities)),
            'std_similarity': float(np.std(similarities)),
            'recommendation': self._get_quality_recommendation(avg_similarity, len(embeddings))
        }

        logger.info(f"Chất lượng embeddings: Similarity={avg_similarity:.3f}, Count={len(embeddings)}")
        return quality_info

    def _get_quality_recommendation(self, avg_similarity: float, num_embeddings: int) -> str:
        """Đưa ra khuyến nghị về chất lượng"""
        if avg_similarity > 0.8 and num_embeddings >= 4:
            return "Chất lượng tuyệt vời"
        elif avg_similarity > 0.6 and num_embeddings >= 3:
            return "Chất lượng tốt"
        elif avg_similarity > 0.4:
            return "Chất lượng khá, nên thêm ảnh hoặc chụp lại"
        else:
            return "Chất lượng kém, cần chụp lại tất cả ảnh"

    async def get_student_info(self, ma_sv: str) -> Optional[Dict]:
        """
        Lấy thông tin sinh viên từ backend - Thử nhiều cách

        Args:
            ma_sv: Mã sinh viên

        Returns:
            Thông tin sinh viên hoặc None
        """
        # Cách 1: Thử lấy qua endpoint thông thường (có thể bị auth)
        try:
            async with aiohttp.ClientSession() as session:
                url = f"{self.backend_api_url}/sinhvien/by-masv/{ma_sv}"
                async with session.get(url, headers=self.headers) as response:
                    if response.status == 200:
                        data = await response.json()
                        logger.info(f"✓ [Normal API] Sinh viên {ma_sv} tồn tại: {data.get('hoTen', 'N/A')}")
                        return data
        except Exception as e:
            logger.debug(f"Normal API failed for {ma_sv}: {e}")

        # Cách 2: Thử với authentication nếu có
        if self.credentials:
            # Đăng nhập nếu chưa có session
            if not self.session_cookies:
                await self.login_session()

            if self.session_cookies:
                try:
                    async with aiohttp.ClientSession(cookies=self.session_cookies) as session:
                        url = f"{self.backend_api_url}/sinhvien/by-masv/{ma_sv}"
                        async with session.get(url, headers=self.headers) as response:
                            if response.status == 200:
                                data = await response.json()
                                logger.info(f"✓ [Auth API] Sinh viên {ma_sv} tồn tại: {data.get('hoTen', 'N/A')}")
                                return data
                except Exception as e:
                    logger.debug(f"Authenticated API failed for {ma_sv}: {e}")

        # Cách 3: Kiểm tra thư mục file tồn tại (fallback logic)
        student_dir = self.student_base_dir / ma_sv
        if student_dir.exists():
            logger.warning(f"⚠️  Không thể verify sinh viên {ma_sv} qua API, nhưng thư mục tồn tại")
            return {
                'maSv': ma_sv,
                'hoTen': f'Student_{ma_sv}',
                'note': 'Directory exists, API verification failed'
            }
        else:
            logger.error(f"❌ Sinh viên {ma_sv} không tồn tại (không có thư mục)")
            return None

    async def save_embedding_to_backend(self, ma_sv: str, embedding: np.ndarray) -> bool:
        """
        Lưu embedding vào backend database - Sử dụng Python API

        Args:
            ma_sv: Mã sinh viên
            embedding: Embedding vector

        Returns:
            True nếu lưu thành công
        """
        # Chuyển embedding thành base64 string như backend expect
        embedding_bytes = embedding.astype(np.float32).tobytes()
        embedding_b64 = base64.b64encode(embedding_bytes).decode('utf-8')

        payload = {
            'embedding': embedding_b64
        }

        # Cách 1: Thử Python API endpoint (không cần auth) - ĐÚNG ENDPOINT
        try:
            async with aiohttp.ClientSession() as session:
                url = f"{self.backend_api_url}/python/students/{ma_sv}/embedding"
                async with session.post(url, json=payload, headers=self.headers) as response:
                    if response.status == 200:
                        logger.info(f"✓ [Python API] Lưu embedding cho sinh viên {ma_sv} thành công")
                        return True
                    else:
                        response_text = await response.text()
                        logger.debug(f"Python API save failed for {ma_sv}: {response.status} - {response_text}")
        except Exception as e:
            logger.debug(f"Python API save failed for {ma_sv}: {e}")

        # Cách 2: Thử với authentication
        if self.credentials:
            # Đăng nhập nếu chưa có session
            if not self.session_cookies:
                await self.login_session()

            if self.session_cookies:
                try:
                    async with aiohttp.ClientSession(cookies=self.session_cookies) as session:
                        url = f"{self.backend_api_url}/sinhvien/students/{ma_sv}/embedding"
                        async with session.post(url, json=payload, headers=self.headers) as response:
                            if response.status == 200:
                                logger.info(f"✓ [Auth API] Lưu embedding cho sinh viên {ma_sv} thành công")
                                return True
                            else:
                                response_text = await response.text()
                                logger.error(f"✗ Auth API save failed for {ma_sv}: {response.status} - {response_text}")
                except Exception as e:
                    logger.debug(f"Authenticated API save failed for {ma_sv}: {e}")

        # Cách 3: Lưu file local (fallback)
        try:
            embeddings_dir = self.project_root / "data" / "embeddings"
            embeddings_dir.mkdir(parents=True, exist_ok=True)

            embedding_file = embeddings_dir / f"{ma_sv}.npy"
            np.save(embedding_file, embedding)

            # Lưu thêm metadata
            metadata_file = embeddings_dir / f"{ma_sv}_metadata.json"
            metadata = {
                'ma_sv': ma_sv,
                'embedding_shape': embedding.shape,
                'embedding_norm': float(np.linalg.norm(embedding)),
                'timestamp': time.time(),
                'note': 'Saved locally due to API failure'
            }
            with open(metadata_file, 'w') as f:
                json.dump(metadata, f, indent=2)

            logger.warning(f"⚠️  Lưu embedding local cho {ma_sv}: {embedding_file}")
            return True
        except Exception as e:
            logger.error(f"❌ Không thể lưu embedding cho {ma_sv}: {e}")
            return False

    async def trigger_feature_extraction(self, ma_sv: str) -> bool:
        """
        Trigger feature extraction qua Face Recognition Service để cập nhật cache

        Args:
            ma_sv: Mã sinh viên

        Returns:
            True nếu trigger thành công
        """
        try:
            async with aiohttp.ClientSession() as session:
                url = f"{self.face_api_url}/api/v1/features/extract/{ma_sv}"
                async with session.post(url, headers=self.headers) as response:
                    if response.status == 200:
                        data = await response.json()
                        logger.info(f"✓ Trigger extraction cho {ma_sv}: {data.get('message')}")
                        return True
                    else:
                        response_text = await response.text()
                        logger.warning(f"Trigger extraction cho {ma_sv} failed: {response.status}")
                        return False

        except Exception as e:
            logger.warning(f"Không thể trigger extraction cho {ma_sv}: {str(e)}")
            return False

    async def process_student(self, ma_sv: str) -> Dict:
        """
        Xử lý một sinh viên cụ thể

        Args:
            ma_sv: Mã sinh viên

        Returns:
            Kết quả xử lý chi tiết
        """
        logger.info(f"🔄 Bắt đầu xử lý sinh viên: {ma_sv}")

        result = {
            'ma_sv': ma_sv,
            'status': 'failed',
            'message': '',
            'metadata': {}
        }

        # 1. Kiểm tra sinh viên có tồn tại trong database
        student_info = await self.get_student_info(ma_sv)
        if not student_info:
            result['message'] = f"Sinh viên {ma_sv} không tồn tại trong database"
            return result

        # 2. Lấy đường dẫn ảnh
        image_paths = self.get_student_image_paths(ma_sv)
        if not image_paths['exists']:
            result['message'] = f"Thư mục sinh viên {ma_sv} không tồn tại"
            return result

        # 3. Collect tất cả ảnh có sẵn
        all_images = []
        if image_paths['profile_image']:
            all_images.append(image_paths['profile_image'])
        all_images.extend(image_paths['face_images'])

        if len(all_images) == 0:
            result['message'] = f"Không tìm thấy ảnh nào cho sinh viên {ma_sv}"
            return result

        logger.info(f"📸 Tìm thấy {len(all_images)} ảnh cho sinh viên {ma_sv}")

        # 4. Trích xuất features
        embeddings, face_metadata = self.extract_face_features(all_images)

        if not embeddings:
            result['message'] = f"Không trích xuất được embedding nào cho sinh viên {ma_sv}"
            result['metadata'] = {'face_metadata': face_metadata}
            return result

        # 5. Kiểm tra chất lượng
        quality_info = self.validate_embeddings_quality(embeddings)

        # 6. Tạo composite embedding
        composite_embedding = self.create_composite_embedding(embeddings, method="mean")

        if composite_embedding is None:
            result['message'] = f"Không thể tạo composite embedding cho sinh viên {ma_sv}"
            return result

        # 7. Lưu embedding vào database
        save_success = await self.save_embedding_to_backend(ma_sv, composite_embedding)

        if save_success:
            # 8. Trigger feature extraction service để cập nhật cache
            await self.trigger_feature_extraction(ma_sv)

            result['status'] = 'success'
            result['message'] = f"✅ Xử lý thành công sinh viên {ma_sv} ({quality_info['recommendation']})"
            result['metadata'] = {
                'student_info': {
                    'ho_ten': student_info.get('hoTen', 'N/A'),
                    'ma_lop': student_info.get('maLop', 'N/A')
                },
                'images': {
                    'total_found': len(all_images),
                    'profile_available': image_paths['profile_image'] is not None,
                    'face_images_count': len(image_paths['face_images'])
                },
                'face_metadata': face_metadata,
                'quality_info': quality_info,
                'embedding_info': {
                    'dimension': len(composite_embedding),
                    'norm': float(np.linalg.norm(composite_embedding))
                }
            }
        else:
            result['message'] = f"❌ Lỗi lưu embedding cho sinh viên {ma_sv}"
            result['metadata'] = {
                'face_metadata': face_metadata,
                'quality_info': quality_info
            }

        return result

    async def process_all_students(self) -> Dict:
        """
        Tự động tìm và xử lý tất cả sinh viên có trong thư mục uploads

        Returns:
            Kết quả xử lý tổng hợp
        """
        logger.info(f"🔍 Tìm kiếm sinh viên trong: {self.student_base_dir}")

        if not self.student_base_dir.exists():
            logger.error(f"Thư mục sinh viên không tồn tại: {self.student_base_dir}")
            return {
                'success': False,
                'error': 'Student base directory not found',
                'results': {}
            }

        # Tìm tất cả thư mục sinh viên
        student_folders = [
            folder.name for folder in self.student_base_dir.iterdir()
            if folder.is_dir() and not folder.name.startswith('.')
        ]

        if not student_folders:
            logger.warning("Không tìm thấy thư mục sinh viên nào")
            return {
                'success': True,
                'total_students': 0,
                'results': {}
            }

        logger.info(f"📂 Tìm thấy {len(student_folders)} thư mục sinh viên")

        results = {}
        success_count = 0

        # Xử lý từng sinh viên
        for ma_sv in student_folders:
            try:
                result = await self.process_student(ma_sv)
                results[ma_sv] = result

                if result['status'] == 'success':
                    success_count += 1

            except Exception as e:
                logger.error(f"❌ Lỗi khi xử lý sinh viên {ma_sv}: {str(e)}")
                results[ma_sv] = {
                    'ma_sv': ma_sv,
                    'status': 'error',
                    'message': f"Exception: {str(e)}",
                    'metadata': {}
                }

        # Tạo báo cáo tổng hợp
        summary = {
            'success': True,
            'total_students': len(student_folders),
            'success_count': success_count,
            'failed_count': len(student_folders) - success_count,
            'success_rate': success_count / len(student_folders) * 100 if student_folders else 0,
            'results': results
        }

        logger.info(
            f"🏁 Hoàn thành xử lý. Thành công: {success_count}/{len(student_folders)} ({summary['success_rate']:.1f}%)")
        return summary


def print_detailed_results(results: Dict):
    """In kết quả chi tiết và đẹp mắt"""
    print("\n" + "=" * 100)
    print("📊 BÁO CÁO TRÍCH XUẤT ĐẶC TRƯNG KHUÔN MẶT SINH VIÊN")
    print("=" * 100)

    print(f"📈 TỔNG QUAN:")
    print(f"   • Tổng số sinh viên: {results['total_students']}")
    print(f"   • Thành công: {results['success_count']} ✅")
    print(f"   • Thất bại: {results['failed_count']} ❌")
    print(f"   • Tỷ lệ thành công: {results['success_rate']:.1f}%")

    print(f"\n📋 CHI TIẾT TỪNG SINH VIÊN:")
    print("-" * 100)

    # Nhóm kết quả theo trạng thái
    success_students = []
    failed_students = []
    error_students = []

    for ma_sv, result in results['results'].items():
        if result['status'] == 'success':
            success_students.append((ma_sv, result))
        elif result['status'] == 'failed':
            failed_students.append((ma_sv, result))
        else:
            error_students.append((ma_sv, result))

    # In sinh viên thành công
    if success_students:
        print(f"\n✅ SINH VIÊN XỬ LÝ THÀNH CÔNG ({len(success_students)}):")
        for ma_sv, result in success_students:
            metadata = result.get('metadata', {})
            student_info = metadata.get('student_info', {})
            images_info = metadata.get('images', {})
            quality_info = metadata.get('quality_info', {})

            print(f"   🎓 {ma_sv} - {student_info.get('ho_ten', 'N/A')}")
            print(
                f"      📸 Ảnh: {images_info.get('total_found', 0)} (Profile: {'✓' if images_info.get('profile_available') else '✗'}, Faces: {images_info.get('face_images_count', 0)})")
            if quality_info.get('avg_similarity'):
                print(
                    f"      🎯 Chất lượng: {quality_info['avg_similarity']:.3f} ({quality_info.get('recommendation', 'N/A')})")

    # In sinh viên thất bại
    if failed_students:
        print(f"\n❌ SINH VIÊN XỬ LÝ THẤT BẠI ({len(failed_students)}):")
        for ma_sv, result in failed_students:
            print(f"   ⚠️  {ma_sv}: {result['message']}")

    # In sinh viên lỗi
    if error_students:
        print(f"\n🚫 SINH VIÊN GẶP LỖI ({len(error_students)}):")
        for ma_sv, result in error_students:
            print(f"   💥 {ma_sv}: {result['message']}")

    print("\n" + "=" * 100)


async def main():
    """
    Hàm main để chạy script
    """
    # ========== CẤU HÌNH HỆ THỐNG ==========
    PROJECT_ROOT = "/home/loki/Desktop/face-attendance"  # ĐƯỜNG DẪN PROJECT CỦA BẠN
    BACKEND_API_URL = "http://localhost:8080/api"  # Spring Boot API
    FACE_API_URL = "http://localhost:8001"  # Face Recognition Service

    # ========== CẤU HÌNH XÁC THỰC (TÙY CHỌN) ==========
    # Nếu cần xác thực, uncomment và điền thông tin:
    CREDENTIALS = {
        'username': 'admin',  # Thay bằng username thực
        'password': 'admin123'  # Thay bằng password thực
    }
    # Hoặc để None nếu không cần xác thực:
    # CREDENTIALS = None

    print("🚀 KHỞI ĐỘNG SCRIPT TRÍCH XUẤT ĐẶC TRƯNG KHUÔN MẶT")
    print("=" * 60)
    print(f"📁 Project root: {PROJECT_ROOT}")
    print(f"🔗 Backend API: {BACKEND_API_URL}")
    print(f"🤖 Face API: {FACE_API_URL}")
    print(f"🔐 Authentication: {'Enabled' if CREDENTIALS else 'Disabled'}")

    # Khởi tạo extractor
    extractor = FaceFeatureExtractor(BACKEND_API_URL, FACE_API_URL, PROJECT_ROOT, CREDENTIALS)

    # Kiểm tra thư mục tồn tại
    if not extractor.student_base_dir.exists():
        print(f"❌ Lỗi: Thư mục sinh viên không tồn tại: {extractor.student_base_dir}")
        print("💡 Hãy kiểm tra lại đường dẫn PROJECT_ROOT")
        return

    print(f"✅ Thư mục sinh viên: {extractor.student_base_dir}")

    # Test kết nối API (nếu có credentials)
    if CREDENTIALS:
        print("🔄 Kiểm tra kết nối API...")
        login_success = await extractor.login_session()
        if login_success:
            print("✅ Kết nối API thành công")
        else:
            print("⚠️  Đăng nhập API thất bại, sẽ thử fallback methods")

    # Xử lý tất cả sinh viên
    logger.info("🔄 Bắt đầu xử lý batch trích xuất đặc trưng...")
    results = await extractor.process_all_students()

    # In kết quả chi tiết
    print_detailed_results(results)

    # Lưu kết quả ra file nếu cần
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    report_file = f"face_extraction_report_{timestamp}.json"

    try:
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2, default=str)
        print(f"💾 Báo cáo đã được lưu: {report_file}")
    except Exception as e:
        print(f"⚠️  Không thể lưu báo cáo: {e}")


if __name__ == "__main__":
    # Chạy async main
    asyncio.run(main())