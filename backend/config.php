<?php
// KosHub Payment Proxy Configuration

// --- Firebase Configuration ---
define('FIREBASE_PROJECT_ID', 'koshub-psdku'); // REPLACE with your actual project ID
// It is recommended to put this in a non-public folder, e.g. /home/username/private/service-account.json
define('SERVICE_ACCOUNT_PATH', getenv('SERVICE_ACCOUNT_PATH') ?: __DIR__ . '/service-account.json');

// --- Admin Security ---
define('ADMIN_TEST_KEY', getenv('ADMIN_TEST_KEY') ?: 'CHANGE_ME_IN_PRODUCTION');

// --- Payment Gateway Configuration ---
$gatewayApiKey = getenv('PAYMENT_API_KEY');
if (!$gatewayApiKey && file_exists(__DIR__ . '/config.local.php')) {
    $localConfig = include(__DIR__ . '/config.local.php');
    $gatewayApiKey = $localConfig['PAYMENT_API_KEY'] ?? null;
}
define('PAYMENT_API_KEY', $gatewayApiKey ?: 'YOUR_FALLBACK_API_KEY');
define('PAYMENT_CREATE_URL', 'https://paymentgateway.alwaysdata.net/api_create.php');
define('PAYMENT_CHECK_URL', 'https://paymentgateway.alwaysdata.net/api_check.php');

// --- Other Settings ---
define('PAYMENT_EXPIRY_MINUTES', 15);
define('TIMEZONE', 'Asia/Jakarta');

date_default_timezone_set(TIMEZONE);
