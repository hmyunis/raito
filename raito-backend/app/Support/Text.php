<?php

declare(strict_types=1);

namespace App\Support;

final class Text
{
    public static function cleanUserText(string $text): string
    {
        $text = trim($text);
        $text = preg_replace("/\r\n|\r/", "\n", $text) ?? $text;
        $text = preg_replace("/[ \t]+/", " ", $text) ?? $text;
        $text = preg_replace("/\n{3,}/", "\n\n", $text) ?? $text;

        return trim($text);
    }

    public static function limit(string $text, int $maxChars): string
    {
        if ($maxChars <= 0) {
            return '';
        }

        if (mb_strlen($text, 'UTF-8') <= $maxChars) {
            return $text;
        }

        return mb_substr($text, 0, $maxChars, 'UTF-8');
    }

    public static function startsWithCommand(string $text, string $command): bool
    {
        $text = trim($text);

        return $text === $command || str_starts_with($text, $command . ' ');
    }

    public static function commandArgument(string $text, string $command): string
    {
        $text = trim($text);

        if ($text === $command) {
            return '';
        }

        if (!str_starts_with($text, $command . ' ')) {
            return '';
        }

        return trim(substr($text, strlen($command)));
    }
}
