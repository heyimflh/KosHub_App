<?php
header('Content-Type: application/json');
require_once 'config.php';
require_once 'FirestoreClient.php';
require_once 'FirebaseAuth.php';

function errorResponse($code, $message, $httpCode = 200) {
    http_response_code($httpCode);
    echo json_encode([
        'success' => false,
        'code' => $code,
        'message' => $message
    ]);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    errorResponse('METHOD_NOT_ALLOWED', 'Hanya menerima request POST', 405);
}

$input = json_decode(file_get_contents('php://input'), true);
if (json_last_error() !== JSON_ERROR_NONE) {
    errorResponse('INVALID_ARGUMENT', 'Invalid JSON input');
}

$idToken = $input['firebase_id_token'] ?? '';
$bookingId = $input['booking_id'] ?? '';
$paymentId = $input['payment_id'] ?? '';

if (empty($idToken) || empty($bookingId) || empty($paymentId)) {
    errorResponse('INVALID_ARGUMENT', 'Parameter firebase_id_token, booking_id, dan payment_id wajib diisi');
}

try {
    $auth = new FirebaseAuth();
    $authResult = $auth->verifyIdToken($idToken);

    if (isset($authResult['error'])) {
        errorResponse($authResult['code'], $authResult['message']);
    }

    $uid = $authResult['uid'];
    $db = new FirestoreClient();
    $paymentRes = $db->getDocument('payments', $paymentId);

    if (isset($paymentRes['error'])) {
        errorResponse('PAYMENT_NOT_FOUND', 'Data pembayaran tidak ditemukan');
    }

    $paymentData = $db->parseFirestoreData($paymentRes['fields']);

    if ($paymentData['studentId'] !== $uid) {
        errorResponse('BOOKING_NOT_OWNED', 'Kamu tidak punya akses ke pembayaran ini');
    }

    if ($paymentData['status'] === 'paid') {
        echo json_encode(['success' => true, 'status' => 'paid']);
        exit;
    }

    // Call Gateway
    $ch = curl_init(PAYMENT_CHECK_URL . "?id=" . $paymentData['gatewayTransactionId']);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $gatewayResponse = curl_exec($ch);
    $gatewayData = json_decode($gatewayResponse, true);
    curl_close($ch);

    if (isset($gatewayData['status']) && $gatewayData['status'] === 'SUCCESS') {
        $now = gmdate('Y-m-d\TH:i:s.000\Z');

        // Deterministic Transaction ID for Idempotency
        $transId = "PAY_" . $paymentId;
        $existingTrans = $db->getDocument('transactions', $transId);

        if (isset($existingTrans['error'])) { // Not found, proceed
            $writes = [
                // 1. Update Payment
                $db->createUpdateWrite('payments', $paymentId, [
                    'status' => 'paid',
                    'paidAt' => $now,
                    'updatedAt' => $now
                ], ['status', 'paidAt', 'updatedAt']),

                // 2. Update Booking
                $db->createUpdateWrite('bookings', $bookingId, [
                    'paymentStatus' => 'paid',
                    'status' => 'waiting_checkin',
                    'paidAt' => $now,
                    'updatedAt' => $now
                ], ['paymentStatus', 'status', 'paidAt', 'updatedAt']),

                // 3. Update Room
                $db->createUpdateWrite('rooms', $paymentData['roomId'], [
                    'status' => 'booked',
                    'updatedAt' => $now
                ], ['status', 'updatedAt']),

                // 4. Create Transaction
                $db->createSetWrite('transactions', $transId, [
                    'bookingId' => $bookingId,
                    'paymentId' => $paymentId,
                    'ownerId' => $paymentData['ownerId'],
                    'studentId' => $uid,
                    'kosId' => $paymentData['kosId'],
                    'roomId' => $paymentData['roomId'],
                    'amount' => $paymentData['amount'],
                    'type' => 'rent_payment',
                    'status' => 'available',
                    'gateway' => 'custom_qris_alwaysdata',
                    'gatewayTransactionId' => $paymentData['gatewayTransactionId'],
                    'createdAt' => $now,
                    'paidAt' => $now
                ])
            ];

            $commitRes = $db->commit($writes);
            if (isset($commitRes['error'])) {
                errorResponse('INTERNAL_ERROR', 'Gagal memproses pembayaran di database');
            }
        }

        echo json_encode(['success' => true, 'status' => 'paid']);
    } else {
        echo json_encode(['success' => true, 'status' => 'pending']);
    }

} catch (Exception $e) {
    errorResponse('INTERNAL_ERROR', 'Terjadi kesalahan internal');
}
