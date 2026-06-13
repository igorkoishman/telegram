# Running Telegram Bot on Mac with Docker

## Important: GPU Limitations on Mac

⚠️ **Docker on Mac CANNOT access GPU** (neither NVIDIA nor Apple Silicon GPU)
- Docker Desktop runs in a Linux VM that doesn't have GPU access
- For GPU acceleration, you need to run Python models natively (outside Docker)
- This setup is optimized for **CPU performance only**

## Quick Start

### Option 1: Automated Script (Recommended)
```bash
./run-docker-mac.sh
```

### Option 2: Manual Docker Compose
```bash
# Build and run
docker-compose -f docker-compose.mac.yml up --build -d

# View logs
docker-compose -f docker-compose.mac.yml logs -f

# Stop
docker-compose -f docker-compose.mac.yml down
```

## Maximum Performance Configuration

### 1. Configure Docker Desktop Resources
Open **Docker Desktop → Settings → Resources** and set:

- **CPUs**: 12-14 (leave 2-4 cores for macOS)
- **Memory**: 12-16 GB (depending on your total RAM)
  - 16GB Mac → allocate 12GB to Docker
  - 32GB Mac → allocate 16GB to Docker
  - 64GB+ Mac → allocate 24GB to Docker
- **Swap**: 2 GB
- **Disk image size**: At least 60 GB (for models)

Click **Apply & Restart** after changes.

### 2. Environment Variables (.env file)
Create a `.env` file in the project root:

```bash
# Required
TELEGRAM_BOT_TOKEN=your_actual_token_here

# Docker Configuration
DOCKER_USERNAME=igorkoishman

# Java Memory Settings (adjust based on Docker RAM allocation)
# If Docker has 12GB, allocate 8GB to Java
# If Docker has 16GB, allocate 10GB to Java
JAVA_OPTS=-Xmx8g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Model Configuration
TRANSLATION_MODELS_AUTO_DOWNLOAD=true
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

### 3. Performance Optimizations Applied

The `docker-compose.mac.yml` includes:

✅ **CPU limits**: Uses 15 CPUs (reserves 8)
✅ **Memory limits**: 12GB max, 4GB reserved
✅ **JVM tuning**: G1GC with optimized settings
✅ **Thread optimization**: OMP and MKL threads configured
✅ **tmpfs**: Fast temporary storage in RAM
✅ **seccomp unconfined**: Removes syscall restrictions for better performance

## Resource Monitoring

### Check Container Resource Usage
```bash
# Real-time stats
docker stats telegram-translator

# Container info
docker inspect telegram-translator | grep -A 10 "Resources"
```

### Check Docker System Resources
```bash
docker system df
docker info | grep -E "CPUs:|Total Memory:"
```

## Troubleshooting

### Issue: Out of Memory
**Solution**: Increase Docker Desktop memory allocation or reduce `JAVA_OPTS` `-Xmx` value

```bash
# Edit .env and reduce from -Xmx8g to -Xmx4g
JAVA_OPTS=-Xmx4g -Xms1g -XX:+UseG1GC
```

### Issue: Slow Model Downloads
**Solution**: Models are cached. First run will be slow, subsequent runs will be fast.

```bash
# Check cached models
docker exec telegram-translator ls -lh /root/.cache/huggingface/hub
docker exec telegram-translator ls -lh /root/.cache/whisper
```

### Issue: Container Keeps Restarting
```bash
# Check logs
docker-compose -f docker-compose.mac.yml logs --tail=100

# Check health
docker inspect telegram-translator | grep -A 20 "Health"
```

## Alternative: Native Execution (Best Performance on Mac)

For **maximum performance with Apple Silicon GPU**, run natively:

```bash
# Install dependencies
brew install python@3.11 ffmpeg

# Install Python packages
pip3 install -r src/main/resources/python/requirements.txt

# Run with Maven
./mvnw spring-boot:run
```

**Advantages of native execution:**
- Access to Apple Silicon GPU via Metal
- No Docker overhead
- Faster I/O operations
- Better memory management

**Disadvantages:**
- Need to manage dependencies manually
- Less portable

## Useful Commands

```bash
# Start container
docker-compose -f docker-compose.mac.yml up -d

# Stop container
docker-compose -f docker-compose.mac.yml down

# Restart container
docker-compose -f docker-compose.mac.yml restart

# View logs (follow)
docker-compose -f docker-compose.mac.yml logs -f

# Enter container shell
docker exec -it telegram-translator bash

# Rebuild without cache
docker-compose -f docker-compose.mac.yml build --no-cache

# Clean up everything
docker-compose -f docker-compose.mac.yml down -v
docker system prune -a
```

## Performance Comparison

| Method | GPU | Startup Time | Processing Speed | Memory Usage |
|--------|-----|--------------|------------------|--------------|
| **Native (Mac)** | ✅ Metal | Fast | Fastest | Moderate |
| **Docker (Mac)** | ❌ CPU only | Moderate | Slower | High |
| **Docker (Linux + NVIDIA)** | ✅ CUDA | Moderate | Very Fast | High |

## Conclusion

For **Mac development/testing**: Docker is fine
For **Mac production**: Consider native execution for better performance
For **best AI performance**: Use Linux with NVIDIA GPU
