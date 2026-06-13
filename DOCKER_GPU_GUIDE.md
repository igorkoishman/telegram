# Running Telegram Bot with NVIDIA GPU on Windows/Linux

## GPU Support Overview

✅ **Windows (WSL2)**: Full NVIDIA GPU support via Docker Desktop + WSL2
✅ **Linux**: Native NVIDIA GPU support with nvidia-docker
❌ **Mac**: No GPU support (neither NVIDIA nor Apple Silicon)

## Prerequisites for Windows

### 1. Install NVIDIA GPU Drivers
- Download latest drivers: https://www.nvidia.com/Download/index.aspx
- Install and restart your computer
- Verify: Open PowerShell and run `nvidia-smi`

### 2. Install WSL2 (if not already installed)
```powershell
# Run in PowerShell as Administrator
wsl --install
wsl --update
```

### 3. Enable GPU in Docker Desktop
1. Open Docker Desktop
2. Go to Settings → Resources → WSL Integration
3. Enable integration with your WSL2 distribution
4. Go to Settings → General
5. Enable "Use the WSL 2 based engine"
6. Click "Apply & Restart"

### 4. Verify GPU Access
```bash
# In WSL2 terminal
docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
```

You should see your GPU information!

## Prerequisites for Linux

### Install NVIDIA Container Toolkit
```bash
# Add repository
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
    sudo tee /etc/apt/sources.list.d/nvidia-docker.list

# Install nvidia-docker2
sudo apt-get update
sudo apt-get install -y nvidia-docker2

# Restart Docker
sudo systemctl restart docker

# Test
sudo docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
```

## Quick Start

### Windows
```bash
# Option 1: Using batch script (double-click or run from cmd)
run-docker-gpu.bat

# Option 2: Using bash script (in WSL2)
./run-docker-gpu.sh
```

### Linux
```bash
./run-docker-gpu.sh
```

### Manual Docker Compose
```bash
# Build and run
docker-compose -f docker-compose.gpu.yml up --build -d

# View logs
docker-compose -f docker-compose.gpu.yml logs -f

# Stop
docker-compose -f docker-compose.gpu.yml down
```

## Configuration

### Environment Variables (.env)
```bash
# Telegram Bot
TELEGRAM_BOT_TOKEN=your_actual_token_here

# Docker
DOCKER_USERNAME=igorkoishman

# Java Memory (GPU version can handle larger models)
JAVA_OPTS=-Xmx12g -Xms4g -XX:+UseG1GC

# Use larger Whisper models with GPU
TRANSLATION_MODELS_WHISPER_MODELS=small,medium
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```

### Resource Limits (in docker-compose.gpu.yml)

The configuration includes:
- **CPUs**: 15 cores limit, 8 cores reserved
- **Memory**: 16GB limit, 8GB reserved
- **GPU**: All available GPUs with compute capability
- **Shared Memory**: 4GB for large models
- **tmpfs**: 4GB for fast temporary storage

## Performance Optimizations

### 1. CUDA Environment Variables
```bash
CUDA_VISIBLE_DEVICES=all              # Use all GPUs
NVIDIA_VISIBLE_DEVICES=all            # Make all GPUs visible
PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512  # Optimize memory allocation
```

### 2. JVM Tuning
```bash
-Xmx12g                    # Maximum heap 12GB
-Xms4g                     # Initial heap 4GB
-XX:+UseG1GC              # G1 Garbage Collector
-XX:+AlwaysPreTouch       # Pre-touch memory for better performance
```

### 3. CPU Thread Optimization
```bash
OMP_NUM_THREADS=8         # OpenMP threads
MKL_NUM_THREADS=8         # Intel MKL threads
```

## Monitoring GPU Usage

### From Host
```bash
# Watch GPU usage
nvidia-smi -l 1

# Detailed monitoring
nvidia-smi dmon -s pucvmet
```

### From Container
```bash
# Enter container
docker exec -it telegram-translator-gpu bash

# Check GPU
nvidia-smi
```

### Docker Stats
```bash
# Container resource usage
docker stats telegram-translator-gpu
```

## Model Configuration

### Recommended Models for GPU

**Small GPU (4-6GB VRAM)**:
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

**Medium GPU (8-12GB VRAM)**:
```bash
TRANSLATION_MODELS_WHISPER_MODELS=small,medium
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```

**Large GPU (16GB+ VRAM)**:
```bash
TRANSLATION_MODELS_WHISPER_MODELS=medium,large-v2
TRANSLATION_MODELS_TRANSLATION_MODELS=nllb
```

## Troubleshooting

### Issue: "nvidia-smi: command not found" in container
**Solution**: Check if GPU drivers are installed on host
```bash
# On host
nvidia-smi
```

### Issue: Docker doesn't recognize GPU
**Solution**:
1. Check Docker Desktop settings (Windows)
2. Verify nvidia-docker2 installation (Linux)
3. Restart Docker daemon

```bash
# Linux
sudo systemctl restart docker

# Windows
Restart Docker Desktop
```

### Issue: Out of GPU Memory
**Solution**: Use smaller models or reduce batch size
```bash
# Edit .env
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
```

### Issue: Slow Performance Despite GPU
**Possible causes**:
1. Model not using GPU (check logs for "cuda" vs "cpu")
2. CPU bottleneck (increase CPU limit)
3. Memory swapping (increase memory limit)

### Issue: Container exits immediately
```bash
# Check logs
docker-compose -f docker-compose.gpu.yml logs

# Check if image was built with GPU support
docker inspect telegram-translator-gpu | grep -i nvidia
```

## Performance Comparison

| Configuration | Whisper (30s audio) | Translation | Total Processing |
|--------------|---------------------|-------------|------------------|
| **CPU only** | ~45s | ~10s | ~55s |
| **GPU (GTX 1660)** | ~8s | ~3s | ~11s |
| **GPU (RTX 3080)** | ~3s | ~1s | ~4s |
| **GPU (RTX 4090)** | ~1.5s | ~0.5s | ~2s |

*Approximate times for reference

## Useful Commands

```bash
# Start with GPU
docker-compose -f docker-compose.gpu.yml up -d

# View logs
docker-compose -f docker-compose.gpu.yml logs -f

# Stop
docker-compose -f docker-compose.gpu.yml down

# Restart
docker-compose -f docker-compose.gpu.yml restart

# Rebuild
docker-compose -f docker-compose.gpu.yml build --no-cache

# Shell access
docker exec -it telegram-translator-gpu bash

# Check GPU in container
docker exec telegram-translator-gpu nvidia-smi

# Remove everything (including volumes)
docker-compose -f docker-compose.gpu.yml down -v
docker system prune -a
```

## Advanced: Multi-GPU Setup

If you have multiple GPUs:

```yaml
# In docker-compose.gpu.yml, specify GPU IDs
environment:
  - CUDA_VISIBLE_DEVICES=0,1  # Use first two GPUs
```

Or use specific GPU:
```bash
# Use only GPU 0
CUDA_VISIBLE_DEVICES=0 docker-compose -f docker-compose.gpu.yml up -d
```

## Best Practices

1. **Start with small models** to verify GPU works, then scale up
2. **Monitor GPU memory** with `nvidia-smi` during processing
3. **Keep drivers updated** for best performance
4. **Use volume mounts** to cache downloaded models
5. **Set resource limits** to prevent system lockup

## Windows-Specific Notes

### WSL2 Memory Management
WSL2 can consume a lot of RAM. Create `.wslconfig` in `C:\Users\YourName\`:

```ini
[wsl2]
memory=24GB
processors=12
swap=8GB
```

### GPU Memory
Windows reserves some GPU memory. If you have 8GB VRAM, expect ~7GB available in WSL2.

### Performance
GPU performance in WSL2 is ~95% of native Linux performance - excellent for most use cases!
