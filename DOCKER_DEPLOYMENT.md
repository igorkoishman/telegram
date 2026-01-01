# Docker Deployment Guide

Complete guide for deploying the Telegram Video Translation Service using Docker on Windows, Linux, and macOS with GPU support.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [GPU Support (NVIDIA)](#gpu-support-nvidia)
- [Environment Configuration](#environment-configuration)
- [Deployment Options](#deployment-options)
- [CI/CD with GitHub Actions](#cicd-with-github-actions)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required on All Systems

- Docker Engine 20.10+ or Docker Desktop
- Docker Compose V2
- 8GB+ RAM (16GB recommended)
- 20GB+ free disk space (for AI models)

### For GPU Support (NVIDIA only)

#### Windows with WSL2

1. **Install WSL2 with Ubuntu**:
   ```powershell
   wsl --install -d Ubuntu-22.04
   ```

2. **Install NVIDIA Driver** (Windows host):
   - Download from [NVIDIA Website](https://www.nvidia.com/Download/index.aspx)
   - Version 470.76 or higher

3. **Install NVIDIA Container Toolkit** (inside WSL2):
   ```bash
   distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
   curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
   curl -s -L https://nvidia.github.io/libnvidia-container/$distribution/libnvidia-container.list | \
       sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
       sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list

   sudo apt-get update
   sudo apt-get install -y nvidia-container-toolkit
   sudo systemctl restart docker
   ```

4. **Verify GPU Access**:
   ```bash
   docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
   ```

#### Linux

1. **Install NVIDIA Driver**:
   ```bash
   sudo apt-get update
   sudo apt-get install -y nvidia-driver-525
   sudo reboot
   ```

2. **Install NVIDIA Container Toolkit**:
   ```bash
   distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
   curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
   curl -s -L https://nvidia.github.io/libnvidia-container/$distribution/libnvidia-container.list | \
       sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
       sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list

   sudo apt-get update
   sudo apt-get install -y nvidia-container-toolkit
   sudo systemctl restart docker
   ```

3. **Verify**:
   ```bash
   nvidia-smi
   docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
   ```

---

## Quick Start

### Option 1: Using Pre-built Images from Docker Hub

#### CPU-only (Any system)

```bash
# Create .env file
cat > .env << EOF
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_WEBHOOK_SECRET=your_secret_here
DOCKER_USERNAME=koishman
EOF

# Start the service
docker-compose -f docker-compose.cpu.yml up -d

# Check logs
docker-compose -f docker-compose.cpu.yml logs -f
```

#### GPU-enabled (NVIDIA systems)

```bash
# Create .env file
cat > .env << EOF
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_WEBHOOK_SECRET=your_secret_here
DOCKER_USERNAME=koishman
EOF

# Start the service with GPU
docker-compose up -d

# Check logs
docker-compose logs -f
```

### Option 2: Build Locally

#### Build CPU image

```bash
docker build -f Dockerfile.cpu -t telegram-translator:latest-cpu .
```

#### Build GPU image

```bash
docker build -f Dockerfile -t telegram-translator:latest-gpu .
```

#### Run locally built image

```bash
# CPU
docker run -d \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -e TELEGRAM_WEBHOOK_SECRET=your_secret \
  -v telegram-models:/app/models \
  --name telegram-bot-cpu \
  telegram-translator:latest-cpu

# GPU
docker run -d \
  --gpus all \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -e TELEGRAM_WEBHOOK_SECRET=your_secret \
  -v telegram-models:/app/models \
  --name telegram-bot-gpu \
  telegram-translator:latest-gpu
```

---

## Environment Configuration

### Required Environment Variables

Create a `.env` file in the project root:

```env
# Telegram Configuration
TELEGRAM_BOT_TOKEN=your_bot_token_from_botfather
TELEGRAM_WEBHOOK_SECRET=your_random_secret_string

# Docker Hub (for pulling pre-built images)
DOCKER_USERNAME=koishman

# Optional: Java Memory Settings
JAVA_OPTS=-Xmx4g -Xms1g

# Optional: Spring Profile
SPRING_PROFILES_ACTIVE=production
```

### Get Your Telegram Bot Token

1. Open Telegram and search for [@BotFather](https://t.me/BotFather)
2. Send `/newbot` and follow instructions
3. Copy the token provided
4. Paste it into your `.env` file

---

## Deployment Options

### Windows with Docker Desktop

#### CPU-only (Recommended for testing)

1. Install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
2. Open PowerShell and navigate to project directory:
   ```powershell
   cd C:\path\to\telegram
   ```
3. Create `.env` file with your configuration
4. Start the service:
   ```powershell
   docker-compose -f docker-compose.cpu.yml up -d
   ```

#### GPU-enabled (WSL2 required)

1. Enable WSL2 in Docker Desktop settings
2. Install NVIDIA drivers and Container Toolkit (see [Prerequisites](#prerequisites))
3. Inside WSL2 Ubuntu:
   ```bash
   cd /mnt/c/path/to/telegram
   docker-compose up -d
   ```

### Linux (Ubuntu/Debian)

#### CPU-only

```bash
# Clone or copy project
cd /home/user/telegram

# Create .env file
nano .env

# Start service
docker-compose -f docker-compose.cpu.yml up -d

# Enable auto-start on boot
sudo systemctl enable docker
```

#### GPU-enabled

```bash
# Install NVIDIA drivers and Container Toolkit first
# (see Prerequisites section)

# Start service with GPU
docker-compose up -d

# Verify GPU is being used
docker exec telegram-translator nvidia-smi
```

### macOS (CPU-only)

```bash
# Install Docker Desktop for Mac
# Navigate to project
cd ~/Projects/telegram

# Create .env file
cat > .env << EOF
TELEGRAM_BOT_TOKEN=your_token
TELEGRAM_WEBHOOK_SECRET=your_secret
EOF

# Start service
docker-compose -f docker-compose.cpu.yml up -d
```

**Note**: macOS doesn't support NVIDIA GPUs in Docker. Use CPU version only.

---

## CI/CD with GitHub Actions

### Automated Build and Deploy

The project includes GitHub Actions workflows that automatically:

1. ✅ Build and test on every push
2. ✅ Build Docker images (both GPU and CPU)
3. ✅ Push images to Docker Hub
4. ✅ Auto-merge approved PRs
5. ✅ Create git tags from version
6. ✅ Create GitHub releases with artifacts

### Setup GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

```
DOCKER_USERNAME=your_dockerhub_username
DOCKER_PASSWORD=your_dockerhub_password
```

### Workflow Triggers

- **Push to `main` or `develop`**: Build and push Docker images
- **Pull Request**: Run tests and build images (but don't push)
- **Create Release**: Build, test, tag, and publish to Docker Hub
- **Auto-merge**: Approved PRs are automatically merged and tagged

### Manual Trigger

You can manually trigger a build from GitHub Actions tab.

---

## Docker Images

### Pre-built Images on Docker Hub

```
koishman/telegram-translator:latest-gpu   # NVIDIA GPU support
koishman/telegram-translator:latest-cpu   # CPU-only
koishman/telegram-translator:v1.0.0-gpu   # Specific version with GPU
koishman/telegram-translator:v1.0.0-cpu   # Specific version CPU-only
```

### Image Variants

| Image | Size | Use Case |
|-------|------|----------|
| `*-gpu` | ~8GB | Windows with WSL2 + NVIDIA, Linux with NVIDIA GPU |
| `*-cpu` | ~6GB | Any system without GPU, macOS, basic testing |

---

## Persistent Storage

Docker volumes are automatically created for:

- `telegram-models`: AI models (6GB+, downloaded on first run)
- `telegram-uploads`: Uploaded videos
- `telegram-outputs`: Processed videos
- `telegram-downloads`: Telegram file cache

### Backup Volumes

```bash
# Backup models volume (important to avoid re-downloading)
docker run --rm -v telegram-models:/data -v $(pwd):/backup \
  ubuntu tar czf /backup/models-backup.tar.gz /data

# Restore models
docker run --rm -v telegram-models:/data -v $(pwd):/backup \
  ubuntu tar xzf /backup/models-backup.tar.gz -C /
```

---

## Monitoring and Logs

### View Logs

```bash
# Follow logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100

# Specific service logs
docker logs telegram-translator -f
```

### Health Check

```bash
# Check health status
docker ps

# Manual health check
curl http://localhost:8080/actuator/health
```

### Resource Usage

```bash
# Monitor resource usage
docker stats telegram-translator

# GPU usage (if GPU variant)
docker exec telegram-translator nvidia-smi
```

---

## Troubleshooting

### GPU Not Detected

```bash
# Verify NVIDIA driver
nvidia-smi

# Verify Docker can access GPU
docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi

# Check container GPU access
docker exec telegram-translator nvidia-smi
```

### Out of Memory

Increase Java heap in `.env`:

```env
JAVA_OPTS=-Xmx8g -Xms2g
```

Restart container:

```bash
docker-compose down
docker-compose up -d
```

### Models Not Downloading

First run downloads ~6GB of AI models. Check logs:

```bash
docker-compose logs -f | grep -i "download\|model"
```

Ensure sufficient disk space:

```bash
df -h
```

### Port Already in Use

Change port in `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # Change 8081 to any available port
```

### Windows WSL2 Issues

```powershell
# Restart WSL
wsl --shutdown
wsl

# Update WSL
wsl --update
```

### Permission Issues (Linux)

```bash
# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Fix volume permissions
sudo chown -R $USER:$USER ./uploads ./outputs ./downloads
```

---

## Updating the Application

### Pull Latest Image

```bash
# Stop current container
docker-compose down

# Pull latest image
docker-compose pull

# Start with new image
docker-compose up -d
```

### Update to Specific Version

```bash
# Edit .env or docker-compose.yml
# Change image tag to desired version
image: koishman/telegram-translator:v1.1.0-gpu

# Restart
docker-compose up -d
```

---

## Production Deployment Checklist

- [ ] Set strong `TELEGRAM_WEBHOOK_SECRET`
- [ ] Configure firewall to allow port 8080
- [ ] Set up SSL/TLS reverse proxy (nginx, Caddy)
- [ ] Enable Docker auto-restart: `restart: unless-stopped`
- [ ] Set up log rotation
- [ ] Configure monitoring (Prometheus, Grafana)
- [ ] Backup AI models volume
- [ ] Set up automated backups for processed files
- [ ] Configure resource limits in docker-compose.yml
- [ ] Test GPU failover to CPU if needed

---

## Support

For issues, feature requests, or questions:

- GitHub Issues: [Create an issue](https://github.com/koishman/telegram/issues)
- Documentation: [README.md](README.md)

---

## License

See [LICENSE](LICENSE) file for details.
