<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Support\Env;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

echo "Local stats endpoint:" . PHP_EOL;
echo "GET http://localhost:8080/api/admin/stats" . PHP_EOL;
echo "Authorization: Bearer " . Env::get('ADMIN_TOKEN', '') . PHP_EOL;
