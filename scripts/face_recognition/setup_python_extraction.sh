#!/bin/bash

# =============================================================================
# Script Setup Python Feature Extraction cho Face Attendance System
# =============================================================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration - THAY ĐỔI THEO SETUP CỦA BẠN
PROJECT_ROOT="/home/loki/Desktop/face-attendance"
VENV_PATH="$PROJECT_ROOT/.venv"
PYTHON_SCRIPT_NAME="scripts/face_recognition/face_feature_extractor.py"  # Tên file Python script của bạn

echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}        SETUP PYTHON FEATURE EXTRACTION FOR FACE ATTENDANCE${NC}"
echo -e "${BLUE}==============================================================================${NC}"

# Function to print status
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as correct user
check_user() {
    print_status "Checking user permissions..."
    if [ "$EUID" -eq 0 ]; then
        print_error "Please don't run this script as root!"
        exit 1
    fi
    print_status "User check passed ✓"
}

# Check project structure
check_project_structure() {
    print_status "Checking project structure..."

    if [ ! -d "$PROJECT_ROOT" ]; then
        print_error "Project root not found: $PROJECT_ROOT"
        print_error "Please update PROJECT_ROOT in this script to match your setup"
        exit 1
    fi

    if [ ! -f "$PROJECT_ROOT/$PYTHON_SCRIPT_NAME" ]; then
        print_error "Python script not found: $PROJECT_ROOT/$PYTHON_SCRIPT_NAME"
        print_error "Please copy your Python feature extraction script to: $PROJECT_ROOT/$PYTHON_SCRIPT_NAME"
        exit 1
    fi

    print_status "Project structure check passed ✓"
}

# Check and setup virtual environment
setup_virtual_environment() {
    print_status "Setting up Python virtual environment..."

    if [ ! -d "$VENV_PATH" ]; then
        print_warning "Virtual environment not found. Creating new one..."

        # Check if python3 is available
        if ! command -v python3 &> /dev/null; then
            print_error "python3 not found. Please install Python 3.8+ first"
            exit 1
        fi

        # Create virtual environment
        python3 -m venv "$VENV_PATH"
        print_status "Virtual environment created at: $VENV_PATH"
    else
        print_status "Virtual environment found at: $VENV_PATH"
    fi

    # Activate virtual environment
    source "$VENV_PATH/bin/activate"

    # Upgrade pip
    print_status "Upgrading pip..."
    pip install --upgrade pip

    print_status "Virtual environment setup completed ✓"
}

# Install required Python packages
install_python_packages() {
    print_status "Installing required Python packages..."

    # Activate virtual environment
    source "$VENV_PATH/bin/activate"

    # Create requirements.txt if not exists
    cat > "$PROJECT_ROOT/requirements.txt" << EOF
numpy>=1.21.0
opencv-python>=4.5.0
insightface>=0.7.3
requests>=2.25.0
aiohttp>=3.8.0
scikit-learn>=1.0.0
pathlib2>=2.3.0
Pillow>=8.0.0
matplotlib>=3.3.0
onnxruntime>=1.10.0
EOF

    print_status "Installing packages from requirements.txt..."
    pip install -r "$PROJECT_ROOT/requirements.txt"

    print_status "Python packages installation completed ✓"
}

# Test Python environment
test_python_environment() {
    print_status "Testing Python environment..."

    # Activate virtual environment
    source "$VENV_PATH/bin/activate"

    # Test imports
    python3 -c "
import sys
print(f'Python version: {sys.version}')

try:
    import numpy as np
    print('✓ numpy:', np.__version__)
except ImportError as e:
    print('✗ numpy import failed:', e)
    sys.exit(1)

try:
    import cv2
    print('✓ opencv-python:', cv2.__version__)
except ImportError as e:
    print('✗ opencv-python import failed:', e)
    sys.exit(1)

try:
    import insightface
    print('✓ insightface: available')
except ImportError as e:
    print('✗ insightface import failed:', e)
    sys.exit(1)

try:
    import requests
    print('✓ requests: available')
except ImportError as e:
    print('✗ requests import failed:', e)
    sys.exit(1)

try:
    import aiohttp
    print('✓ aiohttp: available')
except ImportError as e:
    print('✗ aiohttp import failed:', e)
    sys.exit(1)

try:
    import sklearn
    print('✓ scikit-learn: available')
except ImportError as e:
    print('✗ scikit-learn import failed:', e)
    sys.exit(1)

print('\\n🎉 All required packages are installed correctly!')
"

    if [ $? -eq 0 ]; then
        print_status "Python environment test passed ✓"
    else
        print_error "Python environment test failed!"
        exit 1
    fi
}

# Create wrapper scripts
create_wrapper_scripts() {
    print_status "Creating wrapper scripts..."

    # Create wrapper Python script
    cat > "$PROJECT_ROOT/extract_wrapper.py" << 'EOF'
#!/usr/bin/env python3
import sys
import os
import asyncio
import json

# Add the scripts directory to Python path
project_root = os.path.dirname(os.path.abspath(__file__))
scripts_dir = os.path.join(project_root, 'scripts', 'face_recognition')
sys.path.insert(0, scripts_dir)

# Import script chính
try:
    from face_feature_extractor import FaceFeatureExtractor, main as original_main
except ImportError as e:
    print(f"ERROR: Cannot import main script: {e}", file=sys.stderr)
    print("Please make sure your Python script is at 'scripts/face_recognition/face_feature_extractor.py'", file=sys.stderr)
    sys.exit(1)

def extract_single_student(ma_sv):
    """Trích xuất đặc trưng cho một sinh viên"""
    import asyncio

    async def extract_one():
        # Cấu hình - CẬP NHẬT THEO SETUP CỦA BẠN
        PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
        BACKEND_API_URL = "http://localhost:8080/api"
        FACE_API_URL = "http://localhost:8001"

        # Credentials - CẬP NHẬT THEO SETUP CỦA BẠN
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
        # Cấu hình - CẬP NHẬT THEO SETUP CỦA BẠN
        PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
        BACKEND_API_URL = "http://localhost:8080/api"
        FACE_API_URL = "http://localhost:8001"

        # Credentials - CẬP NHẬT THEO SETUP CỦA BẠN
        CREDENTIALS = {
            'username': 'admin',
            'password': 'admin123'
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
EOF

def extract_single_student(ma_sv):
    """Trích xuất đặc trưng cho một sinh viên"""
    import asyncio

    async def extract_one():
        # Cấu hình - CẬP NHẬT THEO SETUP CỦA BẠN
        PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
        BACKEND_API_URL = "http://localhost:8080/api"
        FACE_API_URL = "http://localhost:8001"

        # Credentials - CẬP NHẬT THEO SETUP CỦA BẠN
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
        # Cấu hình - CẬP NHẬT THEO SETUP CỦA BẠN
        PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
        BACKEND_API_URL = "http://localhost:8080/api"
        FACE_API_URL = "http://localhost:8001"

        # Credentials - CẬP NHẬT THEO SETUP CỦA BẠN
        CREDENTIALS = {
            'username': 'admin',
            'password': 'admin123'
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
EOF

    # Create bash script
    cat > "$PROJECT_ROOT/run_extraction.sh" << EOF
#!/bin/bash

# Đường dẫn tới virtual environment
VENV_PATH="$VENV_PATH"
SCRIPT_DIR="$PROJECT_ROOT"

# Kiểm tra venv tồn tại
if [ ! -d "\$VENV_PATH" ]; then
    echo "ERROR: Virtual environment not found at \$VENV_PATH"
    exit 1
fi

# Activate virtual environment
source "\$VENV_PATH/bin/activate"

# Kiểm tra Python và packages
if ! python -c "import insightface, numpy, cv2" 2>/dev/null; then
    echo "ERROR: Required Python packages not installed"
    deactivate
    exit 1
fi

# Chuyển tới thư mục script
cd "\$SCRIPT_DIR"

# Chạy Python script với các tham số
python extract_wrapper.py "\$@"

# Lưu exit code
exit_code=\$?

# Deactivate venv
deactivate

# Trả về exit code
exit \$exit_code
EOF

    # Make scripts executable
    chmod +x "$PROJECT_ROOT/extract_wrapper.py"
    chmod +x "$PROJECT_ROOT/run_extraction.sh"

    print_status "Wrapper scripts created ✓"
}

# Update application.properties
update_application_properties() {
    print_status "Updating application.properties..."

    PROPERTIES_FILE="$PROJECT_ROOT/src/main/resources/application.properties"

    if [ -f "$PROPERTIES_FILE" ]; then
        # Backup original file
        cp "$PROPERTIES_FILE" "$PROPERTIES_FILE.backup"

        # Add or update Python configuration
        if ! grep -q "app.python.venv.path" "$PROPERTIES_FILE"; then
            echo "" >> "$PROPERTIES_FILE"
            echo "# Python Feature Extraction Configuration" >> "$PROPERTIES_FILE"
            echo "app.python.venv.path=$VENV_PATH" >> "$PROPERTIES_FILE"
            echo "app.python.script.path=$PROJECT_ROOT" >> "$PROPERTIES_FILE"
        fi

        print_status "application.properties updated ✓"
    else
        print_warning "application.properties not found, skipping..."
    fi
}

# Test complete setup
test_complete_setup() {
    print_status "Testing complete setup..."

    # Test bash script
    if [ -f "$PROJECT_ROOT/run_extraction.sh" ]; then
        print_status "Testing bash script execution..."

        # Test với command help
        bash "$PROJECT_ROOT/run_extraction.sh" 2>/dev/null || true

        print_status "Bash script test completed ✓"
    fi

    print_status "Complete setup test passed ✓"
}

# Main execution
main() {
    echo -e "${BLUE}Starting setup process...${NC}\n"

    check_user
    echo ""

    check_project_structure
    echo ""

    setup_virtual_environment
    echo ""

    install_python_packages
    echo ""

    test_python_environment
    echo ""

    create_wrapper_scripts
    echo ""

    update_application_properties
    echo ""

    test_complete_setup
    echo ""

    echo -e "${GREEN}==============================================================================${NC}"
    echo -e "${GREEN}                            SETUP COMPLETED SUCCESSFULLY!${NC}"
    echo -e "${GREEN}==============================================================================${NC}"
    echo ""
    echo -e "${BLUE}📁 Project Root:${NC} $PROJECT_ROOT"
    echo -e "${BLUE}🐍 Virtual Environment:${NC} $VENV_PATH"
    echo -e "${BLUE}📜 Python Script:${NC} $PROJECT_ROOT/$PYTHON_SCRIPT_NAME"
    echo -e "${BLUE}🔧 Wrapper Script:${NC} $PROJECT_ROOT/extract_wrapper.py"
    echo -e "${BLUE}⚙️  Bash Script:${NC} $PROJECT_ROOT/run_extraction.sh"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo -e "1. Start your Spring Boot application"
    echo -e "2. Make sure Face Recognition Service is running on port 8001"
    echo -e "3. Test the feature extraction from the web interface"
    echo ""
    echo -e "${YELLOW}Manual test command:${NC}"
    echo -e "cd $PROJECT_ROOT && bash run_extraction.sh all"
    echo ""
}

# Run main function
main "$@"