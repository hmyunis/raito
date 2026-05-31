<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Services\TelegramClient;
use App\Support\Env;
use App\Support\Response;

$token = $_GET['token'] ?? '';

$expected = (string) Env::get('DEPLOY_TOKEN', '');

if ($expected === '' || !is_string($token) || !hash_equals($expected, $token)) {
    Response::json([
        'ok' => false,
        'error' => [
            'code' => 'forbidden',
            'message' => 'Forbidden.',
        ],
    ], 403);
}

$action = $_GET['action'] ?? '';

if (!is_string($action)) {
    $action = '';
}

$client = new TelegramClient();

if ($action === 'info') {
    Response::json([
        'ok' => true,
        'telegram' => $client->getWebhookInfo(),
    ]);
}

if ($action === 'set') {
    $url = $_GET['url'] ?? '';

    if (!is_string($url) || !str_starts_with($url, 'https://')) {
        Response::json([
            'ok' => false,
            'error' => [
                'code' => 'invalid_webhook_url',
                'message' => 'A valid HTTPS webhook URL is required.',
            ],
        ], 400);
    }

    $secret = (string) Env::get('TELEGRAM_WEBHOOK_SECRET', '');

    if ($secret === '') {
        Response::json([
            'ok' => false,
            'error' => [
                'code' => 'missing_webhook_secret',
                'message' => 'TELEGRAM_WEBHOOK_SECRET is missing.',
            ],
        ], 500);
    }

    Response::json([
        'ok' => true,
        'telegram' => $client->setWebhook($url, $secret),
    ]);
}

if ($action === 'delete') {
    $drop = ($_GET['drop'] ?? '') === '1';

    Response::json([
        'ok' => true,
        'telegram' => $client->deleteWebhook($drop),
    ]);
}

Response::json([
    'ok' => false,
    'error' => [
        'code' => 'invalid_action',
        'message' => 'Use action=info, action=set, or action=delete.',
    ],
], 400);
