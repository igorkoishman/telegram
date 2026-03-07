#!/bin/bash
# Script to run Telegram bot with NVIDIA GPU (Windows/Linux)
# Maximum performance with CUDA acceleration

set -e

echo "🚀 Starting Telegram Bot with NVIDIA GPU acceleration..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker.${NC}"
    exit 1
fi

# Check for NVIDIA Docker runtime
echo -e "\n${YELLOW}🔍 Checking NVIDIA Docker runtime...${NC}"
if docker info 2>/dev/null | grep -q "nvidia"; then
    echo -e "${GREEN}✅ NVIDIA Docker runtime detected${NC}"
elif command -v nvidia-smi &> /dev/null; then
    echo -e "${YELLOW}⚠️  NVIDIA GPU detected but Docker runtime not configured${NC}"
    echo -e "${BLUE}To install NVIDIA Docker runtime:${NC}"
    echo ""
    echo "Windows (WSL2):"
    echo "  1. Install latest NVIDIA drivers for Windows"
    echo "  2. Update WSL2: wsl --update"
    echo "  3. Install NVIDIA Container Toolkit in WSL2"
    echo ""
    echo "Linux:"
    echo "  distribution=\$(. /etc/os-release;echo \$ID\$VERSION_ID)"
    echo "  curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -"
    echo "  curl -s -L https://nvidia.github.io/nvidia-docker/\$distribution/nvidia-docker.list | sudo tee /etc/apt/sources.list.d/nvidia-docker.list"
    echo "  sudo apt-get update && sudo apt-get install -y nvidia-docker2"
    echo "  sudo systemctl restart docker"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${RED}❌ No NVIDIA GPU detected${NC}"
    echo -e "${YELLOW}Use docker-compose.cpu.yml or docker-compose.mac.yml instead${NC}"
    exit 1
fi

# Show GPU information
if command -v nvidia-smi &> /dev/null; then
    echo -e "\n${GREEN}📊 GPU Information:${NC}"
    nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv,noheader
fi

# Check for .env file
if [ ! -f .env ]; then
    echo -e "${RED}❌ .env file not found!${NC}"
    echo "Creating .env template..."
    cat > .env << 'EOF'
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_bot_token_here

# Docker Configuration
DOCKER_USERNAME=igorkoishman

# Java Memory (GPU version - can use larger models)
JAVA_OPTS=-Xmx12g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Model Configuration (larger models for GPU)
TRANSLATION_MODELS_AUTO_DOWNLOAD=true
TRANSLATION_MODELS_WHISPER_MODELS=small,medium
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
EOF
    echo -e "${YELLOW}⚠️  Please edit .env file and add your TELEGRAM_BOT_TOKEN${NC}"
    exit 1
fi

# Source .env file
set -a
source .env
set +a

# Check if token is set
if [ "$TELEGRAM_BOT_TOKEN" = "your_bot_token_here" ] || [ -z "$TELEGRAM_BOT_TOKEN" ]; then
    echo -e "${RED}❌ TELEGRAM_BOT_TOKEN not set in .env file${NC}"
    exit 1
fi

# Stop existing container if running
if docker ps -a --format '{{.Names}}' | grep -q '^telegram-translator-gpu$'; then
    echo -e "${YELLOW}🛑 Stopping existing container...${NC}"
    docker-compose -f docker-compose.gpu.yml down
fi

# Ask user preference
echo -e "\n${GREEN}Choose an option:${NC}"
echo "  1) Build and run (recommended for first time)"
echo "  2) Run existing image"
echo "  3) Rebuild from scratch"
echo "  4) Test GPU in container"
read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        echo -e "\n${GREEN}🔨 Building and starting application with GPU support...${NC}"
        docker-compose -f docker-compose.gpu.yml up --build -d
        ;;
    2)
        echo -e "\n${GREEN}▶️  Starting application...${NC}"
        docker-compose -f docker-compose.gpu.yml up -d
        ;;
    3)
        echo -e "\n${GREEN}🔨 Rebuilding from scratch...${NC}"
        docker-compose -f docker-compose.gpu.yml build --no-cache
        docker-compose -f docker-compose.gpu.yml up -d
        ;;
    4)
        echo -e "\n${GREEN}🧪 Testing GPU in container...${NC}"
        docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

# Wait for container to start
echo -e "\n${YELLOW}⏳ Waiting for container to start...${NC}"
sleep 5

# Test GPU access in container
echo -e "\n${BLUE}🧪 Testing GPU access in container...${NC}"
if docker exec telegram-translator-gpu nvidia-smi &> /dev/null; then
    echo -e "${GREEN}✅ GPU is accessible in container${NC}"
    docker exec telegram-translator-gpu nvidia-smi --query-gpu=name,memory.used,memory.total --format=csv,noheader
else
    echo -e "${YELLOW}⚠️  Could not verify GPU access (container may still be starting)${NC}"
fi

# Show logs
echo -e "\n${GREEN}📋 Container logs (Ctrl+C to exit, container will keep running):${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
docker-compose -f docker-compose.gpu.yml logs -f
