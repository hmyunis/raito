<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Request;
use App\Support\Response;
use App\Support\Uuid;
use PDO;
use Throwable;

final class PanelSyncService
{
    public static function pendingPanels(array $user): void
    {
        $limit = Request::queryInt('limit', 50, 1, 100);
        $pdo = Database::pdo();

        $statement = $pdo->prepare('
            SELECT
                public_id,
                source,
                content,
                content_hash,
                created_at
            FROM remote_panels
            WHERE user_id = :user_id
              AND status = "pending"
            ORDER BY created_at ASC, id ASC
            LIMIT :limit_value
        ');

        $statement->bindValue('user_id', (int) $user['id'], PDO::PARAM_INT);
        $statement->bindValue('limit_value', $limit, PDO::PARAM_INT);
        $statement->execute();

        $panels = $statement->fetchAll(PDO::FETCH_ASSOC);

        $countStatement = $pdo->prepare('
            SELECT COUNT(*) AS pending_count
            FROM remote_panels
            WHERE user_id = :user_id
              AND status = "pending"
        ');
        $countStatement->execute(['user_id' => $user['id']]);
        $countRow = $countStatement->fetch(PDO::FETCH_ASSOC);
        $pendingCount = (int) ($countRow['pending_count'] ?? 0);

        Response::json([
            'ok' => true,
            'pending_count' => $pendingCount,
            'returned_count' => count($panels),
            'panels' => array_map(static function (array $panel): array {
                return [
                    'remote_panel_id' => $panel['public_id'],
                    'source' => $panel['source'],
                    'content' => $panel['content'],
                    'content_hash' => $panel['content_hash'],
                    'created_at' => $panel['created_at'],
                ];
            }, $panels),
        ]);
    }

    public static function markImported(array $user, array $body): void
    {
        $panelIds = self::extractPanelIds($body);
        $clientSyncId = self::nullableClientId($body['client_sync_id'] ?? null);

        $pdo = Database::pdo();
        $syncPublicId = $clientSyncId ?? Uuid::v4();

        try {
            $pdo->beginTransaction();

            $batchInsert = $pdo->prepare('
                INSERT INTO sync_batches (
                    user_id,
                    public_id,
                    status,
                    imported_count,
                    app_version
                )
                VALUES (
                    :user_id,
                    :public_id,
                    "started",
                    0,
                    :app_version
                )
            ');
            $batchInsert->execute([
                'user_id' => $user['id'],
                'public_id' => $syncPublicId,
                'app_version' => self::nullableString($body['app_version'] ?? null, 60),
            ]);

            $results = [];
            $importedCount = 0;

            foreach ($panelIds as $panelPublicId) {
                $result = self::markOnePanelImported($pdo, (int) $user['id'], $panelPublicId);
                if ($result['status'] === 'imported') {
                    $importedCount++;
                }

                $results[] = $result;
            }

            $batchUpdate = $pdo->prepare('
                UPDATE sync_batches
                SET
                    status = "completed",
                    imported_count = :imported_count,
                    completed_at = UTC_TIMESTAMP()
                WHERE public_id = :public_id
                  AND user_id = :user_id
            ');
            $batchUpdate->execute([
                'imported_count' => $importedCount,
                'public_id' => $syncPublicId,
                'user_id' => $user['id'],
            ]);

            $pdo->commit();

            Response::json([
                'ok' => true,
                'sync' => [
                    'sync_id' => $syncPublicId,
                    'requested_count' => count($panelIds),
                    'imported_count' => $importedCount,
                ],
                'results' => $results,
            ]);
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }

            self::recordFailedBatch((int) $user['id'], $syncPublicId, $exception->getMessage(), self::nullableString($body['app_version'] ?? null, 60));
            throw $exception;
        }
    }

    public static function discardPanels(array $user, array $body): void
    {
        $panelIds = self::extractPanelIds($body);
        $pdo = Database::pdo();

        try {
            $pdo->beginTransaction();

            $results = [];
            $discardedCount = 0;

            foreach ($panelIds as $panelPublicId) {
                $result = self::discardOnePanel($pdo, (int) $user['id'], $panelPublicId);
                if ($result['status'] === 'discarded') {
                    $discardedCount++;
                }

                $results[] = $result;
            }

            $pdo->commit();

            Response::json([
                'ok' => true,
                'requested_count' => count($panelIds),
                'discarded_count' => $discardedCount,
                'results' => $results,
            ]);
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }

            throw $exception;
        }
    }

    private static function markOnePanelImported(PDO $pdo, int $userId, string $panelPublicId): array
    {
        $select = $pdo->prepare('
            SELECT id, public_id, status
            FROM remote_panels
            WHERE user_id = :user_id
              AND public_id = :public_id
            LIMIT 1
            FOR UPDATE
        ');
        $select->execute(['user_id' => $userId, 'public_id' => $panelPublicId]);
        $panel = $select->fetch(PDO::FETCH_ASSOC);

        if ($panel === false) {
            return ['remote_panel_id' => $panelPublicId, 'status' => 'not_found'];
        }

        if ($panel['status'] === 'imported') {
            return ['remote_panel_id' => $panelPublicId, 'status' => 'already_imported'];
        }

        if ($panel['status'] === 'discarded') {
            return ['remote_panel_id' => $panelPublicId, 'status' => 'already_discarded'];
        }

        $update = $pdo->prepare('
            UPDATE remote_panels
            SET
                status = "imported",
                imported_at = UTC_TIMESTAMP()
            WHERE id = :id
              AND status = "pending"
        ');
        $update->execute(['id' => $panel['id']]);

        return ['remote_panel_id' => $panelPublicId, 'status' => 'imported'];
    }

    private static function discardOnePanel(PDO $pdo, int $userId, string $panelPublicId): array
    {
        $select = $pdo->prepare('
            SELECT id, public_id, status
            FROM remote_panels
            WHERE user_id = :user_id
              AND public_id = :public_id
            LIMIT 1
            FOR UPDATE
        ');
        $select->execute(['user_id' => $userId, 'public_id' => $panelPublicId]);
        $panel = $select->fetch(PDO::FETCH_ASSOC);

        if ($panel === false) {
            return ['remote_panel_id' => $panelPublicId, 'status' => 'not_found'];
        }

        if ($panel['status'] === 'imported') {
            return ['remote_panel_id' => $panelPublicId, 'status' => 'already_imported'];
        }

        if ($panel['status'] === 'discarded') {
            return ['remote_panel_id' => $panelPublicId, 'status' => 'already_discarded'];
        }

        $update = $pdo->prepare('
            UPDATE remote_panels
            SET
                status = "discarded",
                discarded_at = UTC_TIMESTAMP()
            WHERE id = :id
              AND status = "pending"
        ');
        $update->execute(['id' => $panel['id']]);

        return ['remote_panel_id' => $panelPublicId, 'status' => 'discarded'];
    }

    private static function extractPanelIds(array $body): array
    {
        $raw = $body['panel_ids'] ?? $body['remote_panel_ids'] ?? null;

        if (!is_array($raw)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_panel_ids',
                    'message' => 'panel_ids must be an array of remote panel IDs.',
                ],
            ], 400);
        }

        $max = max(1, min(500, Env::int('MAX_SYNC_PANEL_IDS', 100)));
        $ids = [];

        foreach ($raw as $value) {
            if (!is_string($value)) {
                continue;
            }

            $value = trim($value);
            if ($value === '') {
                continue;
            }

            if (!preg_match('/^[0-9a-fA-F-]{36}$/', $value)) {
                continue;
            }

            $ids[$value] = $value;
        }

        $ids = array_values($ids);

        if (count($ids) < 1) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'empty_panel_ids',
                    'message' => 'At least one valid panel ID is required.',
                ],
            ], 400);
        }

        if (count($ids) > $max) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'too_many_panel_ids',
                    'message' => 'Too many panel IDs were sent in one request.',
                ],
            ], 400);
        }

        return $ids;
    }

    private static function nullableClientId(mixed $value): ?string
    {
        if (!is_string($value)) {
            return null;
        }

        $value = trim($value);
        if ($value === '') {
            return null;
        }

        if (!preg_match('/^[A-Za-z0-9._:-]{1,80}$/', $value)) {
            return null;
        }

        return $value;
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

    private static function recordFailedBatch(int $userId, string $syncPublicId, string $errorMessage, ?string $appVersion): void
    {
        try {
            $pdo = Database::pdo();
            $statement = $pdo->prepare('
                INSERT INTO sync_batches (
                    user_id,
                    public_id,
                    status,
                    imported_count,
                    app_version,
                    error_message,
                    completed_at
                )
                VALUES (
                    :user_id,
                    :public_id,
                    "failed",
                    0,
                    :app_version,
                    :error_message,
                    UTC_TIMESTAMP()
                )
            ');
            $statement->execute([
                'user_id' => $userId,
                'public_id' => Uuid::v4(),
                'app_version' => $appVersion,
                'error_message' => mb_substr($errorMessage, 0, 500, 'UTF-8'),
            ]);
        } catch (Throwable) {
        }
    }
}
