<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Services\TelegramClient;
use App\Support\Env;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

$url = $argv[1] ?? '';
if ($url === '') {
    echo "Usage:" . PHP_EOL;
    echo "php scripts/telegram_set_webhook.php https://yourdomain.com/api/telegram/webhook" . PHP_EOL;
    exit(1);
}

$secret = (string) Env::get('TELEGRAM_WEBHOOK_SECRET', '');
if ($secret === '') {
    echo "TELEGRAM_WEBHOOK_SECRET is missing in .env" . PHP_EOL;
    exit(1);
}

$client = new TelegramClient();
$result = $client->setWebhook($url, $secret);

echo json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) . PHP_EOL;
