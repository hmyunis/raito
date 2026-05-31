<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Hash;
use App\Support\Response;
use App\Support\Token;
use PDO;
use Throwable;

final class TelegramPairingService
{
    public static function createPairingCode(array $user): void
    {
        $ttlMinutes = max(1, min(60, Env::int('PAIRING_CODE_TTL_MINUTES', 10)));
        $pdo = Database::pdo();

        $plainCode = self::generateUniqueCode();
        $codeHash = Hash::pairingCode($plainCode);

        $statement = $pdo->prepare('
            INSERT INTO pairing_codes (
                user_id,
                code_hash,
                expires_at
            )
            VALUES (
                :user_id,
                :code_hash,
                DATE_ADD(UTC_TIMESTAMP(), INTERVAL :ttl_minutes MINUTE)
            )
        ');

        $statement->bindValue('user_id', (int) $user['id'], PDO::PARAM_INT);
        $statement->bindValue('code_hash', $codeHash);
        $statement->bindValue('ttl_minutes', $ttlMinutes, PDO::PARAM_INT);
        $statement->execute();

        Response::json([
            'ok' => true,
            'pairing' => [
                'code' => $plainCode,
                'expires_in_minutes' => $ttlMinutes,
                'instructions' => 'Open the Raito Telegram bot and send: /link ' . $plainCode,
            ],
        ], 201);
    }

    public static function linkTelegramUser(array $telegramUser, string $plainCode): array
    {
        $normalizedCode = Hash::normalizePairingCode($plainCode);
        if (!preg_match('/^[A-Z2-9]{8}$/', $normalizedCode)) {
            return ['ok' => false, 'message' => 'That code format is invalid. Open Raito and generate a fresh pairing code.'];
        }

        $codeHash = Hash::pairingCode($normalizedCode);
        $telegramUserId = (int) ($telegramUser['id'] ?? 0);
        if ($telegramUserId <= 0) {
            return ['ok' => false, 'message' => 'Could not read your Telegram account ID.'];
        }

        $pdo = Database::pdo();
        $maxAttempts = max(1, min(20, Env::int('PAIRING_CODE_MAX_ATTEMPTS', 5)));

        try {
            $pdo->beginTransaction();

            $codeStatement = $pdo->prepare('
                SELECT
                    id,
                    user_id,
                    expires_at,
                    used_at,
                    attempt_count
                FROM pairing_codes
                WHERE code_hash = :code_hash
                LIMIT 1
                FOR UPDATE
            ');
            $codeStatement->execute(['code_hash' => $codeHash]);
            $pairingCode = $codeStatement->fetch(PDO::FETCH_ASSOC);

            if ($pairingCode === false) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That code was not found. Open Raito and generate a fresh pairing code.'];
            }

            $incrementAttempts = $pdo->prepare('UPDATE pairing_codes SET attempt_count = attempt_count + 1 WHERE id = :id');
            $incrementAttempts->execute(['id' => $pairingCode['id']]);

            if ((int) $pairingCode['attempt_count'] >= $maxAttempts) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'Too many attempts for this code. Generate a new code in Raito.'];
            }

            if ($pairingCode['used_at'] !== null) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That code was already used. Generate a new one in Raito.'];
            }

            $expiryCheck = $pdo->prepare('
                SELECT
                    CASE
                        WHEN :expires_at <= UTC_TIMESTAMP() THEN 1
                        ELSE 0
                    END AS expired
            ');
            $expiryCheck->execute(['expires_at' => $pairingCode['expires_at']]);
            $expiry = $expiryCheck->fetch(PDO::FETCH_ASSOC);
            if ((int) ($expiry['expired'] ?? 1) === 1) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That code expired. Generate a new one in Raito.'];
            }

            $userStatement = $pdo->prepare('
                SELECT id, public_id, status
                FROM app_users
                WHERE id = :id
                LIMIT 1
                FOR UPDATE
            ');
            $userStatement->execute(['id' => $pairingCode['user_id']]);
            $user = $userStatement->fetch(PDO::FETCH_ASSOC);

            if ($user === false || $user['status'] !== 'active') {
                $pdo->commit();
                return ['ok' => false, 'message' => 'The Raito account for this code is unavailable.'];
            }

            $existingTelegram = $pdo->prepare('
                SELECT id, user_id, status
                FROM telegram_accounts
                WHERE telegram_user_id = :telegram_user_id
                LIMIT 1
                FOR UPDATE
            ');
            $existingTelegram->execute(['telegram_user_id' => $telegramUserId]);
            $telegramAccount = $existingTelegram->fetch(PDO::FETCH_ASSOC);

            if ($telegramAccount !== false && $telegramAccount['status'] === 'active' && (int) $telegramAccount['user_id'] !== (int) $user['id']) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'This Telegram account is already linked to another Raito user.'];
            }

            $existingTelegramByUser = $pdo->prepare('
                SELECT id, telegram_user_id, status
                FROM telegram_accounts
                WHERE user_id = :user_id
                  AND status = "active"
                LIMIT 1
                FOR UPDATE
            ');
            $existingTelegramByUser->execute(['user_id' => $user['id']]);
            $linkedTelegram = $existingTelegramByUser->fetch(PDO::FETCH_ASSOC);

            if ($linkedTelegram !== false && (int) $linkedTelegram['telegram_user_id'] !== $telegramUserId) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'This Raito account is already linked to another Telegram account.'];
            }

            if ($telegramAccount !== false) {
                $updateTelegram = $pdo->prepare('
                    UPDATE telegram_accounts
                    SET
                        user_id = :user_id,
                        username = :username,
                        first_name = :first_name,
                        last_name = :last_name,
                        status = "active",
                        linked_at = UTC_TIMESTAMP()
                    WHERE id = :id
                ');
                $updateTelegram->execute([
                    'user_id' => $user['id'],
                    'username' => $telegramUser['username'] ?? null,
                    'first_name' => $telegramUser['first_name'] ?? null,
                    'last_name' => $telegramUser['last_name'] ?? null,
                    'id' => $telegramAccount['id'],
                ]);
            } else {
                $insertTelegram = $pdo->prepare('
                    INSERT INTO telegram_accounts (
                        user_id,
                        telegram_user_id,
                        username,
                        first_name,
                        last_name,
                        status,
                        linked_at
                    )
                    VALUES (
                        :user_id,
                        :telegram_user_id,
                        :username,
                        :first_name,
                        :last_name,
                        "active",
                        UTC_TIMESTAMP()
                    )
                ');
                $insertTelegram->execute([
                    'user_id' => $user['id'],
                    'telegram_user_id' => $telegramUserId,
                    'username' => $telegramUser['username'] ?? null,
                    'first_name' => $telegramUser['first_name'] ?? null,
                    'last_name' => $telegramUser['last_name'] ?? null,
                ]);
            }

            $markUsed = $pdo->prepare('
                UPDATE pairing_codes
                SET
                    used_at = UTC_TIMESTAMP(),
                    used_by_telegram_user_id = :used_by_telegram_user_id
                WHERE id = :id
            ');
            $markUsed->execute(['used_by_telegram_user_id' => $telegramUserId, 'id' => $pairingCode['id']]);
            $pdo->commit();

            return ['ok' => true, 'message' => 'Telegram linked successfully. You can now send /status or normal text messages here.'];
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $exception;
        }
    }

    public static function unlinkTelegramUser(int $telegramUserId): array
    {
        $pdo = Database::pdo();
        $statement = $pdo->prepare('
            UPDATE telegram_accounts
            SET status = "unlinked"
            WHERE telegram_user_id = :telegram_user_id
              AND status = "active"
        ');
        $statement->execute(['telegram_user_id' => $telegramUserId]);

        if ($statement->rowCount() === 0) {
            return ['ok' => false, 'message' => 'This Telegram account is not linked yet.'];
        }

        return ['ok' => true, 'message' => 'Telegram account unlinked.'];
    }

    private static function generateUniqueCode(): string
    {
        return Token::pairingCode();
    }
}
