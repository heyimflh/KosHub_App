# Cloud Functions FCM Sender Sample (Node.js)

To enable actual background push notifications, deploy the following function to Firebase Cloud Functions.

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * Triggers when a new notification document is created in Firestore.
 */
exports.sendPushNotification = functions.firestore
    .document('notifications/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notificationData = snapshot.data();
        const recipientId = notificationData.recipientId;

        if (!recipientId) return null;

        // 1. Fetch active tokens for the recipient
        const tokensSnapshot = await admin.firestore()
            .collection('users')
            .document(recipientId)
            .collection('fcmTokens')
            .where('isActive', '==', true)
            .get();

        if (tokensSnapshot.empty) {
            console.log('No active tokens for user:', recipientId);
            return null;
        }

        const tokens = tokensSnapshot.docs.map(doc => doc.data().token);

        // 2. Build the FCM message
        const message = {
            notification: {
                title: notificationData.title,
                body: notificationData.body,
            },
            data: {
                notificationId: context.params.notificationId,
                type: notificationData.type || '',
                targetType: notificationData.targetType || '',
                targetId: notificationData.targetId || '',
            },
            tokens: tokens,
        };

        // 3. Send via Firebase Admin SDK
        try {
            const response = await admin.messaging().sendMulticast(message);
            console.log('Successfully sent push messages:', response.successCount);
            
            // Cleanup invalid tokens if any
            if (response.failureCount > 0) {
                const failedTokens = [];
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        failedTokens.push(tokens[idx]);
                    }
                });
                // Optional: Mark failed tokens as isActive = false in Firestore
            }

            // 4. Update notification status in Firestore
            return snapshot.ref.update({
                isDelivered: true,
                deliveredAt: admin.firestore.FieldValue.serverTimestamp()
            });
        } catch (error) {
            console.error('Error sending push:', error);
            return null;
        }
    });
```

## Setup Instructions
1. Initialize Firebase Functions in your project: `firebase init functions`.
2. Choose JavaScript or TypeScript.
3. Paste the code above into `index.js`.
4. Deploy: `firebase deploy --only functions`.
