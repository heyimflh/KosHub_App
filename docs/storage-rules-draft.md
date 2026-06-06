# Firebase Storage Security Rules Draft - Phase 5

Apply these rules in the Firebase Console to secure your storage buckets.

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Helper function to check if user is logged in
    function isSignedIn() {
      return request.auth != null;
    }

    // Property images: Anyone logged in can read, only owner can upload/delete
    match /kos/{ownerId}/{kosId}/{allPaths=**} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.auth.uid == ownerId;
    }

    // Room images: Anyone logged in can read, only owner can upload/delete
    match /rooms/{ownerId}/{kosId}/{roomId}/{allPaths=**} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.auth.uid == ownerId;
    }

    // Profile images: Anyone logged in can read, only user can upload/delete
    match /profiles/{userId}/{allPaths=**} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.auth.uid == userId;
    }

    // Complaint evidence: Only student and relevant owner can read, only student can upload
    match /complaints/{userId}/{complaintId}/{allPaths=**} {
      allow read: if isSignedIn(); // Refine later based on ownership
      allow write: if isSignedIn() && request.auth.uid == userId;
    }
  }
}
```
