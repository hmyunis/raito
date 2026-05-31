<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Text;
use App\Support\Uuid;
use PDO;
use Throwable;

final class TelegramCaptureService
{
    public static function captureMessage(array $message): array
    {
        $chat = $message['chat'] ?? [];
        $from = $message['from'] ?? [];

        $chatId = (int) ($chat['id'] ?? 0);
        $telegramUserId = (int) ($from['id'] ?? 0);
        $messageId = (int) ($message['message_id'] ?? 0);
        $chatType = (string) ($chat['type'] ?? '');

        if ($chatId === 0 || $telegramUserId === 0 || $messageId === 0) {
            return ['ok' => false, 'reply' => 'I could not read this message correctly.', 'status' => 'ignored'];
        }

        if ($chatType !== 'private') {
            return ['ok' => false, 'reply' => null, 'status' => 'ignored'];
        }

        $text = '';
        if (isset($message['text']) && is_string($message['text'])) {
            $text = Text::cleanUserText($message['text']);
        }

        if ($text === '') {
            return ['ok' => false, 'reply' => 'Text messages only for now. Send a text panel, not media.', 'status' => 'ignored'];
        }

        $pdo = Database::pdo();

        $accountStatement = $pdo->prepare('
            SELECT
                ta.id AS telegram_account_id,
                ta.user_id,
                au.status AS user_status
            FROM telegram_accounts ta
            INNER JOIN app_users au ON au.id = ta.user_id
            WHERE ta.telegram_user_id = :telegram_user_id
              AND ta.status = "active"
              AND au.status = "active"
            LIMIT 1
        ');
        $accountStatement->execute(['telegram_user_id' => $telegramUserId]);
        $account = $accountStatement->fetch(PDO::FETCH_ASSOC);

        if ($account === false) {
            return ['ok' => false, 'reply' => "Raito is not linked yet.\n\nOpen Raito → Settings → Telegram Capture → Generate Code, then send:\n/link YOUR-CODE", 'status' => 'ignored'];
        }

        $maxChars = max(1, Env::int('REMOTE_PANEL_MAX_CHARS', 4000));
        $content = Text::limit($text, $maxChars);
        $contentHash = hash('sha256', $content);
        $publicId = Uuid::v4();

        try {
            $statement = $pdo->prepare('
                INSERT INTO remote_panels (
                    public_id,
                    user_id,
                    telegram_account_id,
                    source,
                    source_chat_id,
                    source_message_id,
                    content,
                    content_hash,
                    status,
                    imported_at,
                    discarded_at
                )
                VALUES (
                    :public_id,
                    :user_id,
                    :telegram_account_id,
                    "telegram",
                    :source_chat_id,
                    :source_message_id,
                    :content,
                    :content_hash,
                    "pending",
                    NULL,
                    NULL
                )
            ');

            $statement->execute([
                'public_id' => $publicId,
                'user_id' => $account['user_id'],
                'telegram_account_id' => $account['telegram_account_id'],
                'source_chat_id' => $chatId,
                'source_message_id' => $messageId,
                'content' => $content,
                'content_hash' => $contentHash,
            ]);
        } catch (Throwable $exception) {
            if ($exception instanceof \PDOException && $exception->getCode() === '23000') {
                return ['ok' => true, 'reply' => null, 'status' => 'ignored'];
            }

            throw $exception;
        }

        return ['ok' => true, 'reply' => 'Captured. It will remain pending until you sync in the app.', 'status' => 'processed'];
    }
}
