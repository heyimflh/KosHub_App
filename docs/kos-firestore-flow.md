# Kos Firestore Flow Documentation - Phase 4

This document describes how real Kos data is managed and displayed using Cloud Firestore.

## 1. Firestore Structure

### Collection: `kos`
- Stores property metadata.
- Managed by owners.
- Key fields: `name`, `price`, `category`, `ownerId`, `availableRooms`.

### Collection: `rooms`
- Stores individual room details.
- Linked to a kos property via `kosId`.
- Statuses: `available`, `booked`, `occupied`.

## 2. Student Home Integration
- `StudentHomeActivity` fetches all kos listings from Firestore using `KosRepository.getAllKosItems()`.
- Data is retrieved asynchronously.
- **Search & Filter**: Performed client-side for better performance during the initial phase.
- **Fallback**: If Firestore is empty or inaccessible, the app falls back to local dummy data to maintain usability.

## 3. Owner Management
- `OwnerManagementActivity` allows owners to see their specific properties.
- **Operations**:
  - **Create**: Generates a new document in the `kos` collection.
  - **Update**: Modifies existing property details.
  - **Delete**: Removes the property document.
  - **Room Management**: Owners can add rooms to their properties and toggle their availability status.

## 4. Image Management
- Kos images are uploaded to **Firebase Storage** at path: `kos/{ownerId}/{kosId}/{timestamp}.jpg`.
- The resulting download URL is saved in the property's `imageUrls` list in Firestore.

## 5. Security Note
- Initial implementation includes client-side ownership checks (`ownerId == currentUID`).
- Firestore security rules (see `docs/firestore-rules-draft.md`) must be applied in the Firebase Console to enforce these constraints at the database level.

## 6. Development Tools
- `DummyKosSeeder`: Can be used to populate a fresh Firestore instance with initial data for testing.
