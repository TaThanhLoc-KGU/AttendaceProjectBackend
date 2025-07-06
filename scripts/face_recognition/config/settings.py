from pydantic_settings import BaseSettings
from typing import Optional
import os
from pathlib import Path


class Settings(BaseSettings):
    # API Configuration
    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8001
    API_RELOAD: bool = True

    # Backend Spring Boot API
    BACKEND_API_URL: str = "http://localhost:8080/api"
    BACKEND_API_TIMEOUT: int = 30

    # Database Configuration (MySQL)
    DB_HOST: str = "localhost"
    DB_PORT: int = 3306
    DB_USER: str = "root"
    DB_PASSWORD: str = "password"
    DB_NAME: str = "face_attendance"

    # InsightFace Model Configuration
    INSIGHTFACE_MODEL: str = "buffalo_l"  # buffalo_l, buffalo_m, buffalo_s
    DETECTION_THRESHOLD: float = 0.5
    RECOGNITION_THRESHOLD: float = 0.6

    # Image Processing
    IMAGE_SIZE: tuple = (112, 112)  # Standard face recognition size
    MAX_IMAGE_SIZE: int = 1920  # Max input image size
    SUPPORTED_FORMATS: list = [".jpg", ".jpeg", ".png", ".bmp"]

    # File Paths - UPDATED to use backend upload directory
    BASE_DIR: Path = Path(__file__).parent.parent

    # Backend upload directory paths
    BACKEND_PROJECT_ROOT: Path = BASE_DIR.parent.parent  # Go up from scripts/face_recognition to project root
    BACKEND_UPLOAD_DIR: Path = BACKEND_PROJECT_ROOT / "src" / "main" / "resources" / "static" / "uploads"
    STUDENT_IMAGES_DIR: Path = BACKEND_UPLOAD_DIR / "students"

    # Module data directory
    DATA_DIR: Path = BASE_DIR / "data"
    EMBEDDINGS_DIR: Path = DATA_DIR / "embeddings"
    LOGS_DIR: Path = BASE_DIR / "logs"

    # Face Detection & Recognition
    MIN_FACE_SIZE: int = 40
    MAX_FACES_PER_IMAGE: int = 1  # Only allow 1 face per image
    EMBEDDING_DIMENSION: int = 512  # InsightFace embedding size

    # Camera Configuration
    DEFAULT_CAMERA_INDEX: int = 0
    CAMERA_WIDTH: int = 1280
    CAMERA_HEIGHT: int = 720
    CAMERA_FPS: int = 30

    # Feature Extraction
    REQUIRED_FACE_IMAGES: int = 5  # Cần 5 ảnh faces + 1 ảnh profile
    EMBEDDING_AGGREGATION: str = "mean"  # mean, median, weighted_mean

    # Real-time Recognition
    RECOGNITION_INTERVAL: float = 0.5  # seconds between recognition attempts
    ATTENDANCE_COOLDOWN: int = 300  # 5 minutes cooldown between attendance

    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_ROTATION: str = "1 day"
    LOG_RETENTION: str = "30 days"

    # Security
    SECRET_KEY: str = "your-secret-key-here"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    class Config:
        env_file = ".env"
        case_sensitive = True

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # Create directories if they don't exist
        self.create_directories()

    def create_directories(self):
        """Tạo các thư mục cần thiết"""
        directories = [
            self.DATA_DIR,
            self.EMBEDDINGS_DIR,
            self.LOGS_DIR
        ]

        for directory in directories:
            directory.mkdir(parents=True, exist_ok=True)

        # Log thông tin đường dẫn
        print(f"Backend project root: {self.BACKEND_PROJECT_ROOT}")
        print(f"Student images directory: {self.STUDENT_IMAGES_DIR}")
        print(f"Embeddings directory: {self.EMBEDDINGS_DIR}")

    def get_student_image_path(self, ma_sv: str) -> Path:
        """Lấy đường dẫn thư mục ảnh sinh viên"""
        return self.STUDENT_IMAGES_DIR / ma_sv

    def get_student_profile_path(self, ma_sv: str) -> Path:
        """Lấy đường dẫn ảnh profile sinh viên"""
        student_dir = self.get_student_image_path(ma_sv)
        # Tìm file profile với các extension khả dĩ
        for ext in ['.jpg', '.jpeg', '.png']:
            profile_path = student_dir / f"profile{ext}"
            if profile_path.exists():
                return profile_path
        return student_dir / "profile.jpg"  # Default

    def get_student_faces_dir(self, ma_sv: str) -> Path:
        """Lấy đường dẫn thư mục faces sinh viên"""
        return self.get_student_image_path(ma_sv) / "faces"

    def get_embedding_path(self, ma_sv: str) -> Path:
        """Lấy đường dẫn file embedding sinh viên"""
        return self.EMBEDDINGS_DIR / f"{ma_sv}.pkl"

    @property
    def database_url(self) -> str:
        """Tạo database URL cho SQLAlchemy"""
        return f"mysql+mysqlconnector://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"


# Singleton instance
settings = Settings()