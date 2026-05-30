<?php
require_once 'config.php';
require_once 'FirestoreClient.php';

header('Content-Type: text/plain');

$testKey = $_GET['key'] ?? '';
if (empty(ADMIN_TEST_KEY) || $testKey !== ADMIN_TEST_KEY) {
    http_response_code(403);
    echo "FORBIDDEN: Invalid or missing test key.";
    exit;
}

try {
    echo "Testing Firestore connectivity...\n";
    $db = new FirestoreClient();

    // Test: Query bookings (limit 1)
    $results = $db->queryCollection('bookings', [], null, 1);

    if (isset($results['error'])) {
        echo "ERROR: " . $results['message'] . "\n";
    } else {
        echo "SUCCESS: Connection established.\n";
    }

} catch (Exception $e) {
    echo "EXCEPTION: " . $e->getMessage() . "\n";
}
