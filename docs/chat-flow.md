# KosHub Realtime Chat Flow

This document describes the implementation of Phase 8: Realtime Chat.

## 1. Firestore Structure

### `chats` Collection
Stores chat room metadata.
- **Document ID**: `studentId_ownerId_kosId` (Consistent to prevent duplicates)
- **Fields**:
  - `id`: String
  - `studentId`: String
  - `studentName`: String
  - `ownerId`: String
  - `ownerName`: String
  - `kosId`: String
  - `kosName`: String
  - `bookingId`: String (Optional)
  - `lastMessage`: String
  - `lastMessageAt`: long
  - `lastSenderId`: String
  - `studentUnreadCount`: int
  - `ownerUnreadCount`: int
  - `createdAt`: long
  - `updatedAt`: long

### `messages` Subcollection (`chats/{chatId}/messages`)
Stores individual messages.
- **Fields**:
  - `id`: String
  - `chatId`: String
  - `senderId`: String
  - `senderName`: String
  - `receiverId`: String
  - `text`: String
  - `type`: "text"
  - `createdAt`: long
  - `isRead`: boolean

## 2. Key Components

### ChatRepository.java
- Singleton handling all Firestore operations.
- `getOrCreateChatRoom`: Ensures a single room exists for a student-owner-kos triplet.
- `sendMessage`: Uses `WriteBatch` to add the message and update chat metadata atomically.
- `listenMessages`: ASCENDING order realtime listener.
- `listenChatsByRole`: Queries chats where the user is a participant, sorted by `lastMessageAt`.
- `markMessagesAsRead`: Resets unread counts for the user's role and marks relevant messages as read.

### Adapters
- `ChatListAdapter`: Realtime list for the Inbox.
- `MessageAdapter`: Multi-view type adapter for left/right chat bubbles.

## 3. Entry Points

### Student Side
- **Kos Detail**: "Chat" icon in top navigation.
- **Waiting List**: "Hubungi Pemilik" button.
- **Profile History**: Long-press on active/waiting check-in items to chat.

### Owner Side
- **Pesan Tab**: Realtime inbox.
- **Booking List**: Click on any booking item to open the chat room.

## 4. Unread Logic
- When a message is sent, the receiver's unread count is incremented.
- When a user opens the chat room, their unread count is reset to 0 in Firestore.
- Messages where `receiverId == currentUid` are marked `isRead = true`.
