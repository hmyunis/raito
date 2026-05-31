<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Logger;
use App\Support\Request;
use App\Support\Response;
use App\Support\Text;
use PDO;
use Throwable;

final class TelegramWebhookService
{
    public static function handle(): void
    {
        self::verifySecretToken();
        $update = Request::jsonBody();

        $updateId = $update['update_id'] ?? null;
        if (!is_int($updateId) && !is_numeric($updateId)) {
            Response::json(['ok' => true, 'ignored' => true, 'reason' => 'missing_update_id']);
        }

        $updateId = (int) $updateId;
        $logState = self::logUpdateReceived($updateId, $update);

        if ($logState === 'duplicate') {
            Response::json(['ok' => true, 'ignored' => true, 'reason' => 'duplicate_update']);
        }

        try {
            $result = self::processUpdate($update);
            self::markUpdate($updateId, $result['status'] ?? 'processed');

            Response::json([
                'ok' => true,
                'status' => $result['status'] ?? 'processed',
            ]);
        } catch (Throwable $exception) {
            self::markUpdate($updateId, 'failed', $exception->getMessage());
            Logger::error('Telegram webhook processing failed', [
                'update_id' => $updateId,
                'error' => $exception->getMessage(),
            ]);

            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'server_error',
                    'message' => 'A server error occurred.',
                ],
            ], 500);
        }
    }

    private static function verifySecretToken(): void
    {
        $expectedSecret = (string) Env::get('TELEGRAM_WEBHOOK_SECRET', '');
        if ($expectedSecret === '') {
            Logger::error('Webhook secret missing.');
            Response::json(['ok' => false, 'error' => ['code' => 'telegram_webhook_not_configured', 'message' => 'Telegram webhook secret is not configured.']], 500);
        }

        $receivedSecret = Request::header('X-Telegram-Bot-Api-Secret-Token');
        if ($receivedSecret === null || !hash_equals($expectedSecret, $receivedSecret)) {
            Logger::warning('Rejected Telegram webhook request due to invalid secret token.', [
                'ip_hash' => Request::clientIpHash(),
                'user_agent_hash' => Request::userAgentHash(),
            ]);

            Response::json(['ok' => false, 'error' => ['code' => 'forbidden', 'message' => 'Forbidden.']], 403);
        }
    }

    private static function processUpdate(array $update): array
    {
        if (!isset($update['message']) || !is_array($update['message'])) {
            return ['status' => 'ignored'];
        }

        $message = $update['message'];
        $chat = $message['chat'] ?? [];
        $from = $message['from'] ?? [];
        $chatId = (int) ($chat['id'] ?? 0);
        $chatType = (string) ($chat['type'] ?? '');
        $telegramUserId = (int) ($from['id'] ?? 0);

        if ($chatId === 0 || $telegramUserId === 0) {
            return ['status' => 'ignored'];
        }

        if ($chatType !== 'private') {
            return ['status' => 'ignored'];
        }

        $text = '';
        if (isset($message['text']) && is_string($message['text'])) {
            $text = Text::cleanUserText($message['text']);
        }

        if ($text !== '') {
            if (Text::startsWithCommand($text, '/start')) {
                return self::handleStartCommand($chatId, $from, $text);
            }
            if (Text::startsWithCommand($text, '/link')) {
                return self::handleLinkCommand($chatId, $from, $text);
            }
            if (Text::startsWithCommand($text, '/help')) {
                self::safeReply($chatId, self::helpMessage());
                return ['status' => 'processed'];
            }
            if (Text::startsWithCommand($text, '/status')) {
                return self::handleStatusCommand($chatId, $telegramUserId);
            }
            if (Text::startsWithCommand($text, '/unlink')) {
                return self::handleUnlinkCommand($chatId, $telegramUserId);
            }
            if (str_starts_with($text, '/')) {
                self::safeReply($chatId, "I do not recognize that command yet.\n\nSend /help to see what I can do.");
                return ['status' => 'ignored'];
            }
        }

        $capture = TelegramCaptureService::captureMessage($message);
        if (($capture['reply'] ?? null) !== null) {
            self::safeReply($chatId, (string) $capture['reply']);
        }

        return ['status' => $capture['status'] ?? 'processed'];
    }

    private static function handleStartCommand(int $chatId, array $from, string $text): array
    {
        $argument = Text::commandArgument($text, '/start');
        if ($argument !== '') {
            $result = TelegramPairingService::linkTelegramUser($from, $argument);
            self::safeReply($chatId, $result['message']);
            return ['status' => $result['ok'] ? 'processed' : 'ignored'];
        }

        self::safeReply($chatId, self::welcomeMessage());
        return ['status' => 'processed'];
    }

    private static function handleLinkCommand(int $chatId, array $from, string $text): array
    {
        $code = Text::commandArgument($text, '/link');
        if ($code === '') {
            self::safeReply($chatId, "Send your pairing code like this:\n/link ABCD-2345");
            return ['status' => 'ignored'];
        }

        $result = TelegramPairingService::linkTelegramUser($from, $code);
        self::safeReply($chatId, $result['message']);
        return ['status' => $result['ok'] ? 'processed' : 'ignored'];
    }

    private static function handleStatusCommand(int $chatId, int $telegramUserId): array
    {
        $pdo = Database::pdo();
        $statement = $pdo->prepare('
            SELECT
                ta.id AS telegram_account_id,
                au.id AS user_id,
                au.public_id
            FROM telegram_accounts ta
            INNER JOIN app_users au ON au.id = ta.user_id
            WHERE ta.telegram_user_id = :telegram_user_id
              AND ta.status = "active"
              AND au.status = "active"
            LIMIT 1
        ');
        $statement->execute(['telegram_user_id' => $telegramUserId]);
        $account = $statement->fetch(PDO::FETCH_ASSOC);

        if ($account === false) {
            self::safeReply($chatId, "Raito is not linked yet.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send:\n/link YOUR-CODE");
            return ['status' => 'ignored'];
        }

        $pendingStatement = $pdo->prepare('
            SELECT COUNT(*) AS pending_count
            FROM remote_panels
            WHERE user_id = :user_id
              AND status = "pending"
        ');
        $pendingStatement->execute(['user_id' => $account['user_id']]);
        $pending = $pendingStatement->fetch(PDO::FETCH_ASSOC);
        $pendingCount = (int) ($pending['pending_count'] ?? 0);

        self::safeReply($chatId, "Raito is linked.\nPending Telegram panels: {$pendingCount}\n\nOpen the app and sync manually to import them.");
        return ['status' => 'processed'];
    }

    private static function handleUnlinkCommand(int $chatId, int $telegramUserId): array
    {
        $result = TelegramPairingService::unlinkTelegramUser($telegramUserId);
        self::safeReply($chatId, $result['message']);
        return ['status' => $result['ok'] ? 'processed' : 'ignored'];
    }

    private static function welcomeMessage(): string
    {
        return "Welcome to Raito.\n\nI can capture text panels from Telegram and hold them until you manually sync in the app.\n\nTo connect:\n1. Open Raito\n2. Go to Settings → Telegram Capture\n3. Generate a code\n4. Send it here like this:\n/link ABCD-2345";
    }

    private static function helpMessage(): string
    {
        return "Raito Telegram Capture\n\nCommands:\n/start — Show intro\n/link CODE — Link Telegram to Raito\n/status — Check pending captured panels\n/unlink — Disconnect Telegram\n/help — Show this help\n\nAfter linking, send any text message here. I will capture it for manual sync.";
    }

    private static function safeReply(int|string $chatId, string $message): void
    {
        try {
            $client = new TelegramClient();
            $client->sendMessage($chatId, $message);
        } catch (Throwable $exception) {
            Logger::warning('Failed to send Telegram reply.', [
                'chat_id' => $chatId,
                'error' => $exception->getMessage(),
            ]);
        }
    }

    private static function logUpdateReceived(int $updateId, array $payload): string
    {
        $pdo = Database::pdo();
        try {
            $statement = $pdo->prepare('
                INSERT INTO bot_update_log (
                    update_id,
                    status,
                    payload_json
                )
                VALUES (
                    :update_id,
                    "received",
                    :payload_json
                )
            ');
            $statement->execute([
                'update_id' => $updateId,
                'payload_json' => json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            ]);
            return 'new';
        } catch (\PDOException $exception) {
            if ($exception->getCode() === '23000') {
                return 'duplicate';
            }
            throw $exception;
        }
    }

    private static function markUpdate(int $updateId, string $status, ?string $errorMessage = null): void
    {
        $allowed = ['processed', 'ignored', 'failed'];
        if (!in_array($status, $allowed, true)) {
            $status = 'processed';
        }

        $pdo = Database::pdo();
        $statement = $pdo->prepare('
            UPDATE bot_update_log
            SET
                status = :status,
                error_message = :error_message,
                processed_at = UTC_TIMESTAMP()
            WHERE update_id = :update_id
        ');
        $statement->execute([
            'status' => $status,
            'error_message' => $errorMessage === null ? null : mb_substr($errorMessage, 0, 500, 'UTF-8'),
            'update_id' => $updateId,
        ]);
    }
}
