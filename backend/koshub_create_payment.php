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

if (empty($idToken) || empty($bookingId)) {
    errorResponse('INVALID_ARGUMENT', 'Parameter firebase_id_token dan booking_id wajib diisi');
}

try {
    $auth = new FirebaseAuth();
    $authResult = $auth->verifyIdToken($idToken);

    if (isset($authResult['error'])) {
        errorResponse($authResult['code'], $authResult['message']);
    }

    $uid = $authResult['uid'];
    $db = new FirestoreClient();
    $bookingResponse = $db->getDocument('bookings', $bookingId);

    if (isset($bookingResponse['error'])) {
        errorResponse('BOOKING_NOT_FOUND', 'Data booking tidak ditemukan.');
    }

    $bookingData = $db->parseFirestoreData($bookingResponse['fields']);

    // Validasi ownership
    if ($bookingData['studentId'] !== $uid) {
        errorResponse('BOOKING_NOT_OWNED', 'Kamu tidak punya akses ke booking ini.');
    }

    // Validasi status
    if ($bookingData['paymentStatus'] === 'paid') {
        echo json_encode(['success' => true, 'message' => 'ALREADY_PAID']);
        exit;
    }

    $allowedStatuses = ['accepted', 'waiting_payment'];
    if (!in_array($bookingData['status'], $allowedStatuses)) {
        errorResponse('BOOKING_NOT_PAYABLE', 'Status booking tidak mengizinkan pembayaran: ' . $bookingData['status']);
    }

    // Hitung nominal server-side
    $amount = $bookingData['totalPrice'] ?? $bookingData['totalAmount'] ?? $bookingData['amount'] ?? $bookingData['price'] ?? 0;

    if ($amount <= 0 && !empty($bookingData['roomId'])) {
        $roomRes = $db->getDocument('rooms', $bookingData['roomId']);
        if (!isset($roomRes['error'])) {
            $roomData = $db->parseFirestoreData($roomRes['fields']);
            $amount = $roomData['price'] ?? 0;
        }
    }

    if ($amount <= 0 && !empty($bookingData['kosId'])) {
        $kosRes = $db->getDocument('kos', $bookingData['kosId']);
        if (!isset($kosRes['error'])) {
            $kosData = $db->parseFirestoreData($kosRes['fields']);
            $amount = $kosData['price'] ?? 0;
        }
    }

    if ($amount <= 0) {
        errorResponse('INVALID_PAYMENT_AMOUNT', 'Nominal pembayaran tidak valid (Rp 0).');
    }

    // Cek payment pending yang belum expired
    $nowTimestamp = gmdate('Y-m-d\TH:i:s\Z');
    $existingPayments = $db->queryCollection('payments', [
        ['bookingId', '==', $bookingId],
        ['status', '==', 'pending'],
        ['expiredAt', '>', $nowTimestamp]
    ], ['field' => 'expiredAt', 'direction' => 'DESCENDING'], 1);

    if (!empty($existingPayments)) {
        $lastPayment = $existingPayments[0]['data'];
        $expiredAtMillis = strtotime($lastPayment['expiredAt']) * 1000;
        echo json_encode([
            'success' => true,
            'paymentId' => $existingPayments[0]['id'],
            'bookingId' => $bookingId,
            'gatewayTransactionId' => $lastPayment['gatewayTransactionId'],
            'totalBayar' => $lastPayment['amount'],
            'qrisString' => $lastPayment['qrisString'],
            'expiredAt' => $expiredAtMillis
        ]);
        exit;
    }

    // Panggil Gateway
    if (empty(PAYMENT_API_KEY) || PAYMENT_API_KEY === 'YOUR_FALLBACK_API_KEY') {
        errorResponse('INTERNAL_ERROR', 'Server configuration error: API key missing');
    }

    $userName = $bookingData['studentName'] ?? $authResult['name'] ?? 'User';
    $ch = curl_init(PAYMENT_CREATE_URL);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'api_key' => PAYMENT_API_KEY,
        'nama' => $userName,
        'nominal' => $amount
    ]));
    $gatewayResponse = curl_exec($ch);
    $gatewayData = json_decode($gatewayResponse, true);
    curl_close($ch);

    if (isset($gatewayData['status']) && $gatewayData['status'] === 'success') {
        $paymentId = bin2hex(random_bytes(10));
        $createdAt = gmdate('Y-m-d\TH:i:s.000\Z');
        $expiredAt = gmdate('Y-m-d\TH:i:s.000\Z', time() + (PAYMENT_EXPIRY_MINUTES * 60));

        $paymentData = [
            'bookingId' => $bookingId,
            'studentId' => $uid,
            'ownerId' => $bookingData['ownerId'],
            'kosId' => $bookingData['kosId'],
            'roomId' => $bookingData['roomId'],
            'amount' => $gatewayData['total_bayar'] ?? $amount,
            'status' => 'pending',
            'gateway' => 'custom_qris_alwaysdata',
            'gatewayTransactionId' => (int)$gatewayData['id_transaksi'],
            'qrisString' => $gatewayData['qris_string'],
            'createdAt' => $createdAt,
            'updatedAt' => $createdAt,
            'expiredAt' => $expiredAt
        ];

        // Atomic commit
        $writes = [
            $db->createUpdateWrite('payments', $paymentId, $paymentData),
            $db->createUpdateWrite('bookings', $bookingId, [
                'paymentStatus' => 'pending',
                'status' => 'waiting_payment',
                'gatewayTransactionId' => (int)$gatewayData['id_transaksi'],
                'paymentId' => $paymentId,
                'updatedAt' => $createdAt
            ], ['paymentStatus', 'status', 'gatewayTransactionId', 'paymentId', 'updatedAt'])
        ];

        $commitRes = $db->commit($writes);

        if (isset($commitRes['error'])) {
            errorResponse('INTERNAL_ERROR', 'Gagal menyimpan data ke database');
        }

        echo json_encode([
            'success' => true,
            'paymentId' => $paymentId,
            'bookingId' => $bookingId,
            'gatewayTransactionId' => (int)$gatewayData['id_transaksi'],
            'totalBayar' => $paymentData['amount'],
            'qrisString' => $gatewayData['qris_string'],
            'expiredAt' => strtotime($expiredAt) * 1000
        ]);

    } else {
        errorResponse('GATEWAY_ERROR', 'Gagal membuat transaksi di payment gateway: ' . ($gatewayData['message'] ?? 'Unknown error'));
    }

} catch (Exception $e) {
    errorResponse('INTERNAL_ERROR', 'Terjadi kesalahan internal');
}
