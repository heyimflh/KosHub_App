<?php
require_once 'config.php';

class FirestoreClient {
    private $projectId;
    private $accessToken;

    public function __construct($projectId = FIREBASE_PROJECT_ID) {
        $this->projectId = $projectId;
    }

    private function getAccessToken() {
        if ($this->accessToken) return $this->accessToken;

        if (!file_exists(SERVICE_ACCOUNT_PATH)) {
            throw new Exception("Service account file not found at " . SERVICE_ACCOUNT_PATH);
        }

        $serviceAccount = json_decode(file_get_contents(SERVICE_ACCOUNT_PATH), true);
        $now = time();
        $payload = [
            "iss" => $serviceAccount['client_email'],
            "sub" => $serviceAccount['client_email'],
            "aud" => "https://oauth2.googleapis.com/token",
            "iat" => $now,
            "exp" => $now + 3600,
            "scope" => "https://www.googleapis.com/auth/datastore"
        ];

        $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
        $base64UrlHeader = $this->base64UrlEncode($header);
        $base64UrlPayload = $this->base64UrlEncode(json_encode($payload));

        $signature = '';
        openssl_sign($base64UrlHeader . "." . $base64UrlPayload, $signature, $serviceAccount['private_key'], "SHA256");
        $base64UrlSignature = $this->base64UrlEncode($signature);

        $jwt = $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;

        $ch = curl_init("https://oauth2.googleapis.com/token");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ]));

        $response = curl_exec($ch);
        $data = json_decode($response, true);
        curl_close($ch);

        if (isset($data['access_token'])) {
            $this->accessToken = $data['access_token'];
            return $this->accessToken;
        } else {
            throw new Exception("Failed to get access token: " . $response);
        }
    }

    private function base64UrlEncode($data) {
        return str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($data));
    }

    public function getDocument($collection, $documentId) {
        $url = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents/{$collection}/{$documentId}";
        return $this->request($url);
    }

    public function createDocument($collection, $documentId, $data) {
        $url = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents/{$collection}?documentId={$documentId}";
        return $this->request($url, 'POST', $this->formatFirestoreData($data));
    }

    public function updateDocument($collection, $documentId, $data, $updateMask = []) {
        $url = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents/{$collection}/{$documentId}";

        $query = [];
        if (!empty($updateMask)) {
            foreach ($updateMask as $field) {
                $query[] = "updateMask.fieldPaths=" . urlencode($field);
            }
        } else {
            foreach (array_keys($data) as $field) {
                $query[] = "updateMask.fieldPaths=" . urlencode($field);
            }
        }

        $url .= "?" . implode('&', $query);
        return $this->request($url, 'PATCH', $this->formatFirestoreData($data));
    }

    public function queryCollection($collection, $where = [], $orderBy = null, $limit = null) {
        $url = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents:runQuery";

        $structuredQuery = [
            'from' => [['collectionId' => $collection]]
        ];

        if (!empty($where)) {
            $filters = [];
            foreach ($where as $w) {
                $filters[] = [
                    'fieldFilter' => [
                        'field' => ['fieldPath' => $w[0]],
                        'op' => $this->mapOperator($w[1]),
                        'value' => $this->formatValue($w[2])
                    ]
                ];
            }
            if (count($filters) > 1) {
                $structuredQuery['where'] = ['compositeFilter' => ['op' => 'AND', 'filters' => $filters]];
            } else {
                $structuredQuery['where'] = $filters[0];
            }
        }

        if ($orderBy) {
            $structuredQuery['orderBy'] = [
                ['field' => ['fieldPath' => $orderBy['field']], 'direction' => $orderBy['direction']]
            ];
        }

        if ($limit) {
            $structuredQuery['limit'] = $limit;
        }

        $response = $this->request($url, 'POST', ['structuredQuery' => $structuredQuery]);

        $results = [];
        if (is_array($response)) {
            foreach ($response as $item) {
                if (isset($item['document'])) {
                    $results[] = [
                        'id' => basename($item['document']['name']),
                        'data' => $this->parseFirestoreData($item['document']['fields'])
                    ];
                }
            }
        }
        return $results;
    }

    public function commit($writes) {
        $url = "https://firestore.googleapis.com/v1/projects/{$this->projectId}/databases/(default)/documents:commit";
        $payload = ['writes' => $writes];
        return $this->request($url, 'POST', $payload);
    }

    public function createUpdateWrite($collection, $documentId, $data, $updateMask = null) {
        $name = "projects/{$this->projectId}/databases/(default)/documents/{$collection}/{$documentId}";
        $write = [
            'update' => [
                'name' => $name,
                'fields' => $this->formatFirestoreData($data)['fields']
            ]
        ];

        if ($updateMask) {
            $write['updateMask'] = ['fieldPaths' => $updateMask];
        } else {
            $write['updateMask'] = ['fieldPaths' => array_keys($data)];
        }

        return $write;
    }

    public function createSetWrite($collection, $documentId, $data) {
        $name = "projects/{$this->projectId}/databases/(default)/documents/{$collection}/{$documentId}";
        return [
            'update' => [
                'name' => $name,
                'fields' => $this->formatFirestoreData($data)['fields']
            ]
        ];
    }

    private function request($url, $method = 'GET', $data = null) {
        $ch = curl_init($url);
        $headers = [
            'Authorization: Bearer ' . $this->getAccessToken(),
            'Content-Type: application/json'
        ];
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        if ($method === 'POST') {
            curl_setopt($ch, CURLOPT_POST, true);
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        } elseif ($method === 'PATCH') {
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PATCH');
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        }

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        $decoded = json_decode($response, true);
        if ($httpCode >= 400) {
            return ['error' => true, 'status' => $httpCode, 'message' => $decoded['error']['message'] ?? 'Unknown error'];
        }

        return $decoded;
    }

    public function formatFirestoreData($data) {
        $fields = [];
        foreach ($data as $key => $value) {
            $fields[$key] = $this->formatValue($value);
        }
        return ['fields' => $fields];
    }

    private function formatValue($value) {
        if (is_null($value)) return ['nullValue' => null];
        if (is_bool($value)) return ['booleanValue' => $value];
        if (is_int($value)) return ['integerValue' => (string)$value];
        if (is_float($value)) return ['doubleValue' => $value];
        if (is_string($value)) {
            if (preg_match('/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z$/', $value)) {
                return ['timestampValue' => $value];
            }
            return ['stringValue' => $value];
        }
        if (is_array($value)) {
            if (isset($value['seconds'])) { // Assume it's a timestamp object or serverValue
                return ['timestampValue' => gmdate('Y-m-d\TH:i:s\Z', $value['seconds'])];
            }
            if (isset($value['__server_timestamp__'])) {
                 // In REST API, we can't easily use serverTimestamp in a regular set/update without transformations
                 // Usually we send current ISO string
                 return ['timestampValue' => gmdate('Y-m-d\TH:i:s.000\Z')];
            }

            // Check if it's associative
            if (array_keys($value) !== range(0, count($value) - 1)) {
                $mapValue = [];
                foreach ($value as $k => $v) {
                    $mapValue[$k] = $this->formatValue($v);
                }
                return ['mapValue' => ['fields' => $mapValue]];
            } else {
                $arrayValue = [];
                foreach ($value as $v) {
                    $arrayValue[] = $this->formatValue($v);
                }
                return ['arrayValue' => ['values' => $arrayValue]];
            }
        }
        return ['stringValue' => (string)$value];
    }

    public function parseFirestoreData($fields) {
        $data = [];
        if (!$fields) return $data;
        foreach ($fields as $key => $value) {
            $data[$key] = $this->parseValue($value);
        }
        return $data;
    }

    private function parseValue($value) {
        if (isset($value['stringValue'])) return $value['stringValue'];
        if (isset($value['integerValue'])) return (int)$value['integerValue'];
        if (isset($value['doubleValue'])) return (float)$value['doubleValue'];
        if (isset($value['booleanValue'])) return (bool)$value['booleanValue'];
        if (isset($value['timestampValue'])) return $value['timestampValue'];
        if (isset($value['mapValue'])) return $this->parseFirestoreData($value['mapValue']['fields'] ?? []);
        if (isset($value['arrayValue'])) {
            $list = [];
            foreach ($value['arrayValue']['values'] ?? [] as $v) {
                $list[] = $this->parseValue($v);
            }
            return $list;
        }
        if (isset($value['nullValue'])) return null;
        return null;
    }

    private function mapOperator($op) {
        $map = [
            '==' => 'EQUAL',
            '<' => 'LESS_THAN',
            '<=' => 'LESS_THAN_OR_EQUAL',
            '>' => 'GREATER_THAN',
            '>=' => 'GREATER_THAN_OR_EQUAL',
            '!=' => 'NOT_EQUAL',
            'array-contains' => 'ARRAY_CONTAINS',
            'in' => 'IN',
            'array-contains-any' => 'ARRAY_CONTAINS_ANY',
            'not-in' => 'NOT_IN'
        ];
        return $map[$op] ?? 'EQUAL';
    }
}
