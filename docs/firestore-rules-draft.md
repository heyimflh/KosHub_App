# Firestore Security Rules Draft (Phase 8)

Add these rules to your Firestore configuration to secure the chat functionality.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ... existing rules ...

    // Chat Rules
    match /chats/{chatId} {
      allow read: if request.auth != null && (request.auth.uid == resource.data.studentId || request.auth.uid == resource.data.ownerId);
      allow create: if request.auth != null && (request.auth.uid == request.resource.data.studentId || request.auth.uid == request.resource.data.ownerId);
      allow update: if request.auth != null && (request.auth.uid == resource.data.studentId || request.auth.uid == resource.data.ownerId);
      
      match /messages/{messageId} {
        allow read: if request.auth != null && 
          (get(/databases/$(database)/documents/chats/$(chatId)).data.studentId == request.auth.uid || 
           get(/databases/$(database)/documents/chats/$(chatId)).data.ownerId == request.auth.uid);
           
        allow create: if request.auth != null && 
          request.resource.data.senderId == request.auth.uid &&
          (get(/databases/$(database)/documents/chats/$(chatId)).data.studentId == request.auth.uid || 
           get(/databases/$(database)/documents/chats/$(chatId)).data.ownerId == request.auth.uid);
           
        allow update: if request.auth != null && 
          (resource.data.receiverId == request.auth.uid); // For marking as read
      }
    }
  }
}
```
