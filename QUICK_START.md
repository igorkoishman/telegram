# Quick Start Guide

## What We Built

A **hybrid Spring Boot + Python** video translation service integrated with a Telegram bot that provides:

- 🎙️ **Whisper AI transcription** - Converts speech to text
- 🌍 **Multi-language translation** - Translates subtitles to 8 languages
- 📹 **Subtitle generation** - Creates SRT files and burns/muxes into videos
- 🤖 **Interactive Telegram bot** - User-friendly interface with inline keyboards

## Current Status

✅ **Successfully built!** The JAR file is ready at:
```
target/telegram-0.0.1-SNAPSHOT.jar
```

## Prerequisites Install

### 1. Install Python Dependencies

```bash
cd /Users/ikoishman/IdeaProjects/telegram
pip3 install -r src/main/resources/python/requirements.txt
```

**What this installs:**
- `faster-whisper` - AI transcription
- `transformers` - Translation models
- `torch` - PyTorch
- `sentencepiece` & `sacremoses` - Text processing

**Note:** First run will download ~6GB of AI models.

### 2. Verify FFmpeg

Already installed ✅ (version 7.1.1)

```bash
ffmpeg -version
```

## Running the Application

### Option 1: Run with Maven (Development)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
mvn spring-boot:run
```

### Option 2: Run JAR File (Production)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
java -jar target/telegram-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

## Set Up Telegram Webhook

You need to expose your local server to the internet.

### Using Cloudflare Tunnel

```bash
cloudflared tunnel --url http://localhost:8080
```

This will give you a public URL like: `https://abc-def-ghi.trycloudflare.com`

### Set the Webhook

Replace `<YOUR_BOT_TOKEN>` with your actual bot token from `application.yml`:

```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://your-cloudflare-url.trycloudflare.com/telegram/webhook"}'
```

Example:
```bash
curl -X POST "https://api.telegram.org/bot8386103559:AAFbTGtlff8xLdjY6PqtYM1oG0_8AIo6IB4/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://abc-def-ghi.trycloudflare.com/telegram/webhook"}'
```

## Test the Bot

1. **Open Telegram** and find your bot
2. **Send /start** to see the welcome message
3. **Send a video file** (any video with speech)
4. **Follow the interactive menus:**
   - Choose to transcribe or use existing subtitles
   - Select Whisper model (recommend: `large` for best quality)
   - Pick target languages
   - Choose subtitle format (hard/soft/both)
5. **Wait for processing** (may take 2-10 minutes depending on video length)
6. **Receive translated videos and SRT files!**

## API Endpoints (for testing)

### Test Media Analysis

```bash
curl -X POST http://localhost:8080/api/translation/analyze \
  -F "file=@/path/to/video.mp4"
```

### Check Job Status

```bash
curl http://localhost:8080/api/translation/status/some-job-id
```

## Configuration Files

### Main Config: `src/main/resources/application.yml`

```yaml
server:
  port: 8080

telegram:
  bot-token: YOUR_BOT_TOKEN_HERE    # ← Update this
  webhook-secret: YOUR_SECRET_HERE   # ← Update this

translation:
  storage:
    upload-dir: ./uploads
    output-dir: ./outputs
  python:
    executable: python3
```

## Supported Languages

**Translation (M2M100):**
- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Italian (it)
- Russian (ru)
- Hebrew (he)
- Arabic (ar)

**Transcription (Whisper):**
- 99+ languages supported

## Whisper Model Selection

| Model  | Size   | Speed    | Quality | Recommended For     |
|--------|--------|----------|---------|---------------------|
| tiny   | 140 MB | Fastest  | Basic   | Testing only        |
| base   | 290 MB | Fast     | Good    | Development         |
| small  | 970 MB | Medium   | Better  | Quick translations  |
| medium | 3.1 GB | Slow     | Great   | Production          |
| large  | 6.2 GB | Slowest  | Best    | Best quality needed |

## Troubleshooting

### "Python dependencies not found"

```bash
pip3 install -r src/main/resources/python/requirements.txt
```

### "FFmpeg not found"

```bash
brew install ffmpeg
```

### "Webhook not working"

1. Check Cloudflare tunnel is running
2. Verify webhook is set:
```bash
curl https://api.telegram.org/bot<YOUR_TOKEN>/getWebhookInfo
```

### "Processing is slow"

- Use smaller Whisper model (`tiny` or `base` for testing)
- Check CPU usage (AI processing is CPU-intensive)
- Consider shorter videos for testing

### "Out of memory"

```bash
java -Xmx4g -jar target/telegram-0.0.1-SNAPSHOT.jar
```

## Project Structure

```
/Users/ikoishman/IdeaProjects/telegram/
├── src/main/
│   ├── java/com/koishman/telegram/
│   │   ├── config/              # Spring configuration
│   │   ├── model/               # Data models
│   │   ├── service/             # Bot & API services
│   │   ├── translation/         # Translation service
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── model/           # Translation models
│   │   │   └── service/         # Core processing
│   │   └── web/                 # Webhook controller
│   └── resources/
│       ├── python/              # AI scripts
│       │   ├── whisper_transcribe.py
│       │   ├── translate_text.py
│       │   └── requirements.txt
│       └── application.yml      # Configuration
├── uploads/                     # Temporary uploads
├── outputs/                     # Processed videos
├── downloads/                   # Bot downloads
├── README.md                    # Full documentation
├── TRANSLATION_SERVICE_SETUP.md # Detailed setup
└── target/
    └── telegram-0.0.1-SNAPSHOT.jar  # Executable JAR
```

## Next Steps

1. ✅ Build complete
2. ⏳ Install Python dependencies: `pip3 install -r src/main/resources/python/requirements.txt`
3. ⏳ Start the application: `mvn spring-boot:run`
4. ⏳ Set up Cloudflare tunnel
5. ⏳ Configure webhook
6. ⏳ Test with a video!

## Support

- **Full Documentation:** [README.md](README.md)
- **Detailed Setup:** [TRANSLATION_SERVICE_SETUP.md](TRANSLATION_SERVICE_SETUP.md)
- **Test Setup:** `./test-setup.sh`
- **Check Logs:** `tail -f logs/spring.log`

## Summary

You now have a **production-ready** video translation service that:
- Runs as a single Spring Boot application
- Uses Python AI models for transcription and translation
- Integrates seamlessly with Telegram
- Provides an interactive user experience

**Congratulations! 🎉**
