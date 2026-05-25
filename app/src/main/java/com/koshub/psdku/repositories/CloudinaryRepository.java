package com.koshub.psdku.repositories;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.CloudinaryConfig;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for Cloudinary operations.
 * Handles image uploads and Firestore URL updates.
 */
public class CloudinaryRepository {
    private static final String TAG = "KosHubCloudinary";
    private static CloudinaryRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private boolean isInitialized = false;

    private CloudinaryRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized CloudinaryRepository getInstance() {
        if (instance == null) {
            instance = new CloudinaryRepository();
        }
        return instance;
    }

    public interface SimpleUploadCallback {
        void onSuccess(String imageUrl);
        void onError(String message);
    }

    private void initMediaManager(Context context) {
        if (isInitialized) return;
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CloudinaryConfig.CLOUD_NAME);
            MediaManager.init(context, config);
            isInitialized = true;
        } catch (IllegalStateException e) {
            // Already initialized
            isInitialized = true;
        }
    }

    public void uploadProfileImage(Context context, Uri imageUri, SimpleUploadCallback callback) {
        initMediaManager(context);
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/profiles/" + uid;
        MediaManager.get().upload(imageUri)
                .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { Log.d(TAG, "Profile upload start"); }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        db.collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                                .update(DatabaseConstants.FIELD_PROFILE_IMAGE_URL, secureUrl,
                                        DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(secureUrl))
                                .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        callback.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    public void uploadKosImage(Context context, Uri imageUri, String kosId, SimpleUploadCallback callback) {
        initMediaManager(context);
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/kos/" + uid + "/" + kosId;
        MediaManager.get().upload(imageUri)
                .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { Log.d(TAG, "Kos upload start"); }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId)
                                .update(DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(secureUrl),
                                        DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(secureUrl))
                                .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    public void uploadRoomImage(Context context, Uri imageUri, String kosId, String roomId, SimpleUploadCallback callback) {
        initMediaManager(context);
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/rooms/" + uid + "/" + kosId + "/" + roomId;
        MediaManager.get().upload(imageUri)
                .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                .option("folder", folder)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                                .update(DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(secureUrl),
                                        DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(secureUrl))
                                .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
}
