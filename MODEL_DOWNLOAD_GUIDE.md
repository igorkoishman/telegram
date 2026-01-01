# Model Download Configuration Guide

## Overview

The application automatically downloads AI models in the background. By default, it downloads **only lightweight models** (tiny and small) for fast startup, and downloads larger models on-demand when you use them.

## Default Configuration (Recommended)

**On Startup:** Downloads tiny + small models (~500MB total) - takes 1-2 minutes
**On Demand:** Larger models download automatically when you first select them in the bot

This gives you:
- ✅ Fast startup (1-2 minutes instead of 10+ minutes)
- ✅ Smaller disk usage (500MB instead of 6GB+)
- ✅ Still have access to all models when needed

---

## How It Works

```
1. Container Starts
   ↓
2. Application Becomes Available (port 8080 ready)
   ↓
3. Background: Downloads tiny + small models
   ↓
4. Model Download Complete (~1-2 minutes)
   ↓
5. Bot Ready to Process Videos!
   ↓
6. User Selects "Large" Model
   ↓
7. Downloads large model on first use
   ↓
8. Cached for future use
```

---

## Configuration Options

### 1. Default (Lightweight - Recommended)

``bash
# In .env file
TRANSLATION_MODELS_AUTO_DOWNLOAD=true
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper,openai-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

**Downloads on startup:**
- Whisper tiny (75MB) - for testing
- Whisper small (460MB) - for quick jobs
- M2M100 translation model (500MB)
- **Total: ~1GB**

### 2. Everything Upfront (Heavy)

```bash
# In .env file
TRANSLATION_MODELS_AUTO_DOWNLOAD=true
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small,medium,large-v3
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper,openai-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```

**Downloads on startup:**
- All Whisper models (tiny → large-v3)
- Both translation models
- **Total: ~6-8GB, takes 10-15 minutes**

### 3. Minimal (Fastest Startup)

```bash
# In .env file
TRANSLATION_MODELS_AUTO_DOWNLOAD=true
TRANSLATION_MODELS_WHISPER_MODELS=tiny
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

**Downloads on startup:**
- Only Whisper tiny + M2M100
- **Total: ~600MB, takes 30-60 seconds**

### 4. On-Demand Only (No Startup Downloads)

```bash
# In .env file
TRANSLATION_MODELS_AUTO_DOWNLOAD=false
```

**Downloads on startup:** Nothing
**Downloads:** Only when you first use each model
**Best for:** Very limited disk space or bandwidth

---

## Model Sizes and Speed

### Whisper Models

| Model | Download Size | Memory Usage | Speed (CPU) | Speed (GPU) | Quality |
|-------|---------------|--------------|-------------|-------------|---------|
| tiny | 75 MB | 400 MB | ⚡⚡⚡ Fast (10s) | ⚡⚡⚡ Very Fast (2s) | Basic |
| base | 145 MB | 500 MB | ⚡⚡⚡ Fast (15s) | ⚡⚡⚡ Very Fast (3s) | Good |
| small | 460 MB | 1 GB | ⚡⚡ Medium (45s) | ⚡⚡ Fast (10s) | Better |
| medium | 1.5 GB | 2.5 GB | ⚡ Slow (3min) | ⚡⚡ Medium (30s) | Great |
| large-v3 | 2.9 GB | 5 GB | 🐌 Very Slow (6min) | ⚡ Slow (1min) | Excellent |

*Times shown for a 1-minute video*

### Translation Models

| Model | Download Size | Languages | Quality |
|-------|---------------|-----------|---------|
| m2m100 | 500 MB | 8 core languages | Good |
| nllb | 2.5 GB | 200+ languages | Better |

**Recommendation:** Start with m2m100 (covers your 8 target languages perfectly)

---

## Docker Usage

### First Run (with default config)

```bash
# Create .env file (uses default lightweight config)
cp .env.example .env
nano .env  # Add your TELEGRAM_BOT_TOKEN

# Start the container
docker-compose -f docker-compose.cpu.yml up -d

# Watch logs - you'll see model downloads
docker logs telegram-translator-cpu -f
```

**Expected logs:**
```
=== MODEL INITIALIZATION STARTED ===
Downloading models: whisper=[tiny, small], backends=[faster-whisper, openai-whisper], translation=[m2m100]
Downloading Whisper model: backend=faster-whisper, size=tiny
✅ Faster-Whisper tiny downloaded
Downloading Whisper model: backend=faster-whisper, size=small
✅ Faster-Whisper small downloaded
Downloading Whisper model: backend=openai-whisper, size=tiny
✅ OpenAI Whisper tiny downloaded
Downloading Whisper model: backend=openai-whisper, size=small
✅ OpenAI Whisper small downloaded
Downloading translation model: m2m100
✅ m2m100 downloaded
=== MODEL INITIALIZATION COMPLETED ===
All models downloaded successfully!
🚀 Starting Telegram Long Polling mode...
📡 Long Polling started. Waiting for updates...
```

### Subsequent Runs (instant startup)

Models are cached in Docker volumes:
- `telegram-models` - All AI models

**No re-download needed!** Startup takes ~10 seconds.

---

## Changing Model Configuration

### Option 1: Update .env and Restart

```bash
# Edit .env file
nano .env

# Change this line:
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small,medium

# Restart container
docker-compose -f docker-compose.cpu.yml restart

# New models download in background
docker logs telegram-translator-cpu -f
```

### Option 2: Let Models Download On-Demand

Don't change anything - when you select a model the bot doesn't have, it downloads automatically!

---

## Disk Space Management

### Check Current Usage

```bash
# Check Docker volume sizes
docker system df -v | grep telegram

# Check total Docker disk usage
docker system df
```

### Cleaning Up

#### Remove Unused Models
```bash
# Stop container
docker-compose -f docker-compose.cpu.yml down

# Remove model cache (will re-download on next start)
docker volume rm telegram-models

# Start fresh
docker-compose -f docker-compose.cpu.yml up -d
```

#### Keep Models, Just Reduce
Edit `.env` to use smaller set:
```bash
# Change from:
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small,medium,large-v3

# To:
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small

# Restart - no re-download, but medium/large won't be available without re-downloading
docker-compose -f docker-compose.cpu.yml restart
```

---

## Bandwidth Considerations

### Low Bandwidth?

Use minimal config:
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

### Unlimited Bandwidth?

Download everything upfront:
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small,medium,large-v3
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper,openai-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```

---

## Model Backends

### Whisper Backends Comparison

| Backend | Speed | Accuracy | Memory | Best For |
|---------|-------|----------|--------|----------|
| faster-whisper | ⚡⚡⚡ Faster | Same | Less | Production (recommended) |
| openai-whisper | ⚡⚡ Slower | Same | More | Testing/compatibility |

**Recommendation:** Use `faster-whisper` only to save disk space and get better performance.

To use only faster-whisper:
```bash
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
```

This cuts model downloads in half!

---

## Troubleshooting

### Download Fails

**Check logs:**
```bash
docker logs telegram-translator-cpu | grep "MODEL INITIALIZATION"
```

**Common issues:**
- Slow internet - wait longer
- Out of disk space - free up space or use minimal config
- Network error - restart container

**Solution:**
```bash
docker-compose -f docker-compose.cpu.yml restart
```

### Out of Disk Space

**Check available space:**
```bash
df -h
```

**Free up space:**
```bash
# Remove all Docker cache (careful - removes ALL Docker data)
docker system prune -a --volumes

# Or just remove this project's models
docker volume rm telegram-models
```

**Use minimal config:**
```bash
# Edit .env
TRANSLATION_MODELS_WHISPER_MODELS=tiny
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

### Download is Slow

This is normal for large models. Progress logs show download speed:

```
92%|█████████████████████ | 2.64G/2.88G [01:42<00:09, 26.2MiB/s]
```

**Tips:**
- Be patient - large-v3 can take 5-10 minutes
- Download overnight if needed
- Use minimal config for faster startup

### Models Not Working After Download

**Verify models downloaded:**
```bash
docker logs telegram-translator-cpu | grep "✅"
```

Should see:
```
✅ Faster-Whisper tiny downloaded
✅ Faster-Whisper small downloaded
✅ m2m100 downloaded
```

**If missing, restart:**
```bash
docker-compose -f docker-compose.cpu.yml restart
```

---

## Recommendations by Use Case

### Personal Testing
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny,small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```
**Startup:** 1-2 minutes | **Disk:** ~1GB

### Production (Balanced)
```bash
TRANSLATION_MODELS_WHISPER_MODELS=small,medium
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```
**Startup:** 2-3 minutes | **Disk:** ~2GB

### Best Quality
```bash
TRANSLATION_MODELS_WHISPER_MODELS=large-v3
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```
**Startup:** 5-8 minutes | **Disk:** ~5GB

### Minimal Disk Space
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```
**Startup:** 30-60 seconds | **Disk:** ~600MB

---

## Summary

**Default configuration (recommended):**
- Downloads tiny + small models (~500MB)
- Fast startup (1-2 minutes)
- Medium/large models download on-demand
- Perfect for most users!

**Want different behavior?**
Edit `.env` file and adjust `TRANSLATION_MODELS_*` variables, then restart the container.

---

## Related Documentation

- [README.md](README.md) - Main documentation
- [LONG_POLLING_SETUP.md](LONG_POLLING_SETUP.md) - Getting started guide
- [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - Complete deployment guide
