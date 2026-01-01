# Cloudflare Tunnel Setup Guide

Complete guide for setting up Cloudflare Tunnel with your Telegram Bot running in Docker.

## Why Do You Need Cloudflare Tunnel?

Even when running your bot on Docker (whether on your Mac, Linux machine, or any private network), you need Cloudflare Tunnel because:

1. **Your bot is behind your home router** - No public IP address
2. **Telegram webhooks require HTTPS** - Telegram's servers need secure HTTPS access
3. **No port forwarding needed** - Cloudflare Tunnel eliminates the need to open ports on your router
4. **Works anywhere** - Your private network, no VPN or proxy required

## Architecture

```
Telegram Servers (Internet)
         ↓ HTTPS
Cloudflare Tunnel (Cloud)
         ↓ Secure Connection
cloudflared container (Docker)
         ↓ HTTP
telegram-bot container (Docker)
         ↓
Your Application (port 8080)
```

---

## Prerequisites

1. **Cloudflare Account** (Free)
   - Sign up at https://dash.cloudflare.com/sign-up

2. **Domain Name** (Required)
   - You need a domain (can be free or paid)
   - Domain must be added to Cloudflare

3. **Docker & Docker Compose** installed

---

## Step-by-Step Setup

### Step 1: Add Your Domain to Cloudflare

1. **Get a domain** (if you don't have one):
   - Free options: Freenom, dot.tk
   - Paid options: Namecheap, GoDaddy, Cloudflare Registrar

2. **Add domain to Cloudflare**:
   - Log in to https://dash.cloudflare.com/
   - Click "Add a Site"
   - Enter your domain name
   - Select the Free plan
   - Update your domain's nameservers to Cloudflare's nameservers
   - Wait for activation (can take up to 24 hours)

### Step 2: Create a Cloudflare Tunnel

1. **Go to Zero Trust Dashboard**:
   - Navigate to https://one.dash.cloudflare.com/
   - If first time, you'll need to set up a team name (can be anything)

2. **Create a Tunnel**:
   - In the left sidebar: **Networks** → **Tunnels**
   - Click **Create a tunnel**
   - Select **Cloudflared** as the connector type
   - Give it a name (e.g., `telegram-bot-tunnel`)
   - Click **Save tunnel**

3. **Get Your Tunnel Token**:
   - After creating, you'll see installation instructions
   - **IMPORTANT**: Copy the tunnel token from the Docker command
   - It looks like: `eyJhIjoiXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX...`
   - Save this token - you'll need it in your `.env` file

4. **Configure Public Hostname**:
   - In the **Public Hostname** tab, click **Add a public hostname**
   - **Subdomain**: `telegram-bot` (or whatever you prefer)
   - **Domain**: Select your domain from the dropdown
   - **Service**:
     - Type: `HTTP`
     - URL: `telegram-bot:8080` (this is the Docker service name)
   - Click **Save hostname**

5. **Your webhook URL will be**:
   ```
   https://telegram-bot.yourdomain.com
   ```

### Step 3: Configure Your Environment

1. **Create `.env` file** (copy from `.env.example`):
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file** with your values:
   ```env
   # Your Telegram bot token from @BotFather
   TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz

   # Any random secret string for webhook security
   TELEGRAM_WEBHOOK_SECRET=my-super-secret-webhook-key-2025

   # Your Cloudflare Tunnel token (the long string you copied)
   CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX...

   # Docker Hub username (optional, for pulling pre-built images)
   DOCKER_USERNAME=igorkoishman

   # Other settings (optional)
   JAVA_OPTS=-Xmx4g -Xms1g
   SPRING_PROFILES_ACTIVE=production
   TRANSLATION_MODELS_AUTO_DOWNLOAD=true
   TRANSLATION_MODELS_WHISPER_MODELS=large-v3
   TRANSLATION_MODELS_WHISPER_BACKENDS=faster-whisper,openai-whisper
   TRANSLATION_MODELS_TRANSLATION_MODELS=m2m100,nllb
   ```

### Step 4: Start Your Services

1. **For CPU-only systems** (Mac, Linux without GPU):
   ```bash
   docker-compose -f docker-compose.cpu.yml up -d
   ```

2. **For GPU systems** (Linux with NVIDIA GPU):
   ```bash
   docker-compose up -d
   ```

3. **Check if services are running**:
   ```bash
   docker ps
   ```

   You should see two containers:
   - `telegram-translator-cpu` (or `telegram-translator`)
   - `telegram-cloudflare-tunnel`

4. **Check logs**:
   ```bash
   # Bot logs
   docker logs telegram-translator-cpu -f

   # Cloudflare tunnel logs
   docker logs telegram-cloudflare-tunnel -f
   ```

### Step 5: Set Up Telegram Webhook

Your bot needs to tell Telegram where to send updates. You have two options:

#### Option A: Automatic (Bot does it on startup)

Your bot should automatically register the webhook when it starts. Check the logs:

```bash
docker logs telegram-translator-cpu | grep -i webhook
```

#### Option B: Manual (if automatic doesn't work)

Use this curl command to set the webhook manually:

```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://telegram-bot.yourdomain.com/webhook/telegram/<YOUR_WEBHOOK_SECRET>",
    "allowed_updates": ["message", "callback_query"]
  }'
```

Replace:
- `<YOUR_BOT_TOKEN>` with your actual bot token
- `telegram-bot.yourdomain.com` with your actual domain
- `<YOUR_WEBHOOK_SECRET>` with the secret from your `.env` file

Verify webhook is set:

```bash
curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getWebhookInfo"
```

---

## Verification

1. **Test the tunnel**:
   ```bash
   curl https://telegram-bot.yourdomain.com/actuator/health
   ```

   Should return:
   ```json
   {"status":"UP"}
   ```

2. **Test your bot**:
   - Open Telegram
   - Find your bot
   - Send `/start`
   - The bot should respond

3. **Check webhook status**:
   ```bash
   curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getWebhookInfo"
   ```

   Should show:
   - `url`: Your webhook URL
   - `has_custom_certificate`: false
   - `pending_update_count`: 0 (or low number)
   - `last_error_date`: should be empty

---

## Troubleshooting

### Problem: Tunnel container keeps restarting

**Check logs**:
```bash
docker logs telegram-cloudflare-tunnel
```

**Common issues**:
- Invalid tunnel token → Double-check your `CLOUDFLARE_TUNNEL_TOKEN` in `.env`
- Tunnel deleted in Cloudflare → Create a new tunnel and update token

### Problem: Bot container is unhealthy

**Check health**:
```bash
docker ps
```

**Check logs**:
```bash
docker logs telegram-translator-cpu
```

**Common issues**:
- Missing `TELEGRAM_BOT_TOKEN` → Check `.env` file
- Application failed to start → Check Java errors in logs
- Port 8080 already in use → Stop other services using that port

### Problem: Webhook not working

**Verify webhook URL is accessible**:
```bash
curl https://telegram-bot.yourdomain.com/actuator/health
```

**Check Telegram webhook info**:
```bash
curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getWebhookInfo"
```

**Common issues**:
- Wrong URL in webhook → Re-set webhook with correct URL
- SSL certificate issues → Cloudflare handles SSL, check tunnel logs
- Cloudflare tunnel not connected → Check cloudflared container logs

### Problem: Bot doesn't respond to messages

1. **Check if webhook receives updates**:
   ```bash
   docker logs telegram-translator-cpu | grep -i "webhook\|update"
   ```

2. **Check application errors**:
   ```bash
   docker logs telegram-translator-cpu | grep -i error
   ```

3. **Verify webhook secret matches**:
   - Secret in `.env` file
   - Secret in webhook URL

### Problem: "Connection refused" or "Cannot connect to Docker"

**Linux/Mac**:
```bash
sudo systemctl start docker
```

**Docker Desktop** (Mac/Windows):
- Make sure Docker Desktop is running
- Restart Docker Desktop

---

## Updating Configuration

### Change Tunnel Token

1. Edit `.env` file:
   ```bash
   nano .env
   ```

2. Update `CLOUDFLARE_TUNNEL_TOKEN`

3. Restart services:
   ```bash
   docker-compose down
   docker-compose -f docker-compose.cpu.yml up -d
   ```

### Change Domain/Subdomain

1. **In Cloudflare Zero Trust Dashboard**:
   - Go to your tunnel
   - Edit the public hostname
   - Update subdomain/domain
   - Save

2. **Update webhook** (if URL changed):
   ```bash
   curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
     -H "Content-Type: application/json" \
     -d '{"url": "https://NEW-SUBDOMAIN.yourdomain.com/webhook/telegram/<YOUR_WEBHOOK_SECRET>"}'
   ```

---

## Running on Different Machines

### Same setup works everywhere:

1. **Mac with Docker Desktop** - ✅ Works
2. **Linux server on your home network** - ✅ Works
3. **Windows with WSL2 and Docker Desktop** - ✅ Works
4. **Raspberry Pi with Docker** - ✅ Works (use CPU version)

No matter where you run it, as long as:
- Docker is installed
- `.env` file has correct `CLOUDFLARE_TUNNEL_TOKEN`
- Machine has internet connection

The tunnel will connect and work!

---

## Stopping and Starting

### Stop everything:
```bash
docker-compose -f docker-compose.cpu.yml down
```

### Start everything:
```bash
docker-compose -f docker-compose.cpu.yml up -d
```

### Restart just one service:
```bash
docker-compose -f docker-compose.cpu.yml restart telegram-bot
docker-compose -f docker-compose.cpu.yml restart cloudflared
```

### View logs:
```bash
# All logs
docker-compose -f docker-compose.cpu.yml logs -f

# Just bot
docker logs telegram-translator-cpu -f

# Just tunnel
docker logs telegram-cloudflare-tunnel -f
```

---

## Security Best Practices

1. **Keep tunnel token secret** - Don't commit `.env` to git
2. **Use strong webhook secret** - Random, at least 16 characters
3. **Rotate secrets periodically** - Update webhook secret every few months
4. **Monitor logs** - Check for suspicious activity
5. **Keep Docker images updated** - Pull latest images regularly

---

## Advanced: Multiple Bots

You can run multiple bots with one Cloudflare account:

1. **Create separate tunnels** for each bot
2. **Use different subdomains**:
   - `bot1.yourdomain.com`
   - `bot2.yourdomain.com`
3. **Each bot gets its own token** in separate `.env` files

---

## Cost

- **Cloudflare Tunnel**: FREE (unlimited bandwidth)
- **Cloudflare Zero Trust**: FREE tier (up to 50 users)
- **Domain**: $10-15/year (or free from some providers)
- **Docker**: FREE
- **Your electricity**: Varies by usage

Total: **~$0-15/year** (just domain cost)

---

## Getting Help

If you encounter issues:

1. **Check logs first**:
   ```bash
   docker-compose -f docker-compose.cpu.yml logs
   ```

2. **Verify configuration**:
   ```bash
   cat .env
   ```

3. **Test connectivity**:
   ```bash
   curl https://telegram-bot.yourdomain.com/actuator/health
   ```

4. **Check Cloudflare dashboard**:
   - Tunnel status
   - Traffic logs
   - Public hostname configuration

---

## Summary

With this setup, you only need to:

1. Get Cloudflare Tunnel token (one-time)
2. Add token to `.env` file
3. Run `docker-compose up -d`
4. Everything else is automatic!

No manual cloudflared installation, no complex networking, just Docker Compose.
