@echo off
REM Multi-platform bootstrap script for Telegram Bot (Windows)
REM This script sets up the local Python virtual environment (venv)
REM and installs dependencies with hardware acceleration support.

echo 🚀 Bootstrapping Telegram Bot environment (Windows)...

REM 1. Check for Java 21
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java not found. Please install JDK 21 and add it to your PATH.
    pause
    exit /b 1
)
echo ✅ Found Java

REM 2. Check for FFmpeg
ffmpeg -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️ FFmpeg not found. Please install FFmpeg (choco install ffmpeg or download from gyan.dev)
) else (
    echo ✅ Found FFmpeg
)

REM 3. Setup Python Virtual Environment
python --version >nul 2>&1
if %errorlevel% neq 0 (
    python3 --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ❌ Python not found. Please install Python 3.10+ and add it to your PATH.
        pause
        exit /b 1
    )
    set PYTHON_CMD=python3
) else (
    set PYTHON_CMD=python
)

echo 📦 Creating virtual environment in .\venv...
%PYTHON_CMD% -m venv venv

REM 4. Install Dependencies
echo 📥 Installing Python dependencies...
call venv\Scripts\activate

REM Upgrade pip
python -m pip install --upgrade pip

REM Install PyTorch for Windows with CUDA 12.1 support
echo 🚀 Installing PyTorch with CUDA support (for NVIDIA RTX GPU)...
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

REM Install other requirements
pip install -r src\main\resources\python\requirements.txt

REM 5. Create/Update .env file
if not exist .env (
    echo 📝 Creating .env from .env.example...
    copy .env.example .env
)

REM Ensure PYTHON_EXECUTABLE is set in .env
set PYTHON_EXE_PATH=%cd%\venv\Scripts\python.exe
findstr /C:"PYTHON_EXECUTABLE" .env >nul
if %errorlevel% equ 0 (
    echo ⚠️ Please manually update PYTHON_EXECUTABLE in .env to: %PYTHON_EXE_PATH%
) else (
    echo. >> .env
    echo # Path to the python executable in the venv >> .env
    echo PYTHON_EXECUTABLE=%PYTHON_EXE_PATH% >> .env
)

echo.
echo ✅ Setup Complete!
echo ------------------------------------------------
echo Venv:   %PYTHON_EXE_PATH%
echo ------------------------------------------------
echo To run the application:
echo 1. Edit .env and add your TELEGRAM_BOT_TOKEN
echo 2. Run: .\mvnw spring-boot:run
echo.
pause
