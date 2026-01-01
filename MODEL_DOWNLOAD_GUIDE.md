# Model Download Configuration Guide

## Overview

The application automatically downloads AI models on startup. You can configure which models to download using environment variables.

## How It Works

1. **Application Starts** → The app becomes available immediately on port 8080
2. **Background Download** → Models start downloading in the background
3. **Models Ready** → Once downloaded, they're cached and reused on subsequent startups

## Configuration

### Environment Variables

Set these in your `.env` file or docker-compose:

```bash
# Enable/disable auto-download
TRANSLATION_MODELS_AUTO_DOWNLOAD=true

# Which Whisper model sizes to download
# Options: tiny, base, small, medium, large, large-v2, large-v3
TRANSLATION_MODELS_WHISPER_MODELS=large-v3

# Which Whisper backends to support
# Options: faster-whisper, openai-whisper
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper,openai-whisper

# Which translation models to download
# Options: m2m100, nllb
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
```

### Examples

**Download only tiny model (fast, less accurate):**
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```

**Download multiple model sizes:**
```bash
TRANSLATION_MODELS_WHISPER_MODELS=tiny,medium,large-v3
```

**Disable auto-download (download on first use):**
```bash
TRANSLATION_MODELS_AUTO_DOWNLOAD=false
```

## Model Sizes

| Model | Size | Speed | Accuracy |
|-------|------|-------|----------|
| tiny | ~75 MB | Very Fast | Low |
| base | ~150 MB | Fast | Medium |
| small | ~460 MB | Medium | Good |
| medium | ~1.5 GB | Slow | Very Good |
| large-v3 | ~3 GB | Very Slow | Excellent |

## Docker Usage

### First Run

```bash
# Create .env file
cp .env.example .env

# Edit .env and set your configuration
nano .env

# Start the container
docker-compose up -d

# Watch the logs to see download progress
docker-compose logs -f telegram-bot
```

You'll see logs like:
```
=== MODEL INITIALIZATION STARTED ===
Downloading Faster-Whisper large-v3...
Downloading OpenAI Whisper large-v3...
Downloading m2m100...
✅ All models downloaded successfully!
=== MODEL INITIALIZATION COMPLETED ===
```

### Subsequent Runs

Models are cached in Docker volumes:
- `whisper-cache` - OpenAI Whisper models
- `huggingface-cache` - Faster-Whisper and translation models

**No re-download needed!** The app starts instantly.

## Changing Models

To download different models:

1. Update your `.env` file
2. Restart the container: `docker-compose restart`
3. New models will be downloaded on startup

## Clearing Cache

To force re-download all models:

```bash
docker-compose down
docker volume rm telegram_whisper-cache telegram_huggingface-cache
docker-compose up -d
```

## Troubleshooting

### Download Fails

Check the logs:
```bash
docker-compose logs telegram-bot | grep "MODEL INITIALIZATION"
```

### Out of Disk Space

Large models need ~30 GB total. To save space:
- Use only one backend (either faster-whisper OR openai-whisper)
- Use smaller models (tiny or small instead of large-v3)
- Use only one translation model (m2m100 OR nllb)

Example minimal config (~5 GB total):
```bash
TRANSLATION_MODELS_WHISPER_MODELS=small
TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper
TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100
```
