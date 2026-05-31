<?php

declare(strict_types=1);

namespace App\Support;

final class Request
{
    public static function method(): string
    {
        return strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
    }

    public static function path(): string
    {
        $uri = $_SERVER['REQUEST_URI'] ?? '/';
        $path = parse_url($uri, PHP_URL_PATH);

        if (!is_string($path) || $path === '') {
            return '/';
        }

        if ($path !== '/') {
            $path = rtrim($path, '/');
        }

        return $path;
    }

    public static function queryString(string $key, ?string $default = null): ?string
    {
        $value = $_GET[$key] ?? null;

        if (!is_string($value)) {
            return $default;
        }

        $value = trim($value);

        return $value === '' ? $default : $value;
    }

    public static function queryInt(string $key, int $default, int $min, int $max): int
    {
        $value = $_GET[$key] ?? null;

        if ($value === null || $value === '') {
            return $default;
        }

        if (!is_numeric($value)) {
            return $default;
        }

        $int = (int) $value;

        if ($int < $min) {
            return $min;
        }

        if ($int > $max) {
            return $max;
        }

        return $int;
    }

    public static function header(string $name): ?string
    {
        $serverKey = 'HTTP_' . strtoupper(str_replace('-', '_', $name));

        if (isset($_SERVER[$serverKey])) {
            return (string) $_SERVER[$serverKey];
        }

        if (strtolower($name) === 'content-type' && isset($_SERVER['CONTENT_TYPE'])) {
            return (string) $_SERVER['CONTENT_TYPE'];
        }

        if (strtolower($name) === 'authorization' && isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
            return (string) $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
        }

        return null;
    }

    public static function jsonBody(): array
    {
        $maxBytes = Env::int('MAX_JSON_BODY_BYTES', 262144);
        $contentLength = (int) ($_SERVER['CONTENT_LENGTH'] ?? 0);

        if ($contentLength > $maxBytes) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'payload_too_large',
                    'message' => 'Request body is too large.',
                ],
            ], 413);
        }

        $raw = file_get_contents('php://input');

        if ($raw === false || trim($raw) === '') {
            return [];
        }

        if (strlen($raw) > $maxBytes) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'payload_too_large',
                    'message' => 'Request body is too large.',
                ],
            ], 413);
        }

        $decoded = json_decode($raw, true);

        if (!is_array($decoded)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_json',
                    'message' => 'Request body must be valid JSON.',
                ],
            ], 400);
        }

        return $decoded;
    }

    public static function clientIp(): string
    {
        return $_SERVER['REMOTE_ADDR'] ?? 'unknown';
    }

    public static function userAgent(): string
    {
        return $_SERVER['HTTP_USER_AGENT'] ?? 'unknown';
    }

    public static function clientIpHash(): string
    {
        return hash('sha256', self::clientIp());
    }

    public static function userAgentHash(): string
    {
        return hash('sha256', self::userAgent());
    }
}
