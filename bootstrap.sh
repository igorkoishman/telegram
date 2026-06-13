#!/bin/bash
# Multi-platform bootstrap script for Telegram Bot (Mac/Linux)
# This script sets up the local Python virtual environment (venv) 
# and installs dependencies with hardware acceleration support.

set -e

echo "🚀 Bootstrapping Telegram Bot environment..."

# 1. Check for Java 21
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo "✅ Found Java: $JAVA_VER"
else
    echo "❌ Java not found. Please install JDK 21."
    exit 1
fi

# 2. Check for FFmpeg
if command -v ffmpeg >/dev/null 2>&1; then
    echo "✅ Found FFmpeg"
else
    echo "⚠️ FFmpeg not found. Installing via Homebrew..."
    if command -v brew >/dev/null 2>&1; then
        brew install ffmpeg
    else
        echo "❌ Homebrew not found. Please install FFmpeg manually."
        exit 1
    fi
fi

# 3. Setup Python Virtual Environment
PYTHON_CMD="python3"
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
    echo "❌ Python 3 not found. Please install Python 3.10+."
    exit 1
fi

echo "📦 Creating virtual environment in ./venv..."
$PYTHON_CMD -m venv venv

# 4. Install Dependencies
echo "📥 Installing Python dependencies..."
source venv/bin/activate

# Upgrade pip
pip install --upgrade pip

# Detect OS and Hardware for PyTorch
OS_NAME=$(uname -s)
ARCH_NAME=$(uname -m)

if [[ "$OS_NAME" == "Darwin" ]]; then
    echo "🍎 Detected macOS ($ARCH_NAME)"
    # Standard torch supports MPS on Mac
    pip install -r src/main/resources/python/requirements.txt
else
    echo "🐧 Detected Linux"
    # For Linux, we might want to check for CUDA
    pip install -r src/main/resources/python/requirements.txt
fi

# 5. Create/Update .env file
if [ ! -f .env ]; then
    echo "📝 Creating .env from .env.example..."
    cp .env.example .env
fi

# Ensure PYTHON_EXECUTABLE is set in .env
PYTHON_EXE_PATH="$(pwd)/venv/bin/python3"
if grep -q "PYTHON_EXECUTABLE" .env; then
    # Update existing line
    sed -i '' "s|PYTHON_EXECUTABLE=.*|PYTHON_EXECUTABLE=$PYTHON_EXE_PATH|g" .env 2>/dev/null || \
    sed -i "s|PYTHON_EXECUTABLE=.*|PYTHON_EXECUTABLE=$PYTHON_EXE_PATH|g" .env
else
    # Append to file
    echo "" >> .env
    echo "# Path to the python executable in the venv" >> .env
    echo "PYTHON_EXECUTABLE=$PYTHON_EXE_PATH" >> .env
fi

echo ""
echo "✅ Setup Complete!"
echo "------------------------------------------------"
echo "Machine: $OS_NAME $ARCH_NAME"
echo "Python: $(venv/bin/python3 --version)"
echo "Venv:   $PYTHON_EXE_PATH"
echo "------------------------------------------------"
echo "To run the application:"
echo "1. Edit .env and add your TELEGRAM_BOT_TOKEN"
echo "2. Run: ./run-local.sh"
echo ""
