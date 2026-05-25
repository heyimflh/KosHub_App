package com.koshub.psdku.repositories;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

/**
 * Repository for Firebase Storage operations.
 * @deprecated Firebase Storage requires Blaze plan in this project.
 * Active image uploads now use {@link com.koshub.psdku.repositories.CloudinaryRepository}.
 */
@Deprecated
public class StorageRepository {
    private static final String TAG = "KosHubStorage";
    private static StorageRepository instance;
    private final FirebaseStorage storage;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private StorageRepository() {
        this.storage = FirebaseService.getStorage();
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized StorageRepository getInstance() {
        if (instance == null) {
            instance = new StorageRepository();
        }
        return instance;
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String message);
    }

    /**
     * Upload property image.
     */
    public void uploadKosImage(Uri imageUri, String kosId, UploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String path = "kos/" + uid + "/" + kosId + "/" + System.currentTimeMillis() + ".jpg";
        uploadFile(imageUri, path, new UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId)
                        .update(DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(downloadUrl),
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(downloadUrl))
                        .addOnFailureListener(e -> callback.onError("Failed to update Firestore: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Upload room image.
     */
    public void uploadRoomImage(Uri imageUri, String kosId, String roomId, UploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String path = "rooms/" + uid + "/" + kosId + "/" + roomId + "/" + System.currentTimeMillis() + ".jpg";
        uploadFile(imageUri, path, new UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                        .update(DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(downloadUrl),
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(downloadUrl))
                        .addOnFailureListener(e -> callback.onError("Failed to update Firestore: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Upload profile image.
     */
    public void uploadProfileImage(Uri imageUri, UploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String path = "profiles/" + uid + "/profile_" + System.currentTimeMillis() + ".jpg";
        uploadFile(imageUri, path, new UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                db.collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                        .update(DatabaseConstants.FIELD_PROFILE_IMAGE_URL, downloadUrl,
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(downloadUrl))
                        .addOnFailureListener(e -> callback.onError("Failed to update profile URL: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Generic file upload helper.
     */
    private void uploadFile(Uri uri, String path, UploadCallback callback) {
        StorageReference ref = storage.getReference().child(path);
        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "Upload success: " + downloadUri.toString());
                            callback.onSuccess(downloadUri.toString());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                            callback.onError("Upload success, but failed to retrieve URL: " + e.getMessage());
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    callback.onError("Upload failed: " + e.getMessage());
                });
    }
}
