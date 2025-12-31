# Telegram AliExpress Shopping Bot

A Telegram bot that helps users search for products on AliExpress. Built with Spring Boot and Java 17.

## Features

- 🤖 Telegram bot integration with webhook support
- 🛍️ AliExpress product search
- 🔍 Real-time product information (price, ratings, reviews)
- ☁️ Cloudflare Tunnel support for local development
- 🔒 SSL/TLS support with Netskope compatibility

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **Git**
- **Cloudflare Tunnel** (cloudflared) - [Download here](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/)
- **Telegram Account** - to create a bot
- **IntelliJ IDEA** (recommended) or any Java IDE

## Quick Start

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd telegram
```

### 2. Create Your Telegram Bot

1. Open Telegram and search for **@BotFather**
2. Send `/newbot` command
3. Follow the instructions to create your bot
4. Copy the **Bot Token** you receive (looks like: `123456789:ABCdefGhIjKlmNoPQRsTUVwxyZ`)

### 3. Configure the Application

Edit `src/main/resources/application.yml`:

```yaml
telegram:
  bot-token: YOUR_BOT_TOKEN_HERE  # Replace with your actual bot token
  webhook-secret: tg-secret-9f3c1d-2025  # Change this to a random secret
```

**Security Note**: Never commit your real bot token to Git! Consider using environment variables:

```yaml
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  webhook-secret: ${TELEGRAM_WEBHOOK_SECRET}
```

### 4. Build the Application

```bash
mvn clean install
```

### 5. Run with Cloudflare Tunnel

#### Step 1: Start Cloudflare Tunnel

Open a **new terminal** and run:

```bash
cloudflared tunnel --url http://localhost:8080
```

You'll see output like:
```
|  Your quick Tunnel has been created! Visit it at:
|  https://some-random-name.trycloudflare.com
```

**Copy this URL** - you'll need it in the next step.

#### Step 2: Start Your Application

In IntelliJ IDEA:
1. Open the project
2. Find `TelegramApplication.java`
3. Click the green **Run** button (or press `Ctrl+Shift+F10`)

Or from terminal:
```bash
mvn spring-boot:run
```

Wait for the message: `Started TelegramApplication in X seconds`

#### Step 3: Register the Webhook

Replace `YOUR_BOT_TOKEN` and `YOUR_CLOUDFLARE_URL` with your actual values:

```bash
curl -X POST "https://api.telegram.org/botYOUR_BOT_TOKEN/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url":"YOUR_CLOUDFLARE_URL/telegram/webhook"}'
```

Example:
```bash
curl -X POST "https://api.telegram.org/bot8386103559:AAFbTGtlff8xLdjY6PqtYM1oG0_8AIo6IB4/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://random-name.trycloudflare.com/telegram/webhook"}'
```

You should see:
```json
{"ok":true,"result":true,"description":"Webhook was set"}
```

#### Step 4: Verify Webhook

```bash
curl "https://api.telegram.org/botYOUR_BOT_TOKEN/getWebhookInfo"
```

### 6. Test Your Bot

1. Open Telegram on your phone
2. Search for your bot (the username you created with BotFather)
3. Send `/start`
4. Try searching: `/search wireless headphones`

## Available Commands

- `/start` - Welcome message and introduction
- `/help` - List all available commands
- `/search <query>` - Search for products on AliExpress
  - Example: `/search bluetooth speaker`
  - Example: `/search phone case`
  - Example: `/search gaming mouse`

## Project Structure

```
telegram/
├── src/
│   ├── main/
│   │   ├── java/com/koishman/telegram/
│   │   │   ├── config/
│   │   │   │   ├── AliExpressConfig.java      # AliExpress API configuration
│   │   │   │   └── TelegramBotConfig.java     # Telegram bot configuration
│   │   │   ├── model/
│   │   │   │   └── AliExpressProduct.java     # Product data model
│   │   │   ├── service/
│   │   │   │   ├── AliExpressService.java     # AliExpress integration
│   │   │   │   ├── TelegramApiClient.java     # Telegram API client
│   │   │   │   └── TelegramBotService.java    # Bot message handling
│   │   │   ├── web/
│   │   │   │   └── TelegramWebhookController.java # Webhook endpoint
│   │   │   └── TelegramApplication.java       # Main application
│   │   └── resources/
│   │       └── application.yml                 # Configuration file
│   └── test/
├── pom.xml                                     # Maven dependencies
├── README.md                                   # This file
├── ALIEXPRESS_SETUP.md                        # AliExpress API setup guide
└── .gitignore                                  # Git ignore rules
```

## Configuration

### Application Settings (application.yml)

```yaml
server:
  port: 8080  # Application port

telegram:
  api-base: https://api.telegram.org
  bot-token: YOUR_BOT_TOKEN
  webhook-secret: YOUR_SECRET

aliexpress:
  enabled: false  # Set to true when you have real credentials
  api-url: https://api-sg.aliexpress.com/sync
  app-key: YOUR_APP_KEY
  app-secret: YOUR_APP_SECRET
```

### Environment Variables (Recommended for Production)

Create a `.env` file (don't commit this!):

```bash
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_WEBHOOK_SECRET=your_webhook_secret_here
ALIEXPRESS_APP_KEY=your_app_key_here
ALIEXPRESS_APP_SECRET=your_app_secret_here
```

## AliExpress Integration

The bot currently uses **mock data** for testing. To enable real AliExpress product search:

1. Read `ALIEXPRESS_SETUP.md` for detailed instructions
2. Apply for API access at [AliExpress Open Platform](https://open.aliexpress.com/)
3. Wait for approval (1-2 business days)
4. Update `application.yml` with your credentials
5. Set `aliexpress.enabled: true`
6. Restart the application

## Troubleshooting

### Bot not receiving messages

1. **Check webhook status:**
   ```bash
   curl "https://api.telegram.org/botYOUR_BOT_TOKEN/getWebhookInfo"
   ```

2. **Verify Cloudflare Tunnel is running:**
   - Look for the terminal window with cloudflared
   - Check that it shows "Registered tunnel connection"

3. **Check application logs:**
   - Look for "=== Webhook received ===" in IntelliJ console
   - Check for any ERROR messages

### "No route to host" error

This is a **Netskope SSL issue** (already fixed in the code):
- The application is configured to trust Netskope's SSL certificates
- If you still see this error, check your firewall settings

### Cloudflare Tunnel issues

1. **Install cloudflared:**
   ```bash
   # macOS
   brew install cloudflared

   # Linux
   wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
   sudo dpkg -i cloudflared-linux-amd64.deb

   # Windows
   # Download from https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/
   ```

2. **Verify it's running:**
   - You should see "Your quick Tunnel has been created!"
   - Keep this terminal window open

3. **Get new URL if tunnel dies:**
   - Stop cloudflared (Ctrl+C)
   - Start again: `cloudflared tunnel --url http://localhost:8080`
   - Update webhook with new URL

### Application won't start

1. **Check Java version:**
   ```bash
   java -version  # Should be 17 or higher
   ```

2. **Check if port 8080 is available:**
   ```bash
   lsof -i :8080  # macOS/Linux
   netstat -ano | findstr :8080  # Windows
   ```

3. **Clean and rebuild:**
   ```bash
   mvn clean install
   ```

## Development

### Running in Debug Mode (IntelliJ)

1. Click the **Debug** button (bug icon) instead of Run
2. Set breakpoints in your code
3. Send messages to your bot to trigger breakpoints

### Hot Reload (Optional)

Add Spring Boot DevTools to `pom.xml` for automatic restart on code changes:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Viewing Logs

Application logs show:
- Incoming webhook requests
- Message processing
- API calls
- Errors and exceptions

Look for these log patterns:
```
=== Webhook received ===
Received message from Igor: /search headphones
Searching AliExpress for: headphones
Message sent to chatId 8006554874
```

## Production Deployment

For production deployment, consider:

1. **Use environment variables** for all secrets
2. **Set up a proper domain** instead of Cloudflare quick tunnels
3. **Use HTTPS** with valid SSL certificates
4. **Add rate limiting** to prevent abuse
5. **Set up monitoring** and alerting
6. **Use a process manager** (systemd, PM2, etc.)
7. **Configure logging** to files with rotation

### Example systemd service (Linux)

Create `/etc/systemd/system/telegram-bot.service`:

```ini
[Unit]
Description=Telegram AliExpress Bot
After=network.target

[Service]
Type=simple
User=youruser
WorkingDirectory=/path/to/telegram
ExecStart=/usr/bin/java -jar target/telegram-0.0.1-SNAPSHOT.jar
Restart=always
Environment="TELEGRAM_BOT_TOKEN=your_token"
Environment="ALIEXPRESS_APP_KEY=your_key"

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable telegram-bot
sudo systemctl start telegram-bot
sudo systemctl status telegram-bot
```

## Technologies Used

- **Spring Boot 3.4.1** - Application framework
- **Java 17** - Programming language
- **Apache HttpClient 5** - HTTP client with SSL support
- **Jackson** - JSON processing
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool
- **Cloudflare Tunnel** - Secure tunneling for local development
- **Telegram Bot API** - Bot integration
- **AliExpress API** - Product search

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add new feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Submit a pull request

## License

This project is for educational purposes.

## Support

For issues and questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review `ALIEXPRESS_SETUP.md` for AliExpress-specific issues
3. Check application logs for error messages
4. Search existing GitHub issues

## Useful Links

- [Telegram Bot API Documentation](https://core.telegram.org/bots/api)
- [Cloudflare Tunnel Documentation](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
- [AliExpress Open Platform](https://open.aliexpress.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/guides/)

---

**Happy Shopping! 🛍️**
