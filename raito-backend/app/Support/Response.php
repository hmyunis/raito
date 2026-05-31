<?php

declare(strict_types=1);

namespace App\Support;

final class Response
{
    public static function json(array $data, int $status = 200, array $headers = []): never
    {
        http_response_code($status);

        self::sendDefaultHeaders();

        foreach ($headers as $name => $value) {
            header($name . ': ' . $value);
        }

        echo json_encode(
            $data,
            JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT
        );

        exit;
    }

    public static function empty(int $status = 204): never
    {
        http_response_code($status);
        self::sendDefaultHeaders();
        exit;
    }

    private static function sendDefaultHeaders(): void
    {
        header('Content-Type: application/json; charset=utf-8');
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('Referrer-Policy: no-referrer');
        header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
        header('Pragma: no-cache');
    }
}
