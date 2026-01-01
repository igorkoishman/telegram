# Setup Checklist

Quick checklist to get your Telegram Video Translation Bot running with Docker and CI/CD.

## ✅ Pre-deployment Checklist

### 1. GitHub Setup
- [ ] Repository exists: `https://github.com/igorkoishman/telegram` ✅ (Already done!)
- [ ] Code is committed locally
- [ ] Ready to push to GitHub

### 2. Docker Hub Setup
- [ ] Create Docker Hub account (if you don't have one): https://hub.docker.com
- [ ] Login as: `igorkoishman`
- [ ] Create Access Token (Settings → Security → New Access Token)
- [ ] Copy the token (you'll need it for GitHub secrets)

### 3. GitHub Secrets Configuration
- [ ] Go to: `https://github.com/igorkoishman/telegram/settings/secrets/actions`
- [ ] Add secret: `DOCKER_USERNAME` = `igorkoishman`
- [ ] Add secret: `DOCKER_PASSWORD` = `<your_docker_hub_token>`

### 4. Telegram Bot Token
- [ ] Get bot token from @BotFather on Telegram
- [ ] Save it somewhere safe (you'll need it to run the container)

---

## 🚀 Deployment Steps

### Step 1: Push to GitHub

```bash
cd /Users/ikoishman/IdeaProjects/telegram

# Check status
git status

# Add all new files
git add .

# Commit
git commit -m "Add Docker support and CI/CD pipeline"

# Push to GitHub
git push origin main
```

### Step 2: Monitor GitHub Actions

1. Go to: `https://github.com/igorkoishman/telegram/actions`
2. Watch the "CI/CD Pipeline" workflow run
3. Wait for it to complete (takes ~10-15 minutes first time)
4. Verify Docker images are pushed to Docker Hub

### Step 3: Test Locally (Optional)

```bash
# Create .env file
cp .env.example .env

# Edit .env and add your TELEGRAM_BOT_TOKEN
nano .env

# Test with docker-compose (CPU version)
docker-compose -f docker-compose.cpu.yml up -d

# Check logs
docker-compose -f docker-compose.cpu.yml logs -f

# Stop when done testing
docker-compose -f docker-compose.cpu.yml down
```

### Step 4: Deploy to Production

#### On Windows with WSL2 + NVIDIA GPU:

```bash
# In WSL2 Ubuntu terminal
cd /mnt/c/path/to/project

# Create .env file
cp .env.example .env
nano .env  # Add your bot token

# Pull latest image
docker-compose pull

# Start service
docker-compose up -d

# Check logs
docker-compose logs -f
```

#### On Linux with NVIDIA GPU:

```bash
# Pull latest image
docker-compose pull

# Start service
docker-compose up -d

# Verify GPU is being used
docker exec telegram-translator nvidia-smi

# Check logs
docker-compose logs -f
```

#### On Mac or CPU-only systems:

```bash
# Pull latest CPU image
docker-compose -f docker-compose.cpu.yml pull

# Start service
docker-compose -f docker-compose.cpu.yml up -d

# Check logs
docker-compose -f docker-compose.cpu.yml logs -f
```

### Step 5: Configure Telegram Webhook

```bash
# Use cloudflare tunnel or ngrok to expose local server
cloudflared tunnel --url http://localhost:8080

# Set webhook (replace with your bot token and public URL)
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://<YOUR_PUBLIC_URL>/telegram/webhook"}'
```

### Step 6: Test the Bot

1. Open Telegram
2. Search for your bot
3. Send `/start`
4. Send a video file
5. Follow the interactive menus
6. Wait for processed videos!

---

## 📊 Verification

### Check Docker Hub
- [ ] Go to: `https://hub.docker.com/r/igorkoishman/telegram-translator`
- [ ] Verify images exist:
  - `latest-gpu`
  - `latest-cpu`

### Check GitHub Actions
- [ ] Go to: `https://github.com/igorkoishman/telegram/actions`
- [ ] Verify workflow completed successfully
- [ ] Green checkmarks on all jobs

### Check Running Container
- [ ] Container is running: `docker ps`
- [ ] Health check passes: `curl http://localhost:8080/actuator/health`
- [ ] No errors in logs: `docker-compose logs`

---

## 🔧 Configuration Files Summary

| File | Purpose | Status |
|------|---------|--------|
| `Dockerfile` | GPU-enabled image | ✅ Created |
| `Dockerfile.cpu` | CPU-only image | ✅ Created |
| `docker-compose.yml` | GPU deployment | ✅ Created |
| `docker-compose.cpu.yml` | CPU deployment | ✅ Created |
| `.dockerignore` | Build optimization | ✅ Created |
| `.env.example` | Environment template | ✅ Created |
| `.github/workflows/ci-cd.yml` | CI/CD pipeline | ✅ Created |
| `DOCKER_DEPLOYMENT.md` | Full deployment guide | ✅ Created |
| `BUILD_AND_RUN.md` | Quick reference | ✅ Created |
| `GITHUB_SETUP.md` | GitHub configuration | ✅ Created |

---

## 🎯 Quick Commands Reference

### Docker Compose Commands

```bash
# Start (GPU)
docker-compose up -d

# Start (CPU)
docker-compose -f docker-compose.cpu.yml up -d

# Stop
docker-compose down

# View logs
docker-compose logs -f

# Restart
docker-compose restart

# Update to latest image
docker-compose pull && docker-compose up -d
```

### Docker Commands

```bash
# Pull latest image
docker pull igorkoishman/telegram-translator:latest-gpu

# Run container
docker run -d --name telegram-bot --gpus all \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  igorkoishman/telegram-translator:latest-gpu

# View logs
docker logs -f telegram-bot

# Stop and remove
docker stop telegram-bot && docker rm telegram-bot

# Check GPU usage
docker exec telegram-bot nvidia-smi
```

### Git Commands

```bash
# Commit and push changes
git add .
git commit -m "Your commit message"
git push origin main

# Create release tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# Check status
git status
git log --oneline -5
```

---

## 🆘 Common Issues

### Issue: GitHub Actions failing

**Solution**: Check that Docker Hub secrets are configured correctly in GitHub repository settings.

### Issue: Docker image not found

**Solution**: Wait for GitHub Actions to complete, then check Docker Hub to verify images were pushed.

### Issue: Container won't start

**Solution**: Check logs with `docker-compose logs` and verify environment variables are set correctly.

### Issue: GPU not detected

**Solution**: Verify NVIDIA drivers and docker runtime are installed correctly (see DOCKER_DEPLOYMENT.md).

### Issue: Webhook not working

**Solution**: Ensure your public URL is accessible and webhook is set correctly with Telegram API.

---

## 📚 Documentation

- **Full Deployment Guide**: [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)
- **Quick Start**: [BUILD_AND_RUN.md](BUILD_AND_RUN.md)
- **GitHub Setup**: [GITHUB_SETUP.md](GITHUB_SETUP.md)
- **Original Setup Guide**: [QUICK_START.md](QUICK_START.md)

---

## ✨ What You've Built

You now have a production-ready Telegram bot with:

- ✅ **Multiple AI Models**: Whisper (faster-whisper & openai-whisper), M2M100, NLLB
- ✅ **GPU Acceleration**: NVIDIA CUDA support for 5-10x faster processing
- ✅ **Docker Support**: Run anywhere with Docker
- ✅ **CI/CD Pipeline**: Automatic builds and deployments
- ✅ **Multi-platform**: Windows, Linux, macOS support
- ✅ **Production Ready**: Health checks, logging, monitoring

**Congratulations! 🎉**
