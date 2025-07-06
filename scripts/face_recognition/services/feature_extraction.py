import cv2
import numpy as np
import pickle
from pathlib import Path
from typing import List, Dict, Optional, Tuple
from loguru import logger
import asyncio
from concurrent.futures import ThreadPoolExecutor

from ..config.settings import settings
from ..models.face_encoder import face_encoder
from ..utils.image_processing import ImageProcessor
from .backend_api import BackendAPIService


class FeatureExtractionService:
    """
    Service để trích xuất đặc trưng khuôn mặt từ 6 ảnh sinh viên
    (1 ảnh profile + 5 ảnh faces)
    """

    def __init__(self):
        self.image_processor = ImageProcessor()
        self.backend_api = BackendAPIService()
        self.executor = ThreadPoolExecutor(max_workers=4)

    async def extract_student_features(self, ma_sv: str) -> Dict:
        """
        Trích xuất đặc trưng khuôn mặt cho sinh viên

        Args:
            ma_sv: Mã sinh viên

        Returns:
            Dict chứa kết quả trích xuất
        """
        try:
            logger.info(f"Starting feature extraction for student: {ma_sv}")

            # 1. Kiểm tra sinh viên tồn tại
            student_info = await self.backend_api.get_student_info(ma_sv)
            if not student_info:
                raise ValueError(f"Student {ma_sv} not found")

            # 2. Kiểm tra và load ảnh
            images_info = await self._load_student_images(ma_sv)
            if not images_info['success']:
                return images_info

            # 3. Trích xuất embeddings từ tất cả ảnh
            embeddings_result = await self._extract_embeddings_from_images(
                ma_sv,
                images_info['images']
            )

            if not embeddings_result['success']:
                return embeddings_result

            # 4. Tổng hợp embeddings
            final_embedding = face_encoder.aggregate_embeddings(
                embeddings_result['embeddings']
            )

            # 5. Lưu embedding
            embedding_saved = await self._save_embedding(ma_sv, final_embedding)

            # 6. Cập nhật database thông qua backend API
            if embedding_saved:
                embedding_str = self._embedding_to_string(final_embedding)
                await self.backend_api.save_student_embedding(ma_sv, embedding_str)

            result = {
                'success': True,
                'ma_sv': ma_sv,
                'student_name': student_info.get('hoTen', 'Unknown'),
                'images_processed': len(embeddings_result['embeddings']),
                'total_images': len(images_info['images']),
                'embedding_dimension': len(final_embedding),
                'quality_score': self._calculate_quality_score(embeddings_result['embeddings']),
                'timestamp': self._get_timestamp()
            }

            logger.success(f"Feature extraction completed for student: {ma_sv}")
            return result

        except Exception as e:
            logger.error(f"Error extracting features for student {ma_sv}: {e}")
            return {
                'success': False,
                'error': str(e),
                'ma_sv': ma_sv,
                'timestamp': self._get_timestamp()
            }

    async def _load_student_images(self, ma_sv: str) -> Dict:
        """Load tất cả ảnh của sinh viên"""
        try:
            student_dir = settings.STUDENT_IMAGES_DIR / ma_sv
            if not student_dir.exists():
                return {
                    'success': False,
                    'error': f"Student directory not found: {student_dir}"
                }

            # Load ảnh profile
            profile_image = None
            profile_path = student_dir / "profile.jpg"
            if profile_path.exists():
                profile_image = cv2.imread(str(profile_path))
                if profile_image is None:
                    logger.warning(f"Could not load profile image: {profile_path}")

            # Load ảnh faces
            faces_dir = student_dir / "faces"
            face_images = []

            if faces_dir.exists():
                for i in range(1, 6):  # face_1.jpg to face_5.jpg
                    face_path = faces_dir / f"face_{i}.jpg"
                    if face_path.exists():
                        image = cv2.imread(str(face_path))
                        if image is not None:
                            face_images.append({
                                'image': image,
                                'path': str(face_path),
                                'type': 'face'
                            })
                        else:
                            logger.warning(f"Could not load face image: {face_path}")

            # Tổng hợp tất cả ảnh
            all_images = []

            if profile_image is not None:
                all_images.append({
                    'image': profile_image,
                    'path': str(profile_path),
                    'type': 'profile'
                })

            all_images.extend(face_images)

            # Kiểm tra số lượng ảnh tối thiểu
            if len(all_images) < settings.REQUIRED_FACE_IMAGES:
                return {
                    'success': False,
                    'error': f"Insufficient images. Found {len(all_images)}, required {settings.REQUIRED_FACE_IMAGES + 1}"
                }

            logger.info(f"Loaded {len(all_images)} images for student {ma_sv}")
            return {
                'success': True,
                'images': all_images,
                'profile_count': 1 if profile_image is not None else 0,
                'face_count': len(face_images)
            }

        except Exception as e:
            logger.error(f"Error loading images for student {ma_sv}: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    async def _extract_embeddings_from_images(self, ma_sv: str, images: List[Dict]) -> Dict:
        """Trích xuất embeddings từ danh sách ảnh"""
        try:
            embeddings = []
            processing_results = []

            for idx, image_info in enumerate(images):
                try:
                    # Preprocess image
                    processed_image = await self._preprocess_image_async(
                        image_info['image']
                    )

                    # Extract embedding
                    embedding = face_encoder.extract_embedding(processed_image)

                    if embedding is not None:
                        embeddings.append(embedding)
                        processing_results.append({
                            'path': image_info['path'],
                            'type': image_info['type'],
                            'success': True
                        })
                        logger.debug(f"Successfully extracted embedding from {image_info['path']}")
                    else:
                        processing_results.append({
                            'path': image_info['path'],
                            'type': image_info['type'],
                            'success': False,
                            'error': 'No face detected'
                        })
                        logger.warning(f"No face detected in {image_info['path']}")

                except Exception as e:
                    processing_results.append({
                        'path': image_info['path'],
                        'type': image_info['type'],
                        'success': False,
                        'error': str(e)
                    })
                    logger.error(f"Error processing {image_info['path']}: {e}")

            # Kiểm tra số lượng embeddings thành công
            if len(embeddings) < 3:  # Cần ít nhất 3 embeddings
                return {
                    'success': False,
                    'error': f"Too few valid embeddings: {len(embeddings)}/3 minimum",
                    'processing_results': processing_results
                }

            logger.info(f"Extracted {len(embeddings)} embeddings from {len(images)} images for student {ma_sv}")
            return {
                'success': True,
                'embeddings': embeddings,
                'processing_results': processing_results
            }

        except Exception as e:
            logger.error(f"Error extracting embeddings for student {ma_sv}: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    async def _preprocess_image_async(self, image: np.ndarray) -> np.ndarray:
        """Tiền xử lý ảnh async"""
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(
            self.executor,
            self.image_processor.preprocess_for_recognition,
            image
        )

    async def _save_embedding(self, ma_sv: str, embedding: np.ndarray) -> bool:
        """Lưu embedding vào file"""
        try:
            # Tạo thư mục embeddings nếu chưa có
            embeddings_dir = settings.EMBEDDINGS_DIR
            embeddings_dir.mkdir(exist_ok=True)

            # Lưu embedding
            embedding_path = embeddings_dir / f"{ma_sv}.pkl"

            embedding_data = {
                'ma_sv': ma_sv,
                'embedding': embedding,
                'dimension': len(embedding),
                'timestamp': self._get_timestamp(),
                'model': settings.INSIGHTFACE_MODEL
            }

            with open(embedding_path, 'wb') as f:
                pickle.dump(embedding_data, f)

            logger.info(f"Saved embedding for student {ma_sv} to {embedding_path}")
            return True

        except Exception as e:
            logger.error(f"Error saving embedding for student {ma_sv}: {e}")
            return False

    def _embedding_to_string(self, embedding: np.ndarray) -> str:
        """Convert embedding array to string for database storage"""
        return ','.join(map(str, embedding.tolist()))

    def _calculate_quality_score(self, embeddings: List[np.ndarray]) -> float:
        """Tính điểm chất lượng của embeddings"""
        if len(embeddings) < 2:
            return 1.0

        # Tính similarity giữa các embeddings
        similarities = []
        for i in range(len(embeddings)):
            for j in range(i + 1, len(embeddings)):
                sim = face_encoder.compare_embeddings(embeddings[i], embeddings[j])
                similarities.append(sim)

        # Điểm chất lượng = similarity trung bình
        return float(np.mean(similarities))

    def _get_timestamp(self) -> str:
        """Lấy timestamp hiện tại"""
        from datetime import datetime
        return datetime.now().isoformat()

    async def get_extraction_status(self, ma_sv: str) -> Dict:
        """Kiểm tra trạng thái trích xuất đặc trưng"""
        try:
            # Kiểm tra file embedding có tồn tại không
            embedding_path = settings.EMBEDDINGS_DIR / f"{ma_sv}.pkl"

            if embedding_path.exists():
                with open(embedding_path, 'rb') as f:
                    data = pickle.load(f)

                return {
                    'success': True,
                    'ma_sv': ma_sv,
                    'status': 'completed',
                    'timestamp': data.get('timestamp'),
                    'dimension': data.get('dimension'),
                    'model': data.get('model')
                }
            else:
                return {
                    'success': True,
                    'ma_sv': ma_sv,
                    'status': 'not_extracted',
                    'message': 'Embedding not found'
                }

        except Exception as e:
            logger.error(f"Error checking extraction status for {ma_sv}: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    async def batch_extract_features(self, ma_sv_list: List[str]) -> Dict:
        """Trích xuất đặc trưng cho nhiều sinh viên"""
        try:
            logger.info(f"Starting batch feature extraction for {len(ma_sv_list)} students")

            results = []
            for ma_sv in ma_sv_list:
                result = await self.extract_student_features(ma_sv)
                results.append(result)

                # Tạm dừng ngắn giữa các lần xử lý
                await asyncio.sleep(0.1)

            # Tổng kết
            successful = sum(1 for r in results if r.get('success', False))
            failed = len(results) - successful

            return {
                'success': True,
                'total_students': len(ma_sv_list),
                'successful': successful,
                'failed': failed,
                'results': results,
                'timestamp': self._get_timestamp()
            }

        except Exception as e:
            logger.error(f"Error in batch feature extraction: {e}")
            return {
                'success': False,
                'error': str(e)
            }