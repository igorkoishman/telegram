# Docker Setup for All Your Computers

This guide covers running the Telegram bot on **all three of your computers**:
- 🍎 **Mac** (Docker, CPU only, no GPU support)
- 🎮 **Windows with NVIDIA GPU** (Docker with full GPU acceleration)
- 🐧 **Small Linux Dell Micro** (Docker, CPU only, lightweight)

---

## Quick Reference

| Computer | File to Use | GPU Support | Performance | Use Case |
|----------|-------------|-------------|-------------|----------|
| **Mac** | `docker-compose.mac.yml` | ❌ No | Medium | Development/Testing |
| **Windows + NVIDIA** | `docker-compose.gpu.yml` | ✅ Yes | **Fastest** | **Production** |
| **Small Linux** | `docker-compose.cpu.yml` | ❌ No | Slow | Backup/Light testing |

---

## 🍎 Setup for Mac

### Configuration
- **File**: `docker-compose.mac.yml`
- **Performance**: CPU only (no GPU access on Mac)
- **Resource Requirements**: 12-16GB RAM, 12-14 CPU cores

### Quick Start
```bash
# Option 1: Automated script
./run-docker-mac.sh

# Option 2: Manual
docker-compose -f docker-compose.mac.yml up --build -d
docker-compose -f docker-compose.mac.yml logs -f
```

### Docker Desktop Settings
1. Open Docker Desktop → Settings → Resources
2. **CPUs**: 12-14 (leave 2-4 for macOS)
3. **Memory**: 12-16GB
4. **Swap**: 2GB
5. Click "Apply & Restart"

### Environment Variables (.env)
```bash
TELEGRAM_BOT_TOKEN=your_bot_token

# Mac settings (conservative for CPU-only)
JAVA_OPTS=-Xmx8g -Xms2g -XX:+UseG1GC
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

### Expected Performance
- Whisper transcription (30s audio): ~45 seconds
- Translation: ~10 seconds
- Good for development and testing

📖 **Detailed Guide**: See `DOCKER_MAC_GUIDE.md`

---

## 🎮 Setup for Windows with NVIDIA GPU

### Configuration
- **File**: `docker-compose.gpu.yml`
- **Performance**: CUDA GPU acceleration (**FASTEST**)
- **Resource Requirements**: 16GB RAM, NVIDIA GPU with 6GB+ VRAM

### Prerequisites
1. Install NVIDIA GPU drivers: https://www.nvidia.com/Download/index.aspx
2. Install WSL2: `wsl --install` (in PowerShell as Admin)
3. Enable GPU in Docker Desktop:
   - Settings → General → "Use WSL 2 based engine"
   - Settings → Resources → WSL Integration → Enable

### Verify GPU Access
```bash
# In WSL2 terminal
docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
```
You should see your GPU information!

### Quick Start
```bash
# Option 1: Windows batch script
run-docker-gpu.bat

# Option 2: WSL2 bash script
./run-docker-gpu.sh

# Option 3: Manual
docker-compose -f docker-compose.gpu.yml up --build -d
docker-compose -f docker-compose.gpu.yml logs -f
```

### Environment Variables (.env)
```bash
TELEGRAM_BOT_TOKEN=your_bot_token

# GPU settings (can use larger models)
JAVA_OPTS=-Xmx12g -Xms4g -XX:+UseG1GC
TRANSLATION_MODELS_WHISPER_MODELS=small,medium
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```

### Expected Performance
- **RTX 3060/3070**: Whisper (30s audio) ~5-8 seconds
- **RTX 3080/3090**: Whisper (30s audio) ~3-4 seconds
- **RTX 4080/4090**: Whisper (30s audio) ~1.5-2 seconds

📖 **Detailed Guide**: See `DOCKER_GPU_GUIDE.md`

---

## 🐧 Setup for Small Linux (Dell Micro)

### Configuration
- **File**: `docker-compose.cpu.yml`
- **Performance**: CPU only, lightweight
- **Resource Requirements**: 4-8GB RAM minimum

### Quick Start
```bash
# Install Docker (if not installed)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
# Logout and login again

# Run the bot
docker-compose -f docker-compose.cpu.yml up --build -d
docker-compose -f docker-compose.cpu.yml logs -f
```

### Environment Variables (.env)
```bash
TELEGRAM_BOT_TOKEN=your_bot_token

# Lightweight settings for small machine
JAVA_OPTS=-Xmx2g -Xms512m
TRANSLATION_MODELS_WHISPER_MODELS=tiny
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

### Resource Limits
The CPU compose file is pre-configured for lightweight operation:
- **Memory**: 4GB max
- **Models**: Only downloads `tiny` Whisper model
- **Optimized**: For low-resource environments

### Expected Performance
- Whisper transcription (30s audio): ~60-120 seconds (depends on CPU)
- Use only for light testing or as a backup

---

## Common Commands (All Platforms)

### Starting the Bot
```bash
# Mac
docker-compose -f docker-compose.mac.yml up -d

# Windows/Linux with GPU
docker-compose -f docker-compose.gpu.yml up -d

# Small Linux without GPU
docker-compose -f docker-compose.cpu.yml up -d
```

### Viewing Logs
```bash
# Follow logs (Ctrl+C to exit, bot keeps running)
docker-compose -f <your-compose-file> logs -f

# Last 100 lines
docker-compose -f <your-compose-file> logs --tail=100
```

### Stopping the Bot
```bash
docker-compose -f <your-compose-file> down
```

### Rebuilding
```bash
# Rebuild and restart
docker-compose -f <your-compose-file> up --build -d

# Full rebuild (no cache)
docker-compose -f <your-compose-file> build --no-cache
docker-compose -f <your-compose-file> up -d
```

### Monitoring
```bash
# Resource usage
docker stats telegram-translator

# GPU usage (Windows/Linux with GPU only)
nvidia-smi -l 1

# Enter container
docker exec -it telegram-translator bash
```

---

## Sharing Configuration Between Computers

### .env File
Create the same `.env` on all three computers with appropriate settings:

**Mac (.env)**
```bash
TELEGRAM_BOT_TOKEN=your_token
JAVA_OPTS=-Xmx8g -Xms2g
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
```

**Windows GPU (.env)**
```bash
TELEGRAM_BOT_TOKEN=your_token
JAVA_OPTS=-Xmx12g -Xms4g
TRANSLATION_MODELS_WHISPER_MODELS=small,medium
```

**Small Linux (.env)**
```bash
TELEGRAM_BOT_TOKEN=your_token
JAVA_OPTS=-Xmx2g -Xms512m
TRANSLATION_MODELS_WHISPER_MODELS=tiny
```

### Using Docker Hub (Optional)
Build on one machine, push to Docker Hub, pull on others:

```bash
# On any machine - build and push
docker-compose -f docker-compose.gpu.yml build
docker push igorkoishman/telegram-translator:gpu

# On other machines - pull and run
docker-compose -f docker-compose.gpu.yml pull
docker-compose -f docker-compose.gpu.yml up -d
```

---

## Troubleshooting

### All Platforms
**Issue**: Container keeps restarting
```bash
docker-compose -f <your-file> logs --tail=50
```

**Issue**: Bot token not recognized
```bash
# Check .env file exists and has correct token
cat .env | grep TELEGRAM_BOT_TOKEN
```

### Mac Specific
**Issue**: Docker says "not enough memory"
- Increase Docker Desktop memory allocation (Settings → Resources)

### Windows GPU Specific
**Issue**: GPU not detected in container
```bash
# Test GPU access
docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
```

**Issue**: WSL2 using too much RAM
Create `C:\Users\YourName\.wslconfig`:
```ini
[wsl2]
memory=16GB
processors=8
```

### Small Linux Specific
**Issue**: Out of memory
```bash
# Reduce Java memory in .env
JAVA_OPTS=-Xmx1g -Xms256m

# Use only tiny model
TRANSLATION_MODELS_WHISPER_MODELS=tiny
```

---

## Recommended Usage Strategy

1. **Development** → Use Mac (`docker-compose.mac.yml`)
   - Easy to develop and test
   - Good performance for testing

2. **Production** → Use Windows with GPU (`docker-compose.gpu.yml`)
   - Fastest processing
   - Best user experience
   - Handle high load

3. **Backup/Testing** → Use Small Linux (`docker-compose.cpu.yml`)
   - Lightweight backup
   - Emergency fallback
   - Basic functionality testing

---

## Performance Summary

| Platform | Whisper (30s) | Translation | Total | Best For |
|----------|---------------|-------------|-------|----------|
| **Windows GPU (RTX 3080)** | 3s | 1s | **4s** | **Production** |
| **Mac (16-core)** | 45s | 10s | 55s | Development |
| **Small Linux** | 90s+ | 15s | 105s+ | Backup only |

---

## File Reference

- `docker-compose.mac.yml` - Mac CPU-optimized configuration
- `docker-compose.gpu.yml` - Windows/Linux GPU configuration
- `docker-compose.cpu.yml` - Small Linux lightweight configuration
- `run-docker-mac.sh` - Mac automated setup script
- `run-docker-gpu.sh` - Linux/WSL2 GPU setup script
- `run-docker-gpu.bat` - Windows GPU setup script
- `DOCKER_MAC_GUIDE.md` - Detailed Mac guide
- `DOCKER_GPU_GUIDE.md` - Detailed GPU guide

---

## Next Steps

1. Choose your platform above
2. Follow the quick start for that platform
3. Create `.env` file with your bot token
4. Run the appropriate docker-compose file
5. Monitor logs to ensure it's working

For detailed troubleshooting and advanced configuration, see the platform-specific guides!
