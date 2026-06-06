# Cloudinary Image Upload Flow Documentation

This document describes the implementation of Cloudinary as the primary image upload and delivery service for KosHub_App.

## 1. Motivation
Cloudinary was chosen as an alternative to Firebase Storage to bypass the billing (Blaze plan) requirement for file uploads, while still maintaining high performance and secure delivery.

## 2. Configuration
- **Cloud Name**: `doyiag572`
- **Upload Preset**: `koshub_unsigned` (Unsigned mode for client-side uploads)
- **Base Folder**: `koshub`

## 3. Architecture
- **[CloudinaryConfig.java](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/java/com/koshub/psdku/utils/CloudinaryConfig.java)**: Centralized configuration. API Secrets are never stored in the application.
- **[CloudinaryRepository.java](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/java/com/koshub/psdku/repositories/CloudinaryRepository.java)**: Handles asynchronous uploads and subsequent Firestore updates.
- **Firestore Synchronization**: Once Cloudinary returns a `secure_url`, the repository updates the relevant Firestore collection (`users`, `kos`, or `rooms`).
- **Image Delivery**: **Glide** is used to fetch and cache images directly from the Cloudinary URLs.

## 4. Storage Structure
- **Profiles**: `koshub/profiles/{userId}`
- **Properties (Kos)**: `koshub/kos/{ownerId}/{kosId}`
- **Rooms**: `koshub/rooms/{ownerId}/{kosId}/{roomId}`

## 5. Security & Stability
- Uses **Unsigned Uploads** with a restricted preset to prevent unauthorized usage.
- Implements graceful error handling: if an upload fails, the application informs the user but maintains the integrity of textual data.
- Firebase Storage (`StorageRepository`) is marked as `@Deprecated`.

## 6. Manual Testing
1. Upload a profile picture and verify it appears in the Cloudinary Media Library and the app UI.
2. Add a new property with an image and confirm the `imageUrls` field in the Firestore `kos` collection contains the `secure_url`.
