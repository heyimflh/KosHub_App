<?php
require_once 'config.php';

class FirebaseAuth {
    private $projectId;
    private static $publicKeys = null;

    public function __construct($projectId = FIREBASE_PROJECT_ID) {
        $this->projectId = $projectId;
    }

    public function verifyIdToken($idToken) {
        $parts = explode('.', $idToken);
        if (count($parts) != 3) {
            throw new Exception("Invalid token format");
        }

        $header = json_decode($this->base64UrlDecode($parts[0]), true);
        $payload = json_decode($this->base64UrlDecode($parts[1]), true);
        $signature = $this->base64UrlDecode($parts[2]);

        if (!$header || !$payload) {
            return ['error' => true, 'code' => 'INVALID_ARGUMENT', 'message' => 'Invalid token data'];
        }

        // 1. Verify Algorithm
        if ($header['alg'] !== 'RS256') {
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Invalid algorithm'];
        }

        // 2. Verify KID and Signature
        try {
            $kid = $header['kid'] ?? null;
            if (!$kid) throw new Exception("KID missing");
            $publicKey = $this->getPublicKey($kid);

            $ok = openssl_verify($parts[0] . "." . $parts[1], $signature, $publicKey, "SHA256");
            if ($ok !== 1) {
                return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Invalid signature'];
            }
        } catch (Exception $e) {
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Verification failed: ' . $e->getMessage()];
        }

        // 3. Verify Claims
        $now = time();
        if ($payload['exp'] < $now) {
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Token expired'];
        }
        if ($payload['iat'] > $now + 300) { // 5 min leeway
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Token issued in future'];
        }
        if ($payload['aud'] !== $this->projectId) {
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Invalid audience'];
        }
        if ($payload['iss'] !== "https://securetoken.google.com/" . $this->projectId) {
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'Invalid issuer'];
        }
        if (empty($payload['sub'])) {
            return ['error' => true, 'code' => 'UNAUTHENTICATED', 'message' => 'UID missing'];
        }

        return ['success' => true, 'uid' => $payload['sub'], 'name' => $payload['name'] ?? 'User'];
    }

    private function getPublicKey($kid) {
        if (self::$publicKeys === null) {
            $ch = curl_init("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com");
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            $response = curl_exec($ch);
            self::$publicKeys = json_decode($response, true);
            curl_close($ch);
        }

        if (!isset(self::$publicKeys[$kid])) {
            throw new Exception("Public key not found for kid: " . $kid);
        }

        return self::$publicKeys[$kid];
    }

    private function base64UrlDecode($data) {
        $remainder = strlen($data) % 4;
        if ($remainder) {
            $padlen = 4 - $remainder;
            $data .= str_repeat('=', $padlen);
        }
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
