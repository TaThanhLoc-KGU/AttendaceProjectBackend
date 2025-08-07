#!/usr/bin/env python3
import sys
import os
import asyncio
import json

# Add current directory to Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Import script chính
from paste import FaceFeatureExtractor, main as original_main

def extract_single_student(ma_sv):
    """Trích xuất đặc trưng cho một sinh viên"""
    import asyncio

    async def extract_one():
        # Cấu hình
        PROJECT_ROOT = "."
        BACKEND_API_URL = "http://localhost:8080/api"
        FACE_API_URL = "http://localhost:8001"

        CREDENTIALS = {
            'username': 'admin',
            'password': 'admin123'
        }

        # Khởi tạo extractor
        extractor = FaceFeatureExtractor(BACKEND_API_URL, FACE_API_URL, PROJECT_ROOT, CREDENTIALS)

        # Xử lý sinh viên
        result = await extractor.process_student(ma_sv)

        # In kết quả dưới dạng JSON để Java đọc
        print("RESULT_JSON_START")
        print(json.dumps(result, ensure_ascii=False, default=str))
        print("RESULT_JSON_END")

        return result

    return asyncio.run(extract_one())

def extract_all_students():
    """Trích xuất đặc trưng cho tất cả sinh viên"""
    import asyncio

    async def extract_all():
        # Cấu hình
        PROJECT_ROOT = "."
        BACKEND_API_URL = "http://localhost:8080/api"
        FACE_API_URL = "http://localhost:8001"

        CREDENTIALS = {
            'username': 'admin',
            'password': 'admin@123'
        }

        # Khởi tạo extractor
        extractor = FaceFeatureExtractor(BACKEND_API_URL, FACE_API_URL, PROJECT_ROOT, CREDENTIALS)

        # Xử lý tất cả sinh viên
        results = await extractor.process_all_students()

        # In kết quả dưới dạng JSON để Java đọc
        print("RESULT_JSON_START")
        print(json.dumps(results, ensure_ascii=False, default=str))
        print("RESULT_JSON_END")

        return results

    return asyncio.run(extract_all())

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python extract_wrapper.py <command> [student_id]")
        print("Commands: single <student_id>, all")
        sys.exit(1)

    command = sys.argv[1]

    try:
        if command == "single" and len(sys.argv) >= 3:
            student_id = sys.argv[2]
            result = extract_single_student(student_id)
            sys.exit(0 if result.get('status') == 'success' else 1)
        elif command == "all":
            results = extract_all_students()
            sys.exit(0 if results.get('success') else 1)
        else:
            print("Invalid command or missing student_id")
            sys.exit(1)

    except Exception as e:
        print(f"ERROR: {str(e)}", file=sys.stderr)
        sys.exit(1)
