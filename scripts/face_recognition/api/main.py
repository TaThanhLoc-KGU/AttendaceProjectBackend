from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import uvicorn
from loguru import logger
import sys
from pathlib import Path

# Add parent directory to path
sys.path.append(str(Path(__file__).parent.parent))

from config.settings import settings
from services.feature_extraction import FeatureExtractionService
from camera.realtime_recognition import RealtimeRecognitionService
from services.backend_api import BackendAPIService

# Global services
feature_service = None
recognition_service = None
backend_api = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    global feature_service, recognition_service, backend_api

    # Startup
    logger.info("Starting Face Recognition Service")

    # Initialize services
    feature_service = FeatureExtractionService()
    recognition_service = RealtimeRecognitionService()
    backend_api = BackendAPIService()

    # Check backend connection
    backend_healthy = await backend_api.health_check()
    if not backend_healthy:
        logger.warning("Backend API is not responding")
    else:
        logger.success("Backend API connection established")

    logger.success("Face Recognition Service started successfully")

    yield

    # Shutdown
    logger.info("Shutting down Face Recognition Service")

    # Stop recognition if running
    if recognition_service and recognition_service.is_running:
        await recognition_service.stop_recognition()

    # Close backend API session
    if backend_api:
        await backend_api.close()

    logger.info("Face Recognition Service stopped")


# Create FastAPI app
app = FastAPI(
    title="Face Recognition Service",
    description="API cho hệ thống nhận diện khuôn mặt và điểm danh",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Trong production nên chỉ định cụ thể
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Root endpoint
@app.get("/")
async def root():
    return {
        "service": "Face Recognition Service",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs"
    }


# Health check endpoint
@app.get("/api/v1/health")
async def health_check():
    """Kiểm tra sức khỏe service"""
    try:
        # Check backend connection
        backend_status = await backend_api.health_check()

        return {
            "status": "healthy",
            "backend_connected": backend_status,
            "recognition_running": recognition_service.is_running if recognition_service else False,
            "total_students": len(recognition_service.student_embeddings) if recognition_service else 0,
            "timestamp": feature_service._get_timestamp() if feature_service else None
        }
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={
                "status": "unhealthy",
                "error": str(e)
            }
        )


# Feature Extraction Endpoints
@app.post("/api/v1/features/extract/{ma_sv}")
async def extract_features(ma_sv: str, background_tasks: BackgroundTasks):
    """
    Trích xuất đặc trưng khuôn mặt cho sinh viên

    Args:
        ma_sv: Mã sinh viên
    """
    try:
        if not feature_service:
            raise HTTPException(status_code=500, detail="Feature service not initialized")

        # Chạy extraction trong background
        background_tasks.add_task(feature_service.extract_student_features, ma_sv)

        return {
            "success": True,
            "message": f"Feature extraction started for student {ma_sv}",
            "ma_sv": ma_sv,
            "status": "processing"
        }

    except Exception as e:
        logger.error(f"Error starting feature extraction for {ma_sv}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/features/extract/{ma_sv}")
async def extract_features_sync(ma_sv: str):
    """
    Trích xuất đặc trưng khuôn mặt cho sinh viên (synchronous)

    Args:
        ma_sv: Mã sinh viên
    """
    try:
        if not feature_service:
            raise HTTPException(status_code=500, detail="Feature service not initialized")

        result = await feature_service.extract_student_features(ma_sv)

        if result['success']:
            return result
        else:
            raise HTTPException(status_code=400, detail=result.get('error', 'Feature extraction failed'))

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error extracting features for {ma_sv}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/features/status/{ma_sv}")
async def get_extraction_status(ma_sv: str):
    """
    Kiểm tra trạng thái trích xuất đặc trưng

    Args:
        ma_sv: Mã sinh viên
    """
    try:
        if not feature_service:
            raise HTTPException(status_code=500, detail="Feature service not initialized")

        result = await feature_service.get_extraction_status(ma_sv)
        return result

    except Exception as e:
        logger.error(f"Error getting extraction status for {ma_sv}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/features/batch-extract")
async def batch_extract_features(ma_sv_list: list[str], background_tasks: BackgroundTasks):
    """
    Trích xuất đặc trưng cho nhiều sinh viên

    Args:
        ma_sv_list: Danh sách mã sinh viên
    """
    try:
        if not feature_service:
            raise HTTPException(status_code=500, detail="Feature service not initialized")

        if len(ma_sv_list) > 50:  # Giới hạn số lượng
            raise HTTPException(status_code=400, detail="Too many students (max 50)")

        # Chạy batch extraction trong background
        background_tasks.add_task(feature_service.batch_extract_features, ma_sv_list)

        return {
            "success": True,
            "message": f"Batch feature extraction started for {len(ma_sv_list)} students",
            "total_students": len(ma_sv_list),
            "status": "processing"
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error starting batch feature extraction: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# Face Recognition Endpoints
@app.post("/api/v1/recognition/start")
async def start_recognition(camera_config: dict):
    """
    Bắt đầu nhận diện khuôn mặt real-time

    Args:
        camera_config: Cấu hình camera
        {
            "camera_id": "string",
            "source": "string or int",
            "ma_lop": "string",
            "detection_area": {
                "x": int,
                "y": int,
                "width": int,
                "height": int
            }
        }
    """
    try:
        if not recognition_service:
            raise HTTPException(status_code=500, detail="Recognition service not initialized")

        # Validate required fields
        required_fields = ['camera_id', 'source', 'ma_lop']
        for field in required_fields:
            if field not in camera_config:
                raise HTTPException(status_code=400, detail=f"Missing required field: {field}")

        result = await recognition_service.start_recognition(camera_config)

        if result['success']:
            return result
        else:
            raise HTTPException(status_code=400, detail=result.get('error', 'Failed to start recognition'))

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error starting recognition: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/recognition/stop")
async def stop_recognition():
    """Dừng nhận diện khuôn mặt real-time"""
    try:
        if not recognition_service:
            raise HTTPException(status_code=500, detail="Recognition service not initialized")

        result = await recognition_service.stop_recognition()

        if result['success']:
            return result
        else:
            raise HTTPException(status_code=400, detail=result.get('error', 'Failed to stop recognition'))

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error stopping recognition: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/recognition/status")
async def get_recognition_status():
    """Lấy trạng thái nhận diện hiện tại"""
    try:
        if not recognition_service:
            raise HTTPException(status_code=500, detail="Recognition service not initialized")

        status = await recognition_service.get_recognition_status()
        return status

    except Exception as e:
        logger.error(f"Error getting recognition status: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# Backend Integration Endpoints
@app.get("/api/v1/students/active")
async def get_active_students():
    """Lấy danh sách sinh viên đang hoạt động từ backend"""
    try:
        if not backend_api:
            raise HTTPException(status_code=500, detail="Backend API not initialized")

        students = await backend_api.get_active_students()
        return {
            "success": True,
            "total": len(students),
            "students": students
        }

    except Exception as e:
        logger.error(f"Error getting active students: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/students/{ma_sv}/info")
async def get_student_info(ma_sv: str):
    """Lấy thông tin sinh viên từ backend"""
    try:
        if not backend_api:
            raise HTTPException(status_code=500, detail="Backend API not initialized")

        student_info = await backend_api.get_student_info(ma_sv)

        if student_info:
            return {
                "success": True,
                "student": student_info
            }
        else:
            raise HTTPException(status_code=404, detail=f"Student {ma_sv} not found")

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting student info for {ma_sv}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/cameras/active")
async def get_active_cameras():
    """Lấy danh sách camera đang hoạt động từ backend"""
    try:
        if not backend_api:
            raise HTTPException(status_code=500, detail="Backend API not initialized")

        cameras = await backend_api.get_active_cameras()
        return {
            "success": True,
            "total": len(cameras),
            "cameras": cameras
        }

    except Exception as e:
        logger.error(f"Error getting active cameras: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# Exception handlers
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": exc.detail,
            "status_code": exc.status_code
        }
    )


@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    logger.error(f"Unhandled exception: {exc}")
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error": "Internal server error",
            "detail": str(exc)
        }
    )


# Main function to run the service
if __name__ == "__main__":
    logger.info(f"Starting Face Recognition Service on {settings.API_HOST}:{settings.API_PORT}")

    uvicorn.run(
        "main:app",
        host=settings.API_HOST,
        port=settings.API_PORT,
        reload=settings.API_RELOAD,
        log_level=settings.LOG_LEVEL.lower()
    )