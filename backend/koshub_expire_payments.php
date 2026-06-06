<?php
require_once 'config.php';
require_once 'FirestoreClient.php';

header('Content-Type: text/plain');

try {
    echo "Running payment expiration...\n";
    $db = new FirestoreClient();
    $now = gmdate('Y-m-d\TH:i:s\Z');

    // Query pending payments that have expired
    $expiredPayments = $db->queryCollection('payments', [
        ['status', '==', 'pending'],
        ['expiredAt', '<', $now]
    ], null, 100);

    if (empty($expiredPayments)) {
        echo "No expired payments found.\n";
        exit;
    }

    echo "Found " . count($expiredPayments) . " expired payments.\n";

    $writes = [];
    foreach ($expiredPayments as $p) {
        $pId = $p['id'];
        $pData = $p['data'];

        // Update payment status
        $writes[] = $db->createUpdateWrite('payments', $pId, [
            'status' => 'expired',
            'updatedAt' => $now
        ], ['status', 'updatedAt']);

        // Update booking status back to unpaid
        $writes[] = $db->createUpdateWrite('bookings', $pData['bookingId'], [
            'paymentStatus' => 'unpaid',
            'updatedAt' => $now
        ], ['paymentStatus', 'updatedAt']);
    }

    if (!empty($writes)) {
        $res = $db->commit($writes);
        if (isset($res['error'])) {
            echo "Error committing changes: " . $res['message'] . "\n";
        } else {
            echo "Successfully expired " . (count($writes) / 2) . " payments.\n";
        }
    }

} catch (Exception $e) {
    echo "Exception: " . $e->getMessage() . "\n";
}
