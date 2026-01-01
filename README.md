# Telegram Video Translation Bot

A hybrid Spring Boot + Python application that provides AI-powered video subtitle generation and translation through a Telegram bot interface.

## Features

✨ **AI-Powered Transcription**
- Whisper AI for accurate speech-to-text
- Multiple model sizes (tiny to large)
- 99+ language support

🌍 **Multi-Language Translation**
- M2M100 neural machine translation
- 8 core languages: English, Spanish, French, German, Italian, Russian, Hebrew, Arabic
- Batch translation for multiple target languages

📹 **Flexible Subtitle Formats**
- Hard-burned subtitles (embedded in video)
- Soft subtitles (separate tracks, toggle-able)
- Both formats simultaneously

🎬 **Media Processing**
- FFmpeg-based video processing
- Multi-track audio/subtitle detection
- Automatic audio extraction

🤖 **Telegram Bot Integration**
- Interactive menu system with inline keyboards
- Real-time progress updates
- Automatic file delivery

## Quick Start

### 1. Prerequisites Check

```bash
./test-setup.sh
```

### 2. Install Python Dependencies

```bash
pip3 install -r src/main/resources/python/requirements.txt
```

This will install:
- faster-whisper (Whisper AI)
- transformers (Translation models)
- torch (PyTorch)
- sentencepiece & sacremoses (Tokenization)

### 3. Build and Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Set Up Telegram Webhook

You need to expose your local server to the internet. Use one of:

**Option A: Cloudflare Tunnel (Recommended)**
```bash
cloudflared tunnel --url http://localhost:8080
```

**Option B: ngrok**
```bash
ngrok http 8080
```

Then set the webhook:
```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://your-tunnel-url.com/telegram/webhook"}'
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Spring Boot (Port 8080)                │
│                                                         │
│  ┌──────────────┐         ┌────────────────────┐       │
│  │ Telegram Bot │ ◄─────► │ Translation API    │       │
│  │ /telegram/*  │         │ /api/translation/* │       │
│  └──────────────┘         └─────────┬──────────┘       │
│                                     │                  │
│                    ┌────────────────┴────────────┐     │
│                    │                              │     │
│            ┌───────▼──────┐              ┌────────▼────┐│
│            │ FFmpeg       │              │ Python AI   ││
│            │ (Java)       │              │ (Subprocess)││
│            └──────────────┘              └─────────────┘│
│            • Extract audio                • Whisper AI  │
│            • Burn subs                    • Translation │
│            • Mux tracks                                 │
└─────────────────────────────────────────────────────────┘
```

## User Workflow

1. **Send Video** → User sends video file to bot
2. **Analyze** → Bot analyzes existing audio/subtitle tracks
3. **Choose Options** → Interactive menus for:
   - Use existing subtitles OR transcribe new
   - Whisper model size (if transcribing)
   - Target languages
   - Subtitle format (hard/soft/both)
4. **Process** → Job submitted, real-time updates
5. **Receive** → Bot sends back processed videos and SRT files

## API Endpoints

### Telegram Bot
- `POST /telegram/webhook` - Receive updates from Telegram

### Translation Service
- `POST /api/translation/analyze` - Analyze media tracks
- `POST /api/translation/upload` - Submit translation job
- `GET /api/translation/status/{jobId}` - Check job status
- `GET /api/translation/download/{filename}` - Download results

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
telegram:
  bot-token: YOUR_BOT_TOKEN_HERE
  webhook-secret: YOUR_SECRET_HERE

translation:
  storage:
    upload-dir: ./uploads
    output-dir: ./outputs
  python:
    executable: python3
```

## Whisper Models

| Model  | Size   | Speed | Accuracy | Use Case          |
|--------|--------|-------|----------|-------------------|
| tiny   | 140 MB | Fast  | Low      | Testing           |
| base   | 290 MB | Fast  | Good     | Development       |
| small  | 970 MB | Medium| Good     | Balanced          |
| medium | 3.1 GB | Slow  | Better   | Production        |
| large  | 6.2 GB | Slowest | Best   | Best quality      |

Models download automatically on first use.

## Supported Languages

**Translation:** en, es, fr, de, it, ru, he, ar
**Transcription:** 99+ languages (via Whisper)

## Project Structure

```
src/main/java/com/koishman/telegram/
├── config/                   # Configuration classes
├── model/                    # Telegram bot DTOs
├── service/                  # Telegram bot services
│   ├── EnhancedTelegramBotService.java
│   ├── SessionManager.java
│   ├── TelegramApiClient.java
│   └── TranslationApiClient.java
├── translation/              # Translation service
│   ├── controller/
│   │   └── TranslationController.java
│   ├── model/
│   │   ├── TranslationJob.java
│   │   ├── SubtitleSegment.java
│   │   └── ...
│   └── service/
│       ├── SubtitleProcessingService.java
│       ├── WhisperService.java
│       ├── TranslationService.java
│       ├── FFmpegService.java
│       └── ...
└── web/
    └── TelegramWebhookController.java

src/main/resources/
├── python/
│   ├── whisper_transcribe.py
│   ├── translate_text.py
│   └── requirements.txt
└── application.yml
```

## Troubleshooting

### Python Dependencies
```bash
pip3 install -r src/main/resources/python/requirements.txt
```

### FFmpeg Not Found
```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt-get install ffmpeg
```

### Out of Memory
```bash
# Increase heap size
java -Xmx4g -jar target/telegram-0.0.1-SNAPSHOT.jar

# Or use smaller Whisper models
```

### Slow Processing
- Use smaller Whisper model (tiny/base)
- Reduce video resolution
- Enable GPU if available (requires CUDA setup)

## Development

### Run Tests
```bash
./mvnw test
```

### Build Production JAR
```bash
./mvnw clean package
java -jar target/telegram-0.0.1-SNAPSHOT.jar
```

### Debug Mode
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## Documentation

- [Translation Service Setup Guide](TRANSLATION_SERVICE_SETUP.md) - Detailed setup instructions
- [Python Translation Service Reference](/Users/ikoishman/PycharmProjects/translation) - Original Python implementation

## Tech Stack

- **Java 17** - Core application
- **Spring Boot 3.4.1** - Web framework
- **Python 3.8+** - AI/ML processing
- **faster-whisper** - Speech-to-text
- **HuggingFace Transformers** - Translation models
- **FFmpeg** - Video processing
- **Apache HttpClient 5** - HTTP communication
- **Telegram Bot API** - Bot integration

## Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## License

[Add your license here]

## Support

For issues or questions:
- Check logs: `tail -f logs/spring.log`
- Verify setup: `./test-setup.sh`
- Review documentation: `TRANSLATION_SERVICE_SETUP.md`

---

Built with ❤️ using Spring Boot, Python, and AI
