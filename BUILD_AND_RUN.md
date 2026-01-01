# Quick Build and Run Guide

## 🚀 Fastest Way to Run

### 1. Create Environment File

```bash
cp .env.example .env
# Edit .env and add your TELEGRAM_BOT_TOKEN
```

### 2. Choose Your Platform

#### Windows with NVIDIA GPU (WSL2)

```bash
# In WSL2 Ubuntu terminal
docker-compose up -d
docker-compose logs -f
```

#### Windows/Mac/Linux (CPU-only)

```bash
docker-compose -f docker-compose.cpu.yml up -d
docker-compose -f docker-compose.cpu.yml logs -f
```

#### Linux with NVIDIA GPU

```bash
docker-compose up -d
docker-compose logs -f
```

---

## 🔨 Build Docker Images Locally

### Build GPU Version

```bash
docker build -t telegram-translator:gpu -f Dockerfile .
```

### Build CPU Version

```bash
docker build -t telegram-translator:cpu -f Dockerfile.cpu .
```

---

## 🎯 Run Specific Version

### Run GPU Version

```bash
docker run -d \
  --name telegram-bot \
  --gpus all \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -e TELEGRAM_WEBHOOK_SECRET=your_secret \
  -v telegram-models:/app/models \
  telegram-translator:gpu
```

### Run CPU Version

```bash
docker run -d \
  --name telegram-bot \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -e TELEGRAM_WEBHOOK_SECRET=your_secret \
  -v telegram-models:/app/models \
  telegram-translator:cpu
```

---

## 📦 Use Pre-built Images from Docker Hub

### GPU Version

```bash
docker run -d \
  --name telegram-bot \
  --gpus all \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  koishman/telegram-translator:latest-gpu
```

### CPU Version

```bash
docker run -d \
  --name telegram-bot \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  koishman/telegram-translator:latest-cpu
```

---

## 🛠️ Common Commands

### View Logs

```bash
# Using docker-compose
docker-compose logs -f

# Using docker directly
docker logs -f telegram-bot
```

### Stop Container

```bash
# Using docker-compose
docker-compose down

# Using docker directly
docker stop telegram-bot
docker rm telegram-bot
```

### Restart Container

```bash
# Using docker-compose
docker-compose restart

# Using docker directly
docker restart telegram-bot
```

### Check GPU Usage (GPU version only)

```bash
docker exec telegram-bot nvidia-smi
```

### Access Container Shell

```bash
docker exec -it telegram-bot /bin/bash
```

---

## 🧹 Cleanup

### Remove Container and Images

```bash
# Stop and remove container
docker-compose down

# Remove images
docker rmi telegram-translator:gpu telegram-translator:cpu

# Remove volumes (WARNING: deletes AI models)
docker volume rm telegram-models telegram-uploads telegram-outputs telegram-downloads
```

### Remove All Project-related Docker Resources

```bash
docker-compose down -v --rmi all
```

---

## 🔍 Troubleshooting

### Check if Container is Running

```bash
docker ps | grep telegram
```

### Check Resource Usage

```bash
docker stats telegram-bot
```

### View Container Details

```bash
docker inspect telegram-bot
```

### Test Application Health

```bash
curl http://localhost:8080/actuator/health
```

---

## 📝 Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `TELEGRAM_BOT_TOKEN` | Yes | - | Bot token from @BotFather |
| `TELEGRAM_WEBHOOK_SECRET` | No | `tg-secret-9f3c1d-2025` | Webhook security secret |
| `JAVA_OPTS` | No | `-Xmx2g -Xms512m` | Java memory settings |
| `SPRING_PROFILES_ACTIVE` | No | `default` | Spring profile |

---

## 🌐 Expose to Internet (for Telegram Webhook)

### Using Cloudflare Tunnel (Recommended)

```bash
# Install cloudflared
# Visit: https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/

# Run tunnel
cloudflared tunnel --url http://localhost:8080
```

### Set Telegram Webhook

```bash
# Replace YOUR_BOT_TOKEN and YOUR_PUBLIC_URL
curl -X POST "https://api.telegram.org/botYOUR_BOT_TOKEN/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://YOUR_PUBLIC_URL/telegram/webhook"}'
```

---

## 💡 Tips

1. **First run is slow**: AI models (~6GB) download on first start
2. **Use volumes**: Models persist across container restarts
3. **Monitor resources**: Use `docker stats` to check CPU/RAM usage
4. **GPU vs CPU**: GPU is 5-10x faster but requires NVIDIA hardware
5. **Backup models**: Save the `telegram-models` volume to avoid re-downloading

---

For complete documentation, see [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)
