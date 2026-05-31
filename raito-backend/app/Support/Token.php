<?php

declare(strict_types=1);

namespace App\Support;

final class Token
{
    public static function deviceToken(): string
    {
        return Base64Url::encode(random_bytes(32));
    }

    public static function pairingCode(): string
    {
        $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
        $length = strlen($alphabet);
        $code = '';

        for ($i = 0; $i < 8; $i++) {
            $code .= $alphabet[random_int(0, $length - 1)];
        }

        return substr($code, 0, 4) . '-' . substr($code, 4, 4);
    }

    public static function webhookSecret(): string
    {
        return Base64Url::encode(random_bytes(32));
    }
}
