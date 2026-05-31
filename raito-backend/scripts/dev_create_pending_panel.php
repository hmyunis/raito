<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Database\Database;
use App\Support\Uuid;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

$userPublicId = $argv[1] ?? '';
$content = $argv[2] ?? '';

if ($userPublicId === '' || $content === '') {
    echo "Usage:" . PHP_EOL;
    echo "php scripts/dev_create_pending_panel.php USER_PUBLIC_ID \"Panel content here\"" . PHP_EOL;
    exit(1);
}

$pdo = Database::pdo();

$userStatement = $pdo->prepare('
    SELECT id, public_id
    FROM app_users
    WHERE public_id = :public_id
      AND status = "active"
    LIMIT 1
');

$userStatement->execute(['public_id' => $userPublicId]);
$user = $userStatement->fetch();

if ($user === false) {
    echo "Active user not found." . PHP_EOL;
    exit(1);
}

$panelPublicId = Uuid::v4();

$insert = $pdo->prepare('
    INSERT INTO remote_panels (
        public_id,
        user_id,
        telegram_account_id,
        source,
        source_chat_id,
        source_message_id,
        content,
        content_hash,
        status
    )
    VALUES (
        :public_id,
        :user_id,
        NULL,
        "telegram",
        NULL,
        NULL,
        :content,
        :content_hash,
        "pending"
    )
');

$insert->execute([
    'public_id' => $panelPublicId,
    'user_id' => $user['id'],
    'content' => $content,
    'content_hash' => hash('sha256', $content),
]);

echo json_encode([
    'ok' => true,
    'remote_panel_id' => $panelPublicId,
    'user_public_id' => $userPublicId,
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) . PHP_EOL;
