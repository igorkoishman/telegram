# Translation Service Setup Guide

## Overview

This Spring Boot application provides video subtitle generation and translation capabilities using:
- **Java** for web APIs, file management, and FFmpeg operations
- **Python** for AI/ML (Whisper transcription, M2M100 translation)
- **FFmpeg** for video/audio processing

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot Application (Port 8080)                         │
│                                                             │
│ ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│ │  Telegram   │  │  Translation │  │  File Storage &   │  │
│ │  Bot API    │──│  REST API    │──│  Job Tracking     │  │
│ └─────────────┘  └──────────────┘  └────────────────────┘  │
│                         │                                   │
│         ┌───────────────┴────────────────┐                 │
│         │                                 │                 │
│  ┌──────▼──────┐                  ┌──────▼──────┐          │
│  │   FFmpeg    │                  │   Python    │          │
│  │  Subprocess │                  │  Subprocess │          │
│  └─────────────┘                  └─────────────┘          │
│   • Extract audio                  • Whisper AI            │
│   • Burn subtitles                 • M2M100 Translation    │
│   • Mux subtitles                                           │
└─────────────────────────────────────────────────────────────┘
```

## Prerequisites

### 1. Java Development Kit (JDK)
- Java 17 or higher
- Check: `java -version`

### 2. Maven
- Maven 3.6+ (included via mvnw wrapper)
- Check: `./mvnw --version`

### 3. FFmpeg
Install FFmpeg with subtitle support:

**macOS:**
```bash
brew install ffmpeg
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install ffmpeg
```

**Verify:**
```bash
ffmpeg -version
ffprobe -version
```

### 4. Python Environment
Python 3.8+ required for AI models.

**Create virtual environment:**
```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

**Install Python dependencies:**
```bash
cd src/main/resources/python
pip install -r requirements.txt
```

**Dependencies installed:**
- `faster-whisper` - Optimized Whisper implementation
- `transformers` - HuggingFace models
- `torch` - PyTorch for ML models
- `sentencepiece` - Tokenization for translation
- `sacremoses` - Text preprocessing

## Installation

### Step 1: Clone and Build

```bash
cd /Users/ikoishman/IdeaProjects/telegram
./mvnw clean install
```

### Step 2: Configure Application

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

telegram:
  api-base: https://api.telegram.org
  bot-token: YOUR_BOT_TOKEN_HERE
  webhook-secret: YOUR_SECRET_HERE
  translation-api-base: http://localhost:8080
  temp-download-dir: ./downloads

translation:
  storage:
    upload-dir: ./uploads
    output-dir: ./outputs
  python:
    executable: python3  # Or path to venv: venv/bin/python3
    scripts-dir: src/main/resources/python
```

### Step 3: Create Storage Directories

```bash
mkdir -p uploads outputs downloads
```

### Step 4: Test Python Scripts

```bash
cd src/main/resources/python

# Test Whisper (downloads model on first run)
python3 whisper_transcribe.py test_audio.wav --model tiny

# Test Translation
python3 translate_text.py "Hello world" --source en --target es
```

## Running the Application

### Development Mode

```bash
./mvnw spring-boot:run
```

### Production Mode

```bash
# Build JAR
./mvnw clean package

# Run
java -jar target/telegram-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### 1. Analyze Media
```bash
POST /api/translation/analyze
Content-Type: multipart/form-data

Parameters:
- file: Video file to analyze

Response:
{
  "tracks": [
    {
      "index": 0,
      "type": "audio",
      "codec": "aac",
      "lang": "eng",
      "default": 1
    },
    {
      "index": 1,
      "type": "subtitle",
      "codec": "srt",
      "lang": "eng"
    }
  ]
}
```

### 2. Upload and Process Video
```bash
POST /api/translation/upload
Content-Type: multipart/form-data

Parameters:
- file: Video file
- langs: Space-separated language codes (e.g., "en es fr")
- model: Whisper model size (tiny, base, small, medium, large)
- model_type: "faster-whisper" (default)
- subtitle_burn_type: "hard", "soft", or "both"
- use_subtitles_only: true/false
- original_lang: Source language code (optional)
- audio_track: Audio track index (optional)
- subtitle_track: Subtitle track index (optional)

Response:
{
  "job_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 3. Check Job Status
```bash
GET /api/translation/status/{jobId}

Response:
{
  "status": "done",
  "outputs": {
    "orig_srt": "filename_orig.srt",
    "en_srt": "filename_en.srt",
    "en": "filename_en.mp4"
  },
  "duration_seconds": "123"
}
```

### 4. Download Output File
```bash
GET /api/translation/download/{filename}?jobId={jobId}

Returns: Video or subtitle file
```

## Model Management

### Whisper Models

Models are downloaded automatically on first use and cached in `~/.cache/huggingface/`.

**Model Sizes:**
- `tiny` - 39M parameters, fastest, least accurate
- `base` - 74M parameters
- `small` - 244M parameters
- `medium` - 769M parameters
- `large` - 1550M parameters, best accuracy, slowest

**Disk Space Required:**
- tiny: ~140 MB
- base: ~290 MB
- small: ~970 MB
- medium: ~3.1 GB
- large: ~6.2 GB

### Translation Models

M2M100 models download automatically:
- `facebook/m2m100_418M` - 418M parameters (default)
- `facebook/m2m100_1.2B` - 1.2B parameters (better quality)

**Supported Languages:**
en, es, fr, de, it, ru, he, ar

## Telegram Bot Integration

The bot uses the following workflow:

1. **User sends video** → Webhook receives update
2. **Download file** → `TelegramApiClient.downloadFile()`
3. **Analyze media** → `POST /api/translation/analyze`
4. **Show options** → Interactive keyboard (Whisper model, languages, burn type)
5. **Submit job** → `POST /api/translation/upload`
6. **Poll status** → `GET /api/translation/status/{jobId}` every 10 seconds
7. **Download results** → `GET /api/translation/download/{filename}`
8. **Send to user** → `TelegramApiClient.sendVideo()` or `sendDocument()`

## Troubleshooting

### Python Scripts Not Found
```bash
# Ensure you're in the project root
cd /Users/ikoishman/IdeaProjects/telegram

# Check script paths
ls -la src/main/resources/python/
```

### FFmpeg Not Found
```bash
# Add to PATH or use full path in code
which ffmpeg
which ffprobe
```

### Out of Memory (Large Models)
```bash
# Increase JVM heap size
java -Xmx4g -jar target/telegram-0.0.1-SNAPSHOT.jar

# Or use smaller Whisper models (tiny, base, small)
```

### Slow Processing
- Use GPU if available (requires NVIDIA GPU + CUDA)
- Use smaller models (tiny/base for testing)
- Reduce video resolution before processing

### Model Download Issues
```bash
# Manually download models
python3 -c "from faster_whisper import WhisperModel; WhisperModel('large')"
python3 -c "from transformers import M2M100ForConditionalGeneration; M2M100ForConditionalGeneration.from_pretrained('facebook/m2m100_418M')"
```

## Performance Tips

1. **Use appropriate Whisper model:**
   - Development/testing: `tiny` or `base`
   - Production: `medium` or `large`

2. **Enable caching:**
   - Models are cached after first download
   - Don't delete `~/.cache/huggingface/`

3. **Optimize FFmpeg:**
   - Use hardware acceleration if available
   - Consider pre-processing videos (resolution, codec)

4. **Async processing:**
   - Jobs run asynchronously
   - Multiple jobs can process concurrently

## File Structure

```
telegram/
├── src/main/
│   ├── java/com/koishman/telegram/
│   │   ├── config/
│   │   │   ├── AsyncConfig.java
│   │   │   └── TelegramBotConfig.java
│   │   ├── model/
│   │   │   ├── JobResponse.java
│   │   │   ├── JobStatus.java
│   │   │   ├── MediaAnalysis.java
│   │   │   ├── MediaTrack.java
│   │   │   └── UserSession.java
│   │   ├── service/
│   │   │   ├── EnhancedTelegramBotService.java
│   │   │   ├── SessionManager.java
│   │   │   ├── TelegramApiClient.java
│   │   │   └── TranslationApiClient.java
│   │   ├── translation/
│   │   │   ├── controller/
│   │   │   │   └── TranslationController.java
│   │   │   ├── model/
│   │   │   │   ├── TranslationJob.java
│   │   │   │   ├── TranslationJobRequest.java
│   │   │   │   ├── MediaTrackInfo.java
│   │   │   │   └── SubtitleSegment.java
│   │   │   └── service/
│   │   │       ├── FileStorageService.java
│   │   │       ├── JobTrackingService.java
│   │   │       ├── FFmpegService.java
│   │   │       ├── WhisperService.java
│   │   │       ├── TranslationService.java
│   │   │       └── SubtitleProcessingService.java
│   │   ├── web/
│   │   │   └── TelegramWebhookController.java
│   │   └── TelegramApplication.java
│   └── resources/
│       ├── python/
│       │   ├── whisper_transcribe.py
│       │   ├── translate_text.py
│       │   └── requirements.txt
│       └── application.yml
├── uploads/          # Uploaded videos (temporary)
├── outputs/          # Processed videos (by job ID)
├── downloads/        # Telegram bot downloads
└── pom.xml
```

## Next Steps

1. **Set up webhook** for Telegram bot
2. **Configure Cloudflare tunnel** or ngrok for HTTPS
3. **Test complete workflow** with a sample video
4. **Monitor logs** for any issues
5. **Optimize** model sizes and processing parameters

## Support

For issues or questions:
1. Check logs: `tail -f logs/spring.log`
2. Verify FFmpeg: `ffmpeg -version`
3. Test Python scripts independently
4. Check disk space for model cache

Happy translating! 🎬🌍
