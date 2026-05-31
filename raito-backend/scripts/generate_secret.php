<?php

declare(strict_types=1);

require __DIR__ . '/../app/bootstrap.php';

use App\Support\Token;

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

echo Token::webhookSecret() . PHP_EOL;
