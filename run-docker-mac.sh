#!/bin/bash
# Script to run Telegram bot on Mac with maximum performance
# This handles Docker resource configuration automatically

set -e

echo "🚀 Starting Telegram Bot on Mac with optimized settings..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi

# Check Docker resource allocation
echo -e "\n${YELLOW}📊 Checking Docker resource allocation...${NC}"
DOCKER_CPUS=$(docker info 2>/dev/null | grep "CPUs:" | awk '{print $2}')
DOCKER_MEM=$(docker info 2>/dev/null | grep "Total Memory:" | awk '{print $3$4}')

echo "Current Docker settings:"
echo "  CPUs: $DOCKER_CPUS"
echo "  Memory: $DOCKER_MEM"

# Recommend settings
echo -e "\n${YELLOW}💡 Recommended Docker Desktop settings for maximum performance:${NC}"
echo "  1. Open Docker Desktop → Settings → Resources"
echo "  2. Set CPUs: 12-14 (leave 2-4 for macOS)"
echo "  3. Set Memory: 12-16 GB (depending on your total RAM)"
echo "  4. Set Swap: 2 GB"
echo "  5. Click 'Apply & Restart'"
echo ""
read -p "Have you configured Docker resources? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}⚠️  Continuing with current settings. Performance may be limited.${NC}"
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

# Java Memory (adjust based on Docker memory allocation)
JAVA_OPTS=-Xmx8g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Model Configuration (lightweight for faster startup)
TRANSLATION_MODELS_AUTO_DOWNLOAD=true
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
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
if docker ps -a --format '{{.Names}}' | grep -q '^telegram-translator$'; then
    echo -e "${YELLOW}🛑 Stopping existing container...${NC}"
    docker-compose -f docker-compose.mac.yml down
fi

# Ask user preference
echo -e "\n${GREEN}Choose an option:${NC}"
echo "  1) Build and run (recommended for first time)"
echo "  2) Run existing image"
echo "  3) Rebuild from scratch"
read -p "Enter your choice (1-3): " choice

case $choice in
    1)
        echo -e "\n${GREEN}🔨 Building and starting application...${NC}"
        docker-compose -f docker-compose.mac.yml up --build -d
        ;;
    2)
        echo -e "\n${GREEN}▶️  Starting application...${NC}"
        docker-compose -f docker-compose.mac.yml up -d
        ;;
    3)
        echo -e "\n${GREEN}🔨 Rebuilding from scratch...${NC}"
        docker-compose -f docker-compose.mac.yml build --no-cache
        docker-compose -f docker-compose.mac.yml up -d
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

# Wait for container to start
echo -e "\n${YELLOW}⏳ Waiting for container to start...${NC}"
sleep 5

# Show logs
echo -e "\n${GREEN}📋 Container logs (Ctrl+C to exit, container will keep running):${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
docker-compose -f docker-compose.mac.yml logs -f

# Note: When user presses Ctrl+C, the container continues running in background
