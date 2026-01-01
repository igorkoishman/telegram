# Telegram Video Translation Bot

AI-powered Telegram bot that transcribes, translates, and adds subtitles to videos automatically.

## Features

- 🎙️ **AI Transcription** - Whisper AI converts speech to text in 99+ languages
- 🌍 **Multi-Language Translation** - Translate to 8 languages (English, Spanish, French, German, Italian, Russian, Hebrew, Arabic)
- 📹 **Flexible Subtitles** - Hard-burned, soft subtitles, or both
- 🤖 **Interactive Bot** - Easy-to-use Telegram interface with inline keyboards
- ⚡ **Two Deployment Modes** - Long Polling (no setup) or Webhooks (production)
- 🐳 **Docker Ready** - One command deployment with Docker Compose

---

## Quick Start (Choose Your Path)

### Path 1: Long Polling Mode (Recommended for Beginners)
**Perfect for:** Personal use, testing, home networks
**Pros:** No domain, no SSL, no Cloudflare - just works!
**Time:** 5 minutes

```bash
# 1. Get your bot token from @BotFather on Telegram

# 2. Create environment file
cp .env.example .env
# Edit .env and add your TELEGRAM_BOT_TOKEN

# 3. Start the bot (CPU-only, works on any computer)
docker-compose -f docker-compose.cpu.yml up -d

# 4. Test it! Open Telegram and send /start to your bot
```

That's it! See [LONG_POLLING_SETUP.md](LONG_POLLING_SETUP.md) for details.

---

### Path 2: Webhook Mode (Production)
**Perfect for:** High-traffic bots, multiple users, instant responses
**Requirements:** Domain name, Cloudflare Tunnel
**Time:** 15 minutes

See [CLOUDFLARE_TUNNEL_SETUP.md](CLOUDFLARE_TUNNEL_SETUP.md) for complete guide.

---

## System Requirements

### Minimum (CPU-only)
- 4GB RAM
- 20GB free disk space (for AI models)
- Docker & Docker Compose
- Internet connection

### Recommended (with GPU)
- NVIDIA GPU with 6GB+ VRAM
- 16GB RAM
- 30GB free disk space
- NVIDIA drivers + CUDA Toolkit
- Docker with nvidia-docker support

---

## Documentation Guide

**Start Here:**
1. [LONG_POLLING_SETUP.md](LONG_POLLING_SETUP.md) - Easiest way to get started (no webhooks)
2. [MODEL_DOWNLOAD_GUIDE.md](MODEL_DOWNLOAD_GUIDE.md) - Configure which AI models to download

**Production Deployment:**
3. [CLOUDFLARE_TUNNEL_SETUP.md](CLOUDFLARE_TUNNEL_SETUP.md) - Webhook mode with Cloudflare
4. [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - Complete Docker deployment guide

**Reference:**
- [BUILD_AND_RUN.md](BUILD_AND_RUN.md) - Quick command reference
- [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) - Deployment checklist

---

## Deployment Comparison

| Feature | Long Polling | Webhooks |
|---------|-------------|----------|
| **Setup Time** | 5 minutes | 15 minutes |
| **Requirements** | Bot token only | Bot token + domain + Cloudflare |
| **Cost** | 100% Free | ~$10/year (domain) |
| **Best For** | Personal use, testing | Production, high traffic |
| **Network** | Works anywhere | Needs public URL |
| **Latency** | 1-2 seconds | Instant |
| **Current Mode** | ✅ **This is active by default** | Requires configuration |

---

## How to Use the Bot

1. **Send a video** to your bot on Telegram
2. **Bot analyzes** the video and shows you options:
   - Use existing subtitles or transcribe new ones
   - Choose Whisper model size (tiny, small, medium, large)
   - Select target languages
   - Pick subtitle format (hard-burned, soft, or both)
3. **Bot processes** your video (2-10 minutes depending on length)
4. **Receive** translated videos and SRT subtitle files

---

## Configuration

### Basic Setup (.env file)

```env
# Required: Your bot token from @BotFather
TELEGRAM_BOT_TOKEN=your_token_here

# Choose mode: 'polling' (default, no setup) or 'webhook' (requires domain)
TELEGRAM_MODE=polling

# Optional: Webhook secret (only needed for webhook mode)
TELEGRAM_WEBHOOK_SECRET=your_random_secret

# Optional: Which models to download on startup (default: tiny,small)
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper,openai-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

### Model Selection

**On Startup:** Only downloads tiny and small models (~500MB total) for fast startup
**On Demand:** Larger models (medium, large) download when you first use them

| Model | Size | Speed | Quality | Best For |
|-------|------|-------|---------|----------|
| tiny | 75MB | ⚡⚡⚡ Fast | Basic | Testing |
| small | 460MB | ⚡⚡ Medium | Good | Quick jobs |
| medium | 1.5GB | ⚡ Slow | Great | Balanced |
| large-v3 | 3GB | 🐌 Very Slow | Best | Best quality |

See [MODEL_DOWNLOAD_GUIDE.md](MODEL_DOWNLOAD_GUIDE.md) for details.

---

## Platform Support

### CPU-Only (Any System)
```bash
docker-compose -f docker-compose.cpu.yml up -d
```
**Works on:** Mac, Windows, Linux, Raspberry Pi

### GPU-Accelerated (NVIDIA only)
```bash
docker-compose up -d
```
**Works on:** Linux with NVIDIA GPU, Windows WSL2 with NVIDIA

**Note:** GPU processing is 5-10x faster than CPU.

---

## Common Commands

```bash
# Start bot (Long Polling, CPU-only)
docker-compose -f docker-compose.cpu.yml up -d

# View logs
docker logs telegram-translator-cpu -f

# Stop bot
docker-compose -f docker-compose.cpu.yml down

# Restart bot
docker-compose -f docker-compose.cpu.yml restart

# Check status
docker ps
curl http://localhost:8080/actuator/health
```

---

## Troubleshooting

### Bot doesn't respond
```bash
# Check if container is running
docker ps

# Check logs for errors
docker logs telegram-translator-cpu

# Verify bot token
cat .env | grep TELEGRAM_BOT_TOKEN
```

### Models download slowly
First download takes 5-10 minutes (tiny+small models ~500MB).
Subsequent startups are instant (models are cached).

### Out of memory
Edit `.env` and reduce memory:
```env
JAVA_OPTS=-Xmx2g -Xms512m
```

### Need faster processing
- Use GPU version (5-10x faster)
- Use smaller model (tiny instead of large)
- Reduce video resolution before uploading

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│            Telegram Bot (Docker Container)          │
│                                                     │
│  Mode 1: Long Polling (Default)                    │
│  ┌────────────┐                                     │
│  │ Your Bot   │ ←─── Asks Telegram servers         │
│  │            │ ───→ Gets updates                   │
│  └────────────┘                                     │
│                                                     │
│  Mode 2: Webhook (Optional)                        │
│  ┌────────────┐   ┌──────────────┐                 │
│  │ Your Bot   │ ← │ Cloudflare   │ ← Telegram      │
│  │            │   │ Tunnel       │                 │
│  └────────────┘   └──────────────┘                 │
│                                                     │
│  ┌──────────────────────────────────────┐           │
│  │ Processing Pipeline                  │           │
│  │ 1. FFmpeg extracts audio             │           │
│  │ 2. Whisper AI transcribes speech     │           │
│  │ 3. Translation AI translates text    │           │
│  │ 4. FFmpeg burns/muxes subtitles      │           │
│  └──────────────────────────────────────┘           │
└─────────────────────────────────────────────────────┘
```

---

## Tech Stack

- **Spring Boot 2.7** - Java application framework
- **Python 3.11** - AI model runtime
- **faster-whisper** - Speech-to-text transcription
- **OpenAI Whisper** - Alternative transcription backend
- **M2M100** - Neural machine translation
- **FFmpeg** - Video/audio processing
- **Docker** - Containerization
- **Telegram Bot API** - Bot integration

---

## Supported Languages

**Transcription (Whisper):** 99+ languages including English, Spanish, French, German, Italian, Russian, Hebrew, Arabic, Chinese, Japanese, Korean, and more.

**Translation (M2M100):** English, Spanish, French, German, Italian, Russian, Hebrew, Arabic

---

## Project Structure

```
telegram/
├── src/main/
│   ├── java/com/koishman/telegram/
│   │   ├── config/           # Spring configuration
│   │   ├── service/          # Bot services
│   │   │   ├── TelegramLongPollingService.java    # Polling mode
│   │   │   └── EnhancedTelegramBotService.java    # Bot logic
│   │   ├── translation/      # Translation service
│   │   │   ├── service/
│   │   │   │   ├── WhisperService.java            # Transcription
│   │   │   │   ├── TranslationService.java        # Translation
│   │   │   │   └── FFmpegService.java             # Video processing
│   │   └── web/
│   │       └── TelegramWebhookController.java     # Webhook mode
│   └── resources/
│       ├── python/           # AI Python scripts
│       └── application.yml   # App configuration
├── docker-compose.cpu.yml    # CPU-only deployment
├── docker-compose.yml        # GPU deployment
├── Dockerfile.cpu            # CPU Docker image
├── Dockerfile                # GPU Docker image
└── .env.example              # Environment template
```

---

## FAQ

**Q: Which mode should I use - Long Polling or Webhooks?**
A: Start with Long Polling (default). It's simpler, free, and works everywhere. Switch to webhooks later if you need instant responses or high traffic.

**Q: Do I need a domain name?**
A: Not for Long Polling mode (default). Only needed for Webhook mode.

**Q: How much does it cost?**
A: Long Polling mode is 100% free. Webhook mode costs ~$10/year for a domain.

**Q: Can I run this on my laptop?**
A: Yes! Long Polling mode works on any computer with Docker.

**Q: Do I need a GPU?**
A: No, but GPU makes processing 5-10x faster. CPU version works fine for personal use.

**Q: How long does video processing take?**
A: With CPU: ~5-10 minutes per video. With GPU: ~1-2 minutes per video.

**Q: Which Whisper model should I use?**
A: Start with `small` for balanced speed/quality. Use `tiny` for testing, `large-v3` for best quality.

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

---

## License

[Add your license here]

---

## Support

For issues or questions:
- **Check logs:** `docker logs telegram-translator-cpu -f`
- **Verify setup:** `curl http://localhost:8080/actuator/health`
- **Review docs:** See documentation links above
- **GitHub Issues:** Report bugs and request features

---

## What's Next?

After getting your bot running:
1. Try different Whisper models to find the right speed/quality balance
2. Test with videos in different languages
3. Experiment with soft vs hard subtitles
4. Consider GPU deployment for faster processing
5. Switch to webhook mode for production use

---

Built with ❤️ using Spring Boot, Python, and AI
