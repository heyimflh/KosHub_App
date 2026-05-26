# Real Booking Flow Documentation - Phase 6

This document describes the implementation of the real booking system for KosHub using Cloud Firestore.

## 1. Booking Data Structure
Collection: `bookings`
- **id**: Firestore Document ID
- **studentId**: ID of the student making the request
- **ownerId**: ID of the property owner
- **kosId**: ID of the property
- **kosName**: Name of the property for quick display
- **status**: Current state of the booking (`pending`, `accepted`, `rejected`, `waiting_checkin`, `active`, `cancelled`)
- **totalPrice**: Amount to be paid
- **durationMonth**: Length of stay in months
- **paymentStatus**: Status of payment (`unpaid`, `paid`)

## 2. States and Transitions

### Pending
- Created when a Student clicks "Booking Sekarang" on the Property Detail page.
- Appears in the Student's "Waiting List" and the Owner's "Booking Masuk".

### Accepted
- Owner clicks "Terima" in `OwnerBookingActivity`.
- Student receives an updated status and can proceed to payment simulation.

### Waiting Check-in
- Student clicks "Bayar Sekarang" (simulated).
- `paymentStatus` becomes `paid`.

### Active
- Student clicks "Sudah Ambil Kunci" (can be done from Profile or Waiting List).
- Room status in the `rooms` collection is updated to `occupied`.

## 3. Integration Details
- **`BookingRepository`**: Central logic for all state transitions and queries. Includes duplicate booking checks.
- **`PropertyDetailBookingActivity`**: Initiates the flow. Now requires real Firestore IDs for Kos and Owner.
- **`WaitingListQueueActivity`**: Displays real-time status for students.
- **`OwnerBookingActivity`**: Allows owners to manage incoming requests.

## 4. Limitations (Phase 6)
- **Payment Gateway**: Simulated via dialogs; no real financial transactions.
- **Chat**: Remains dummy.
- **Notifications**: Relies on UI refreshes; no FCM (Push Notifications) yet.
