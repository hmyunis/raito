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
        if (isset($update['callback_query']) && is_array($update['callback_query'])) {
            return self::handleCallbackQuery($update['callback_query']);
        }

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
            if (Text::startsWithCommand($text, '/buckets')) {
                return self::handleBucketsCommand($chatId, $telegramUserId);
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
        $account = SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId);
        if ($account === null) {
            self::safeReply($chatId, "Raito is not linked yet.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send:\n/link YOUR-CODE");
            return ['status' => 'ignored'];
        }

        $pdo = Database::pdo();
        $pendingStatement = $pdo->prepare('
            SELECT COUNT(*) AS pending_count
            FROM remote_panels
            WHERE user_id = :user_id
              AND status = "pending"
        ');
        $pendingStatement->execute(['user_id' => $account['user_id']]);
        $pending = $pendingStatement->fetch(PDO::FETCH_ASSOC);
        $pendingCount = (int) ($pending['pending_count'] ?? 0);
        $syncedBucketCount = SyncedBucketService::countBucketsForUser((int) $account['user_id']);
        $replyMarkup = $syncedBucketCount > 0
            ? ['inline_keyboard' => [[['text' => 'View Synced Buckets', 'callback_data' => 'buckets:1']]]]
            : null;

        self::safeReply(
            $chatId,
            "Raito is linked.\nPending Telegram panels: {$pendingCount}\nSynced buckets: {$syncedBucketCount}\n\nOpen the app to sync pending panels, or browse synced buckets below.",
            $replyMarkup
        );
        return ['status' => 'processed'];
    }

    private static function handleBucketsCommand(int $chatId, int $telegramUserId): array
    {
        $account = SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId);
        if ($account === null) {
            self::safeReply($chatId, "Raito is not linked yet.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send:\n/link YOUR-CODE");
            return ['status' => 'ignored'];
        }

        self::safeReply(
            $chatId,
            self::renderBucketsListMessage((int) $account['user_id'], 1),
            self::bucketsListKeyboard((int) $account['user_id'], 1)
        );
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
        return "Welcome to Raito.\n\nI can capture text panels from Telegram and hold them until you manually sync in the app. I can also browse any buckets you explicitly sync from the app.\n\nTo connect:\n1. Open Raito\n2. Go to Settings → Telegram Capture\n3. Generate a code\n4. Send it here like this:\n/link ABCD-2345";
    }

    private static function helpMessage(): string
    {
        return "Raito Telegram Capture\n\nCommands:\n/start — Show intro\n/link CODE — Link Telegram to Raito\n/status — Check pending captured panels and synced buckets\n/buckets — Browse synced buckets and tasks\n/unlink — Disconnect Telegram\n/help — Show this help\n\nAfter linking, send any text message here. I will capture it for manual sync. To browse tasks from Telegram, enable bucket sync inside the app for the buckets you want exposed.";
    }

    private static function handleCallbackQuery(array $callbackQuery): array
    {
        $callbackQueryId = (string) ($callbackQuery['id'] ?? '');
        $from = $callbackQuery['from'] ?? [];
        $message = $callbackQuery['message'] ?? [];
        $data = trim((string) ($callbackQuery['data'] ?? ''));
        $chatId = (int) (($message['chat']['id'] ?? 0));
        $messageId = (int) (($message['message_id'] ?? 0));
        $telegramUserId = (int) ($from['id'] ?? 0);

        if ($callbackQueryId === '' || $chatId === 0 || $messageId === 0 || $telegramUserId === 0 || $data === '') {
            return ['status' => 'ignored'];
        }

        $account = SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId);
        if ($account === null) {
            self::safeAnswerCallback($callbackQueryId, 'Link your Raito account first.');
            self::safeReply($chatId, "Raito is not linked yet.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send:\n/link YOUR-CODE");
            return ['status' => 'ignored'];
        }

        if (preg_match('/^buckets:(\d+)$/', $data, $matches) === 1) {
            $page = max(1, (int) $matches[1]);
            self::safeAnswerCallback($callbackQueryId);
            self::safeEditOrReply(
                $chatId,
                $messageId,
                self::renderBucketsListMessage((int) $account['user_id'], $page),
                self::bucketsListKeyboard((int) $account['user_id'], $page)
            );
            return ['status' => 'processed'];
        }

        if (preg_match('/^bucket:(\d+):(\d+)$/', $data, $matches) === 1) {
            $clientBucketId = (int) $matches[1];
            $page = max(1, (int) $matches[2]);
            $bucketView = SyncedBucketService::getBucketWithTasks((int) $account['user_id'], $clientBucketId, $page);
            self::safeAnswerCallback($callbackQueryId, $bucketView === null ? 'That bucket is no longer synced.' : null);
            if ($bucketView === null) {
                self::safeEditOrReply(
                    $chatId,
                    $messageId,
                    "That bucket is no longer synced from the app.\n\nUse /buckets to refresh the current list.",
                    ['inline_keyboard' => [[['text' => 'Refresh Buckets', 'callback_data' => 'buckets:1']]]]
                );
                return ['status' => 'ignored'];
            }

            self::safeEditOrReply(
                $chatId,
                $messageId,
                self::renderBucketDetailMessage($bucketView),
                self::bucketDetailKeyboard($bucketView)
            );
            return ['status' => 'processed'];
        }

        self::safeAnswerCallback($callbackQueryId, 'That action is not recognized.');
        return ['status' => 'ignored'];
    }

    private static function safeReply(int|string $chatId, string $message, ?array $replyMarkup = null): void
    {
        try {
            $client = new TelegramClient();
            $client->sendMessage($chatId, $message, $replyMarkup);
        } catch (Throwable $exception) {
            Logger::warning('Failed to send Telegram reply.', [
                'chat_id' => $chatId,
                'error' => $exception->getMessage(),
            ]);
        }
    }

    private static function safeEditOrReply(int|string $chatId, int $messageId, string $message, ?array $replyMarkup = null): void
    {
        try {
            $client = new TelegramClient();
            $client->editMessageText($chatId, $messageId, $message, $replyMarkup);
        } catch (Throwable $exception) {
            Logger::warning('Failed to edit Telegram message, falling back to send.', [
                'chat_id' => $chatId,
                'message_id' => $messageId,
                'error' => $exception->getMessage(),
            ]);
            self::safeReply($chatId, $message, $replyMarkup);
        }
    }

    private static function safeAnswerCallback(string $callbackQueryId, ?string $message = null): void
    {
        try {
            $client = new TelegramClient();
            $client->answerCallbackQuery($callbackQueryId, $message);
        } catch (Throwable $exception) {
            Logger::warning('Failed to answer Telegram callback query.', [
                'callback_query_id' => $callbackQueryId,
                'error' => $exception->getMessage(),
            ]);
        }
    }

    private static function renderBucketsListMessage(int $userId, int $page): string
    {
        $result = SyncedBucketService::listBucketsForUser($userId, $page);
        if (($result['total_count'] ?? 0) === 0) {
            return "No buckets are synced to Telegram yet.\n\nIn the Raito app, open a bucket and enable Telegram sync for it. Once enabled, it will appear here with buttons for browsing its tasks.";
        }

        $lines = [
            "Raito Synced Buckets",
            "",
            "Choose a bucket to inspect in Telegram.",
            "",
        ];

        foreach ($result['buckets'] as $index => $bucket) {
            $done = (int) ($bucket['completed_task_count'] ?? 0);
            $total = (int) ($bucket['task_count'] ?? 0);
            $statusIcon = ((int) ($bucket['is_completed'] ?? 0) === 1) ? '✅' : '⬜';
            $lines[] = ($index + 1 + (($result['page'] - 1) * $result['per_page'])) . ". {$statusIcon} {$bucket['name']}";
            $lines[] = "   {$done}/{$total} tasks complete";
        }

        if (($result['has_next'] ?? false) || ($result['has_prev'] ?? false)) {
            $lines[] = "";
            $lines[] = "Page {$result['page']}";
        }

        return implode("\n", $lines);
    }

    private static function bucketsListKeyboard(int $userId, int $page): ?array
    {
        $result = SyncedBucketService::listBucketsForUser($userId, $page);
        if (($result['total_count'] ?? 0) === 0) {
            return ['inline_keyboard' => [[['text' => 'Refresh', 'callback_data' => 'buckets:1']]]];
        }

        $rows = [];
        foreach ($result['buckets'] as $bucket) {
            $rows[] = [[
                'text' => (string) $bucket['name'],
                'callback_data' => 'bucket:' . (int) $bucket['client_bucket_id'] . ':1',
            ]];
        }

        $nav = [];
        if ($result['has_prev']) {
            $nav[] = ['text' => 'Prev', 'callback_data' => 'buckets:' . max(1, $page - 1)];
        }
        if ($result['has_next']) {
            $nav[] = ['text' => 'Next', 'callback_data' => 'buckets:' . ($page + 1)];
        }
        if ($nav !== []) {
            $rows[] = $nav;
        }

        return ['inline_keyboard' => $rows];
    }

    private static function renderBucketDetailMessage(array $bucketView): string
    {
        $bucket = $bucketView['bucket'];
        $lines = [
            'Bucket: ' . $bucket['name'],
            'Discipline: ' . (($bucket['discipline'] ?? '') !== '' ? $bucket['discipline'] : 'Unknown'),
            'Companion: ' . (($bucket['companion_id'] ?? '') !== '' ? $bucket['companion_id'] : 'Unknown'),
        ];

        if (($bucket['deadline'] ?? null) !== null) {
            $lines[] = 'Deadline: ' . $bucket['deadline'];
        }

        $lines[] = 'Tasks: ' . (int) $bucketView['total_count'];
        $lines[] = '';
        $lines[] = 'Legend: ◉ synced snapshot · ✅ complete · ⬜ open';
        $lines[] = '';

        if ($bucketView['tasks'] === []) {
            $lines[] = 'No tasks were synced for this bucket yet.';
        } else {
            foreach ($bucketView['tasks'] as $task) {
                $syncIndicator = (($task['synced_at'] ?? null) !== null) ? '◉' : '○';
                $completionIndicator = ((int) ($task['is_completed'] ?? 0) === 1) ? '✅' : '⬜';
                $pinIndicator = ((int) ($task['is_pinned'] ?? 0) === 1) ? '📌 ' : '';
                $lines[] = "{$syncIndicator} {$completionIndicator} {$pinIndicator}{$task['name']}";
            }
        }

        if (($bucketView['has_next'] ?? false) || ($bucketView['has_prev'] ?? false)) {
            $lines[] = '';
            $lines[] = 'Page ' . (int) $bucketView['page'];
        }

        return implode("\n", $lines);
    }

    private static function bucketDetailKeyboard(array $bucketView): array
    {
        $bucket = $bucketView['bucket'];
        $rows = [];
        $nav = [];

        if ($bucketView['has_prev']) {
            $nav[] = [
                'text' => 'Prev Tasks',
                'callback_data' => 'bucket:' . (int) $bucket['client_bucket_id'] . ':' . max(1, ((int) $bucketView['page']) - 1),
            ];
        }
        if ($bucketView['has_next']) {
            $nav[] = [
                'text' => 'Next Tasks',
                'callback_data' => 'bucket:' . (int) $bucket['client_bucket_id'] . ':' . (((int) $bucketView['page']) + 1),
            ];
        }
        if ($nav !== []) {
            $rows[] = $nav;
        }

        $rows[] = [[
            'text' => 'Back To Buckets',
            'callback_data' => 'buckets:1',
        ]];

        return ['inline_keyboard' => $rows];
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
