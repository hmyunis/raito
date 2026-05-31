<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Hash;
use App\Support\Request;
use App\Support\Response;
use App\Support\Token;
use App\Support\Uuid;
use PDO;

final class AuthService
{
    public static function registerDevice(array $body): void
    {
        $displayName = self::nullableString($body['display_name'] ?? null, 120);
        $deviceLabel = self::nullableString($body['device_label'] ?? null, 160);

        $plainToken = Token::deviceToken();
        $tokenHash = Hash::token($plainToken);
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
            'display_name' => $displayName,
            'device_label' => $deviceLabel,
            'device_token_hash' => $tokenHash,
        ]);

        Response::json([
            'ok' => true,
            'user' => [
                'public_id' => $publicId,
                'display_name' => $displayName,
                'device_label' => $deviceLabel,
            ],
            'device_token' => $plainToken,
            'important' => 'Store this token securely in the app. It will not be shown again.',
        ], 201);
    }

    public static function requireUser(): array
    {
        $header = Request::header('Authorization');

        if ($header === null || !preg_match('/^Bearer\s+(.+)$/i', trim($header), $matches)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'missing_auth_token',
                    'message' => 'Authorization Bearer token is required.',
                ],
            ], 401);
        }

        $plainToken = trim($matches[1]);

        if ($plainToken === '' || strlen($plainToken) > 300) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_auth_token',
                    'message' => 'Authorization token is invalid.',
                ],
            ], 401);
        }

        $tokenHash = Hash::token($plainToken);
        $pdo = Database::pdo();

        $statement = $pdo->prepare('
            SELECT
                id,
                public_id,
                display_name,
                device_label,
                status,
                last_seen_at,
                created_at,
                updated_at
            FROM app_users
            WHERE device_token_hash = :device_token_hash
            LIMIT 1
        ');

        $statement->execute([
            'device_token_hash' => $tokenHash,
        ]);

        $user = $statement->fetch(PDO::FETCH_ASSOC);

        if ($user === false || $user['status'] !== 'active') {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'unauthorized',
                    'message' => 'Authorization token is invalid or inactive.',
                ],
            ], 401);
        }

        $update = $pdo->prepare('
            UPDATE app_users
            SET last_seen_at = UTC_TIMESTAMP()
            WHERE id = :id
        ');

        $update->execute(['id' => $user['id']]);

        return $user;
    }

    public static function me(): void
    {
        $user = self::requireUser();

        Response::json([
            'ok' => true,
            'user' => [
                'public_id' => $user['public_id'],
                'display_name' => $user['display_name'],
                'device_label' => $user['device_label'],
                'status' => $user['status'],
                'last_seen_at' => $user['last_seen_at'],
                'created_at' => $user['created_at'],
            ],
        ]);
    }

    private static function nullableString(mixed $value, int $maxLength): ?string
    {
        if (!is_string($value)) {
            return null;
        }

        $value = trim($value);
        if ($value === '') {
            return null;
        }

        if (mb_strlen($value, 'UTF-8') > $maxLength) {
            $value = mb_substr($value, 0, $maxLength, 'UTF-8');
        }

        return $value;
    }
}
