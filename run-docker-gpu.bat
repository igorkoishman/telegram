@echo off
REM Script to run Telegram bot with NVIDIA GPU on Windows
REM Maximum performance with CUDA acceleration

echo 🚀 Starting Telegram Bot with NVIDIA GPU acceleration...
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not running. Please start Docker Desktop.
    pause
    exit /b 1
)

REM Check for NVIDIA GPU
nvidia-smi >nul 2>&1
if errorlevel 1 (
    echo ❌ No NVIDIA GPU detected or drivers not installed
    echo.
    echo Please install NVIDIA GPU drivers from:
    echo https://www.nvidia.com/Download/index.aspx
    pause
    exit /b 1
)

REM Show GPU information
echo 📊 GPU Information:
nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv,noheader
echo.

REM Check for .env file
if not exist .env (
    echo ❌ .env file not found!
    echo Creating .env template...
    (
        echo # Telegram Bot Configuration
        echo TELEGRAM_BOT_TOKEN=your_bot_token_here
        echo.
        echo # Docker Configuration
        echo DOCKER_USERNAME=igorkoishman
        echo.
        echo # Java Memory ^(GPU version - can use larger models^)
        echo JAVA_OPTS=-Xmx12g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
        echo.
        echo # Model Configuration ^(larger models for GPU^)
        echo TRANSLATION_MODELS_AUTO_DOWNLOAD=true
        echo TRANSLATION_MODELS_WHISPER_MODELS=small,medium
        echo TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
        echo TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
    ) > .env
    echo ⚠️  Please edit .env file and add your TELEGRAM_BOT_TOKEN
    pause
    exit /b 1
)

REM Check if token is set
findstr /C:"TELEGRAM_BOT_TOKEN=your_bot_token_here" .env >nul
if not errorlevel 1 (
    echo ❌ TELEGRAM_BOT_TOKEN not set in .env file
    echo Please edit .env and set your actual bot token
    pause
    exit /b 1
)

REM Stop existing container if running
docker ps -a --format "{{.Names}}" | findstr "^telegram-translator-gpu$" >nul
if not errorlevel 1 (
    echo 🛑 Stopping existing container...
    docker-compose -f docker-compose.gpu.yml down
)

REM Ask user preference
echo.
echo Choose an option:
echo   1) Build and run (recommended for first time)
echo   2) Run existing image
echo   3) Rebuild from scratch
echo   4) Test GPU in container
echo.
set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" (
    echo.
    echo 🔨 Building and starting application with GPU support...
    docker-compose -f docker-compose.gpu.yml up --build -d
) else if "%choice%"=="2" (
    echo.
    echo ▶️  Starting application...
    docker-compose -f docker-compose.gpu.yml up -d
) else if "%choice%"=="3" (
    echo.
    echo 🔨 Rebuilding from scratch...
    docker-compose -f docker-compose.gpu.yml build --no-cache
    docker-compose -f docker-compose.gpu.yml up -d
) else if "%choice%"=="4" (
    echo.
    echo 🧪 Testing GPU in container...
    docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
    pause
    exit /b 0
) else (
    echo Invalid choice
    pause
    exit /b 1
)

REM Wait for container to start
echo.
echo ⏳ Waiting for container to start...
timeout /t 5 /nobreak >nul

REM Test GPU access in container
echo.
echo 🧪 Testing GPU access in container...
docker exec telegram-translator-gpu nvidia-smi >nul 2>&1
if not errorlevel 1 (
    echo ✅ GPU is accessible in container
    docker exec telegram-translator-gpu nvidia-smi --query-gpu=name,memory.used,memory.total --format=csv,noheader
) else (
    echo ⚠️  Could not verify GPU access ^(container may still be starting^)
)

REM Show logs
echo.
echo 📋 Container logs (Ctrl+C to exit, container will keep running):
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
docker-compose -f docker-compose.gpu.yml logs -f
