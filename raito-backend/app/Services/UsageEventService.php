<?php

declare(strict_types=1);

namespace App\Services;

use App\Database\Database;
use App\Support\Env;
use App\Support\Request;
use App\Support\Response;
use PDO;
use PDOException;

final class UsageEventService
{
    public static function trackSingle(array $user, array $body): void
    {
        $event = self::normalizeEvent($body);
        $result = self::insertEvent($user, $event);

        Response::json([
            'ok' => true,
            'stored' => $result === 'stored',
            'duplicate' => $result === 'duplicate',
        ], 201);
    }

    public static function trackBatch(array $user, array $body): void
    {
        $events = $body['events'] ?? null;

        if (!is_array($events)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_events',
                    'message' => 'events must be an array.',
                ],
            ], 400);
        }

        $max = max(1, min(200, Env::int('MAX_BATCH_EVENTS', 50)));
        if (count($events) > $max) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'too_many_events',
                    'message' => 'Too many events were sent in one request.',
                ],
            ], 400);
        }

        $stored = 0;
        $duplicates = 0;
        $results = [];

        foreach ($events as $eventBody) {
            if (!is_array($eventBody)) {
                $results[] = ['stored' => false, 'duplicate' => false, 'error' => 'invalid_event'];
                continue;
            }

            $event = self::normalizeEvent($eventBody);
            $result = self::insertEvent($user, $event);

            if ($result === 'stored') {
                $stored++;
            } elseif ($result === 'duplicate') {
                $duplicates++;
            }

            $results[] = [
                'stored' => $result === 'stored',
                'duplicate' => $result === 'duplicate',
            ];
        }

        Response::json([
            'ok' => true,
            'stored_count' => $stored,
            'duplicate_count' => $duplicates,
            'results' => $results,
        ], 201);
    }

    private static function normalizeEvent(array $body): array
    {
        $eventName = $body['event_name'] ?? null;

        if (!is_string($eventName)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_event_name',
                    'message' => 'event_name is required.',
                ],
            ], 400);
        }

        $eventName = trim($eventName);
        if ($eventName === '' || !preg_match('/^[a-z0-9_]{2,80}$/', $eventName)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_event_name',
                    'message' => 'event_name is invalid.',
                ],
            ], 400);
        }

        $clientEventId = self::nullableString($body['client_event_id'] ?? null, 80);
        $clientTimeAt = self::nullableString($body['client_time_at'] ?? null, 40);
        $payload = $body['payload'] ?? [];

        if (!is_array($payload)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'invalid_payload',
                    'message' => 'payload must be an object.',
                ],
            ], 400);
        }

        $encodedPayload = null;
        if ($payload !== []) {
            $encodedPayload = json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        }

        if ($encodedPayload !== null && strlen($encodedPayload) > Env::int('MAX_EVENT_PAYLOAD_BYTES', 12000)) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'payload_too_large',
                    'message' => 'Event payload is too large.',
                ],
            ], 413);
        }

        return [
            'event_name' => $eventName,
            'client_event_id' => $clientEventId,
            'client_time_at' => $clientTimeAt,
            'payload' => $payload,
        ];
    }

    private static function insertEvent(array $user, array $event): string
    {
        $pdo = Database::pdo();

        $clientTime = self::normalizeClientTime($event['client_time_at']);
        $encodedPayload = $event['payload'] === [] ? null : json_encode($event['payload'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

        try {
            $statement = $pdo->prepare('
                INSERT INTO usage_events (
                    user_id,
                    event_name,
                    client_event_id,
                    event_payload,
                    client_time_at,
                    ip_hash,
                    user_agent_hash
                )
                VALUES (
                    :user_id,
                    :event_name,
                    :client_event_id,
                    :event_payload,
                    :client_time_at,
                    :ip_hash,
                    :user_agent_hash
                )
            ');

            $statement->execute([
                'user_id' => $user['id'],
                'event_name' => $event['event_name'],
                'client_event_id' => $event['client_event_id'],
                'event_payload' => $encodedPayload,
                'client_time_at' => $clientTime,
                'ip_hash' => Request::clientIpHash(),
                'user_agent_hash' => Request::userAgentHash(),
            ]);

            return 'stored';
        } catch (PDOException $exception) {
            if ($exception->getCode() === '23000') {
                return 'duplicate';
            }

            throw $exception;
        }
    }

    private static function normalizeClientTime(?string $value): ?string
    {
        if ($value === null) {
            return null;
        }

        $timestamp = strtotime($value);
        if ($timestamp === false) {
            return null;
        }

        return gmdate('Y-m-d H:i:s', $timestamp);
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
