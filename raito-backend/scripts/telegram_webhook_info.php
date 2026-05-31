<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Services\TelegramClient;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

$client = new TelegramClient();
$result = $client->getWebhookInfo();

echo json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) . PHP_EOL;
