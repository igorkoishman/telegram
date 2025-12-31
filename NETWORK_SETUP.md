# Network Setup Guide

## Testing from Home vs Work

### Problem
The Taobao/AliExpress API gateway (`gw.api.taobao.com`) is blocked by your corporate network (Netskope proxy/VPN). This causes timeouts when trying to use the API.

### Solution

#### When Testing from HOME (Recommended for API testing):

1. **Connect to your home WiFi** (not company VPN)
2. **Set `aliexpress.enabled: true` in `application.yml`**
3. **Start the application**
4. **Test with `/search toys`**

Expected behavior:
- API should work and return real product data
- Response time: 2-5 seconds
- You'll see logs like: `"API response status: 200"` and `"API returned X products"`

#### When Testing from WORK (Corporate network):

1. **Set `aliexpress.enabled: false` in `application.yml`**
2. **Start the application**
3. **Test with `/search toys`**

Expected behavior:
- Skips API completely
- Goes directly to fallback (search redirect)
- Response time: instant
- You'll see: `"Creating search redirect for query: toys"`

## Quick Configuration Switch

### Edit `src/main/resources/application.yml`:

**For HOME:**
```yaml
aliexpress:
  enabled: true    # <-- Set to TRUE
```

**For WORK:**
```yaml
aliexpress:
  enabled: false   # <-- Set to FALSE
```

## How to Test the API at Home

1. **Before leaving work:**
   - Commit your code: `git add . && git commit -m "Ready for home testing"`
   - Make sure you have the code on your laptop

2. **At home:**
   ```bash
   # Make sure you're on your home WiFi (not VPN)
   cd /path/to/telegram

   # Edit application.yml - set enabled: true

   # Start the app
   mvn spring-boot:run

   # In another terminal, start cloudflare tunnel
   cloudflared tunnel --url http://localhost:8080

   # Test from your phone with: /search toys
   ```

3. **What to look for in logs:**
   - ✅ Good: `"API response status: 200"` - API is working!
   - ✅ Good: `"API returned 10 products"` - Got real data!
   - ❌ Bad: `"Read timed out"` - Still blocked (might still be on VPN)

## Testing the API Gateway Connection

### Quick test if Taobao API is reachable:

**From work (will fail):**
```bash
curl -I --max-time 5 https://gw.api.taobao.com/router/rest
# Expected: Operation timed out after 5 seconds
```

**From home (should work):**
```bash
curl -I --max-time 5 https://gw.api.taobao.com/router/rest
# Expected: HTTP/1.1 200 OK
```

## Architecture

```
Telegram Bot
    ↓
If enabled=true:
    → Try Taobao API (30 second timeout)
        ✅ Success? Return real products
        ❌ Timeout? Fall to next step
    ↓
If enabled=false OR API failed:
    → Return search redirect
        ✅ Always works
        ↓ User clicks link → Real AliExpress
```

## Troubleshooting

### "API still times out at home"
- Check if you're still connected to company VPN
- Try: `curl -I https://gw.api.taobao.com/router/rest`
- If it works, the API should work
- If it still times out, check your home router/firewall

### "I want to use it at work"
- Set `enabled: false` in `application.yml`
- The bot will work but provide search redirect links instead of real products
- Users can click the link to see products on AliExpress website

### "Can I make the API work through the proxy?"
- No, the corporate firewall blocks `gw.api.taobao.com`
- You'd need to request your IT department to whitelist it (unlikely)

## Current Status

- **API Credentials:** ✅ Valid (app-key: 520562)
- **API Code:** ✅ Implemented with proper MD5 signing
- **Network from work:** ❌ Blocked by corporate proxy
- **Network from home:** 🔄 Should work (needs testing)
- **Fallback mode:** ✅ Always works (search redirect)
