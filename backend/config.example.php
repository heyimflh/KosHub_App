<?php
// KosHub Payment Proxy Configuration (Example)

// --- Firebase Configuration ---
define('FIREBASE_PROJECT_ID', 'your-project-id');
// For AlwaysData, path usually starts with /home/username/
define('SERVICE_ACCOUNT_PATH', '/home/your_username/private/service-account.json');

// --- Admin Security ---
define('ADMIN_TEST_KEY', 'your-random-test-key');

// --- Payment Gateway Configuration ---
// It is recommended to set PAYMENT_API_KEY as an environment variable
$gatewayApiKey = getenv('PAYMENT_API_KEY') ?: 'YOUR_API_KEY_HERE';
define('PAYMENT_API_KEY', $gatewayApiKey);

define('PAYMENT_CREATE_URL', 'https://paymentgateway.alwaysdata.net/api_create.php');
define('PAYMENT_CHECK_URL', 'https://paymentgateway.alwaysdata.net/api_check.php');

// --- Other Settings ---
define('PAYMENT_EXPIRY_MINUTES', 15);
define('TIMEZONE', 'Asia/Jakarta');

date_default_timezone_set(TIMEZONE);
