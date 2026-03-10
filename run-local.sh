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
mkdir -p models/huggingface models/torch models/cache

echo "🚀 Starting Telegram Bot with local model cache..."
echo "📦 Models will be stored in: $(pwd)/models/"
echo ""

# Activate virtual environment and run
source venv/bin/activate
./mvnw spring-boot:run
