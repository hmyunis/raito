# Raito Backend Plesk Deployment

This guide deploys the Raito PHP backend to Plesk without touching the existing Astro portfolio at:

```txt
https://example.com
```

Do not upload this backend into the existing portfolio `httpdocs` folder. Deploy it on a separate subdomain, for example:

```txt
https://api.example.com
```

The backend webhook URL will then be:

```txt
https://api.example.com/api/telegram/webhook
```

## 1. Target Layout

Your current portfolio stays where it is:

```txt
httpdocs/
  _assets/
  projects/
  index.html
  favicon.ico
  ...
```

Create a separate Plesk subdomain for the backend. Plesk usually creates a separate folder beside `httpdocs`, such as:

```txt
api.example.com/
```

Inside that backend folder, the deployed files should look like:

```txt
api.example.com/
  app/
  migrations/
  public/
  scripts/
  storage/
  .env
  .htaccess
  .env.example
```

Recommended backend document root:

```txt
api.example.com/public
```

If Plesk names the folder differently, use that actual folder name. The important rule is: keep backend files out of the portfolio `httpdocs` folder.

## 2. Create A Backend Subdomain In Plesk

In Plesk:

1. Go to `Websites & Domains`
2. Click `Add Subdomain`
3. Use a backend subdomain, for example:

```txt
api
```

4. For document root, use a backend-specific folder, preferably:

```txt
api.example.com/public
```

If Plesk does not let you choose `/public` yet, create the subdomain first with:

```txt
api.example.com
```

Then later change its document root to:

```txt
api.example.com/public
```

Do not change the document root for `example.com`. That would affect the Astro portfolio.

## 3. Add The DNS Record In Your DNS Provider

Plesk creating the subdomain does not automatically make it resolvable when DNS is managed outside Plesk. If your registrar, hosting provider, or DNS dashboard manages the zone, you must add the subdomain record there. The errors below mean DNS is missing:

```txt
Domain api.example.com resolve problems detected
NXDOMAIN looking up A for api.example.com
No such host is known
```

In your DNS dashboard for `example.com`, add this record:

```txt
Host: api
Type: A
Value: PLESK_SERVER_IPV4
TTL: 86400
```

Use the IPv4 address assigned to your Plesk web hosting account. If you are not sure what it is, check Plesk hosting details or the existing `A` record for the root domain.

Do not change the existing records for:

```txt
@
www
```

Those are used by the portfolio at `https://example.com`.

Do not add an `AAAA` record unless Plesk gives you an IPv6 address. Let’s Encrypt can issue the certificate with only an `A` record.

Save the DNS changes, then wait for DNS propagation. With the current `86400` TTL, propagation can take time. Check from PowerShell:

```powershell
Resolve-DnsName api.example.com -Type A
```

Expected:

```txt
Name      : api.example.com
Type      : A
IPAddress : PLESK_SERVER_IPV4
```

Also check:

```powershell
Test-NetConnection api.example.com -Port 443
```

Do not request the Let’s Encrypt certificate until `Resolve-DnsName` returns the A record. If DNS still returns NXDOMAIN, Let’s Encrypt will fail again.

The Plesk permission-denied or DNS warning toast is expected when Plesk is not authoritative for your external DNS zone. In that setup, your external DNS provider is the source of truth.

## 4. Required PHP Settings

For the backend subdomain only, set:

```txt
PHP version: 8.3.x
```

Required PHP extensions:

```txt
pdo_mysql
mbstring
curl
json
```

Recommended settings:

```txt
display_errors = Off
log_errors = On
memory_limit = 128M or higher
max_execution_time = 30
post_max_size = 2M or higher
upload_max_filesize = 2M
```

## 5. Generate Production Secrets

Open PowerShell locally and run:

```powershell
function New-Base64UrlSecret {
    param([int]$Bytes = 32)

    $buffer = New-Object byte[] $Bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($buffer)

    [Convert]::ToBase64String($buffer).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}
```

Generate four secrets:

```powershell
New-Base64UrlSecret 32
New-Base64UrlSecret 32
New-Base64UrlSecret 32
New-Base64UrlSecret 32
```

Use them for:

```env
APP_KEY=
TELEGRAM_WEBHOOK_SECRET=
ADMIN_TOKEN=
DEPLOY_TOKEN=
```

Keep these private.

## 6. Create The Telegram Bot

In Telegram:

1. Open `@BotFather`
2. Send `/newbot`
3. Choose a display name, for example `Raito Capture`
4. Choose a username ending in `bot`, for example `raito_capture_bot`
5. Copy the bot token

Optional BotFather setup:

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

## 7. Create Production `.env`

Create a local file named `.env.production`:

```env
APP_NAME=Raito
APP_ENV=production
APP_DEBUG=false
APP_VERSION=0.1.0
APP_TIMEZONE=Africa/Addis_Ababa
APP_BASE_URL=https://api.example.com

APP_KEY=PASTE_GENERATED_APP_KEY
ADMIN_TOKEN=PASTE_GENERATED_ADMIN_TOKEN
DEPLOY_TOKEN=PASTE_GENERATED_DEPLOY_TOKEN

DB_HOST=YOUR_PLESK_DB_HOST
DB_PORT=YOUR_PLESK_DB_PORT
DB_NAME=YOUR_PLESK_DB_NAME
DB_USER=YOUR_PLESK_DB_USER
DB_PASS=YOUR_PLESK_DB_PASSWORD

TELEGRAM_BOT_TOKEN=PASTE_BOTFATHER_TOKEN
TELEGRAM_WEBHOOK_SECRET=PASTE_GENERATED_TELEGRAM_WEBHOOK_SECRET
TELEGRAM_CONNECT_TIMEOUT_SECONDS=20
TELEGRAM_TIMEOUT_SECONDS=45
TELEGRAM_RETRY_ATTEMPTS=3

PAIRING_CODE_TTL_MINUTES=10
PAIRING_CODE_MAX_ATTEMPTS=5
REMOTE_PANEL_MAX_CHARS=4000

MAX_JSON_BODY_BYTES=262144
MAX_EVENT_PAYLOAD_BYTES=12000
MAX_BATCH_EVENTS=50
MAX_SYNC_PANEL_IDS=100
```

Use exact database values from:

```txt
Plesk -> Databases -> Connection info
```

If Plesk shows host and port together:

```txt
mysql-db01.remote:31636
```

split them:

```env
DB_HOST=mysql-db01.remote
DB_PORT=31636
```

## 8. Run Local Check Before Upload

From the project root:

```powershell
docker compose up -d --build
docker compose exec php php scripts/migrate.php
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/db-check
```

If these fail locally, fix them before uploading.

## 9. Create The Upload ZIP

From the project root:

```powershell
Compress-Archive `
  -Path app,migrations,public,scripts,storage,.htaccess,.env.example,.gitignore `
  -DestinationPath raito-backend-upload.zip `
  -Force
```

Do not include:

```txt
.env
.git/
docker-compose.yml
docker/
TODO files
local database data
```

## 10. Upload To The Backend Subdomain Folder

In Plesk File Manager:

1. Open `Home directory`
2. Open the backend subdomain folder, for example:

```txt
api.example.com
```

3. Upload `raito-backend-upload.zip`
4. Extract it inside `api.example.com`

Expected backend folder:

```txt
api.example.com/
  app/
  migrations/
  public/
  scripts/
  storage/
  .htaccess
  .env.example
```

Do not extract this ZIP into:

```txt
httpdocs/
```

That is your portfolio folder.

## 11. Add `.env` To The Backend Folder

In Plesk File Manager:

1. Open the backend folder, for example `api.example.com`
2. Create a file named `.env`
3. Paste the contents of `.env.production`
4. Fill in all production values

Confirm:

```env
APP_ENV=production
APP_DEBUG=false
APP_BASE_URL=https://api.example.com
```

## 12. Set Backend Document Root

In Plesk:

1. Go to `Websites & Domains`
2. Select the backend subdomain, for example `api.example.com`
3. Open `Hosting & DNS`
4. Open `Hosting Settings`
5. Set document root to:

```txt
api.example.com/public
```

6. Save

Do not edit the hosting settings for `example.com` unless you intend to change the portfolio.

## 13. Create MySQL Database

In Plesk:

1. Go to `Databases`
2. Create a database for this backend, for example `raito_backend`
3. Create a database user, for example `raito_user`
4. Give that user full permissions on the backend database
5. Copy connection info into `.env`

The user needs:

```txt
CREATE TABLE
ALTER TABLE
INSERT
UPDATE
SELECT
DELETE
INDEX
```

## 14. Enable SSL For The Backend Subdomain

In Plesk:

1. Go to `SSL/TLS Certificates`
2. Issue or enable a certificate for:

```txt
api.example.com
```

3. Enable HTTP to HTTPS redirect for the backend subdomain
4. Enable HSTS only after everything works

Test:

```powershell
Invoke-RestMethod https://api.example.com/api/health
```

Expected:

```json
{
  "ok": true,
  "service": "raito-backend"
}
```

Also confirm your portfolio still works:

```txt
https://example.com
```

## 15. Run Production Migrations

### Option A: Plesk Terminal Or SSH

Use the backend folder, not `httpdocs`:

```bash
cd ~/api.example.com
php scripts/migrate.php
```

Expected first run:

```txt
Applying: 001_initial_schema.sql
Applied: 001_initial_schema.sql
Applying: 002_usage_event_deduplication.sql
Applied: 002_usage_event_deduplication.sql
Migrations complete.
```

Run again:

```bash
php scripts/migrate.php
```

Expected:

```txt
Already applied: 001_initial_schema.sql
Already applied: 002_usage_event_deduplication.sql
Migrations complete.
```

### Option B: No SSH Browser Migration

Use only if Plesk has no terminal.

Open:

```txt
https://api.example.com/__deploy_migrate.php?token=YOUR_DEPLOY_TOKEN
```

Expected:

```json
{
  "ok": true,
  "results": [
    {
      "migration": "001_initial_schema.sql",
      "status": "applied"
    },
    {
      "migration": "002_usage_event_deduplication.sql",
      "status": "applied"
    }
  ]
}
```

Then delete:

```txt
api.example.com/public/__deploy_migrate.php
```

Do not leave it online.

## 16. Test Production API

Health:

```powershell
Invoke-RestMethod https://api.example.com/api/health
```

Register a test user:

```powershell
$body = @{
  display_name = "Production Test"
  device_label = "Windows Test"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "https://api.example.com/api/auth/register-device" `
  -ContentType "application/json" `
  -Body $body
```

Copy `device_token`, then test:

```powershell
$token = "PASTE_PRODUCTION_DEVICE_TOKEN"

Invoke-RestMethod `
  -Method Get `
  -Uri "https://api.example.com/api/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

Create a pairing code:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "https://api.example.com/api/telegram/create-pairing-code" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body "{}"
```

Admin stats:

```powershell
$adminToken = "PASTE_PRODUCTION_ADMIN_TOKEN"

Invoke-RestMethod `
  -Method Get `
  -Uri "https://api.example.com/api/admin/stats" `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

## 17. Set Telegram Webhook

Webhook URL:

```txt
https://api.example.com/api/telegram/webhook
```

### Option A: Plesk Terminal Or SSH

```bash
cd ~/api.example.com
php scripts/telegram_set_webhook.php https://api.example.com/api/telegram/webhook
php scripts/telegram_webhook_info.php
```

Expected set response:

```json
{
  "ok": true,
  "result": true,
  "description": "Webhook was set"
}
```

### Option B: No SSH Browser Tool

Set:

```txt
https://api.example.com/__telegram_tools.php?action=set&token=YOUR_DEPLOY_TOKEN&url=https://api.example.com/api/telegram/webhook
```

Check:

```txt
https://api.example.com/__telegram_tools.php?action=info&token=YOUR_DEPLOY_TOKEN
```

Then delete:

```txt
api.example.com/public/__telegram_tools.php
```

Do not leave it online.

## 18. Test Telegram End To End

In Telegram, open your bot.

Send:

```txt
/start
```

Generate a pairing code:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "https://api.example.com/api/telegram/create-pairing-code" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body "{}"
```

Send the returned code to the bot:

```txt
/link ABCD-2345
```

Expected:

```txt
Telegram linked successfully. You can now send /status or normal text messages here.
```

Send a normal text message:

```txt
Finish chapter 4 assignment
```

Expected:

```txt
Captured. It will remain pending until you sync in the app.
```

Fetch pending panels:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "https://api.example.com/api/telegram/pending-panels?limit=50" `
  -Headers @{ Authorization = "Bearer $token" }
```

## 19. Cleanup

Delete these if you used browser-based deployment tools:

```txt
api.example.com/public/__deploy_migrate.php
api.example.com/public/__telegram_tools.php
```

Confirm these do not expose source or secrets:

```txt
https://api.example.com/__deploy_migrate.php
https://api.example.com/__telegram_tools.php
https://api.example.com/.env
https://api.example.com/app/bootstrap.php
https://api.example.com/migrations/001_initial_schema.sql
https://api.example.com/scripts/migrate.php
```

Confirm the portfolio is still unaffected:

```txt
https://example.com
```

## 20. Common Failures

### Portfolio changed or disappeared

You uploaded backend files into `httpdocs` or changed the `example.com` document root.

Fix:

```txt
Restore portfolio files in httpdocs
Move backend files to api.example.com
Set only the api.example.com document root to api.example.com/public
```

### `https://api.example.com/api/health` returns 404

Check:

```txt
Backend subdomain exists
Document root is api.example.com/public
public/.htaccess exists
Apache rewrite is enabled
```

### `/api/me` says `missing_auth_token`

Confirm `public/.htaccess` contains:

```apache
SetEnvIf Authorization "(.+)" HTTP_AUTHORIZATION=$1
```

### Database connection failed

Check `.env`:

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASS=
```

Use exact values from `Plesk -> Databases -> Connection info`.

### Telegram webhook says forbidden

`TELEGRAM_WEBHOOK_SECRET` does not match the webhook `secret_token`.

Fix:

```bash
cd ~/api.example.com
php scripts/telegram_set_webhook.php https://api.example.com/api/telegram/webhook
```

### Telegram scripts timeout

Check outbound HTTPS from the Plesk server to Telegram:

```bash
cd ~/api.example.com
php scripts/telegram_network_check.php
```

If it fails, the server cannot reach `api.telegram.org` over port `443`. Ask the host whether Telegram API traffic is blocked.

### Pairing code is always invalid

Likely causes:

```txt
APP_KEY changed after code generation
Code expired
Code typed incorrectly
Code generated locally but entered into the production bot
```

Pairing codes are tied to the backend database and `APP_KEY`.

## 21. Production Endpoint List

All backend endpoints live on the backend subdomain:

```txt
GET  https://api.example.com/api/health

POST https://api.example.com/api/auth/register-device
GET  https://api.example.com/api/me

POST https://api.example.com/api/telegram/create-pairing-code
POST https://api.example.com/api/telegram/webhook
GET  https://api.example.com/api/telegram/pending-panels
POST https://api.example.com/api/telegram/mark-imported
POST https://api.example.com/api/telegram/discard-panels

POST https://api.example.com/api/events
POST https://api.example.com/api/events/batch

GET  https://api.example.com/api/admin/stats
```

The portfolio remains:

```txt
GET https://example.com
```

## 22. Final Checklist

```txt
[ ] Existing portfolio remains in httpdocs
[ ] Backend subdomain exists, for example api.example.com
[ ] Backend files are uploaded to api.example.com, not httpdocs
[ ] Backend document root is api.example.com/public
[ ] Production .env exists in api.example.com
[ ] APP_ENV=production
[ ] APP_DEBUG=false
[ ] PHP 8.3 is selected for backend subdomain
[ ] pdo_mysql, mbstring, curl, json are enabled
[ ] MySQL database and user exist
[ ] SSL works for api.example.com
[ ] https://example.com still loads the Astro portfolio
[ ] https://api.example.com/api/health works
[ ] Migrations are applied
[ ] /api/auth/register-device works
[ ] /api/me works with Bearer token
[ ] Telegram bot exists
[ ] TELEGRAM_BOT_TOKEN is in production .env
[ ] Telegram webhook points to https://api.example.com/api/telegram/webhook
[ ] /start works in Telegram
[ ] /link CODE works
[ ] Telegram text capture works
[ ] /api/telegram/pending-panels returns captured text
[ ] /api/admin/stats works
[ ] public/__deploy_migrate.php is deleted
[ ] public/__telegram_tools.php is deleted
[ ] .env is not publicly accessible
```


