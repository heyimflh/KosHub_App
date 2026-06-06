# Authentication Flow Documentation - Phase 3

This document describes the implementation of real Firebase Authentication for KosHub.

## 1. Registration Flow (Email/Password)
1. User enters name, email, phone, and password.
2. App validates inputs (email format, password length).
3. `FirebaseAuth.createUserWithEmailAndPassword` is called.
4. On success:
   - A verification email is sent automatically.
   - A user document is created in Firestore: `users/{uid}`.
   - User is signed out immediately.
   - Redirected back to `LoginActivity` with a success message.

## 2. Login Flow (Email/Password)
1. User enters email and password.
2. `FirebaseAuth.signInWithEmailAndPassword` is called.
3. App checks if `emailVerified` is true.
4. If not verified:
   - User is blocked from entering.
   - A dialog offers to resend the verification email.
5. If verified:
   - User profile is updated in Firestore (`emailVerified: true`).
   - Role is fetched from Firestore.
   - Redirected to `StudentHomeActivity` or `OwnerDashboardActivity` based on role.

## 3. Google Sign-In Flow
1. User selects role (Student/Owner).
2. User clicks "Sign in with Google".
3. Account chooser opens.
4. On success:
   - Firebase signs in with Google credential.
   - App checks if a user document exists in Firestore.
   - If document exists: Role is fetched from Firestore (original role choice is ignored).
   - If document is new: User document is created with the selected role.
   - Redirected based on role.

## 4. Forgot Password Flow
1. User clicks "Forgot Password".
2. Dialog prompts for email.
3. `FirebaseAuth.sendPasswordResetEmail` is called.
4. Generic success message is shown to prevent account enumeration.

## 5. Firestore User Document Structure
Collection: `users`
- **id**: String (UID)
- **name**: String
- **email**: String
- **phone**: String
- **role**: String ("student" | "owner")
- **profileImageUrl**: String
- **provider**: String ("email" | "google")
- **emailVerified**: boolean
- **createdAt**: long
- **updatedAt**: long

## 6. Security and State Management
- `SessionManager` stores basic user info locally for UI convenience.
- `FirebaseAuth.getCurrentUser()` is the source of truth for auth state.
- `SplashActivity` handles auto-login and redirects authenticated users.

## 7. Configuration Requirements
- Firebase Console: Email/Password and Google providers enabled.
- SHA-1 and SHA-256 fingerprints added to Firebase Console for Google Sign-In.
- `google-services.json` must be up to date.
