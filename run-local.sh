#!/bin/bash
# Startup script for running the bot locally on Mac

# Load environment variables from .env (properly handle values with spaces)
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# Set model cache directories to project folder
export HF_HOME="$(pwd)/models"
export TRANSFORMERS_CACHE="$(pwd)/models/huggingface"
export TORCH_HOME="$(pwd)/models/torch"
export XDG_CACHE_HOME="$(pwd)/models/cache"

# Create directories if they don't exist
mkdir -p models/huggingface models/torch models/cache uploads outputs downloads

echo "🚀 Starting Telegram Bot with local model cache..."
echo "📦 Models will be stored in: $(pwd)/models/"
echo "🐍 Python: ${PYTHON_EXECUTABLE:-./venv/bin/python3}"
echo ""

# Export JAVA_HOME for JDK 21 if installed via Homebrew
if [ -d "/opt/homebrew/opt/openjdk@21" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
fi

# Run with Remote Debugging enabled on port 5005
MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ./mvnw spring-boot:run
