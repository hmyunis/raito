<?php

declare(strict_types=1);

namespace App\Database;

use App\Support\Env;
use PDO;
use PDOException;

final class Database
{
    private static ?PDO $pdo = null;

    public static function pdo(): PDO
    {
        if (self::$pdo !== null) {
            return self::$pdo;
        }

        $host = Env::get('DB_HOST', 'localhost');
        $port = Env::get('DB_PORT', '3306');
        $name = Env::get('DB_NAME', '');
        $user = Env::get('DB_USER', '');
        $pass = Env::get('DB_PASS', '');

        if ($name === '' || $user === '') {
            throw new PDOException('Database configuration is incomplete.');
        }

        $dsn = "mysql:host={$host};port={$port};dbname={$name};charset=utf8mb4";

        self::$pdo = new PDO($dsn, $user, $pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);

        self::$pdo->exec("SET time_zone = '+00:00'");
        self::$pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");

        return self::$pdo;
    }
}
