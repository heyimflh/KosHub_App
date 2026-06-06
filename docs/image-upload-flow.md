# Image Upload Flow Documentation - Phase 5

This document describes the implementation of Firebase Storage for image uploads and Firestore URL persistence.

## 1. Storage Path Strategy
- **Kos Images**: `kos/{ownerId}/{kosId}/{timestamp}.jpg`
- **Room Images**: `rooms/{ownerId}/{kosId}/{roomId}/{timestamp}.jpg`
- **Profile Images**: `profiles/{userId}/profile_{timestamp}.jpg`
- **Complaint Evidence**: `complaints/{userId}/{complaintId}/{timestamp}.jpg`

## 2. Technical Components
- **StorageRepository**: The central hub for all Storage operations. It handles file uploads, download URL retrieval, and Firestore document updates.
- **Image Picker**: Uses the modern `ActivityResultLauncher` with `GetContent()` contract for safe and simple image selection.
- **Image Loading**: Integrated **Glide** to efficiently load and cache remote images from Firebase Storage URLs.

## 3. Integrated Flows

### Property (Kos) Photos
- Triggered during the "Add Kos" process in `OwnerDashboardActivity`.
- After the Firestore document is created, the selected image is uploaded.
- The resulting URL is appended to the `imageUrls` list field in the `kos` collection.

### User Profiles
- Implemented in `ProfileHistoryActivity` (Student) and `OwnerProfileSettingsActivity` (Owner).
- Clicking the profile avatar or edit button opens the image picker.
- Success updates the `profileImageUrl` field in the `users` collection and refreshes the local avatar.

## 4. Stability & Error Handling
- **Fallback Mechanism**: If Firebase Storage is not active or the upload fails, the application displays a toast and continues without the image, preventing crashes.
- **UI Refresh**: Activities use Glide to immediately update views once a new URL is available.
- **Legacy Compatibility**: Local drawable resources are used as placeholders or fallback for properties without remote images.

## 5. Next Steps
- Implement room photo uploads in the "Add Room" flow.
- Implement evidence upload for complaints once the complaint system is real.
- Set up automatic deletion of old files from Storage when an image is replaced or deleted.
