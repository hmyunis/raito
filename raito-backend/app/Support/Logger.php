<?php

declare(strict_types=1);

namespace App\Support;

final class Logger
{
    public static function info(string $message, array $context = []): void
    {
        self::write('info', $message, $context);
    }

    public static function warning(string $message, array $context = []): void
    {
        self::write('warning', $message, $context);
    }

    public static function error(string $message, array $context = []): void
    {
        self::write('error', $message, $context);
    }

    private static function write(string $level, string $message, array $context): void
    {
        $logDir = BASE_PATH . '/storage/logs';

        if (!is_dir($logDir)) {
            mkdir($logDir, 0775, true);
        }

        $file = $logDir . '/app-' . gmdate('Y-m-d') . '.log';

        $record = [
            'timestamp_utc' => gmdate('c'),
            'level' => $level,
            'message' => $message,
            'context' => self::maskSensitiveData($context),
        ];

        file_put_contents(
            $file,
            json_encode($record, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) . PHP_EOL,
            FILE_APPEND | LOCK_EX
        );
    }

    private static function maskSensitiveData(array $context): array
    {
        $sensitiveKeys = [
            'token',
            'bot_token',
            'password',
            'secret',
            'authorization',
            'api_key',
        ];

        foreach ($context as $key => $value) {
            $lowerKey = strtolower((string) $key);

            foreach ($sensitiveKeys as $sensitiveKey) {
                if (str_contains($lowerKey, $sensitiveKey)) {
                    $context[$key] = '[masked]';
                    continue 2;
                }
            }

            if (is_array($value)) {
                $context[$key] = self::maskSensitiveData($value);
            }
        }

        return $context;
    }
}
