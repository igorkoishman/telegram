# AliExpress API Setup Guide

This bot integrates with the official AliExpress API to search for products.

## Getting API Access

### 1. Apply for AliExpress API Access

Visit the **[AliExpress Open Platform](https://open.aliexpress.com/)** and follow these steps:

1. Sign in with your AliExpress seller account (or create one)
2. Navigate to the **Developer Center**
3. Click **"Create App"** or **"My Apps"**
4. Fill out the application form with:
   - **App Name**: Your bot/application name
   - **App Description**: Describe your Telegram bot for product search
   - **Business Type**: Select appropriate category
   - **Expected API Usage**: Product search and display

### 2. Wait for Approval

- AliExpress will review your application within **1-2 business days**
- You'll receive an email with approval status
- Once approved, you'll get:
  - **App Key** (also called API Key)
  - **App Secret** (also called API Secret)

### 3. Configure Your Bot

Once you have your credentials, update `src/main/resources/application.yml`:

```yaml
aliexpress:
  enabled: true  # Change from false to true
  app-key: YOUR_ACTUAL_APP_KEY
  app-secret: YOUR_ACTUAL_APP_SECRET
```

## Testing Without API Access

The bot currently works with **mock data** for testing purposes. You can try it immediately:

1. Start your bot
2. Send: `/search wireless headphones`
3. You'll see mock products returned

This allows you to test the bot functionality while waiting for API approval.

## Available Bot Commands

- `/start` - Welcome message
- `/help` - Show all commands
- `/search <query>` - Search for products
  - Example: `/search bluetooth speaker`
  - Example: `/search phone case`

## API Documentation

For developers who want to implement real API calls:

- [AliExpress API Documentation](https://openservice.aliexpress.com/doc/api.htm)
- [API Guide](https://zuplo.com/learning-center/aliexpress-api-guide)
- [AliExpress Developers Portal](https://open.aliexpress.com/)

## Troubleshooting

### "AliExpress API is disabled" in logs
- This is normal when `enabled: false` in config
- The bot will use mock data instead

### "Failed to search AliExpress products"
- Check your API credentials in `application.yml`
- Verify your app is approved and active
- Check the logs for specific error messages

## Next Steps

1. **Apply for API access** at https://open.aliexpress.com/
2. **Wait for approval** (1-2 business days)
3. **Update configuration** with your real credentials
4. **Restart the bot** and test with real data!

---

**Note**: Mock data is for testing only and shows example products with placeholder links.
