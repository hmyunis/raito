<?php

declare(strict_types=1);

namespace App\Services;

use App\Support\Env;
use App\Support\Logger;

final class TelegramClient
{
    private string $botToken;

    public function __construct(?string $botToken = null)
    {
        $this->botToken = $botToken ?? (string) Env::get('TELEGRAM_BOT_TOKEN', '');

        if ($this->botToken === '') {
            throw new \RuntimeException('TELEGRAM_BOT_TOKEN is missing.');
        }
    }

    public function sendMessage(int|string $chatId, string $text, ?array $replyMarkup = null): array
    {
        $payload = [
            'chat_id' => $chatId,
            'text' => $text,
            'disable_web_page_preview' => true,
        ];

        if ($replyMarkup !== null) {
            $payload['reply_markup'] = $replyMarkup;
        }

        return $this->api('sendMessage', $payload);
    }

    public function setWebhook(string $url, string $secretToken): array
    {
        return $this->api('setWebhook', [
            'url' => $url,
            'secret_token' => $secretToken,
            'drop_pending_updates' => false,
            'allowed_updates' => ['message'],
        ]);
    }

    public function deleteWebhook(bool $dropPendingUpdates = false): array
    {
        return $this->api('deleteWebhook', [
            'drop_pending_updates' => $dropPendingUpdates,
        ]);
    }

    public function getWebhookInfo(): array
    {
        return $this->api('getWebhookInfo', []);
    }

    private function api(string $method, array $payload): array
    {
        $url = 'https://api.telegram.org/bot' . $this->botToken . '/' . $method;
        $json = json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

        if ($json === false) {
            throw new \RuntimeException('Failed to encode Telegram request JSON.');
        }

        $responseBody = null;
        $httpCode = 0;
        $connectTimeout = max(5, min(60, Env::int('TELEGRAM_CONNECT_TIMEOUT_SECONDS', 20)));
        $requestTimeout = max($connectTimeout + 5, min(120, Env::int('TELEGRAM_TIMEOUT_SECONDS', 45)));
        $retryAttempts = max(1, min(5, Env::int('TELEGRAM_RETRY_ATTEMPTS', 3)));

        if (function_exists('curl_init')) {
            $lastError = '';
            $transientErrors = [6, 7, 28, 35, 52, 56];

            for ($attempt = 1; $attempt <= $retryAttempts; $attempt++) {
                $curl = curl_init($url);
                if ($curl === false) {
                    throw new \RuntimeException('Failed to initialize cURL.');
                }

                curl_setopt_array($curl, [
                    CURLOPT_POST => true,
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
                    CURLOPT_POSTFIELDS => $json,
                    CURLOPT_CONNECTTIMEOUT => $connectTimeout,
                    CURLOPT_TIMEOUT => $requestTimeout,
                ]);

                $responseBody = curl_exec($curl);
                $httpCode = (int) curl_getinfo($curl, CURLINFO_HTTP_CODE);
                $errno = curl_errno($curl);
                $lastError = curl_error($curl);
                curl_close($curl);

                if ($responseBody !== false) {
                    break;
                }

                Logger::warning('Telegram cURL request failed', [
                    'method' => $method,
                    'attempt' => $attempt,
                    'max_attempts' => $retryAttempts,
                    'errno' => $errno,
                    'error' => $lastError,
                ]);

                if (!in_array($errno, $transientErrors, true) || $attempt === $retryAttempts) {
                    Logger::error('Telegram cURL request failed permanently', [
                        'method' => $method,
                        'errno' => $errno,
                        'error' => $lastError,
                    ]);
                    throw new \RuntimeException('Telegram request failed: ' . $lastError);
                }

                sleep($attempt);
            }
        } else {
            $context = stream_context_create([
                'http' => [
                    'method' => 'POST',
                    'header' => "Content-Type: application/json\r\n",
                    'content' => $json,
                    'timeout' => $requestTimeout,
                ],
            ]);

            $responseBody = file_get_contents($url, false, $context);
            if ($responseBody === false) {
                Logger::error('Telegram stream request failed', ['method' => $method]);
                throw new \RuntimeException('Telegram request failed.');
            }

            $httpCode = 200;
        }

        $decoded = json_decode((string) $responseBody, true);
        if (!is_array($decoded)) {
            Logger::error('Telegram returned invalid JSON', [
                'method' => $method,
                'http_code' => $httpCode,
                'body' => mb_substr((string) $responseBody, 0, 500, 'UTF-8'),
            ]);
            throw new \RuntimeException('Telegram returned invalid JSON.');
        }

        if (($decoded['ok'] ?? false) !== true) {
            Logger::warning('Telegram API returned non-ok response', [
                'method' => $method,
                'http_code' => $httpCode,
                'response' => $decoded,
            ]);
        }

        return $decoded;
    }
}
