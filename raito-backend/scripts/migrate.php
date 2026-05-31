<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Database\Database;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
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
    echo "No migrations directory found." . PHP_EOL;
    exit(1);
}

sort($files);

foreach ($files as $file) {
    $migrationName = basename($file);
    $checksum = hash_file('sha256', $file);

    $check = $pdo->prepare('SELECT checksum FROM migrations WHERE migration = :migration LIMIT 1');
    $check->execute([
        'migration' => $migrationName,
    ]);

    $existing = $check->fetch();

    if ($existing !== false) {
        if ($existing['checksum'] !== $checksum) {
            echo "ERROR: Migration file changed after it was applied: {$migrationName}" . PHP_EOL;
            echo "Never edit an already-applied migration. Create a new migration instead." . PHP_EOL;
            exit(1);
        }

        echo "Already applied: {$migrationName}" . PHP_EOL;
        continue;
    }

    echo "Applying: {$migrationName}" . PHP_EOL;

    $sql = file_get_contents($file);

    if ($sql === false) {
        echo "ERROR: Could not read migration: {$migrationName}" . PHP_EOL;
        exit(1);
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

        echo "Applied: {$migrationName}" . PHP_EOL;
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }

        echo "ERROR applying {$migrationName}: " . $exception->getMessage() . PHP_EOL;
        exit(1);
    }
}

echo "Migrations complete." . PHP_EOL;

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
