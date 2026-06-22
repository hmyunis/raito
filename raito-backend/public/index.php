<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Database\Database;
use App\Services\AdminStatsService;
use App\Services\AppUpdateService;
use App\Services\AuthService;
use App\Services\PanelSyncService;
use App\Services\SyncedBucketService;
use App\Services\TelegramTaskOperationService;
use App\Services\TelegramPairingService;
use App\Services\TelegramWebhookService;
use App\Services\UsageEventService;
use App\Support\Env;
use App\Support\Logger;
use App\Support\Request;
use App\Support\Response;

$method = Request::method();
$path = Request::path();

if ($method === 'OPTIONS') {
    Response::empty(204);
}

if ($method === 'GET' && $path === '/api/health') {
    Response::json([
        'ok' => true,
        'service' => 'raito-backend',
        'app_name' => Env::get('APP_NAME', 'Raito'),
        'version' => Env::get('APP_VERSION', '0.1.0'),
        'environment' => Env::get('APP_ENV', 'local'),
        'time_utc' => gmdate('c'),
    ]);
}

if ($method === 'GET' && $path === '/api/app-update/android') {
    AppUpdateService::android();
}

if ($method === 'GET' && $path === '/api/db-check') {
    if (Env::get('APP_ENV') === 'production') {
        Response::json([
            'ok' => false,
            'error' => [
                'code' => 'not_found',
                'message' => 'Endpoint not found.',
            ],
        ], 404);
    }

    $pdo = Database::pdo();

    $statement = $pdo->query('SELECT DATABASE() AS database_name, UTC_TIMESTAMP() AS db_time_utc');
    $result = $statement->fetch();

    Logger::info('Database check completed', [
        'database' => $result['database_name'] ?? null,
    ]);

    Response::json([
        'ok' => true,
        'database' => $result['database_name'] ?? null,
        'db_time_utc' => $result['db_time_utc'] ?? null,
    ]);
}

if ($method === 'POST' && $path === '/api/auth/register-device') {
    AuthService::registerDevice(Request::jsonBody());
}

if ($method === 'GET' && $path === '/api/me') {
    AuthService::me();
}

if ($method === 'POST' && $path === '/api/telegram/create-pairing-code') {
    $user = AuthService::requireUser();
    TelegramPairingService::createPairingCode($user);
}

if ($method === 'POST' && $path === '/api/telegram/webhook') {
    TelegramWebhookService::handle();
}

if ($method === 'GET' && $path === '/api/telegram/pending-panels') {
    $user = AuthService::requireUser();
    PanelSyncService::pendingPanels($user);
}

if ($method === 'POST' && $path === '/api/telegram/mark-imported') {
    $user = AuthService::requireUser();
    PanelSyncService::markImported($user, Request::jsonBody());
}

if ($method === 'POST' && $path === '/api/telegram/discard-panels') {
    $user = AuthService::requireUser();
    PanelSyncService::discardPanels($user, Request::jsonBody());
}

if ($method === 'GET' && $path === '/api/telegram/task-operations/pending') {
    $user = AuthService::requireUser();
    TelegramTaskOperationService::listPending($user);
}

if ($method === 'POST' && $path === '/api/telegram/task-operations/acknowledge') {
    $user = AuthService::requireUser();
    TelegramTaskOperationService::acknowledge($user, Request::jsonBody());
}

if ($method === 'POST' && $path === '/api/telegram/synced-buckets/snapshot') {
    $user = AuthService::requireUser();
    SyncedBucketService::syncSnapshot($user, Request::jsonBody());
}

if ($method === 'POST' && $path === '/api/events') {
    $user = AuthService::requireUser();
    UsageEventService::trackSingle($user, Request::jsonBody());
}

if ($method === 'POST' && $path === '/api/events/batch') {
    $user = AuthService::requireUser();
    UsageEventService::trackBatch($user, Request::jsonBody());
}

if ($method === 'GET' && $path === '/api/admin/stats') {
    AdminStatsService::stats();
}

Response::json([
    'ok' => false,
    'error' => [
        'code' => 'not_found',
        'message' => 'Endpoint not found.',
    ],
], 404);
