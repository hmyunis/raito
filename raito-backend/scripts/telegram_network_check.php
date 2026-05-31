<?php

declare(strict_types=1);

if (PHP_SAPI !== 'cli') {
    echo "This script can only be run from the command line." . PHP_EOL;
    exit(1);
}

$host = 'api.telegram.org';
$ip = gethostbyname($host);
$tcpStart = microtime(true);
$tcp = @fsockopen('ssl://' . $host, 443, $tcpErrno, $tcpError, 20);
$tcpElapsed = round(microtime(true) - $tcpStart, 3);

if (is_resource($tcp)) {
    fclose($tcp);
}

$curl = curl_init('https://' . $host);

if ($curl === false) {
    echo json_encode([
        'ok' => false,
        'error' => 'Failed to initialize cURL.',
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES) . PHP_EOL;
    exit(1);
}

curl_setopt_array($curl, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CONNECTTIMEOUT => 20,
    CURLOPT_TIMEOUT => 45,
]);

$curlStart = microtime(true);
$body = curl_exec($curl);
$curlElapsed = round(microtime(true) - $curlStart, 3);
$curlErrno = curl_errno($curl);
$curlError = curl_error($curl);
$httpCode = (int) curl_getinfo($curl, CURLINFO_HTTP_CODE);
curl_close($curl);

echo json_encode([
    'ok' => is_resource($tcp) && $curlErrno === 0,
    'dns' => [
        'host' => $host,
        'ip' => $ip,
    ],
    'tcp_tls' => [
        'connected' => is_resource($tcp),
        'errno' => $tcpErrno ?? 0,
        'error' => $tcpError ?? '',
        'elapsed_seconds' => $tcpElapsed,
    ],
    'https' => [
        'ok' => $curlErrno === 0,
        'errno' => $curlErrno,
        'error' => $curlError,
        'http_code' => $httpCode,
        'body_bytes' => is_string($body) ? strlen($body) : 0,
        'elapsed_seconds' => $curlElapsed,
    ],
], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES) . PHP_EOL;
