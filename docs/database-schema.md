# KosHub Database Schema Design

This document describes the structure of the Firestore database for KosHub.

## Collections

### 1. users
Stores user profile information for both students and owners.
- **id**: String (Primary Key / Auth UID)
- **name**: String
- **email**: String
- **phone**: String
- **role**: String ("student" | "owner")
- **profileImageUrl**: String
- **createdAt**: long (Timestamp)

### 2. kos
Stores information about kos properties.
- **id**: String (Primary Key)
- **ownerId**: String (Reference to users.id)
- **name**: String
- **address**: String
- **description**: String
- **price**: double (Base price or starting price)
- **category**: String ("putra" | "putri" | "campur")
- **facilities**: List<String>
- **imageUrls**: List<String>
- **rating**: double
- **latitude**: double
- **longitude**: double
- **availableRooms**: int
- **isPremium**: boolean
- **createdAt**: long (Timestamp)

### 3. rooms
Stores information about specific rooms within a kos property.
- **id**: String (Primary Key)
- **kosId**: String (Reference to kos.id)
- **ownerId**: String (Reference to users.id)
- **roomName**: String
- **price**: double
- **status**: String ("available" | "booked" | "occupied")

### 4. bookings
Stores rental booking transactions.
- **id**: String (Primary Key)
- **studentId**: String (Reference to users.id)
- **ownerId**: String (Reference to users.id)
- **kosId**: String (Reference to kos.id)
- **roomId**: String (Reference to rooms.id)
- **status**: String ("pending" | "accepted" | "rejected" | "active" | "completed" | "cancelled")
- **bookingDate**: long (Timestamp)
- **checkInDate**: long (Timestamp)
- **totalPrice**: double
- **createdAt**: long (Timestamp)

### 5. favorites
Stores kos properties favorited by students.
- **id**: String (Primary Key)
- **userId**: String (Reference to users.id)
- **kosId**: String (Reference to kos.id)
- **createdAt**: long (Timestamp)

### 6. complaints
Stores complaints submitted by tenants.
- **id**: String (Primary Key)
- **studentId**: String (Reference to users.id)
- **ownerId**: String (Reference to users.id)
- **kosId**: String (Reference to kos.id)
- **bookingId**: String (Reference to bookings.id)
- **title**: String
- **description**: String
- **imageUrl**: String
- **status**: String ("new" | "process" | "done" | "rejected")
- **createdAt**: long (Timestamp)

### 7. chats
Stores chat room headers between students and owners.
- **id**: String (Primary Key)
- **studentId**: String (Reference to users.id)
- **ownerId**: String (Reference to users.id)
- **kosId**: String (Reference to kos.id)
- **lastMessage**: String
- **lastMessageAt**: long (Timestamp)
- **createdAt**: long (Timestamp)

### 8. messages (Subcollection of chats)
Stores individual messages within a chat room.
- Location: `chats/{chatId}/messages/{messageId}`
- **id**: String (Primary Key)
- **chatId**: String (Reference to chats.id)
- **senderId**: String (Reference to users.id)
- **text**: String
- **createdAt**: long (Timestamp)
- **isRead**: boolean

### 9. transactions
Stores financial transactions related to bookings.
- **id**: String (Primary Key)
- **ownerId**: String (Reference to users.id)
- **studentId**: String (Reference to users.id)
- **bookingId**: String (Reference to bookings.id)
- **amount**: double
- **type**: String ("booking_payment")
- **status**: String ("pending" | "available" | "withdrawn")
- **createdAt**: long (Timestamp)

### 10. withdrawals
Stores withdrawal requests from owners.
- **id**: String (Primary Key)
- **ownerId**: String (Reference to users.id)
- **amount**: double
- **bankName**: String
- **accountNumber**: String
- **accountHolder**: String
- **status**: String ("pending" | "processing" | "success" | "failed")
- **createdAt**: long (Timestamp)

### 11. reviews
Stores reviews and ratings left by students.
- **id**: String (Primary Key)
- **studentId**: String (Reference to users.id)
- **kosId**: String (Reference to kos.id)
- **bookingId**: String (Reference to bookings.id)
- **rating**: double
- **comment**: String
- **createdAt**: long (Timestamp)

## Relationships

- **User (Owner) -> Kos**: 1-to-Many. An owner can manage multiple kos properties.
- **Kos -> Rooms**: 1-to-Many. A kos property consists of multiple rooms.
- **Student -> Booking**: 1-to-Many. A student can make multiple bookings.
- **Booking -> (Student, Owner, Kos, Room)**: Many-to-1. A booking connects these entities.
- **Complaint -> (Student, Owner, Kos, Booking)**: Many-to-1. A complaint is filed by a student for a specific kos/booking.
- **Chat -> (Student, Owner, Kos)**: Many-to-1. A chat room is associated with a specific property and the two parties involved.
- **Transaction -> Booking**: 1-to-1. A transaction records the payment for a booking.
- **Withdrawal -> User (Owner)**: Many-to-1. An owner can make multiple withdrawals.
- **Review -> (Student, Kos, Booking)**: Many-to-1. A review is linked to a student, property, and a completed stay (booking).
