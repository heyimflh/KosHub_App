# Firestore Rules Draft (Finance & Withdraw)

These are draft rules for securing the finance and withdrawal collections. Do not apply these automatically via code; they should be updated in the Firebase Console.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Transactions
    match /transactions/{transactionId} {
      // Owners can only read their own transactions
      allow read: if request.auth != null && resource.data.ownerId == request.auth.uid;
      
      // Creation should ideally be via Cloud Functions for security, 
      // but for client-side simulation:
      allow create: if request.auth != null; 
      
      // Only system or specific logic should update status
      allow update: if request.auth != null && resource.data.ownerId == request.auth.uid;
    }
    
    // Withdrawals
    match /withdrawals/{withdrawalId} {
      // Owners can only read their own withdrawals
      allow read: if request.auth != null && resource.data.ownerId == request.auth.uid;
      
      // Owners can create withdrawals for themselves
      allow create: if request.auth != null && request.resource.data.ownerId == request.auth.uid;
      
      // Owners should NOT be able to change status to 'success' themselves
      allow update: if request.auth != null && 
                    resource.data.ownerId == request.auth.uid && 
                    !request.resource.data.diff(resource.data).affectedKeys().hasAny(['status', 'amount']);
    }
  }
}
```
