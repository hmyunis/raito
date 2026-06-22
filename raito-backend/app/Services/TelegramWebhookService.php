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
    private const MENU_INBOX = '📥 Inbox';
    private const MENU_BUCKETS = '🪣 Buckets';
    private const MENU_STATUS = '📊 Status';
    private const MENU_HELP = '❓ Help';
    private const MENU_LINK = '🔗 Link Raito';
    private const MENU_UNLINK = '🔌 Unlink';

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
            self::notifyUserAboutFailure($update);

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
                self::safeReply($chatId, self::helpMessage(), self::mainMenuKeyboard(self::isLinked($telegramUserId)));
                return ['status' => 'processed'];
            }
            if (Text::startsWithCommand($text, '/status')) {
                return self::handleStatusCommand($chatId, $telegramUserId);
            }
            if (Text::startsWithCommand($text, '/inbox')) {
                return self::handleInboxCommand($chatId, $telegramUserId);
            }
            if (Text::startsWithCommand($text, '/buckets')) {
                return self::handleBucketsCommand($chatId, $telegramUserId);
            }
            if (Text::startsWithCommand($text, '/tasks')) {
                return self::handleBucketsCommand($chatId, $telegramUserId);
            }
            if (Text::startsWithCommand($text, '/unlink')) {
                return self::handleUnlinkCommand($chatId, $telegramUserId);
            }
            if (str_starts_with($text, '/')) {
                self::safeReply($chatId, "I do not recognize that command yet.\n\nTap one of the menu buttons below, or send /help to see everything I can do.", self::mainMenuKeyboard(self::isLinked($telegramUserId)));
                return ['status' => 'ignored'];
            }

            $menuAction = self::matchMainMenuAction($text);
            if ($menuAction !== null) {
                return self::handleMainMenuAction($chatId, $telegramUserId, $menuAction);
            }
        }

        $capture = TelegramCaptureService::captureMessage($message);
        if (($capture['reply'] ?? null) !== null) {
            $linked = self::isLinked($telegramUserId);
            $replyMarkup = (($capture['ok'] ?? false) === true && ($capture['status'] ?? '') === 'processed')
                ? self::captureSuccessKeyboard()
                : self::mainMenuKeyboard($linked);
            self::safeReply($chatId, (string) $capture['reply'], $replyMarkup);
        }

        return ['status' => $capture['status'] ?? 'processed'];
    }

    private static function handleStartCommand(int $chatId, array $from, string $text): array
    {
        $argument = Text::commandArgument($text, '/start');
        if ($argument !== '') {
            $result = TelegramPairingService::linkTelegramUser($from, $argument);
            $reply = $result['ok']
                ? $result['message'] . "\n\nYou can use the button menu below to open your inbox, browse buckets, or check sync status."
                : $result['message'];
            self::safeReply($chatId, $reply, self::mainMenuKeyboard($result['ok']));
            return ['status' => $result['ok'] ? 'processed' : 'ignored'];
        }

        self::safeReply($chatId, self::welcomeMessage(), self::mainMenuKeyboard(self::isLinked((int) ($from['id'] ?? 0))));
        return ['status' => 'processed'];
    }

    private static function handleLinkCommand(int $chatId, array $from, string $text): array
    {
        $code = Text::commandArgument($text, '/link');
        if ($code === '') {
            self::safeReply($chatId, self::linkPromptMessage(), self::mainMenuKeyboard(false));
            return ['status' => 'ignored'];
        }

        $result = TelegramPairingService::linkTelegramUser($from, $code);
        $reply = $result['ok']
            ? $result['message'] . "\n\nYou are ready. Use the buttons below as your main navigation."
            : $result['message'];
        self::safeReply($chatId, $reply, self::mainMenuKeyboard($result['ok']));
        return ['status' => $result['ok'] ? 'processed' : 'ignored'];
    }

    private static function handleStatusCommand(int $chatId, int $telegramUserId): array
    {
        $account = SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId);
        if ($account === null) {
            self::safeReply($chatId, self::notLinkedMessage(), self::mainMenuKeyboard(false));
            return ['status' => 'ignored'];
        }

        self::safeReply(
            $chatId,
            self::renderStatusMessage((int) $account['user_id']),
            self::mainMenuKeyboard(true)
        );
        return ['status' => 'processed'];
    }

    private static function renderStatusMessage(int $userId): string
    {
        $pdo = Database::pdo();
        $pendingStatement = $pdo->prepare('
            SELECT COUNT(*) AS pending_count
            FROM remote_panels
            WHERE user_id = :user_id
              AND status = "pending"
        ');
        $pendingStatement->execute(['user_id' => $userId]);
        $pending = $pendingStatement->fetch(PDO::FETCH_ASSOC);
        $pendingCount = (int) ($pending['pending_count'] ?? 0);
        $queuedStatement = $pdo->prepare('
            SELECT COUNT(*) AS queued_count
            FROM telegram_task_operations
            WHERE user_id = :user_id
              AND status = "pending"
        ');
        $queuedStatement->execute(['user_id' => $userId]);
        $queued = $queuedStatement->fetch(PDO::FETCH_ASSOC);
        $queuedCount = (int) ($queued['queued_count'] ?? 0);
        $syncedBucketCount = SyncedBucketService::countBucketsForUser($userId);

        return "✅ Raito is linked and listening.\n\n📥 Inbox waiting for routing: {$pendingCount}\n🔄 Actions waiting for app sync: {$queuedCount}\n🪣 Synced buckets: {$syncedBucketCount}\n\nUse the menu buttons below to jump in. If you send a new text message here, I will capture it automatically.";
    }

    private static function handleBucketsCommand(int $chatId, int $telegramUserId): array
    {
        $account = SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId);
        if ($account === null) {
            self::safeReply($chatId, self::notLinkedMessage(), self::mainMenuKeyboard(false));
            return ['status' => 'ignored'];
        }

        self::safeTyping($chatId);
        self::safeReply(
            $chatId,
            self::renderBucketsListMessage((int) $account['user_id'], 1),
            self::bucketsListKeyboard((int) $account['user_id'], 1)
        );
        return ['status' => 'processed'];
    }

    private static function handleInboxCommand(int $chatId, int $telegramUserId): array
    {
        $account = SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId);
        if ($account === null) {
            self::safeReply($chatId, self::notLinkedMessage(), self::mainMenuKeyboard(false));
            return ['status' => 'ignored'];
        }

        self::safeTyping($chatId);
        self::safeReply(
            $chatId,
            self::renderInboxMessage((int) $account['user_id'], 1),
            self::inboxKeyboard((int) $account['user_id'], 1)
        );
        return ['status' => 'processed'];
    }

    private static function handleUnlinkCommand(int $chatId, int $telegramUserId): array
    {
        $result = TelegramPairingService::unlinkTelegramUser($telegramUserId);
        $reply = $result['ok']
            ? $result['message'] . "\n\nYou can link again any time from the app and then paste the new code here."
            : $result['message'];
        self::safeReply($chatId, $reply, self::mainMenuKeyboard(false));
        return ['status' => $result['ok'] ? 'processed' : 'ignored'];
    }

    private static function welcomeMessage(): string
    {
        return "✨ Welcome to Raito.\n\nI can capture text from Telegram, route inbox items into your synced buckets, and let you mark tasks done or undone without leaving chat.\n\nUse the button menu below as the main way to navigate. Slash commands still work too.\n\nTo connect:\n1. Open Raito\n2. Go to Settings → Telegram Capture\n3. Generate a code\n4. Send it here like this:\n/link ABCD-2345";
    }

    private static function helpMessage(): string
    {
        return "🤖 Raito Telegram Assistant\n\nPrimary navigation:\n• Tap the menu buttons for Inbox, Buckets, Status, Help, and Link/Unlink\n• After linking, just send me text and I will capture it into your Telegram inbox automatically\n\nSlash commands still supported:\n/start — Show intro\n/link CODE — Link Telegram to Raito\n/status — Check inbox and synced bucket counts\n/inbox — Browse Telegram inbox items and send them to a bucket\n/buckets — Browse synced buckets and tasks\n/tasks — Same as /buckets\n/unlink — Disconnect Telegram\n/help — Show this help";
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
            self::safeReply($chatId, self::notLinkedMessage(), self::mainMenuKeyboard(false));
            return ['status' => 'ignored'];
        }

        if (preg_match('/^buckets:(\d+)$/', $data, $matches) === 1) {
            $page = max(1, (int) $matches[1]);
            self::safeAnswerCallback($callbackQueryId, 'Loading buckets...');
            self::safeEditOrReply(
                $chatId,
                $messageId,
                self::renderBucketsListMessage((int) $account['user_id'], $page),
                self::bucketsListKeyboard((int) $account['user_id'], $page)
            );
            return ['status' => 'processed'];
        }

        if (preg_match('/^inbox:(\d+)$/', $data, $matches) === 1) {
            $page = max(1, (int) $matches[1]);
            self::safeAnswerCallback($callbackQueryId, 'Loading inbox...');
            self::safeEditOrReply(
                $chatId,
                $messageId,
                self::renderInboxMessage((int) $account['user_id'], $page),
                self::inboxKeyboard((int) $account['user_id'], $page)
            );
            return ['status' => 'processed'];
        }

        if (preg_match('/^status:(\d+)$/', $data) === 1) {
            self::safeAnswerCallback($callbackQueryId, 'Refreshing status...');
            self::safeReply($chatId, self::renderStatusMessage((int) $account['user_id']), self::mainMenuKeyboard(true));
            return ['status' => 'processed'];
        }

        if (preg_match('/^pick:([0-9a-fA-F-]{36}):(\d+)$/', $data, $matches) === 1) {
            $panelPublicId = $matches[1];
            $page = max(1, (int) $matches[2]);
            self::safeAnswerCallback($callbackQueryId, 'Loading buckets...');
            self::safeEditOrReply(
                $chatId,
                $messageId,
                self::renderBucketPickerMessage((int) $account['user_id'], $panelPublicId, $page),
                self::bucketPickerKeyboard((int) $account['user_id'], $panelPublicId, $page)
            );
            return ['status' => 'processed'];
        }

        if (preg_match('/^pickb:([0-9a-fA-F-]{36}):(\d+):(\d+)$/', $data, $matches) === 1) {
            $panelPublicId = $matches[1];
            $bucketClientId = (int) $matches[2];
            $page = max(1, (int) $matches[3]);
            $result = TelegramTaskOperationService::queueCreateFromPanel(
                (int) $account['user_id'],
                (int) $account['telegram_account_id'],
                $panelPublicId,
                $bucketClientId
            );
            self::safeAnswerCallback($callbackQueryId, $result['message']);
            self::safeEditOrReply(
                $chatId,
                $messageId,
                self::renderInboxMessage((int) $account['user_id'], $page),
                self::inboxKeyboard((int) $account['user_id'], $page)
            );
            return ['status' => 'processed'];
        }

        if (preg_match('/^bucket:(\d+):(\d+)$/', $data, $matches) === 1) {
            $clientBucketId = (int) $matches[1];
            $page = max(1, (int) $matches[2]);
            $bucketView = SyncedBucketService::getBucketWithTasks((int) $account['user_id'], $clientBucketId, $page);
            self::safeAnswerCallback($callbackQueryId, $bucketView === null ? 'That bucket is no longer synced.' : 'Opening bucket...');
            if ($bucketView === null) {
                self::safeEditOrReply(
                    $chatId,
                    $messageId,
                    "⚠️ That bucket is no longer synced from the app.\n\nRefresh the list and try again.",
                    self::navigationFooter([['text' => '🔄 Refresh Buckets', 'callback_data' => 'buckets:1']])
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

        if (preg_match('/^toggle:(\d+):(\d+):([01]):(\d+)$/', $data, $matches) === 1) {
            $bucketClientId = (int) $matches[1];
            $taskClientId = (int) $matches[2];
            $desiredCompletion = $matches[3] === '1';
            $page = max(1, (int) $matches[4]);
            $result = TelegramTaskOperationService::queueTaskCompletionToggle(
                (int) $account['user_id'],
                (int) $account['telegram_account_id'],
                $bucketClientId,
                $taskClientId,
                $desiredCompletion
            );
            self::safeAnswerCallback($callbackQueryId, $result['message']);
            $bucketView = SyncedBucketService::getBucketWithTasks((int) $account['user_id'], $bucketClientId, $page);
            if ($bucketView !== null) {
                self::safeEditOrReply(
                    $chatId,
                    $messageId,
                    self::renderBucketDetailMessage($bucketView),
                    self::bucketDetailKeyboard($bucketView)
                );
            }
            return ['status' => 'processed'];
        }

        self::safeAnswerCallback($callbackQueryId, 'That action is not recognized.');
        return ['status' => 'ignored'];
    }

    private static function safeReply(int|string $chatId, string $message, ?array $replyMarkup = null): void
    {
        try {
            $client = new TelegramClient();
            $response = $client->sendMessage($chatId, $message, $replyMarkup);
            if (($response['ok'] ?? false) !== true) {
                Logger::warning('Telegram sendMessage returned non-ok response.', [
                    'chat_id' => $chatId,
                    'response' => $response,
                ]);
            }
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
            $response = $client->editMessageText($chatId, $messageId, $message, $replyMarkup);
            if (($response['ok'] ?? false) === true) {
                return;
            }

            $description = strtolower((string) ($response['description'] ?? ''));
            if (str_contains($description, 'message is not modified')) {
                return;
            }

            throw new \RuntimeException($description !== '' ? $description : 'Telegram editMessageText returned non-ok response.');
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
            return "🪣 No buckets are synced to Telegram yet.\n\nIn the Raito app, open a bucket and enable Telegram sync for it. As soon as the app syncs, it will appear here for task browsing and quick check-offs.";
        }

        $lines = [
            "🪣 Raito Buckets",
            "",
            "Choose a bucket to inspect and manage from Telegram.",
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

    private static function renderInboxMessage(int $userId, int $page): string
    {
        $result = TelegramTaskOperationService::listPendingInbox($userId, $page);
        if (($result['total_count'] ?? 0) === 0) {
            return "📥 Telegram inbox is clear.\n\nSend a text message to this bot and it will appear here. Then tap it to route it into one of your synced buckets.";
        }

        $lines = [
            "📥 Telegram Inbox",
            "",
            "Choose an item, then pick the bucket that should receive it in the mobile app.",
            "",
        ];

        foreach ($result['items'] as $index => $item) {
            $preview = Text::limit((string) $item['content'], 72);
            $lines[] = ($index + 1 + (($result['page'] - 1) * $result['per_page'])) . ". " . $preview;
        }

        if (($result['has_next'] ?? false) || ($result['has_prev'] ?? false)) {
            $lines[] = "";
            $lines[] = "Page {$result['page']}";
        }

        return implode("\n", $lines);
    }

    private static function renderBucketPickerMessage(int $userId, string $panelPublicId, int $page): string
    {
        $selected = TelegramTaskOperationService::findPendingInboxItem($userId, $panelPublicId);
        $preview = $selected !== null ? Text::limit((string) $selected['content'], 120) : 'Selected inbox item';

        return "🧭 Choose a bucket for this inbox item:\n\n{$preview}\n\nI will queue it immediately, and the app will create it on the next sync.";
    }

    private static function bucketsListKeyboard(int $userId, int $page): ?array
    {
        $result = SyncedBucketService::listBucketsForUser($userId, $page);
        if (($result['total_count'] ?? 0) === 0) {
            return self::navigationFooter([['text' => '🔄 Refresh Buckets', 'callback_data' => 'buckets:1']]);
        }

        $rows = [];
        foreach ($result['buckets'] as $bucket) {
            $rows[] = [[
                'text' => '🪣 ' . Text::limit((string) $bucket['name'], 28),
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

        return self::navigationFooter(...$rows);
    }

    private static function inboxKeyboard(int $userId, int $page): array
    {
        $result = TelegramTaskOperationService::listPendingInbox($userId, $page);
        if (($result['total_count'] ?? 0) === 0) {
            return self::navigationFooter(
                [['text' => '🔄 Refresh Inbox', 'callback_data' => 'inbox:1']],
                [['text' => '🪣 View Buckets', 'callback_data' => 'buckets:1']]
            );
        }

        $rows = [];
        foreach ($result['items'] as $item) {
            $rows[] = [[
                'text' => '➕ Add: ' . Text::limit((string) $item['content'], 24),
                'callback_data' => 'pick:' . $item['public_id'] . ':' . $page,
            ]];
        }

        $nav = [];
        if ($result['has_prev']) {
            $nav[] = ['text' => 'Prev', 'callback_data' => 'inbox:' . max(1, $page - 1)];
        }
        if ($result['has_next']) {
            $nav[] = ['text' => 'Next', 'callback_data' => 'inbox:' . ($page + 1)];
        }
        if ($nav !== []) {
            $rows[] = $nav;
        }

        return self::navigationFooter(
            ...$rows,
            [['text' => '🪣 View Buckets', 'callback_data' => 'buckets:1']]
        );
    }

    private static function bucketPickerKeyboard(int $userId, string $panelPublicId, int $page): array
    {
        $result = SyncedBucketService::listBucketsForUser($userId, $page);
        $rows = [];

        foreach ($result['buckets'] as $bucket) {
            $rows[] = [[
                'text' => '🪣 ' . Text::limit((string) $bucket['name'], 28),
                'callback_data' => 'pickb:' . $panelPublicId . ':' . (int) $bucket['client_bucket_id'] . ':' . $page,
            ]];
        }

        $nav = [];
        if ($result['has_prev']) {
            $nav[] = ['text' => 'Prev Buckets', 'callback_data' => 'pick:' . $panelPublicId . ':' . max(1, $page - 1)];
        }
        if ($result['has_next']) {
            $nav[] = ['text' => 'Next Buckets', 'callback_data' => 'pick:' . $panelPublicId . ':' . ($page + 1)];
        }
        if ($nav !== []) {
            $rows[] = $nav;
        }

        return self::navigationFooter(
            ...$rows,
            [['text' => '⬅️ Back To Inbox', 'callback_data' => 'inbox:1']]
        );
    }

    private static function renderBucketDetailMessage(array $bucketView): string
    {
        $bucket = $bucketView['bucket'];
        $lines = [
            '🪣 Bucket: ' . $bucket['name'],
            '🏷️ Discipline: ' . (($bucket['discipline'] ?? '') !== '' ? $bucket['discipline'] : 'Unknown'),
            '🧍 Companion: ' . (($bucket['companion_id'] ?? '') !== '' ? $bucket['companion_id'] : 'Unknown'),
        ];

        if (($bucket['deadline'] ?? null) !== null) {
            $lines[] = '⏰ Deadline: ' . $bucket['deadline'];
        }

        $lines[] = '📋 Tasks: ' . (int) $bucketView['total_count'];
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

        foreach ($bucketView['tasks'] as $task) {
            $isCompleted = (int) ($task['is_completed'] ?? 0) === 1;
            $rows[] = [[
                'text' => ($isCompleted ? '↩️ Mark Undone: ' : '✅ Mark Done: ') . Text::limit((string) $task['name'], 20),
                'callback_data' => 'toggle:' . (int) $bucket['client_bucket_id'] . ':' . (int) $task['client_task_id'] . ':' . ($isCompleted ? '0' : '1') . ':' . (int) $bucketView['page'],
            ]];
        }

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

        return self::navigationFooter(
            ...$rows,
            [['text' => '⬅️ Back To Buckets', 'callback_data' => 'buckets:1']],
            [['text' => '📥 Inbox', 'callback_data' => 'inbox:1']]
        );
    }

    private static function handleMainMenuAction(int $chatId, int $telegramUserId, string $action): array
    {
        return match ($action) {
            'inbox' => self::handleInboxCommand($chatId, $telegramUserId),
            'buckets' => self::handleBucketsCommand($chatId, $telegramUserId),
            'status' => self::handleStatusCommand($chatId, $telegramUserId),
            'help' => self::handleHelpMenuAction($chatId, $telegramUserId),
            'link' => self::handleLinkPromptAction($chatId),
            'unlink' => self::handleUnlinkCommand($chatId, $telegramUserId),
            default => ['status' => 'ignored'],
        };
    }

    private static function handleHelpMenuAction(int $chatId, int $telegramUserId): array
    {
        self::safeReply($chatId, self::helpMessage(), self::mainMenuKeyboard(self::isLinked($telegramUserId)));
        return ['status' => 'processed'];
    }

    private static function handleLinkPromptAction(int $chatId): array
    {
        self::safeReply($chatId, self::linkPromptMessage(), self::mainMenuKeyboard(false));
        return ['status' => 'processed'];
    }

    private static function isLinked(int $telegramUserId): bool
    {
        if ($telegramUserId <= 0) {
            return false;
        }

        return SyncedBucketService::findActiveAccountByTelegramUser($telegramUserId) !== null;
    }

    private static function matchMainMenuAction(string $text): ?string
    {
        $normalized = preg_replace('/[^\pL\pN]+/u', '', mb_strtolower(trim($text), 'UTF-8'));
        if (!is_string($normalized) || $normalized === '') {
            return null;
        }

        return match ($normalized) {
            'inbox' => 'inbox',
            'buckets', 'bucket', 'tasks', 'task' => 'buckets',
            'status' => 'status',
            'help' => 'help',
            'link', 'linkraito' => 'link',
            'unlink' => 'unlink',
            default => null,
        };
    }

    private static function mainMenuKeyboard(bool $isLinked): array
    {
        $keyboard = [
            [
                ['text' => self::MENU_INBOX],
                ['text' => self::MENU_BUCKETS],
            ],
            [
                ['text' => self::MENU_STATUS],
                ['text' => self::MENU_HELP],
            ],
        ];

        $keyboard[] = [[
            'text' => $isLinked ? self::MENU_UNLINK : self::MENU_LINK,
        ]];

        return [
            'keyboard' => $keyboard,
            'resize_keyboard' => true,
            'is_persistent' => true,
            'input_field_placeholder' => $isLinked
                ? 'Send a task note or tap a menu button'
                : 'Paste your pairing code or tap a menu button',
        ];
    }

    private static function captureSuccessKeyboard(): array
    {
        return ['inline_keyboard' => [
            [
                ['text' => '📥 Open Inbox', 'callback_data' => 'inbox:1'],
                ['text' => '📊 Status', 'callback_data' => 'status:1'],
            ],
            [
                ['text' => '🪣 View Buckets', 'callback_data' => 'buckets:1'],
            ],
        ]];
    }

    private static function navigationFooter(array ...$rows): array
    {
        $rows[] = [
            ['text' => '📥 Inbox', 'callback_data' => 'inbox:1'],
            ['text' => '🪣 Buckets', 'callback_data' => 'buckets:1'],
        ];
        $rows[] = [[
            'text' => '🔄 Refresh Status',
            'callback_data' => 'status:1',
        ]];

        return ['inline_keyboard' => $rows];
    }

    private static function linkPromptMessage(): string
    {
        return "🔗 Link your Raito account.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send it here like:\n/link ABCD-2345";
    }

    private static function notLinkedMessage(): string
    {
        return "🔒 Raito is not linked yet.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send it here like:\n/link ABCD-2345";
    }

    private static function safeTyping(int|string $chatId): void
    {
        try {
            $client = new TelegramClient();
            $client->sendChatAction($chatId, 'typing');
        } catch (Throwable $exception) {
            Logger::warning('Failed to send Telegram typing action.', [
                'chat_id' => $chatId,
                'error' => $exception->getMessage(),
            ]);
        }
    }

    private static function notifyUserAboutFailure(array $update): void
    {
        try {
            if (isset($update['callback_query']) && is_array($update['callback_query'])) {
                $callbackQuery = $update['callback_query'];
                $callbackQueryId = (string) ($callbackQuery['id'] ?? '');
                $message = $callbackQuery['message'] ?? [];
                $chatId = (int) (($message['chat']['id'] ?? 0));
                if ($callbackQueryId !== '') {
                    self::safeAnswerCallback($callbackQueryId, 'Something went wrong. Please try again.');
                }
                if ($chatId !== 0) {
                    self::safeReply($chatId, "⚠️ I hit a temporary problem while processing that action. Please try again in a moment.");
                }
                return;
            }

            if (isset($update['message']) && is_array($update['message'])) {
                $message = $update['message'];
                $chatId = (int) (($message['chat']['id'] ?? 0));
                $telegramUserId = (int) (($message['from']['id'] ?? 0));
                if ($chatId !== 0) {
                    self::safeReply(
                        $chatId,
                        "⚠️ I hit a temporary problem while processing that message. Nothing was lost on purpose, but please try again.",
                        self::mainMenuKeyboard(self::isLinked($telegramUserId))
                    );
                }
            }
        } catch (Throwable $exception) {
            Logger::warning('Failed to notify Telegram user about webhook failure.', [
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
