# Telegram Video Translation Bot - Docker Setup

## 🚀 Quick Start Guide

Choose your platform and run the appropriate command:

### 🍎 Mac
```bash
./run-docker-mac.sh
```

### 🎮 Windows with NVIDIA GPU
```bash
run-docker-gpu.bat
```

### 🐧 Linux (Small Dell Micro or any CPU-only)
```bash
docker-compose -f docker-compose.cpu.yml up --build -d
```

---

## 📋 What You Need

### Before Starting (All Platforms)
1. Docker installed and running
2. `.env` file with your `TELEGRAM_BOT_TOKEN`
3. At least 4GB free RAM
4. At least 20GB free disk space (for models)

### Platform-Specific Requirements

| Platform | Docker Desktop RAM | CPU Cores | GPU | Notes |
|----------|-------------------|-----------|-----|-------|
| **Mac** | 12-16GB | 12-14 | None | Cannot use Mac GPU |
| **Windows + NVIDIA** | 16GB | 12+ | NVIDIA 6GB+ VRAM | **Best performance** |
| **Small Linux** | 4-8GB | 4+ | None | Lightweight mode |

---

## 📁 Files Overview

| File | Purpose | For |
|------|---------|-----|
| `docker-compose.mac.yml` | Mac-optimized, CPU only | 🍎 Mac |
| `docker-compose.gpu.yml` | GPU-accelerated, maximum performance | 🎮 Windows/Linux with NVIDIA |
| `docker-compose.cpu.yml` | Lightweight, CPU only | 🐧 Small Linux / any CPU-only system |
| `docker-compose.yml` | Original GPU config | 🎮 Alternative for GPU systems |
| `run-docker-mac.sh` | Automated Mac setup | 🍎 Mac users |
| `run-docker-gpu.sh` | Automated GPU setup (bash) | 🐧 Linux users |
| `run-docker-gpu.bat` | Automated GPU setup (batch) | 🎮 Windows users |

---

## ⚙️ Configuration

### 1. Create .env File

Create a file named `.env` in the project root:

```bash
# Required - Your Telegram bot token
TELEGRAM_BOT_TOKEN=your_actual_bot_token_here

# Optional - Docker username for pushing images
DOCKER_USERNAME=igorkoishman

# Adjust based on your platform:

# For Mac (8GB Docker RAM)
JAVA_OPTS=-Xmx6g -Xms2g -XX:+UseG1GC
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small

# For Windows GPU (16GB+ system RAM)
JAVA_OPTS=-Xmx12g -Xms4g -XX:+UseG1GC
TRANSLATION_MODELS_WHISPER_MODELS=small,medium

# For Small Linux (4-8GB RAM)
JAVA_OPTS=-Xmx2g -Xms512m
TRANSLATION_MODELS_WHISPER_MODELS=tiny
```

### 2. Get Your Bot Token

1. Message [@BotFather](https://t.me/botfather) on Telegram
2. Send `/newbot`
3. Follow instructions
4. Copy the token to your `.env` file

---

## 🔧 Platform-Specific Setup

### 🍎 Mac Setup

**Important**: Docker on Mac **cannot access GPU** (neither NVIDIA nor Apple Silicon)

1. **Configure Docker Desktop**:
   - Open Docker Desktop → Settings → Resources
   - CPUs: 12-14 (leave 2-4 for macOS)
   - Memory: 12-16GB (increase from default 7.65GB!)
   - Click "Apply & Restart"

2. **Create .env file** (see above)

3. **Run**:
   ```bash
   ./run-docker-mac.sh
   ```
   Or manually:
   ```bash
   docker-compose -f docker-compose.mac.yml up --build -d
   ```

📖 **Full guide**: `DOCKER_MAC_GUIDE.md`

---

### 🎮 Windows with NVIDIA GPU

**Best Performance** - Recommended for production!

1. **Install NVIDIA Drivers**:
   - Download from: https://www.nvidia.com/Download/index.aspx
   - Install and restart

2. **Install/Update WSL2**:
   ```powershell
   # Run in PowerShell as Administrator
   wsl --install
   wsl --update
   ```

3. **Enable GPU in Docker Desktop**:
   - Settings → General → Enable "Use WSL 2 based engine"
   - Settings → Resources → WSL Integration → Enable for your distro
   - Apply & Restart

4. **Verify GPU access**:
   ```bash
   docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
   ```
   You should see your GPU!

5. **Create .env file** (see above)

6. **Run**:
   ```bash
   # Double-click or run from cmd
   run-docker-gpu.bat
   ```
   Or in WSL2:
   ```bash
   ./run-docker-gpu.sh
   ```

📖 **Full guide**: `DOCKER_GPU_GUIDE.md`

---

### 🐧 Small Linux (Dell Micro)

Lightweight setup for resource-constrained systems.

1. **Install Docker** (if needed):
   ```bash
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   # Logout and login
   ```

2. **Create .env file** with lightweight settings:
   ```bash
   TELEGRAM_BOT_TOKEN=your_token
   JAVA_OPTS=-Xmx2g -Xms512m
   TRANSLATION_MODELS_WHISPER_MODELS=tiny
   ```

3. **Run**:
   ```bash
   docker-compose -f docker-compose.cpu.yml up --build -d
   ```

---

## 📊 Performance Comparison

| Platform | Whisper Transcription* | Best For |
|----------|----------------------|----------|
| **Windows RTX 3080** | ~3 seconds | **Production ⭐** |
| **Mac (16-core CPU)** | ~45 seconds | Development ✏️ |
| **Small Linux** | ~90+ seconds | Backup/Testing 🔧 |

*For 30 seconds of audio

---

## 🛠️ Common Commands

### Start the Bot
```bash
# Mac
docker-compose -f docker-compose.mac.yml up -d

# Windows/Linux GPU
docker-compose -f docker-compose.gpu.yml up -d

# Small Linux CPU
docker-compose -f docker-compose.cpu.yml up -d
```

### View Logs
```bash
# Follow logs (Ctrl+C to exit, bot continues running)
docker-compose -f <your-file> logs -f

# Last 100 lines
docker-compose -f <your-file> logs --tail=100
```

### Stop the Bot
```bash
docker-compose -f <your-file> down
```

### Rebuild
```bash
# Quick rebuild
docker-compose -f <your-file> up --build -d

# Full rebuild (clears cache)
docker-compose -f <your-file> build --no-cache
```

### Check Status
```bash
# Container status
docker ps

# Resource usage
docker stats

# GPU usage (GPU platforms only)
nvidia-smi -l 1
```

---

## 🐛 Troubleshooting

### Bot Not Starting

```bash
# Check logs
docker-compose -f <your-file> logs --tail=50

# Common issues:
# 1. Missing or incorrect TELEGRAM_BOT_TOKEN in .env
# 2. Port 8080 already in use
# 3. Not enough RAM/disk space
```

### Mac: "Not enough memory"
- Increase Docker Desktop memory: Settings → Resources → Memory

### Windows: GPU not detected
```bash
# Test GPU
docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi

# If fails:
# - Update NVIDIA drivers
# - Enable WSL2 GPU in Docker Desktop
# - Run 'wsl --update'
```

### Linux: Out of memory
```bash
# Edit .env - reduce memory
JAVA_OPTS=-Xmx1g -Xms256m
TRANSLATION_MODELS_WHISPER_MODELS=tiny
```

---

## 📚 Documentation

- **`DOCKER_SETUP_ALL_PLATFORMS.md`** - Complete guide for all 3 platforms
- **`DOCKER_MAC_GUIDE.md`** - Detailed Mac setup and optimization
- **`DOCKER_GPU_GUIDE.md`** - Detailed GPU setup for Windows/Linux

---

## 💡 Tips

1. **First time?** Start with `tiny` Whisper model, test it works, then upgrade to `small` or `medium`

2. **Multiple computers?** Use the same `.env` but adjust `JAVA_OPTS` and `WHISPER_MODELS` per platform

3. **Models are cached** - First run downloads models (slow), subsequent runs are fast

4. **Production deployment** - Use Windows GPU version for best user experience

5. **Development** - Use Mac version for convenience and testing

---

## 🎯 Recommended Strategy

```
┌─────────────┬──────────────────┬─────────────────┐
│   Computer  │   Use For        │   Config File   │
├─────────────┼──────────────────┼─────────────────┤
│ Mac         │ Development      │ mac.yml         │
│ Windows GPU │ Production ⭐     │ gpu.yml         │
│ Small Linux │ Backup/Testing   │ cpu.yml         │
└─────────────┴──────────────────┴─────────────────┘
```

---

## ❓ Need Help?

1. Check platform-specific guide in `DOCKER_SETUP_ALL_PLATFORMS.md`
2. Review logs: `docker-compose -f <file> logs`
3. Verify `.env` file is correct
4. Check system resources (RAM, disk space)

---

**Ready to start?** Just run the appropriate script for your platform! 🚀
