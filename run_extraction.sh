#!/bin/bash

# Đường dẫn tới virtual environment
VENV_PATH="scripts/face_recognition/.venv"
SCRIPT_DIR="."

# Kiểm tra venv tồn tại
if [ ! -d "$VENV_PATH" ]; then
    echo "ERROR: Virtual environment not found at $VENV_PATH"
    exit 1
fi

# Activate virtual environment
source "$VENV_PATH/bin/activate"

# Kiểm tra Python và packages
if ! python -c "import insightface, numpy, cv2" 2>/dev/null; then
    echo "ERROR: Required Python packages not installed"
    deactivate
    exit 1
fi

# Chuyển tới thư mục script
cd "$SCRIPT_DIR"

# Chạy Python script với các tham số
python extract_wrapper.py "$@"

# Lưu exit code
exit_code=$?

# Deactivate venv
deactivate

# Trả về exit code
exit $exit_code
