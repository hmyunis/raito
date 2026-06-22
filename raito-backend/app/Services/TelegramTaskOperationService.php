<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Response;
use App\Support\Uuid;
use PDO;
use Throwable;

final class TelegramTaskOperationService
{
    public static function listPending(array $user): void
    {
        $limit = self::sanitizeLimit($_GET['limit'] ?? null, 50, 1, 100);
        $pdo = Database::pdo();

        $statement = $pdo->prepare('
            SELECT
                public_id,
                operation_type,
                target_bucket_client_id,
                target_task_client_id,
                task_name,
                desired_completion,
                created_at
            FROM telegram_task_operations
            WHERE user_id = :user_id
              AND status = "pending"
            ORDER BY created_at ASC, id ASC
            LIMIT :limit_value
        ');
        $statement->bindValue('user_id', (int) $user['id'], PDO::PARAM_INT);
        $statement->bindValue('limit_value', $limit, PDO::PARAM_INT);
        $statement->execute();

        $operations = $statement->fetchAll(PDO::FETCH_ASSOC);

        Response::json([
            'ok' => true,
            'pending_count' => count($operations),
            'operations' => array_map(static function (array $operation): array {
                return [
                    'operation_id' => $operation['public_id'],
                    'operation_type' => $operation['operation_type'],
                    'target_bucket_client_id' => (int) $operation['target_bucket_client_id'],
                    'target_task_client_id' => isset($operation['target_task_client_id']) ? (int) $operation['target_task_client_id'] : null,
                    'task_name' => $operation['task_name'],
                    'desired_completion' => isset($operation['desired_completion']) ? ((int) $operation['desired_completion'] === 1) : null,
                    'created_at' => $operation['created_at'],
                ];
            }, $operations),
        ]);
    }

    public static function acknowledge(array $user, array $body): void
    {
        $results = self::extractAcknowledgements($body);
        $pdo = Database::pdo();
        $appliedCount = 0;
        $failedCount = 0;
        $ignoredCount = 0;

        try {
            $pdo->beginTransaction();

            foreach ($results as $result) {
                $status = self::acknowledgeOne($pdo, (int) $user['id'], $result);
                if ($status === 'applied') {
                    $appliedCount++;
                } elseif ($status === 'failed') {
                    $failedCount++;
                } else {
                    $ignoredCount++;
                }
            }

            $pdo->commit();
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }

            throw $exception;
        }

        Response::json([
            'ok' => true,
            'acknowledged_count' => count($results),
            'applied_count' => $appliedCount,
            'failed_count' => $failedCount,
            'ignored_count' => $ignoredCount,
        ]);
    }

    public static function queueCreateFromPanel(int $userId, int $telegramAccountId, string $panelPublicId, int $bucketClientId): array
    {
        $pdo = Database::pdo();

        try {
            $pdo->beginTransaction();

            $panelStatement = $pdo->prepare('
                SELECT id, public_id, content, status
                FROM remote_panels
                WHERE user_id = :user_id
                  AND public_id = :public_id
                LIMIT 1
                FOR UPDATE
            ');
            $panelStatement->execute([
                'user_id' => $userId,
                'public_id' => $panelPublicId,
            ]);
            $panel = $panelStatement->fetch(PDO::FETCH_ASSOC);

            if ($panel === false) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That inbox item is no longer available.'];
            }

            if ($panel['status'] !== 'pending') {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That inbox item was already handled.'];
            }

            if (!SyncedBucketService::bucketExistsForUser($userId, $bucketClientId)) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That bucket is no longer synced from the app.'];
            }

            $existingStatement = $pdo->prepare('
                SELECT public_id, status
                FROM telegram_task_operations
                WHERE source_remote_panel_id = :source_remote_panel_id
                LIMIT 1
                FOR UPDATE
            ');
            $existingStatement->execute(['source_remote_panel_id' => $panel['id']]);
            $existing = $existingStatement->fetch(PDO::FETCH_ASSOC);

            if ($existing !== false) {
                if ($existing['status'] === 'pending') {
                    $pdo->commit();
                    return ['ok' => true, 'message' => 'That inbox item is already queued for sync.'];
                }

                if ($existing['status'] === 'applied') {
                    $pdo->commit();
                    return ['ok' => true, 'message' => 'That inbox item already reached the app.'];
                }

                $update = $pdo->prepare('
                    UPDATE telegram_task_operations
                    SET
                        telegram_account_id = :telegram_account_id,
                        operation_type = "create_task",
                        target_bucket_client_id = :target_bucket_client_id,
                        target_task_client_id = NULL,
                        task_name = :task_name,
                        desired_completion = NULL,
                        status = "pending",
                        client_created_task_id = NULL,
                        error_message = NULL,
                        applied_at = NULL,
                        failed_at = NULL
                    WHERE source_remote_panel_id = :source_remote_panel_id
                ');
                $update->execute([
                    'telegram_account_id' => $telegramAccountId,
                    'target_bucket_client_id' => $bucketClientId,
                    'task_name' => self::limitTaskName((string) $panel['content']),
                    'source_remote_panel_id' => $panel['id'],
                ]);
            } else {
                $insert = $pdo->prepare('
                    INSERT INTO telegram_task_operations (
                        public_id,
                        user_id,
                        telegram_account_id,
                        source_remote_panel_id,
                        operation_type,
                        target_bucket_client_id,
                        target_task_client_id,
                        task_name,
                        desired_completion,
                        status
                    )
                    VALUES (
                        :public_id,
                        :user_id,
                        :telegram_account_id,
                        :source_remote_panel_id,
                        "create_task",
                        :target_bucket_client_id,
                        NULL,
                        :task_name,
                        NULL,
                        "pending"
                    )
                ');
                $insert->execute([
                    'public_id' => Uuid::v4(),
                    'user_id' => $userId,
                    'telegram_account_id' => $telegramAccountId,
                    'source_remote_panel_id' => $panel['id'],
                    'target_bucket_client_id' => $bucketClientId,
                    'task_name' => self::limitTaskName((string) $panel['content']),
                ]);
            }

            $pdo->commit();

            return ['ok' => true, 'message' => 'Queued for the app. Open Raito to sync it into that bucket.'];
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }

            throw $exception;
        }
    }

    public static function queueTaskCompletionToggle(
        int $userId,
        int $telegramAccountId,
        int $bucketClientId,
        int $taskClientId,
        bool $desiredCompletion
    ): array {
        $pdo = Database::pdo();

        try {
            $pdo->beginTransaction();

            $task = SyncedBucketService::findSyncedTaskForUpdate($pdo, $userId, $bucketClientId, $taskClientId);
            if ($task === null) {
                $pdo->commit();
                return ['ok' => false, 'message' => 'That task is no longer synced from the app.'];
            }

            $insert = $pdo->prepare('
                INSERT INTO telegram_task_operations (
                    public_id,
                    user_id,
                    telegram_account_id,
                    source_remote_panel_id,
                    operation_type,
                    target_bucket_client_id,
                    target_task_client_id,
                    task_name,
                    desired_completion,
                    status
                )
                VALUES (
                    :public_id,
                    :user_id,
                    :telegram_account_id,
                    NULL,
                    "set_task_completion",
                    :target_bucket_client_id,
                    :target_task_client_id,
                    :task_name,
                    :desired_completion,
                    "pending"
                )
            ');
            $insert->execute([
                'public_id' => Uuid::v4(),
                'user_id' => $userId,
                'telegram_account_id' => $telegramAccountId,
                'target_bucket_client_id' => $bucketClientId,
                'target_task_client_id' => $taskClientId,
                'task_name' => $task['name'],
                'desired_completion' => $desiredCompletion ? 1 : 0,
            ]);

            SyncedBucketService::applyTaskCompletionToSnapshot($pdo, (int) $task['synced_bucket_id'], $taskClientId, $desiredCompletion);
            $pdo->commit();

            return ['ok' => true, 'message' => $desiredCompletion ? 'Marked done. The app will sync it shortly.' : 'Marked undone. The app will sync it shortly.'];
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }

            throw $exception;
        }
    }

    public static function listPendingInbox(int $userId, int $page, int $perPage = 5): array
    {
        $page = max(1, $page);
        $perPage = max(1, min(10, $perPage));
        $offset = ($page - 1) * $perPage;
        $pdo = Database::pdo();

        $countStatement = $pdo->prepare('
            SELECT COUNT(*) AS inbox_count
            FROM remote_panels rp
            LEFT JOIN telegram_task_operations tto
                ON tto.source_remote_panel_id = rp.id
               AND tto.status IN ("pending", "applied")
            WHERE rp.user_id = :user_id
              AND rp.status = "pending"
              AND tto.id IS NULL
        ');
        $countStatement->execute(['user_id' => $userId]);
        $totalCount = (int) (($countStatement->fetch(PDO::FETCH_ASSOC)['inbox_count'] ?? 0));

        $statement = $pdo->prepare('
            SELECT
                rp.public_id,
                rp.content,
                rp.created_at
            FROM remote_panels rp
            LEFT JOIN telegram_task_operations tto
                ON tto.source_remote_panel_id = rp.id
               AND tto.status IN ("pending", "applied")
            WHERE rp.user_id = :user_id
              AND rp.status = "pending"
              AND tto.id IS NULL
            ORDER BY rp.created_at DESC, rp.id DESC
            LIMIT :limit_value OFFSET :offset_value
        ');
        $statement->bindValue('user_id', $userId, PDO::PARAM_INT);
        $statement->bindValue('limit_value', $perPage, PDO::PARAM_INT);
        $statement->bindValue('offset_value', $offset, PDO::PARAM_INT);
        $statement->execute();

        return [
            'page' => $page,
            'per_page' => $perPage,
            'total_count' => $totalCount,
            'has_prev' => $page > 1,
            'has_next' => ($offset + $perPage) < $totalCount,
            'items' => $statement->fetchAll(PDO::FETCH_ASSOC),
        ];
    }

    public static function findPendingInboxItem(int $userId, string $panelPublicId): ?array
    {
        $pdo = Database::pdo();
        $statement = $pdo->prepare('
            SELECT
                rp.public_id,
                rp.content,
                rp.created_at
            FROM remote_panels rp
            LEFT JOIN telegram_task_operations tto
                ON tto.source_remote_panel_id = rp.id
               AND tto.status IN ("pending", "applied")
            WHERE rp.user_id = :user_id
              AND rp.public_id = :public_id
              AND rp.status = "pending"
              AND tto.id IS NULL
            LIMIT 1
        ');
        $statement->execute([
            'user_id' => $userId,
            'public_id' => $panelPublicId,
        ]);
        $item = $statement->fetch(PDO::FETCH_ASSOC);

        return $item === false ? null : $item;
    }

    private static function acknowledgeOne(PDO $pdo, int $userId, array $result): string
    {
        $select = $pdo->prepare('
            SELECT id, status, operation_type, source_remote_panel_id
            FROM telegram_task_operations
            WHERE user_id = :user_id
              AND public_id = :public_id
            LIMIT 1
            FOR UPDATE
        ');
        $select->execute([
            'user_id' => $userId,
            'public_id' => $result['operation_id'],
        ]);
        $operation = $select->fetch(PDO::FETCH_ASSOC);
        if ($operation === false) {
            return 'ignored';
        }

        $status = $result['status'];
        $update = $pdo->prepare('
            UPDATE telegram_task_operations
            SET
                status = :status,
                client_created_task_id = :client_created_task_id,
                error_message = :error_message,
                applied_at = :applied_at,
                failed_at = :failed_at
            WHERE id = :id
        ');
        $update->execute([
            'status' => $status,
            'client_created_task_id' => $result['client_created_task_id'],
            'error_message' => $result['error_message'],
            'applied_at' => $status === 'applied' ? gmdate('Y-m-d H:i:s') : null,
            'failed_at' => $status === 'failed' ? gmdate('Y-m-d H:i:s') : null,
            'id' => $operation['id'],
        ]);

        if ($status === 'applied' && $operation['operation_type'] === 'create_task' && $operation['source_remote_panel_id'] !== null) {
            $panelUpdate = $pdo->prepare('
                UPDATE remote_panels
                SET
                    status = "imported",
                    imported_at = UTC_TIMESTAMP()
                WHERE id = :id
                  AND status = "pending"
            ');
            $panelUpdate->execute(['id' => $operation['source_remote_panel_id']]);
        }

        return $status;
    }

    private static function extractAcknowledgements(array $body): array
    {
        $raw = $body['operations'] ?? null;
        if (!is_array($raw)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_operations',
                    'message' => 'operations must be an array.',
                ],
            ], 400);
        }

        $results = [];
        foreach ($raw as $row) {
            if (!is_array($row)) {
                continue;
            }

            $operationId = self::nullableUuid($row['operation_id'] ?? null);
            $status = self::ackStatus($row['status'] ?? null);
            if ($operationId === null || $status === null) {
                continue;
            }

            $results[] = [
                'operation_id' => $operationId,
                'status' => $status,
                'client_created_task_id' => self::nullablePositiveInt($row['client_created_task_id'] ?? null),
                'error_message' => self::nullableString($row['error_message'] ?? null, 500),
            ];
        }

        if ($results === []) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'empty_operations',
                    'message' => 'At least one valid operation acknowledgement is required.',
                ],
            ], 400);
        }

        return $results;
    }

    private static function sanitizeLimit(mixed $value, int $default, int $min, int $max): int
    {
        if (!is_numeric($value)) {
            return $default;
        }

        return max($min, min($max, (int) $value));
    }

    private static function limitTaskName(string $value): string
    {
        $value = trim($value);
        if (mb_strlen($value, 'UTF-8') > 240) {
            $value = mb_substr($value, 0, 240, 'UTF-8');
        }

        return $value;
    }

    private static function nullableUuid(mixed $value): ?string
    {
        if (!is_string($value)) {
            return null;
        }

        $value = trim($value);
        return preg_match('/^[0-9a-fA-F-]{36}$/', $value) === 1 ? $value : null;
    }

    private static function ackStatus(mixed $value): ?string
    {
        if (!is_string($value)) {
            return null;
        }

        $value = trim($value);
        return in_array($value, ['applied', 'failed', 'ignored'], true) ? $value : null;
    }

    private static function nullablePositiveInt(mixed $value): ?int
    {
        if (!is_int($value) && !is_numeric($value)) {
            return null;
        }

        $int = (int) $value;
        return $int > 0 ? $int : null;
    }

    private static function nullableString(mixed $value, int $maxLength): ?string
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
}
