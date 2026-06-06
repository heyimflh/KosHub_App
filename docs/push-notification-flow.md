# KosHub Push Notification Flow

## Overview
Phase 11 implements a robust notification system using Firebase Cloud Messaging (FCM) and Firestore. The system is split into two parts:
1.  **Notification Center (Firestore)**: Stores every notification event as a document in the `notifications` collection.
2.  **Push Delivery (FCM)**: Sends real-time push messages to devices via an external backend (Cloud Functions).

## Architecture
1.  **App Event**: A repository (e.g., `BookingRepository`) performs an action and calls `NotificationRepository.createNotification()`.
2.  **Notification Center**: A new document is created in the `notifications` collection.
3.  **GUI Update**: Both student and owner apps listen to this collection in real-time to update unread badges and notification lists.
4.  **Backend Trigger**: A Cloud Function (Node.js) triggers on `onCreate` for the `notifications` collection.
5.  **FCM Send**: The Cloud Function fetches the recipient's FCM tokens from `users/{uid}/fcmTokens` and sends the push message via Firebase Admin SDK.

## Key Components
- **FCMTokenRepository**: Saves device tokens to Firestore.
- **NotificationRepository**: Manages notification documents and real-time listeners.
- **KosHubMessagingService**: Handles incoming FCM messages while the app is in foreground/background.
- **NotificationActivity**: GUI to view and manage (mark as read) notifications.

## Notification Types
- `booking_new`, `booking_accepted`, `booking_rejected`
- `chat_message`
- `complaint_new`, `complaint_process`, `complaint_done`, `complaint_rejected`
- `withdraw_requested`, `withdraw_processing`, `withdraw_success`, `withdraw_failed`

## Security
- **Server Key**: The FCM Server Key is NOT stored in the Android app. It must be kept in the Cloud Functions environment.
- **Rules**: Firestore security rules should ensure users can only read/update notifications where they are the `recipientId`.
