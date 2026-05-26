package com.koshub.psdku.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.koshub.psdku.models.AppNotification;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository for managing App Notifications in Firestore.
 */
public class NotificationRepository {
    private static final String TAG = "KosHubNotification";
    private static NotificationRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private NotificationRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    public interface NotificationListCallback {
        void onSuccess(List<AppNotification> notifications);
        void onError(String message);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public void createNotification(String recipientId, String senderId, String type, String title, String body, String targetType, String targetId) {
        if (recipientId == null || recipientId.isEmpty()) return;

        DocumentReference docRef = db.collection(DatabaseConstants.COLLECTION_NOTIFICATIONS).document();
        AppNotification notification = new AppNotification(docRef.getId(), recipientId, senderId, type, title, body, targetType, targetId);

        docRef.set(notification)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification created for: " + recipientId))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating notification", e));
    }

    public ListenerRegistration listenNotificationsForCurrentUser(NotificationListCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not logged in");
            return null;
        }

        return db.collection(DatabaseConstants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<AppNotification> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            AppNotification n = doc.toObject(AppNotification.class);
                            n.setId(doc.getId());
                            list.add(n);
                        }
                        // Manual sort by createdAt desc
                        Collections.sort(list, (n1, n2) -> Long.compare(n2.getCreatedAt(), n1.getCreatedAt()));
                        callback.onSuccess(list);
                    }
                });
    }

    public ListenerRegistration listenUnreadCount(CountCallback callback) {
        String uid = auth.getUid();
        if (uid == null) return null;

        return db.collection(DatabaseConstants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        callback.onSuccess(value.size());
                    }
                });
    }

    public void markAsRead(String notificationId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_NOTIFICATIONS).document(notificationId)
                .update("read", true, "readAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void markAllAsRead(SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection(DatabaseConstants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    long now = System.currentTimeMillis();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "read", true, "readAt", now);
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess();
                    }).addOnFailureListener(e -> {
                        if (callback != null) callback.onError(e.getMessage());
                    });
                });
    }
}
