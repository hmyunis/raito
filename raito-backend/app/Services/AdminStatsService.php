<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Request;
use App\Support\Response;
use PDO;

final class AdminStatsService
{
    public static function stats(): void
    {
        self::requireAdmin();

        $pdo = Database::pdo();

        Response::json([
            'ok' => true,
            'users' => self::userStats($pdo),
            'telegram' => self::telegramStats($pdo),
            'remote_panels' => self::remotePanelStats($pdo),
            'sync' => self::syncStats($pdo),
            'events' => self::eventStats($pdo),
            'recent_users' => self::recentUsers($pdo),
            'recent_sync_batches' => self::recentSyncBatches($pdo),
        ]);
    }

    private static function requireAdmin(): void
    {
        $expected = (string) Env::get('ADMIN_TOKEN', '');
        $received = Request::header('Authorization');

        if ($expected === '' || $received === null || !preg_match('/^Bearer\s+(.+)$/i', trim($received), $matches) || !hash_equals($expected, trim($matches[1]))) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'forbidden',
                    'message' => 'Forbidden.',
                ],
            ], 403);
        }
    }

    private static function userStats(PDO $pdo): array
    {
        $row = self::singleRow($pdo, '
            SELECT
                COUNT(*) AS total_users,
                SUM(status = "active") AS active_users,
                SUM(status = "blocked") AS blocked_users,
                SUM(created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 DAY)) AS new_24h,
                SUM(created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS new_7d,
                SUM(created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)) AS new_30d,
                SUM(last_seen_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 DAY)) AS active_24h,
                SUM(last_seen_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS active_7d,
                SUM(last_seen_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)) AS active_30d
            FROM app_users
        ');

        return self::intValues($row);
    }

    private static function telegramStats(PDO $pdo): array
    {
        $row = self::singleRow($pdo, '
            SELECT
                COUNT(*) AS total_links,
                SUM(status = "active") AS active_links,
                SUM(linked_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 DAY)) AS linked_24h,
                SUM(linked_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS linked_7d,
                SUM(linked_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)) AS linked_30d
            FROM telegram_accounts
        ');

        return self::intValues($row);
    }

    private static function remotePanelStats(PDO $pdo): array
    {
        $byStatus = self::rows($pdo, '
            SELECT status, COUNT(*) AS count_value
            FROM remote_panels
            GROUP BY status
        ');

        $statusCounts = [
            'pending' => 0,
            'imported' => 0,
            'discarded' => 0,
        ];

        foreach ($byStatus as $row) {
            $status = (string) $row['status'];
            if (array_key_exists($status, $statusCounts)) {
                $statusCounts[$status] = (int) $row['count_value'];
            }
        }

        $recent = self::singleRow($pdo, '
            SELECT
                COUNT(*) AS created_24h,
                SUM(created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS created_7d,
                SUM(imported_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 DAY)) AS imported_24h,
                SUM(imported_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS imported_7d
            FROM remote_panels
            WHERE created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)
               OR imported_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)
        ');

        return [
            'by_status' => $statusCounts,
            'recent' => self::intValues($recent),
        ];
    }

    private static function syncStats(PDO $pdo): array
    {
        $byStatus = self::rows($pdo, '
            SELECT status, COUNT(*) AS count_value
            FROM sync_batches
            GROUP BY status
        ');

        $statusCounts = [
            'started' => 0,
            'completed' => 0,
            'failed' => 0,
        ];

        foreach ($byStatus as $row) {
            $status = (string) $row['status'];
            if (array_key_exists($status, $statusCounts)) {
                $statusCounts[$status] = (int) $row['count_value'];
            }
        }

        $recent = self::singleRow($pdo, '
            SELECT
                COUNT(*) AS syncs_24h,
                SUM(started_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS syncs_7d,
                SUM(status = "failed" AND started_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)) AS failures_7d,
                SUM(imported_count) AS total_imported_by_sync
            FROM sync_batches
            WHERE started_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY)
        ');

        return [
            'by_status' => $statusCounts,
            'recent' => self::intValues($recent),
        ];
    }

    private static function eventStats(PDO $pdo): array
    {
        $topEvents = self::rows($pdo, '
            SELECT
                event_name,
                COUNT(*) AS count_value
            FROM usage_events
            WHERE server_time_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)
            GROUP BY event_name
            ORDER BY count_value DESC
            LIMIT 25
        ');

        $dailyEvents = self::rows($pdo, '
            SELECT
                DATE(server_time_at) AS event_date,
                COUNT(*) AS count_value
            FROM usage_events
            WHERE server_time_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 14 DAY)
            GROUP BY DATE(server_time_at)
            ORDER BY event_date DESC
        ');

        return [
            'top_events_7d' => array_map(static function (array $row): array {
                return [
                    'event_name' => $row['event_name'],
                    'count' => (int) $row['count_value'],
                ];
            }, $topEvents),
            'daily_events_14d' => array_map(static function (array $row): array {
                return [
                    'date' => $row['event_date'],
                    'count' => (int) $row['count_value'],
                ];
            }, $dailyEvents),
        ];
    }

    private static function recentUsers(PDO $pdo): array
    {
        return self::rows($pdo, '
            SELECT
                public_id,
                display_name,
                device_label,
                status,
                last_seen_at,
                created_at
            FROM app_users
            ORDER BY created_at DESC
            LIMIT 10
        ');
    }

    private static function recentSyncBatches(PDO $pdo): array
    {
        return self::rows($pdo, '
            SELECT
                public_id AS sync_id,
                status,
                imported_count,
                app_version,
                error_message,
                started_at,
                completed_at
            FROM sync_batches
            ORDER BY started_at DESC
            LIMIT 10
        ');
    }

    private static function singleRow(PDO $pdo, string $sql): array
    {
        $statement = $pdo->query($sql);
        $row = $statement->fetch(PDO::FETCH_ASSOC);
        return is_array($row) ? $row : [];
    }

    private static function rows(PDO $pdo, string $sql): array
    {
        $statement = $pdo->query($sql);
        $rows = $statement->fetchAll(PDO::FETCH_ASSOC);
        return is_array($rows) ? $rows : [];
    }

    private static function intValues(array $row): array
    {
        $result = [];

        foreach ($row as $key => $value) {
            $result[$key] = (int) ($value ?? 0);
        }

        return $result;
    }
}
