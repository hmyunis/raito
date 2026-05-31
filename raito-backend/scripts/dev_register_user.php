<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Database\Database;
use App\Support\Hash;
use App\Support\Token;
use App\Support\Uuid;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

$plainToken = Token::deviceToken();
$publicId = Uuid::v4();

$pdo = Database::pdo();
$statement = $pdo->prepare('
    INSERT INTO app_users (
        public_id,
        display_name,
        device_label,
        device_token_hash,
        status,
        last_seen_at
    )
    VALUES (
        :public_id,
        :display_name,
        :device_label,
        :device_token_hash,
        "active",
        UTC_TIMESTAMP()
    )
');

$statement->execute([
    'public_id' => $publicId,
    'display_name' => 'Local Dev User',
    'device_label' => 'Windows Dev Device',
    'device_token_hash' => Hash::token($plainToken),
]);

echo json_encode([
    'public_id' => $publicId,
    'device_token' => $plainToken,
], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES) . PHP_EOL;
