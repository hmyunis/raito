<?php

declare(strict_types=1);

namespace App\Services;

use App\Support\Env;
use App\Support\Response;

final class AppUpdateService
{
    public static function android(): void
    {
        $latestVersion = self::nullableTrimmedString(Env::get('ANDROID_UPDATE_LATEST_VERSION'));
        $downloadUrl = self::nullableTrimmedString(Env::get('ANDROID_UPDATE_DOWNLOAD_URL'));
        $minSupportedVersion = self::nullableTrimmedString(Env::get('ANDROID_UPDATE_MIN_SUPPORTED_VERSION'));

        if ($latestVersion === null || $downloadUrl === null) {
            Response::json([
                'ok' => true,
                'update' => null,
            ]);
        }

        $releaseTitle = self::nullableTrimmedString(Env::get('ANDROID_UPDATE_RELEASE_TITLE'))
            ?? ('Version ' . $latestVersion);
        $publishedAt = self::nullableTrimmedString(Env::get('ANDROID_UPDATE_PUBLISHED_AT'));
        $releaseNotes = self::normalizeReleaseNotes(Env::get('ANDROID_UPDATE_RELEASE_NOTES', ''));

        Response::json([
            'ok' => true,
            'update' => [
                'platform' => 'android',
                'prompt_enabled' => Env::bool('ANDROID_UPDATE_PROMPT_ENABLED', false),
                'latest_version' => $latestVersion,
                'min_supported_version' => $minSupportedVersion,
                'download_url' => $downloadUrl,
                'release_title' => $releaseTitle,
                'release_notes' => $releaseNotes,
                'published_at' => $publishedAt,
            ],
        ]);
    }

    private static function normalizeReleaseNotes(mixed $value): array
    {
        $text = trim((string) $value);
        if ($text === '') {
            return [];
        }

        $decoded = str_replace('\n', "\n", $text);
        $segments = preg_split('/\r\n|\r|\n/', $decoded) ?: [];

        $notes = [];
        foreach ($segments as $segment) {
            $line = trim($segment);
            if ($line !== '') {
                $notes[] = $line;
            }
        }

        return $notes;
    }

    private static function nullableTrimmedString(mixed $value): ?string
    {
        $text = trim((string) $value);
        return $text === '' ? null : $text;
    }
}
