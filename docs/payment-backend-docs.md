# KosHub Payment Backend Documentation

This document explains the technical flow of the KosHub payment system using Firebase Cloud Functions and the custom QRIS gateway.

## Components

### 1. `createPaymentForBooking` (Callable)
- **Input**: `{ "bookingId": "STRING" }`
- **Logic**:
    - Validates that the caller owns the booking.
    - Checks for existing non-expired pending payments for the same booking to avoid duplicate QRIS.
    - Calculates the amount from `booking.totalPrice`, falling back to `room.price` or `kos.price`.
    - Proxies a POST request to `https://paymentgateway.alwaysdata.net/api_create.php`.
    - Stores the QRIS string and gateway transaction ID in the `payments` collection.
- **Output**: QRIS string, total amount, and expiry timestamp.

### 2. `checkPaymentStatus` (Callable)
- **Input**: `{ "bookingId": "STRING", "paymentId": "STRING" }`
- **Logic**:
    - Calls `https://paymentgateway.alwaysdata.net/api_check.php?id=ID`.
    - If status is `SUCCESS`, it executes a **Firestore Transaction** to:
        1. Mark payment as `paid`.
        2. Mark booking as `paid` and status as `waiting_checkin`.
        3. Mark room as `booked`.
        4. Create a record in the `transactions` collection for financial tracking.
    - Returns current status to the app.

### 3. `expireOldPayments` (Scheduled)
- Runs every 5 minutes.
- Finds `pending` payments where `expiredAt < now`.
- Updates statuses to `expired` in `payments` and `unpaid` in `bookings`.

### 4. `paymentWebhook` (HTTP)
- Receives automated notifications from the gateway (if configured).
- Implements the same atomic update logic as `checkPaymentStatus`.

## Deployment Instructions

1. **Install Dependencies**:
   ```bash
   cd functions
   npm install
   ```

2. **Configure API Key**:
   Set the payment gateway API key in Firebase Secrets:
   ```bash
   firebase functions:secrets:set PAYMENT_API_KEY
   ```

3. **Deploy**:
   ```bash
   firebase deploy --only functions
   ```

## Security Measures
- **No Hardcoded Keys**: The API key is stored in Secret Manager.
- **Server-side Validation**: Amount and ownership are verified on the server.
- **Atomicity**: Firestore transactions ensure data consistency across multiple collections.
- **Idempotency**: Transaction logic prevents duplicate balance updates.
