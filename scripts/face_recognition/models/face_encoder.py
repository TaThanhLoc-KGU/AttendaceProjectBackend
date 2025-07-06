import cv2
import numpy as np
import insightface
from typing import List, Optional, Tuple
from loguru import logger
from ..config.settings import settings


class FaceEncoder:
    """
    Wrapper class cho InsightFace model để trích xuất đặc trưng khuôn mặt
    """

    def __init__(self):
        self.model = None
        self.is_initialized = False
        self._initialize_model()

    def _initialize_model(self):
        """Khởi tạo InsightFace model"""
        try:
            logger.info(f"Initializing InsightFace model: {settings.INSIGHTFACE_MODEL}")

            # Khởi tạo model InsightFace
            self.model = insightface.app.FaceAnalysis(
                name=settings.INSIGHTFACE_MODEL,
                providers=['CPUExecutionProvider']  # Có thể thêm 'CUDAExecutionProvider' nếu có GPU
            )

            # Chuẩn bị model với kích thước ảnh
            self.model.prepare(ctx_id=0, det_size=(640, 640))

            self.is_initialized = True
            logger.success("InsightFace model initialized successfully")

        except Exception as e:
            logger.error(f"Failed to initialize InsightFace model: {e}")
            self.is_initialized = False
            raise

    def detect_faces(self, image: np.ndarray) -> List[dict]:
        """
        Phát hiện khuôn mặt trong ảnh

        Args:
            image: Ảnh đầu vào (BGR format)

        Returns:
            List các khuôn mặt được phát hiện với thông tin bbox và embedding
        """
        if not self.is_initialized:
            raise RuntimeError("FaceEncoder not initialized")

        try:
            # Phát hiện khuôn mặt
            faces = self.model.get(image)

            # Lọc khuôn mặt theo threshold
            valid_faces = []
            for face in faces:
                if face.det_score >= settings.DETECTION_THRESHOLD:
                    face_info = {
                        'bbox': face.bbox.astype(int),
                        'landmark': face.landmark,
                        'embedding': face.normed_embedding,
                        'confidence': float(face.det_score),
                        'age': getattr(face, 'age', None),
                        'gender': getattr(face, 'gender', None)
                    }
                    valid_faces.append(face_info)

            logger.debug(f"Detected {len(valid_faces)} valid faces")
            return valid_faces

        except Exception as e:
            logger.error(f"Error detecting faces: {e}")
            return []

    def extract_embedding(self, image: np.ndarray) -> Optional[np.ndarray]:
        """
        Trích xuất embedding từ ảnh chứa 1 khuôn mặt

        Args:
            image: Ảnh đầu vào (BGR format)

        Returns:
            Embedding vector hoặc None nếu không phát hiện được khuôn mặt
        """
        faces = self.detect_faces(image)

        if len(faces) == 0:
            logger.warning("No face detected in image")
            return None

        if len(faces) > 1:
            logger.warning(f"Multiple faces detected ({len(faces)}), using the largest one")
            # Chọn khuôn mặt có bbox lớn nhất
            faces = sorted(faces, key=lambda x: self._calculate_face_area(x['bbox']), reverse=True)

        return faces[0]['embedding']

    def extract_multiple_embeddings(self, images: List[np.ndarray]) -> List[np.ndarray]:
        """
        Trích xuất embedding từ nhiều ảnh

        Args:
            images: Danh sách các ảnh (BGR format)

        Returns:
            Danh sách embeddings
        """
        embeddings = []
        for i, image in enumerate(images):
            embedding = self.extract_embedding(image)
            if embedding is not None:
                embeddings.append(embedding)
                logger.debug(f"Successfully extracted embedding from image {i + 1}")
            else:
                logger.warning(f"Failed to extract embedding from image {i + 1}")

        return embeddings

    def aggregate_embeddings(self, embeddings: List[np.ndarray], method: str = None) -> np.ndarray:
        """
        Tổng hợp nhiều embeddings thành 1 embedding đại diện

        Args:
            embeddings: Danh sách embeddings
            method: Phương pháp tổng hợp (mean, median, weighted_mean)

        Returns:
            Embedding đại diện
        """
        if not embeddings:
            raise ValueError("Empty embeddings list")

        if len(embeddings) == 1:
            return embeddings[0]

        method = method or settings.EMBEDDING_AGGREGATION
        embeddings_array = np.array(embeddings)

        if method == "mean":
            aggregated = np.mean(embeddings_array, axis=0)
        elif method == "median":
            aggregated = np.median(embeddings_array, axis=0)
        elif method == "weighted_mean":
            # Có thể implement weighted average dựa trên quality score
            weights = np.ones(len(embeddings)) / len(embeddings)
            aggregated = np.average(embeddings_array, axis=0, weights=weights)
        else:
            raise ValueError(f"Unknown aggregation method: {method}")

        # Normalize embedding
        aggregated = aggregated / np.linalg.norm(aggregated)

        logger.info(f"Aggregated {len(embeddings)} embeddings using {method} method")
        return aggregated

    def compare_embeddings(self, embedding1: np.ndarray, embedding2: np.ndarray) -> float:
        """
        So sánh 2 embeddings và trả về similarity score

        Args:
            embedding1: Embedding thứ nhất
            embedding2: Embedding thứ hai

        Returns:
            Similarity score (0-1, càng cao càng giống)
        """
        # Cosine similarity
        similarity = np.dot(embedding1, embedding2) / (
                np.linalg.norm(embedding1) * np.linalg.norm(embedding2)
        )
        return float(similarity)

    def is_same_person(self, embedding1: np.ndarray, embedding2: np.ndarray) -> bool:
        """
        Kiểm tra 2 embeddings có phải cùng 1 người không

        Args:
            embedding1: Embedding thứ nhất
            embedding2: Embedding thứ hai

        Returns:
            True nếu cùng 1 người
        """
        similarity = self.compare_embeddings(embedding1, embedding2)
        return similarity >= settings.RECOGNITION_THRESHOLD

    def _calculate_face_area(self, bbox: np.ndarray) -> float:
        """Tính diện tích bbox của khuôn mặt"""
        x1, y1, x2, y2 = bbox
        return (x2 - x1) * (y2 - y1)

    def __del__(self):
        """Cleanup when object is destroyed"""
        if hasattr(self, 'model') and self.model is not None:
            del self.model
            logger.debug("FaceEncoder model cleaned up")


# Singleton instance
face_encoder = FaceEncoder()