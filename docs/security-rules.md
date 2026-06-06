# Firestore Security Rules Documentation

This document describes the security implementation for KosHub's Firestore database.

## Core Principles
1. **Default Deny:** All access is denied unless explicitly allowed.
2. **Ownership-based Access:** Users can only read/write data they own.
3. **Role-based Logic:** Differentiation between `student` and `owner`.
4. **Data Integrity:** Status transitions and field modifications are validated.

## Role Definitions
- **Student:** Can view Kos/Rooms, create Bookings (pending), manage their own Complaints, Chats, and Favorites. Can create Reviews for completed bookings.
- **Owner:** Can manage their own Kos/Rooms, process Bookings (accept/reject), respond to Complaints, manage Chats, and view their own Finance (Transactions/Withdrawals).

## Collection Rules Summary

### Users (`/users/{uid}`)
- **Read/Write:** Only by the user themselves (`request.auth.uid == userId`).
- **FCM Tokens:** Only by the user themselves.

### Kos (`/kos/{kosId}`)
- **Read:** Any authenticated user.
- **Create/Delete:** Only by the Owner.
- **Update:** 
    - Full update by Owner.
    - Partial update by Student (only `rating`, `ratingAverage`, `ratingCount`, `updatedAt`) during review submission.

### Rooms (`/rooms/{roomId}`)
- **Read:** Any authenticated user.
- **Create/Delete:** Only by the Owner.
- **Update:**
    - Full update by Owner.
    - Partial update by Student (only `status`, `updatedAt`) during booking check-in process.

### Bookings (`/bookings/{bookingId}`)
- **Read:** Only by the Student or Owner involved.
- **Create:** Only by a Student (initial status must be `pending`).
- **Update:**
    - Student: Can `cancel` or move to `waiting_checkin`/`active`.
    - Owner: Can `accept`, `reject`, or `complete`.

### Chats (`/chats/{chatId}`)
- **Read/Write:** Only by the Student and Owner participants.
- **Messages:** Only by chat participants. `senderId` must match `auth.uid`.

### Transactions & Withdrawals
- **Transactions:** Readable by participating Student and Owner.
- **Withdrawals:** Readable/Manageable only by the Owner.

### Notifications
- **Read:** Only by the recipient.
- **Create:** Allowed for authenticated users (to notify others of events like new bookings/chats).

## Query Compatibility
All Android repository queries must include filters for `studentId` or `ownerId` as appropriate to avoid `PERMISSION_DENIED` errors. The `KosHubSecurity` log tag can be used to monitor these events.

## Testing Checklist (Firebase Console)
1. [ ] Student A cannot read Student B's user document.
2. [ ] Student A cannot read Student B's bookings.
3. [ ] Owner A cannot update Owner B's Kos details.
4. [ ] Non-chat participant cannot read messages in a chat room.
5. [ ] Student cannot create a review without a `completed` booking.
6. [ ] Student cannot change their role to `owner` in their user document.

## Production Considerations
- **Finance:** Withdrawal processing and transaction status should ideally move to server-side (Cloud Functions) for production.
- **Notifications:** Notification creation should ideally be triggered server-side based on database events.
