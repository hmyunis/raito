<p align="center">
  <img src="raito-android/app/src/main/res/drawable/img_app_icon_padded.png" alt="Raito logo" width="140" />
</p>

<h1 align="center">Raito</h1>

<p align="center">
  A full-stack productivity command center for focused work, local-first task management, and Telegram-powered capture.
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/platform-Android-1E8A4C?style=for-the-badge&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-0B57D0?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img alt="PHP" src="https://img.shields.io/badge/PHP-8.3-777BB4?style=for-the-badge&logo=php&logoColor=white" />
  <img alt="Docker" src="https://img.shields.io/badge/Docker-ready-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img alt="License" src="https://img.shields.io/badge/license-MIT-111827?style=for-the-badge" />
</p>

Raito is a full-stack productivity system built around focused work, lightweight task capture, and progress you can actually see. The Android app gives users a stylized task-and-focus experience with buckets, chapters, companion characters, progress tracking, reminders, and Telegram-powered remote capture. The backend provides device registration, Telegram pairing, webhook ingestion, manual sync, usage events, and admin statistics.

## Table Of Contents

- [What Raito Does](#what-raito-does)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Repository Layout](#repository-layout)
- [Getting Started](#getting-started)
- [Android Setup](#android-setup)
- [Backend Setup](#backend-setup)
- [Telegram Capture Flow](#telegram-capture-flow)
- [API Overview](#api-overview)
- [Local Development Notes](#local-development-notes)
- [Common Commands](#common-commands)
- [Testing Status](#testing-status)
- [Security And Privacy](#security-and-privacy)
- [License](#license)

The repository contains both sides of the product:

```txt
raito/
├── raito-android/   Android app built with Kotlin, Jetpack Compose, Room, Retrofit, and Material 3
├── raito-backend/   PHP 8.3 backend with MySQL, Telegram webhook support, and Docker tooling
└── LICENSE
```

## What Raito Does

Raito is designed for people who want a personal productivity app that feels more like a command center than a plain checklist.

Core ideas:

- Buckets organize work into focused areas.
- Tasks live inside buckets and can be completed, edited, reset, or imported.
- Focus mode helps users work through a selected task with a timer-based flow.
- Progress views surface completed buckets, points, and activity.
- Companion characters react to bucket state and task progress.
- Telegram capture lets users send quick text to a bot and later import those messages into the Android app.
- The backend keeps remote captures, pairing state, sync events, and admin metrics separate from the local-first Android database.

## Features

### Android App

- Kotlin and Jetpack Compose UI.
- Bucket and task management.
- Focus timer with local notifications.
- Local persistence with Room.
- Companion character system with multiple moods and visual states.
- Progress dashboard for completed buckets and activity.
- Shop/settings areas for personalization.
- Export and restore support for app data.
- Manual Telegram sync flow:
  - Register device with backend.
  - Generate pairing code.
  - Link Telegram account through the bot.
  - Fetch pending Telegram messages.
  - Import selected messages as local tasks.
  - Mark imported or discarded messages on the server.

### Backend

- PHP 8.3 API.
- MySQL persistence.
- Docker-based local development.
- Device registration and bearer-token authentication.
- Telegram pairing code generation and validation.
- Telegram webhook endpoint with secret-token verification.
- Pending remote panel storage.
- Manual import/discard sync endpoints.
- Usage event ingestion with deduplication.
- Admin stats endpoint.
- Local development scripts for migrations, test users, Telegram tooling, and diagnostics.

## Tech Stack

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Retrofit
- OkHttp
- Moshi
- Coil
- Coroutines
- KSP
- Robolectric and Roborazzi for tests/screenshots

### Backend

- PHP 8.3
- Apache
- MySQL
- PDO
- Docker Compose
- Telegram Bot API
- Cloudflare Tunnel for local HTTPS webhook testing

## Repository Layout

```txt
raito-android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/example/
│       │   ├── data/       Room entities, DAOs, repository, network API
│       │   ├── ui/         Compose screens, theme, view model
│       │   └── util/       Companion and date utilities
│       └── test/           Unit, Robolectric, and screenshot tests
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── .env.example

raito-backend/
├── app/
│   ├── Database/
│   ├── Services/
│   └── Support/
├── docker/
├── migrations/
├── public/
├── scripts/
├── docker-compose.yml
├── DEPLOYMENT.md
└── README.md
```

## Getting Started

### Prerequisites

Install:

- Android Studio
- JDK 17
- Docker Desktop
- PowerShell
- Cloudflare Tunnel CLI, `cloudflared`, if testing Telegram webhooks locally
- Telegram account, if testing bot pairing and capture

## Android Setup

From the repository root:

```powershell
cd raito-android
```

Create local Android secrets if needed:

```powershell
Copy-Item .env.example .env
```

The app uses the Android Secrets Gradle Plugin. Real values belong in `.env`, which is ignored by Git.

Build the debug app:

```powershell
.\gradlew.bat assembleDebug
```

Run tests:

```powershell
.\gradlew.bat test
```

The debug APK is generated at:

```txt
raito-android/app/build/outputs/apk/debug/app-debug.apk
```

## Backend Setup

From the repository root:

```powershell
cd raito-backend
```

Create the local environment file:

```powershell
Copy-Item .env.example .env
```

Start the backend stack:

```powershell
docker compose up -d --build
```

Run migrations:

```powershell
docker compose exec php php scripts/migrate.php
```

Check the API:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/db-check
```

Local services:

```txt
API:        http://localhost:8080
phpMyAdmin: http://localhost:8081
MySQL:      localhost:3307
```

For the full backend workflow, including Telegram bot setup, Cloudflare Tunnel, webhook registration, test users, admin stats, and troubleshooting, see:

[raito-backend/README.md](raito-backend/README.md)

## Telegram Capture Flow

The Telegram integration is intentionally manual-sync oriented. Messages sent to the bot are not automatically inserted into local Android data. Instead, they are stored on the backend as pending remote panels until the app imports them.

High-level flow:

1. The Android app registers a device with the backend.
2. The app generates a short pairing code.
3. The user sends `/link CODE` to the Telegram bot.
4. Telegram messages are captured by the backend webhook.
5. The Android app fetches pending panels.
6. The user selects which messages to import as tasks.
7. The app marks imported or discarded panels on the backend.

This keeps the Android app local-first while still allowing quick capture from Telegram.

## API Overview

Key backend endpoints:

```txt
GET  /api/health
GET  /api/db-check
POST /api/auth/register-device
GET  /api/me
POST /api/telegram/create-pairing-code
POST /api/telegram/webhook
GET  /api/telegram/pending-panels
POST /api/telegram/mark-imported
POST /api/telegram/discard-panels
POST /api/events
POST /api/events/batch
GET  /api/admin/stats
```

Most app-facing endpoints use bearer-token authentication after device registration.

## Local Development Notes

- `local.properties`, `.env`, signing keys, Gradle caches, IDE state, APKs, and build outputs are intentionally ignored.
- Android release signing expects environment variables such as `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`.
- Telegram webhook testing requires HTTPS. For local testing, run a Cloudflare quick tunnel in front of `http://localhost:8080`.
- If the Cloudflare tunnel URL changes, register the Telegram webhook again.
- Backend migrations are tracked in `raito-backend/migrations`.
- Do not edit already-applied migrations for deployed databases. Add a new migration instead.

## Common Commands

### Android

```powershell
cd raito-android
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat clean
```

### Backend

```powershell
cd raito-backend
docker compose up -d --build
docker compose exec php php scripts/migrate.php
docker compose logs -f php
docker compose down
```

### Telegram Tools

```powershell
cd raito-backend
docker compose exec php php scripts/telegram_set_webhook.php "https://your-domain.example/api/telegram/webhook"
docker compose exec php php scripts/telegram_webhook_info.php
docker compose exec php php scripts/telegram_delete_webhook.php
```

## Testing Status

The Android app has unit, Robolectric, Compose test, and screenshot-test dependencies configured. The backend includes script-based local verification for migrations, device registration, pending panel creation, manual sync, event ingestion, and admin stats.

Useful checks:

```powershell
cd raito-android
.\gradlew.bat assembleDebug
.\gradlew.bat test

cd ..\raito-backend
docker compose up -d --build
docker compose exec php php scripts/migrate.php
Invoke-RestMethod http://localhost:8080/api/health
```

## Security And Privacy

- Real `.env` files are not committed.
- Keystores and local certificates are ignored.
- Device tokens are stored hashed on the backend.
- Pairing codes are hashed and expire.
- Telegram webhook requests are protected with a secret token.
- Admin endpoints require an admin bearer token.
- Usage events support client-side deduplication.

## License

This project is licensed under the terms in [LICENSE](LICENSE).
