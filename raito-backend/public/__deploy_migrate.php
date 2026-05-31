<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Database\Database;
use App\Support\Env;
use App\Support\Response;

$token = $_GET['token'] ?? '';

$expected = (string) Env::get('DEPLOY_TOKEN', '');

if ($expected === '' || !is_string($token) || !hash_equals($expected, $token)) {
    Response::json([
        'ok' => false,
        'error' => [
            'code' => 'forbidden',
            'message' => 'Forbidden.',
        ],
    ], 403);
}

$pdo = Database::pdo();

$pdo->exec("
    CREATE TABLE IF NOT EXISTS migrations (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        migration VARCHAR(255) NOT NULL,
        checksum CHAR(64) NOT NULL,
        applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

        PRIMARY KEY (id),
        UNIQUE KEY uq_migrations_migration (migration)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
");

$files = glob(BASE_PATH . '/migrations/*.sql');

if ($files === false) {
    Response::json([
        'ok' => false,
        'error' => [
            'code' => 'migrations_not_found',
            'message' => 'No migrations directory found.',
        ],
    ], 500);
}

sort($files);

$results = [];

foreach ($files as $file) {
    $migrationName = basename($file);
    $checksum = hash_file('sha256', $file);

    $check = $pdo->prepare('SELECT checksum FROM migrations WHERE migration = :migration LIMIT 1');
    $check->execute(['migration' => $migrationName]);

    $existing = $check->fetch();

    if ($existing !== false) {
        if ($existing['checksum'] !== $checksum) {
            Response::json([
                'ok' => false,
                'error' => [
                    'code' => 'migration_checksum_mismatch',
                    'message' => 'Migration file changed after it was applied: ' . $migrationName,
                ],
            ], 500);
        }

        $results[] = [
            'migration' => $migrationName,
            'status' => 'already_applied',
        ];

        continue;
    }

    $sql = file_get_contents($file);

    if ($sql === false) {
        Response::json([
            'ok' => false,
            'error' => [
                'code' => 'migration_read_failed',
                'message' => 'Could not read migration: ' . $migrationName,
            ],
        ], 500);
    }

    $statements = splitSqlStatements($sql);

    try {
        foreach ($statements as $statement) {
            $pdo->exec($statement);
        }

        $insert = $pdo->prepare('
            INSERT INTO migrations (migration, checksum)
            VALUES (:migration, :checksum)
        ');

        $insert->execute([
            'migration' => $migrationName,
            'checksum' => $checksum,
        ]);

        $results[] = [
            'migration' => $migrationName,
            'status' => 'applied',
        ];
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }

        Response::json([
            'ok' => false,
            'error' => [
                'code' => 'migration_failed',
                'message' => $exception->getMessage(),
                'migration' => $migrationName,
            ],
        ], 500);
    }
}

Response::json([
    'ok' => true,
    'results' => $results,
]);

function splitSqlStatements(string $sql): array
{
    $sql = preg_replace('/^\s*--.*$/m', '', $sql);

    if ($sql === null) {
        return [];
    }

    $parts = explode(';', $sql);
    $statements = [];

    foreach ($parts as $part) {
        $statement = trim($part);

        if ($statement !== '') {
            $statements[] = $statement;
        }
    }

    return $statements;
}
