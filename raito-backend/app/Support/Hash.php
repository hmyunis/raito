<?php

declare(strict_types=1);

namespace App\Support;

final class Hash
{
    public static function token(string $plain): string
    {
        return hash('sha256', $plain);
    }

    public static function pairingCode(string $code): string
    {
        $appKey = (string) Env::get('APP_KEY', '');

        if ($appKey === '') {
            throw new \RuntimeException('APP_KEY is missing.');
        }

        return hash_hmac('sha256', self::normalizePairingCode($code), $appKey);
    }

    public static function normalizePairingCode(string $code): string
    {
        $code = strtoupper(trim($code));
        $code = str_replace([' ', '-', '_'], '', $code);

        return $code;
    }
}
