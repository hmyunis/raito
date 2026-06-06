<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Response;
use PDO;
use Throwable;

final class SyncedBucketService
{
    public static function syncSnapshot(array $user, array $body): void
    {
        $buckets = self::extractBuckets($body);
        $pdo = Database::pdo();
        $userId = (int) $user['id'];

        try {
            $pdo->beginTransaction();

            $existingBucketsStatement = $pdo->prepare('
                SELECT id, client_bucket_id
                FROM synced_buckets
                WHERE user_id = :user_id
                FOR UPDATE
            ');
            $existingBucketsStatement->execute(['user_id' => $userId]);
            $existingBuckets = $existingBucketsStatement->fetchAll(PDO::FETCH_ASSOC);
            $bucketIdByClientId = [];
            foreach ($existingBuckets as $row) {
                $bucketIdByClientId[(int) $row['client_bucket_id']] = (int) $row['id'];
            }

            $upsertBucket = $pdo->prepare('
                INSERT INTO synced_buckets (
                    user_id,
                    client_bucket_id,
                    name,
                    discipline,
                    companion_id,
                    aura_ink,
                    deadline,
                    is_completed,
                    client_timestamp,
                    synced_at
                )
                VALUES (
                    :user_id,
                    :client_bucket_id,
                    :name,
                    :discipline,
                    :companion_id,
                    :aura_ink,
                    :deadline,
                    :is_completed,
                    :client_timestamp,
                    UTC_TIMESTAMP()
                )
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    discipline = VALUES(discipline),
                    companion_id = VALUES(companion_id),
                    aura_ink = VALUES(aura_ink),
                    deadline = VALUES(deadline),
                    is_completed = VALUES(is_completed),
                    client_timestamp = VALUES(client_timestamp),
                    synced_at = UTC_TIMESTAMP()
            ');

            $selectBucketId = $pdo->prepare('
                SELECT id
                FROM synced_buckets
                WHERE user_id = :user_id
                  AND client_bucket_id = :client_bucket_id
                LIMIT 1
            ');

            $upsertTask = $pdo->prepare('
                INSERT INTO synced_bucket_tasks (
                    synced_bucket_id,
                    client_task_id,
                    name,
                    time_remaining,
                    is_completed,
                    is_overdue,
                    description,
                    due_datetime,
                    is_pinned,
                    client_created_at,
                    synced_at
                )
                VALUES (
                    :synced_bucket_id,
                    :client_task_id,
                    :name,
                    :time_remaining,
                    :is_completed,
                    :is_overdue,
                    :description,
                    :due_datetime,
                    :is_pinned,
                    :client_created_at,
                    UTC_TIMESTAMP()
                )
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    time_remaining = VALUES(time_remaining),
                    is_completed = VALUES(is_completed),
                    is_overdue = VALUES(is_overdue),
                    description = VALUES(description),
                    due_datetime = VALUES(due_datetime),
                    is_pinned = VALUES(is_pinned),
                    client_created_at = VALUES(client_created_at),
                    synced_at = UTC_TIMESTAMP()
            ');

            $deleteAllTasksForBucket = $pdo->prepare('
                DELETE FROM synced_bucket_tasks
                WHERE synced_bucket_id = :synced_bucket_id
            ');

            $retainedBucketClientIds = [];
            $syncedTaskCount = 0;

            foreach ($buckets as $bucket) {
                $retainedBucketClientIds[] = $bucket['chapter_id'];

                $upsertBucket->execute([
                    'user_id' => $userId,
                    'client_bucket_id' => $bucket['chapter_id'],
                    'name' => $bucket['name'],
                    'discipline' => $bucket['discipline'],
                    'companion_id' => $bucket['companion_id'],
                    'aura_ink' => $bucket['aura_ink'],
                    'deadline' => $bucket['deadline'],
                    'is_completed' => $bucket['is_completed'] ? 1 : 0,
                    'client_timestamp' => $bucket['timestamp'],
                ]);

                $syncedBucketId = $bucketIdByClientId[$bucket['chapter_id']] ?? null;
                if ($syncedBucketId === null) {
                    $selectBucketId->execute([
                        'user_id' => $userId,
                        'client_bucket_id' => $bucket['chapter_id'],
                    ]);
                    $syncedBucketId = (int) ($selectBucketId->fetchColumn() ?: 0);
                    $bucketIdByClientId[$bucket['chapter_id']] = $syncedBucketId;
                }

                if ($syncedBucketId <= 0) {
                    continue;
                }

                $retainedTaskIds = [];
                foreach ($bucket['tasks'] as $task) {
                    $retainedTaskIds[] = $task['task_id'];
                    $syncedTaskCount++;
                    $upsertTask->execute([
                        'synced_bucket_id' => $syncedBucketId,
                        'client_task_id' => $task['task_id'],
                        'name' => $task['name'],
                        'time_remaining' => $task['time_remaining'],
                        'is_completed' => $task['is_completed'] ? 1 : 0,
                        'is_overdue' => $task['is_overdue'] ? 1 : 0,
                        'description' => $task['description'],
                        'due_datetime' => $task['due_datetime'],
                        'is_pinned' => $task['is_pinned'] ? 1 : 0,
                        'client_created_at' => $task['created_at'],
                    ]);
                }

                if ($retainedTaskIds === []) {
                    $deleteAllTasksForBucket->execute(['synced_bucket_id' => $syncedBucketId]);
                } else {
                    $placeholders = implode(',', array_fill(0, count($retainedTaskIds), '?'));
                    $statement = $pdo->prepare(sprintf(
                        'DELETE FROM synced_bucket_tasks WHERE synced_bucket_id = ? AND client_task_id NOT IN (%s)',
                        $placeholders
                    ));
                    $statement->execute(array_merge([$syncedBucketId], $retainedTaskIds));
                }
            }

            if ($retainedBucketClientIds === []) {
                $clearBuckets = $pdo->prepare('DELETE FROM synced_buckets WHERE user_id = :user_id');
                $clearBuckets->execute(['user_id' => $userId]);
            } else {
                $placeholders = implode(',', array_fill(0, count($retainedBucketClientIds), '?'));
                $deleteBuckets = $pdo->prepare(sprintf(
                    'DELETE FROM synced_buckets WHERE user_id = ? AND client_bucket_id NOT IN (%s)',
                    $placeholders
                ));
                $deleteBuckets->execute(array_merge([$userId], $retainedBucketClientIds));
            }

            $pdo->commit();

            Response::json([
                'ok' => true,
                'synced_bucket_count' => count($buckets),
                'synced_task_count' => $syncedTaskCount,
            ]);
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }

            throw $exception;
        }
    }

    public static function findActiveAccountByTelegramUser(int $telegramUserId): ?array
    {
        $pdo = Database::pdo();
        $statement = $pdo->prepare('
            SELECT
                ta.id AS telegram_account_id,
                au.id AS user_id,
                au.public_id
            FROM telegram_accounts ta
            INNER JOIN app_users au ON au.id = ta.user_id
            WHERE ta.telegram_user_id = :telegram_user_id
              AND ta.status = "active"
              AND au.status = "active"
            LIMIT 1
        ');
        $statement->execute(['telegram_user_id' => $telegramUserId]);
        $account = $statement->fetch(PDO::FETCH_ASSOC);

        return $account === false ? null : $account;
    }

    public static function countBucketsForUser(int $userId): int
    {
        $pdo = Database::pdo();
        $statement = $pdo->prepare('SELECT COUNT(*) FROM synced_buckets WHERE user_id = :user_id');
        $statement->execute(['user_id' => $userId]);

        return (int) ($statement->fetchColumn() ?: 0);
    }

    public static function listBucketsForUser(int $userId, int $page, int $perPage = 6): array
    {
        $page = max(1, $page);
        $perPage = max(1, min(12, $perPage));
        $offset = ($page - 1) * $perPage;
        $pdo = Database::pdo();

        $count = self::countBucketsForUser($userId);
        $statement = $pdo->prepare('
            SELECT
                sb.client_bucket_id,
                sb.name,
                sb.discipline,
                sb.deadline,
                sb.is_completed,
                sb.synced_at,
                COUNT(sbt.id) AS task_count,
                SUM(CASE WHEN sbt.is_completed = 1 THEN 1 ELSE 0 END) AS completed_task_count
            FROM synced_buckets sb
            LEFT JOIN synced_bucket_tasks sbt ON sbt.synced_bucket_id = sb.id
            WHERE sb.user_id = :user_id
            GROUP BY sb.id
            ORDER BY sb.is_completed ASC, sb.updated_at DESC, sb.id DESC
            LIMIT :limit_value OFFSET :offset_value
        ');
        $statement->bindValue('user_id', $userId, PDO::PARAM_INT);
        $statement->bindValue('limit_value', $perPage, PDO::PARAM_INT);
        $statement->bindValue('offset_value', $offset, PDO::PARAM_INT);
        $statement->execute();

        return [
            'page' => $page,
            'per_page' => $perPage,
            'total_count' => $count,
            'has_prev' => $page > 1,
            'has_next' => ($offset + $perPage) < $count,
            'buckets' => $statement->fetchAll(PDO::FETCH_ASSOC),
        ];
    }

    public static function getBucketWithTasks(int $userId, int $clientBucketId, int $page, int $perPage = 8): ?array
    {
        $page = max(1, $page);
        $perPage = max(1, min(20, $perPage));
        $offset = ($page - 1) * $perPage;
        $pdo = Database::pdo();

        $bucketStatement = $pdo->prepare('
            SELECT
                id,
                client_bucket_id,
                name,
                discipline,
                companion_id,
                aura_ink,
                deadline,
                is_completed,
                synced_at
            FROM synced_buckets
            WHERE user_id = :user_id
              AND client_bucket_id = :client_bucket_id
            LIMIT 1
        ');
        $bucketStatement->execute([
            'user_id' => $userId,
            'client_bucket_id' => $clientBucketId,
        ]);
        $bucket = $bucketStatement->fetch(PDO::FETCH_ASSOC);
        if ($bucket === false) {
            return null;
        }

        $countStatement = $pdo->prepare('
            SELECT COUNT(*) AS task_count
            FROM synced_bucket_tasks
            WHERE synced_bucket_id = :synced_bucket_id
        ');
        $countStatement->execute(['synced_bucket_id' => $bucket['id']]);
        $taskCount = (int) (($countStatement->fetch(PDO::FETCH_ASSOC)['task_count'] ?? 0));

        $taskStatement = $pdo->prepare('
            SELECT
                client_task_id,
                name,
                time_remaining,
                is_completed,
                is_overdue,
                description,
                due_datetime,
                is_pinned,
                synced_at
            FROM synced_bucket_tasks
            WHERE synced_bucket_id = :synced_bucket_id
            ORDER BY is_pinned DESC, is_completed ASC, COALESCE(client_created_at, 0) DESC, id DESC
            LIMIT :limit_value OFFSET :offset_value
        ');
        $taskStatement->bindValue('synced_bucket_id', (int) $bucket['id'], PDO::PARAM_INT);
        $taskStatement->bindValue('limit_value', $perPage, PDO::PARAM_INT);
        $taskStatement->bindValue('offset_value', $offset, PDO::PARAM_INT);
        $taskStatement->execute();

        return [
            'bucket' => $bucket,
            'page' => $page,
            'per_page' => $perPage,
            'total_count' => $taskCount,
            'has_prev' => $page > 1,
            'has_next' => ($offset + $perPage) < $taskCount,
            'tasks' => $taskStatement->fetchAll(PDO::FETCH_ASSOC),
        ];
    }

    private static function extractBuckets(array $body): array
    {
        $rawBuckets = $body['buckets'] ?? null;
        if (!is_array($rawBuckets)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_buckets',
                    'message' => 'buckets must be an array of synced bucket snapshots.',
                ],
            ], 400);
        }

        $maxBuckets = max(0, min(100, Env::int('MAX_SYNCED_BUCKETS', 30)));
        if (count($rawBuckets) > $maxBuckets) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'too_many_buckets',
                    'message' => 'Too many synced buckets were sent.',
                ],
            ], 400);
        }

        $maxTasksPerBucket = max(1, min(1000, Env::int('MAX_SYNCED_BUCKET_TASKS', 250)));
        $buckets = [];

        foreach ($rawBuckets as $rawBucket) {
            if (!is_array($rawBucket)) {
                continue;
            }

            $chapterId = self::positiveInt($rawBucket['chapter_id'] ?? null);
            $name = self::limitedString($rawBucket['name'] ?? null, 160);
            if ($chapterId === null || $name === null) {
                continue;
            }

            $rawTasks = $rawBucket['tasks'] ?? [];
            if (!is_array($rawTasks) || count($rawTasks) > $maxTasksPerBucket) {
                Response::json([
                    'ok' => false,
                    'error' => [
                        'code' => 'invalid_bucket_tasks',
                        'message' => 'Each synced bucket must include a valid tasks array within size limits.',
                    ],
                ], 400);
            }

            $tasks = [];
            foreach ($rawTasks as $rawTask) {
                if (!is_array($rawTask)) {
                    continue;
                }

                $taskId = self::positiveInt($rawTask['task_id'] ?? null);
                $taskName = self::limitedString($rawTask['name'] ?? null, 240);
                if ($taskId === null || $taskName === null) {
                    continue;
                }

                $tasks[] = [
                    'task_id' => $taskId,
                    'name' => $taskName,
                    'time_remaining' => self::limitedString($rawTask['time_remaining'] ?? null, 40),
                    'is_completed' => self::boolValue($rawTask['is_completed'] ?? false),
                    'is_overdue' => self::boolValue($rawTask['is_overdue'] ?? false),
                    'description' => self::limitedString($rawTask['description'] ?? null, 4000),
                    'due_datetime' => self::limitedString($rawTask['due_datetime'] ?? null, 80),
                    'created_at' => self::nonNegativeInt($rawTask['created_at'] ?? null),
                    'is_pinned' => self::boolValue($rawTask['is_pinned'] ?? false),
                ];
            }

            $buckets[] = [
                'chapter_id' => $chapterId,
                'name' => $name,
                'discipline' => self::limitedString($rawBucket['discipline'] ?? null, 80),
                'companion_id' => self::limitedString($rawBucket['companion_id'] ?? null, 80),
                'aura_ink' => self::limitedString($rawBucket['aura_ink'] ?? null, 32),
                'deadline' => self::limitedString($rawBucket['deadline'] ?? null, 80),
                'is_completed' => self::boolValue($rawBucket['is_completed'] ?? false),
                'timestamp' => self::nonNegativeInt($rawBucket['timestamp'] ?? null),
                'tasks' => $tasks,
            ];
        }

        return $buckets;
    }

    private static function positiveInt(mixed $value): ?int
    {
        if (!is_int($value) && !is_numeric($value)) {
            return null;
        }

        $int = (int) $value;
        return $int > 0 ? $int : null;
    }

    private static function nonNegativeInt(mixed $value): ?int
    {
        if (!is_int($value) && !is_numeric($value)) {
            return null;
        }

        $int = (int) $value;
        return $int >= 0 ? $int : null;
    }

    private static function limitedString(mixed $value, int $maxLength): ?string
    {
        if (!is_string($value)) {
            return null;
        }

        $value = trim($value);
        if ($value === '') {
            return null;
        }

        if (mb_strlen($value, 'UTF-8') > $maxLength) {
            $value = mb_substr($value, 0, $maxLength, 'UTF-8');
        }

        return $value;
    }

    private static function boolValue(mixed $value): bool
    {
        if (is_bool($value)) {
            return $value;
        }

        if (is_int($value) || is_float($value)) {
            return $value !== 0;
        }

        if (is_string($value)) {
            return in_array(strtolower(trim($value)), ['1', 'true', 'yes', 'on'], true);
        }

        return false;
    }
}
