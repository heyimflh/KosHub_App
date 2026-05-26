package com.koshub.psdku.repositories;

import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository to manage FCM tokens in Firestore.
 */
public class FCMTokenRepository {
    private static final String TAG = "KosHubFCM";
    private static FCMTokenRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FCMTokenRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized FCMTokenRepository getInstance() {
        if (instance == null) {
            instance = new FCMTokenRepository();
        }
        return instance;
    }

    public interface TokenCallback {
        void onSuccess();
        void onError(String message);
    }

    public void saveCurrentToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }

            String token = task.getResult();
            saveToken(token, null);
        });
    }

    public void saveToken(String token, TokenCallback callback) {
        String uid = auth.getUid();
        if (uid == null || token == null) {
            if (callback != null) callback.onError("UID or Token is null");
            return;
        }

        // Use a safe ID for the token document (e.g., first 20 chars of token or hash)
        String tokenId = token.substring(Math.max(0, token.length() - 20));

        Map<String, Object> data = new HashMap<>();
        data.put("id", tokenId);
        data.put("token", token);
        data.put("platform", "android");
        data.put("deviceName", Build.MODEL);
        data.put("isActive", true);
        data.put("updatedAt", System.currentTimeMillis());
        data.put("lastSeenAt", System.currentTimeMillis());

        db.collection(DatabaseConstants.COLLECTION_USERS)
                .document(uid)
                .collection(DatabaseConstants.COLLECTION_FCM_TOKENS)
                .document(tokenId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Token saved successfully for user: " + uid);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving token", e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void deactivateCurrentToken(TokenCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            if (callback != null) callback.onSuccess();
            return;
        }

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                String tokenId = token.substring(Math.max(0, token.length() - 20));

                db.collection(DatabaseConstants.COLLECTION_USERS)
                        .document(uid)
                        .collection(DatabaseConstants.COLLECTION_FCM_TOKENS)
                        .document(tokenId)
                        .update("isActive", false, "updatedAt", System.currentTimeMillis())
                        .addOnCompleteListener(t -> {
                            if (callback != null) callback.onSuccess();
                        });
            } else {
                if (callback != null) callback.onSuccess();
            }
        });
    }
}
