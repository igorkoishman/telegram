# Long Polling Mode - Free Setup (No Cloudflare Needed!)

## What is Long Polling?

Your bot now works in **Long Polling mode**, which means:
- ✅ **NO webhooks needed**
- ✅ **NO Cloudflare Tunnel needed**
- ✅ **NO domain name needed**
- ✅ **NO public IP needed**
- ✅ **100% FREE** - works on any private network
- ✅ **Works everywhere** - Mac, Linux, Windows (with Docker)

## How It Works

```
Your Bot (Docker)  ←──→  Telegram Servers
    (Asks for updates)     (Sends updates)
```

Your bot continuously asks Telegram "Any new messages?" and Telegram responds with updates. No external services needed!

---

## Quick Start

### 1. Build and Start

```bash
# For CPU-only (Mac, Linux without GPU)
docker-compose -f docker-compose.cpu.yml up -d

# For GPU (Linux with NVIDIA)
docker-compose up -d
```

That's it! Your bot is now running and listening for messages.

### 2. Test Your Bot

1. Open Telegram
2. Find your bot (search for your bot username)
3. Send `/start`
4. Bot should respond!

### 3. Check Logs

```bash
# View bot logs
docker logs telegram-translator-cpu -f

# You should see:
# "🚀 Starting Telegram Long Polling mode..."
# "📡 Long Polling started. Waiting for updates..."
```

---

## Environment Variables

Your `.env` file should have:

```env
# Your bot token from @BotFather
TELEGRAM_BOT_TOKEN=your_token_here

# Set to 'polling' for Long Polling mode
TELEGRAM_MODE=polling
```

That's all you need!

---

## Commands

### Start/Stop the Bot

```bash
# Start
docker-compose -f docker-compose.cpu.yml up -d

# Stop
docker-compose -f docker-compose.cpu.yml down

# Restart
docker-compose -f docker-compose.cpu.yml restart

# View logs
docker-compose -f docker-compose.cpu.yml logs -f
```

### Check Status

```bash
# Check if container is running
docker ps

# Check application health
curl http://localhost:8080/actuator/health
```

---

## Advantages of Long Polling

### Pros:
- ✅ Simple setup - no external services
- ✅ Works on any network (home, office, private)
- ✅ No domain or SSL certificate needed
- ✅ Free forever
- ✅ Reliable for personal/small scale use

### Cons:
- ⚠️ Your Docker container must be running 24/7
- ⚠️ If container stops, bot stops receiving messages
- ⚠️ Slightly higher latency (~1-2 seconds) compared to webhooks

---

## When to Use Long Polling vs Webhooks

### Use Long Polling (Current Setup):
- ✅ Personal use
- ✅ Development/testing
- ✅ Home network deployment
- ✅ Small number of users (< 100)
- ✅ Don't want to pay for domain/hosting

### Use Webhooks:
- Production deployment with many users
- Need instant message delivery
- Have a public server/domain
- Want to scale horizontally

For most personal projects, **Long Polling is perfect!**

---

## Troubleshooting

### Problem: Bot doesn't respond to messages

**Check if bot is running:**
```bash
docker ps
```

**Check logs for errors:**
```bash
docker logs telegram-translator-cpu
```

**Verify bot token:**
```bash
cat .env | grep TELEGRAM_BOT_TOKEN
```

### Problem: "Unauthorized" error

Your bot token is invalid or expired. Get a new token from @BotFather.

### Problem: Bot receives old messages

This is normal after restart. Long polling will process pending updates.

### Problem: Container keeps restarting

**Check Java memory settings:**
```env
# In .env file, reduce memory if needed:
JAVA_OPTS=-Xmx2g -Xms512m
```

**View detailed logs:**
```bash
docker logs telegram-translator-cpu --tail=100
```

---

## Running on Different Machines

### Same setup works on:

1. **Mac (Docker Desktop)** ✅
   ```bash
   docker-compose -f docker-compose.cpu.yml up -d
   ```

2. **Linux (Home Server)** ✅
   ```bash
   docker-compose -f docker-compose.cpu.yml up -d
   ```

3. **Windows (WSL2 + Docker Desktop)** ✅
   ```powershell
   docker-compose -f docker-compose.cpu.yml up -d
   ```

4. **Raspberry Pi** ✅ (use CPU version)
   ```bash
   docker-compose -f docker-compose.cpu.yml up -d
   ```

No matter where you run it, as long as:
- Docker is installed
- `.env` file has your bot token
- Machine has internet connection

The bot will work!

---

## Performance

Long Polling is very efficient:
- Uses minimal bandwidth
- Low CPU usage when idle
- Telegram's long polling timeout is 30 seconds (we ask every 30s)
- Messages arrive within 1-2 seconds

For personal use, you won't notice any difference from webhooks!

---

## Security

Long Polling is secure:
- ✅ All communication is over HTTPS
- ✅ Bot token is kept private in Docker
- ✅ No ports exposed to the internet
- ✅ No webhook endpoint to secure

The only thing to protect is your bot token (keep `.env` file private).

---

## Cost Comparison

### Long Polling (This Setup):
- Docker: **FREE**
- Telegram API: **FREE**
- **Total: $0/month**

### Webhook Setup:
- Domain: ~$10-15/year
- SSL Certificate: FREE (with Cloudflare)
- Cloudflare Tunnel: FREE
- **Total: ~$10-15/year**

For personal projects, Long Polling saves you money!

---

## Migration to Webhooks (Optional)

If you later want to switch to webhooks:

1. Set up Cloudflare Tunnel (see `CLOUDFLARE_TUNNEL_SETUP.md`)
2. Change in `.env`:
   ```env
   TELEGRAM_MODE=webhook
   CLOUDFLARE_TUNNEL_TOKEN=your_token
   ```
3. Restart:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

The bot automatically switches modes!

---

## Summary

You now have a **fully functional Telegram bot** running in Long Polling mode:

- ✅ No external services needed
- ✅ No domain or SSL setup
- ✅ No credit card required
- ✅ Works on your private network
- ✅ 100% FREE forever

Just run `docker-compose up -d` and you're good to go!

---

## Getting Help

If you encounter issues:

1. **Check logs**: `docker logs telegram-translator-cpu -f`
2. **Verify token**: `cat .env | grep TELEGRAM_BOT_TOKEN`
3. **Test health**: `curl http://localhost:8080/actuator/health`
4. **Check container**: `docker ps`

Your bot is ready to use! 🎉
