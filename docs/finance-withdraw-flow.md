# Finance and Withdraw Flow Simulation (Phase 9)

## Overview
This document describes the simulated finance and withdrawal system for KosHub owners, implemented using Firebase Firestore. This is NOT a real payment gateway integration.

## 1. Firestore Collections

### Transactions (`transactions`)
Stores all financial events related to bookings.
- `id`: Unique Transaction ID
- `ownerId`: Owner's User ID
- `studentId`: Student's User ID
- `bookingId`: Associated Booking ID
- `kosId`: Associated Kos ID
- `kosName`: Name of the Kos
- `amount`: Transaction amount (double)
- `type`: `booking_payment`
- `status`: `pending`, `available`, `withdrawn`, `cancelled`
- `createdAt`: Timestamp
- `updatedAt`: Timestamp
- `availableAt`: Timestamp (when student confirms key taken)
- `withdrawalId`: ID of the withdrawal if status is `withdrawn`

### Withdrawals (`withdrawals`)
Stores withdrawal requests from owners.
- `id`: Unique Withdrawal ID
- `ownerId`: Owner's User ID
- `amount`: Requested amount (double)
- `bankName`: Target bank name
- `accountNumber`: Target account number
- `accountHolder`: Name of the account holder
- `status`: `pending`, `processing`, `success`, `failed`
- `note`: Admin note or reason for failure
- `createdAt`: Timestamp
- `updatedAt`: Timestamp
- `processedAt`: Timestamp (when status becomes success/failed)

## 2. Transaction Lifecycle

1. **Creation (Pending)**: 
   - Triggered when a student performs a "Simulated Payment" for a booking.
   - Status: `pending`.
   - The amount is reflected in the owner's `pendingBalance`.

2. **Becoming Available**:
   - Triggered when a student clicks "Sudah Ambil Kunci" (Key Taken).
   - Status changes from `pending` to `available`.
   - The amount is reflected in the owner's `availableBalance`.

3. **Cancellation**:
   - Triggered if a booking is rejected by the owner or cancelled by the student while the transaction is still `pending`.
   - Status changes to `cancelled`.

4. **Withdrawal (Withdrawn)**:
   - Status changes to `withdrawn` once the owner's withdrawal request is processed (simulated).

## 3. Balance Calculations (FinanceSummary)

- **Total Income**: Sum of transactions with status `available` OR `withdrawn`.
- **Available Balance**: (Sum of transactions with status `available`) - (Sum of withdrawals with status `pending` OR `processing`).
  - *Note: We subtract pending withdrawals to prevent double withdrawing with the same balance.*
- **Pending Balance**: Sum of transactions with status `pending`.

## 4. Withdrawal Flow

1. Owner enters bank details and amount in `OwnerWithdrawActivity`.
2. System validates:
   - `amount > 0`
   - `amount <= availableBalance`
   - All bank fields are filled.
3. A new document is created in `withdrawals` with status `pending`.
4. The `availableBalance` in the UI immediately reflects the deduction (if using the formula above).
5. (Optional/Simulation) Status can be changed manually in Firebase Console to `processing` -> `success`.

## 5. Security & Constraints
- Only Java + XML used.
- No real payment gateway (Midtrans/Xendit).
- No real bank transfers.
- All IDs (`ownerId`, `studentId`) are fetched from `FirebaseAuth.getInstance().getCurrentUser()`.
- Manual sorting in Java to avoid Firestore composite index errors.
