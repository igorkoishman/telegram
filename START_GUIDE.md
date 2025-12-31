# Daily Startup Guide - Telegram AliExpress Bot

This guide shows you exactly how to start your Telegram bot each day.

## Prerequisites Check

Before starting, make sure you have:
- ✅ Java 17 installed
- ✅ Maven installed
- ✅ Cloudflare Tunnel (cloudflared) installed
- ✅ Your Telegram bot token (already configured in application.yml)

---

## Step-by-Step Startup Process

### 1. Open Terminal Window #1 - Start Cloudflare Tunnel

```bash
cd /Users/ikoishman/IdeaProjects/telegram
cloudflared tunnel --url http://localhost:8080
```

**Expected output:**
```
Your quick Tunnel has been created! Visit it at:
https://some-random-name.trycloudflare.com
```

**⚠️ IMPORTANT:** Copy the URL (e.g., `https://some-random-name.trycloudflare.com`)
**Keep this terminal window open!**

---

### 2. Open Terminal Window #2 - Start the Application

**Option A: Using Maven (Recommended)**

```bash
cd /Users/ikoishman/IdeaProjects/telegram
mvn spring-boot:run
```

**Option B: Using IntelliJ IDEA**

1. Open IntelliJ IDEA
2. Open the `telegram` project
3. Find `TelegramApplication.java` in the Project Explorer
4. Right-click and select "Run 'TelegramApplication'"
5. Or click the green Run button (▶️)

**Expected output:**
```
Started TelegramApplication in X seconds (process running for X)
```

**Keep this running!**

---

### 3. Open Terminal Window #3 - Register Webhook

Replace `YOUR_CLOUDFLARE_URL` with the URL from Step 1:

```bash
curl -X POST "https://api.telegram.org/bot8386103559:AAFbTGtlff8xLdjY6PqtYM1oG0_8AIo6IB4/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url":"YOUR_CLOUDFLARE_URL/telegram/webhook"}'
```

**Example:**
```bash
curl -X POST "https://api.telegram.org/bot8386103559:AAFbTGtlff8xLdjY6PqtYM1oG0_8AIo6IB4/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://random-name.trycloudflare.com/telegram/webhook"}'
```

**Expected response:**
```json
{"ok":true,"result":true,"description":"Webhook was set"}
```

---

### 4. Verify Webhook Registration

```bash
curl "https://api.telegram.org/bot8386103559:AAFbTGtlff8xLdjY6PqtYM1oG0_8AIo6IB4/getWebhookInfo"
```

**Check that:**
- ✅ `"url"` matches your Cloudflare URL + `/telegram/webhook`
- ✅ `"pending_update_count": 0` (no pending messages)
- ✅ `"last_error_date"` is not present (or very old)

---

### 5. Test Your Bot

Open Telegram on your phone and test:

```
/start
/help
/search wireless headphones
/search bluetooth speaker
/search phone case
```

---

## Quick Start (All Commands Together)

If you want to copy-paste everything at once:

### Terminal 1:
```bash
cd /Users/ikoishman/IdeaProjects/telegram
cloudflared tunnel --url http://localhost:8080
# Copy the URL that appears!
```

### Terminal 2:
```bash
cd /Users/ikoishman/IdeaProjects/telegram
mvn spring-boot:run
# Wait for "Started TelegramApplication"
```

### Terminal 3 (Replace YOUR_CLOUDFLARE_URL):
```bash
curl -X POST "https://api.telegram.org/bot8386103559:AAFbTGtlff8xLdjY6PqtYM1oG0_8AIo6IB4/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{"url":"YOUR_CLOUDFLARE_URL/telegram/webhook"}'
```

---

## Troubleshooting

### Problem: "Port 8080 already in use"

**Solution:** Kill existing Java process

```bash
# Find the process
jps -l | grep TelegramApplication

# Kill it (replace XXXX with the process ID)
kill -9 XXXX

# Or kill all Java processes (if you're sure)
pkill -f TelegramApplication
```

### Problem: Bot not responding

**Check:**
1. Cloudflare tunnel is running (Terminal 1 shows "connection registered")
2. Application is running (Terminal 2 shows "Started TelegramApplication")
3. Webhook is registered correctly (run getWebhookInfo command)

### Problem: "No route to host" error

**This is already fixed!** Your code has Netskope SSL certificate trust enabled.

### Problem: Cloudflare URL changed

**You need to:**
1. Get the new URL from Terminal 1
2. Re-register the webhook using the curl command in Step 3

---

## Shutdown Process

When you're done for the day:

1. **Terminal 1:** Press `Ctrl+C` to stop Cloudflare Tunnel
2. **Terminal 2:** Press `Ctrl+C` to stop the application
   - Or in IntelliJ: Click the red Stop button (⬛)

**Note:** The webhook URL will change next time you start Cloudflare Tunnel, so you'll need to register it again!

---

## Bot Commands

Once running, your bot supports:

- `/start` - Welcome message
- `/help` - Show all commands
- `/search <query>` - Search AliExpress products
  - Examples:
    - `/search wireless headphones`
    - `/search bluetooth speaker`
    - `/search gaming mouse`
    - `/search phone case`

---

## Features

✅ **Real AliExpress Product Search** - Web scraping (100% free, no API key needed)
✅ **Live Product Data** - Titles, prices, ratings, order counts, links
✅ **Fallback Mode** - Uses mock data if scraping fails
✅ **SSL Support** - Works with Netskope corporate SSL
✅ **Cloudflare Tunnel** - Secure public access to localhost

---

## Important Notes

1. **Cloudflare URL changes** every time you restart the tunnel
2. **Always register the webhook** after starting Cloudflare Tunnel
3. **Keep both Terminal 1 and Terminal 2 running** while using the bot
4. **Test with /start first** to verify the bot is responding

---

## Need Help?

- Check `README.md` for detailed documentation
- Check `ALIEXPRESS_SETUP.md` for AliExpress API setup (optional)
- Review application logs in Terminal 2 for errors
- Run `getWebhookInfo` to debug webhook issues

---

**Happy coding! 🚀**
