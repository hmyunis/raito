# Raito Backend

PHP 8.3 backend for Raito. It provides device registration, Telegram pairing, Telegram webhook capture, manual Android sync, usage events, and admin stats.

The backend runs locally with Docker. Telegram requires webhook URLs to be HTTPS, so local Telegram testing uses Cloudflare Tunnel in front of `http://localhost:8080`.

## What Runs Locally

Docker starts three services:

```txt
raito_php          http://localhost:8080
raito_mysql        localhost:3307
raito_phpmyadmin   http://localhost:8081
```

The API entry point is:

```txt
http://localhost:8080/api/health
```

## Prerequisites

Install these first:

1. Docker Desktop
2. Cloudflare Tunnel CLI, `cloudflared`
3. PowerShell
4. Telegram account

Check Docker:

```powershell
docker --version
docker compose version
```

Check Cloudflare Tunnel:

```powershell
cloudflared --version
```

If `cloudflared` is missing on Windows, install it:

```powershell
winget install --id Cloudflare.cloudflared
```

Close and reopen PowerShell after installing, then run:

```powershell
cloudflared --version
```

## 1. Configure Local Environment

The local `.env` file is required and is intentionally ignored by Git.

For local development it should contain values like:

```env
APP_NAME=Raito
APP_ENV=local
APP_DEBUG=true
APP_VERSION=0.1.0
APP_TIMEZONE=Africa/Addis_Ababa

DB_HOST=db
DB_PORT=3306
DB_NAME=raito_dev
DB_USER=raito_user
DB_PASS=raito_dev_password

MAX_JSON_BODY_BYTES=262144

APP_KEY=local_change_this_to_a_long_random_secret

TELEGRAM_BOT_TOKEN=
TELEGRAM_WEBHOOK_SECRET=local_telegram_webhook_secret_change_me
TELEGRAM_CONNECT_TIMEOUT_SECONDS=20
TELEGRAM_TIMEOUT_SECONDS=45
TELEGRAM_RETRY_ATTEMPTS=3

PAIRING_CODE_TTL_MINUTES=10
PAIRING_CODE_MAX_ATTEMPTS=5
REMOTE_PANEL_MAX_CHARS=4000

ADMIN_TOKEN=local_admin_token_change_this
MAX_EVENT_PAYLOAD_BYTES=12000
MAX_BATCH_EVENTS=50
MAX_SYNC_PANEL_IDS=100
```

Keep `TELEGRAM_BOT_TOKEN` empty until you create a Telegram bot.

## 2. Start Docker

From the project root:

```powershell
docker compose up -d --build
```

Confirm the containers are running:

```powershell
docker compose ps
```

Expected containers:

```txt
raito_php
raito_mysql
raito_phpmyadmin
```

Confirm PHP has the required extensions:

```powershell
docker compose exec php php -m
```

You should see:

```txt
curl
mbstring
PDO
pdo_mysql
json
```

## 3. Run Migrations

Run:

```powershell
docker compose exec php php scripts/migrate.php
```

Expected first successful run:

```txt
Applying: 001_initial_schema.sql
Applied: 001_initial_schema.sql
Applying: 002_usage_event_deduplication.sql
Applied: 002_usage_event_deduplication.sql
Migrations complete.
```

Run it again:

```powershell
docker compose exec php php scripts/migrate.php
```

Expected repeat run:

```txt
Already applied: 001_initial_schema.sql
Already applied: 002_usage_event_deduplication.sql
Migrations complete.
```

## 4. Test Local API

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected:

```txt
ok      : True
service : raito-backend
```

Database check:

```powershell
Invoke-RestMethod http://localhost:8080/api/db-check
```

Expected:

```txt
ok       : True
database : raito_dev
```

## 5. Create A Local Test User

Run:

```powershell
docker compose exec php php scripts/dev_register_user.php
```

Copy the returned values:

```json
{
  "public_id": "USER_PUBLIC_ID",
  "device_token": "DEVICE_TOKEN"
}
```

Set the token in PowerShell:

```powershell
$token = "PASTE_DEVICE_TOKEN_HERE"
```

Test authenticated user lookup:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

## 6. Test Manual Sync Without Telegram

Create a fake pending Telegram panel:

```powershell
docker compose exec php php scripts/dev_create_pending_panel.php PASTE_USER_PUBLIC_ID_HERE "Finish database assignment from Telegram"
```

Fetch pending panels:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/telegram/pending-panels?limit=50" `
  -Headers @{ Authorization = "Bearer $token" }
```

Copy the returned `remote_panel_id`.

Mark the panel imported:

```powershell
$body = @{
  client_sync_id = "local-sync-001"
  app_version = "1.0.0"
  panel_ids = @("PASTE_REMOTE_PANEL_ID_HERE")
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/telegram/mark-imported" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $body
```

## 7. Test Usage Events

Send one analytics event:

```powershell
$eventBody = @{
  client_event_id = "local-event-001"
  event_name = "manual_sync_completed"
  client_time_at = "2026-05-31T15:30:00+03:00"
  payload = @{
    imported_count = 1
  }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/events" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $eventBody
```

## 8. Test Admin Stats

Set the local admin token:

```powershell
$adminToken = "local_admin_token_change_this"
```

Call stats:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/stats" `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

Expected top-level fields:

```txt
ok
users
telegram
remote_panels
sync
events
recent_users
recent_sync_batches
```

## 9. Create A Telegram Bot

Open Telegram and talk to `@BotFather`.

Send:

```txt
/newbot
```

Choose a display name, for example:

```txt
Raito Capture
```

Choose a username ending in `bot`, for example:

```txt
raito_capture_bot
```

BotFather will return a bot token. Put it in `.env`:

```env
TELEGRAM_BOT_TOKEN=PASTE_BOTFATHER_TOKEN_HERE
```

Restart PHP so the container reloads `.env`:

```powershell
docker compose restart php
```

Optional BotFather commands:

```txt
/setdescription
Capture quick text panels into Raito for manual import.

/setabouttext
Raito Telegram Capture

/setcommands
start - Show setup instructions
link - Link Telegram to Raito
status - Check pending panels
unlink - Disconnect Telegram
help - Show help
```

## 10. Start Cloudflare Tunnel For Telegram HTTPS

Telegram webhooks require HTTPS. Localhost is HTTP, so start a Cloudflare quick tunnel.

Open a second PowerShell terminal and keep it running:

```powershell
cloudflared tunnel --url http://localhost:8080 --no-autoupdate
```

Cloudflare prints a URL like:

```txt
https://example-random-words.trycloudflare.com
```

Do not close this terminal while testing Telegram. If you close it, the tunnel stops and Telegram can no longer reach your local backend.

Check the tunnel from a third PowerShell terminal:

```powershell
$tunnelUrl = "PASTE_TRYCLOUDFLARE_URL_HERE"

Invoke-RestMethod "$tunnelUrl/api/health"
```

Expected:

```txt
ok      : True
service : raito-backend
```

## 11. Register The Telegram Webhook

Telegram must call:

```txt
https://YOUR_TRYCLOUDFLARE_URL/api/telegram/webhook
```

Set the webhook:

```powershell
$tunnelUrl = "PASTE_TRYCLOUDFLARE_URL_HERE"

docker compose exec php php scripts/telegram_set_webhook.php "$tunnelUrl/api/telegram/webhook"
```

Check webhook info:

```powershell
docker compose exec php php scripts/telegram_webhook_info.php
```

Expected response includes:

```json
{
  "ok": true,
  "result": {
    "url": "https://YOUR_TRYCLOUDFLARE_URL/api/telegram/webhook"
  }
}
```

Important: a quick tunnel URL changes every time you restart `cloudflared`. Every time the URL changes, run `telegram_set_webhook.php` again.

## 12. Test Telegram Pairing Locally

Create a pairing code for the local user:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/telegram/create-pairing-code" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body "{}"
```

Copy the returned code, for example:

```txt
ABCD-2345
```

Open your Telegram bot and send:

```txt
/start
```

Then send:

```txt
/link ABCD-2345
```

Expected bot reply:

```txt
Telegram linked successfully.
```

Now send a normal text message to the bot:

```txt
Finish database assignment
```

Expected bot reply:

```txt
Captured. It will remain pending until you sync in the app.
```

Fetch pending panels again:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/telegram/pending-panels?limit=50" `
  -Headers @{ Authorization = "Bearer $token" }
```

The Telegram message should appear in the `panels` list.

## 13. Useful Docker Commands

Start:

```powershell
docker compose up -d
```

Rebuild:

```powershell
docker compose up -d --build
```

Stop:

```powershell
docker compose down
```

View PHP logs:

```powershell
docker compose logs -f php
```

View MySQL logs:

```powershell
docker compose logs -f db
```

Restart only PHP:

```powershell
docker compose restart php
```

Delete containers and the local MySQL volume:

```powershell
docker compose down -v
```

Only use `down -v` when you are okay losing the local database.

## 14. Useful Telegram Webhook Commands

Show webhook info:

```powershell
docker compose exec php php scripts/telegram_webhook_info.php
```

Delete webhook:

```powershell
docker compose exec php php scripts/telegram_delete_webhook.php
```

Delete webhook and drop pending Telegram updates:

```powershell
docker compose exec php php scripts/telegram_delete_webhook.php --drop
```

Generate a webhook secret:

```powershell
docker compose exec php php scripts/generate_secret.php
```

## 15. phpMyAdmin

Open:

```txt
http://localhost:8081
```

Use:

```txt
Server: db
Username: raito_user
Password: raito_dev_password
Database: raito_dev
```

## 16. Troubleshooting

If `docker compose up -d --build` hangs the first time, pull the PHP base image and rebuild:

```powershell
docker pull php:8.3-apache
docker compose up -d --build
```

If migrations fail after editing an already-applied migration, create a new migration file instead of changing the old one.

If Telegram does not reply:

```powershell
docker compose logs -f php
docker compose exec php php scripts/telegram_webhook_info.php
```

Then confirm:

```txt
cloudflared is still running
TELEGRAM_BOT_TOKEN is set in .env
TELEGRAM_WEBHOOK_SECRET is set in .env
The webhook URL points to the current trycloudflare.com URL
The webhook URL ends with /api/telegram/webhook
```

If `telegram_webhook_info.php` returns a timeout like `Telegram request failed: Connection timed out`, the backend is failing to make an outbound HTTPS connection to Telegram. This is separate from the Cloudflare tunnel. Check the host route first:

```powershell
Test-NetConnection api.telegram.org -Port 443
Invoke-WebRequest -Uri https://api.telegram.org -TimeoutSec 30
```

Then check from inside Docker:

```powershell
docker compose exec php php -r "echo gethostbyname('api.telegram.org'), PHP_EOL;"
docker compose exec php php scripts/telegram_network_check.php

@'
<?php
$ch = curl_init('https://api.telegram.org');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CONNECTTIMEOUT => 20,
    CURLOPT_TIMEOUT => 45,
]);
$body = curl_exec($ch);
var_export([
    'errno' => curl_errno($ch),
    'error' => curl_error($ch),
    'http' => curl_getinfo($ch, CURLINFO_HTTP_CODE),
]);
curl_close($ch);
'@ | docker compose exec -T php php
```

If the host test fails, try a different network, VPN, or firewall/DNS configuration. If the host works but Docker fails, restart Docker Desktop and rebuild:

```powershell
docker compose down
docker compose up -d --build
```

If authenticated API calls return `missing_auth_token`, confirm `public/.htaccess` contains:

```apache
SetEnvIf Authorization "(.+)" HTTP_AUTHORIZATION=$1
```

If the database check fails, confirm the database container is healthy:

```powershell
docker compose ps
docker compose logs db
```

## Current Verification

The local Docker backend was verified with:

```txt
docker compose up -d --build
docker compose exec php php scripts/migrate.php
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/db-check
docker compose exec php php scripts/dev_register_user.php
docker compose exec php php scripts/dev_create_pending_panel.php ...
GET /api/telegram/pending-panels
POST /api/telegram/mark-imported
POST /api/events
GET /api/admin/stats
cloudflared tunnel --url http://localhost:8080 --no-autoupdate
GET https://<trycloudflare-url>/api/health
```
