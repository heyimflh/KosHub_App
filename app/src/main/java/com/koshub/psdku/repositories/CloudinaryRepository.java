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
import com.koshub.psdku.utils.ImageCompressor;
import com.koshub.psdku.utils.UploadValidator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Cloudinary operations.
 * Handles image validation, compression, and uploads.
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

    public interface MultiUploadCallback {
        void onSuccess(List<String> imageUrls);
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
            isInitialized = true;
        }
    }

    /**
     * Core upload method with validation and compression.
     */
    public void uploadImage(Context context, Uri imageUri, String folder, SimpleUploadCallback callback) {
        initMediaManager(context);

        // 1. Validate
        UploadValidator.ValidationResult validation = UploadValidator.validateImage(context, imageUri);
        if (!validation.isValid) {
            callback.onError(validation.message);
            return;
        }

        // 2. Compress & Upload
        new Thread(() -> {
            try {
                File compressedFile = ImageCompressor.compressImage(context, imageUri);
                
                MediaManager.get().upload(Uri.fromFile(compressedFile))
                        .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                        .option("folder", folder)
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                Log.d(TAG, "Upload started to folder: " + folder);
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {}

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                String secureUrl = (String) resultData.get("secure_url");
                                // Delete temp file
                                if (compressedFile.exists()) compressedFile.delete();
                                callback.onSuccess(secureUrl);
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
                                if (compressedFile.exists()) compressedFile.delete();
                                callback.onError(error.getDescription());
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {}
                        }).dispatch();

            } catch (IOException e) {
                Log.e(TAG, "Compression failed", e);
                callback.onError("Gagal memproses gambar: " + e.getMessage());
            }
        }).start();
    }

    // --- Specialized Upload Methods ---

    public void uploadProfileImage(Context context, Uri imageUri, SimpleUploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/profiles/" + uid;
        uploadImage(context, imageUri, folder, new SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                db.collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                        .update(DatabaseConstants.FIELD_PROFILE_IMAGE_URL, imageUrl,
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(imageUrl))
                        .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void uploadLegalDoc(Context context, Uri imageUri, String docType, SimpleUploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/documents/" + uid;
        String field = docType.equalsIgnoreCase("ktp") ? DatabaseConstants.FIELD_DOC_KTP : DatabaseConstants.FIELD_DOC_SKU;

        uploadImage(context, imageUri, folder, new SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                db.collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                        .update(field, imageUrl,
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(imageUrl))
                        .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void uploadKosImage(Context context, Uri imageUri, String kosId, SimpleUploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/kos/" + uid + "/" + kosId;
        uploadImage(context, imageUri, folder, new SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId)
                        .update(DatabaseConstants.FIELD_IMAGE_URL, imageUrl,
                                DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(imageUrl),
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(imageUrl))
                        .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Sequential upload for multiple images.
     */
    public void uploadMultipleKosImages(Context context, List<Uri> imageUris, List<String> existingUrls, String kosId, MultiUploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            // If no new images, just sync existing ones if provided
            if (existingUrls != null) {
                updateKosImageUrlsInFirestore(kosId, existingUrls, true, callback);
            } else {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }

        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        if (existingUrls != null) uploadedUrls.addAll(existingUrls);
        
        uploadSequentially(context, imageUris, 0, uploadedUrls, uid, kosId, callback);
    }

    private void uploadSequentially(Context context, List<Uri> uris, int index, List<String> results, String uid, String kosId, MultiUploadCallback callback) {
        if (index >= uris.size()) {
            // All images uploaded, update Firestore (Overwrite mode because we merged)
            updateKosImageUrlsInFirestore(kosId, results, true, callback);
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/kos/" + uid + "/" + kosId;
        uploadImage(context, uris.get(index), folder, new SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                results.add(imageUrl);
                uploadSequentially(context, uris, index + 1, results, uid, kosId, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError("Upload ke-" + (index + 1) + " gagal: " + message);
            }
        });
    }

    private void updateKosImageUrlsInFirestore(String kosId, List<String> urls, boolean isOverwrite, MultiUploadCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        
        if (isOverwrite) {
            updates.put(DatabaseConstants.FIELD_IMAGE_URLS, urls);
        } else {
            updates.put(DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(urls.toArray()));
        }

        if (!urls.isEmpty()) {
            updates.put(DatabaseConstants.FIELD_IMAGE_URL, urls.get(0)); // Set first as main cover
        } else {
            updates.put(DatabaseConstants.FIELD_IMAGE_URL, ""); 
        }

        updates.put(DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());

        db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(urls))
                .addOnFailureListener(e -> callback.onError("Gagal update data kos: " + e.getMessage()));
    }

    public void uploadRoomImage(Context context, Uri imageUri, String kosId, String roomId, SimpleUploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/rooms/" + uid + "/" + kosId + "/" + roomId;
        uploadImage(context, imageUri, folder, new SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                        .update(DatabaseConstants.FIELD_IMAGE_URLS, FieldValue.arrayUnion(imageUrl),
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(imageUrl))
                        .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void uploadComplaintEvidence(Context context, Uri imageUri, String complaintId, SimpleUploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/complaints/" + uid + "/" + complaintId;
        uploadImage(context, imageUri, folder, new SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                db.collection(DatabaseConstants.COLLECTION_COMPLAINTS).document(complaintId)
                        .update(DatabaseConstants.FIELD_IMAGE_URL, imageUrl,
                                DatabaseConstants.FIELD_EVIDENCE_IMAGE_URLS, FieldValue.arrayUnion(imageUrl),
                                DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(imageUrl))
                        .addOnFailureListener(e -> callback.onError("Firestore update failed: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void uploadChatMessage(Context context, Uri imageUri, SimpleUploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        String folder = CloudinaryConfig.BASE_FOLDER + "/chats/" + uid;
        uploadImage(context, imageUri, folder, callback);
    }

    /**
     * Helper to get optimized image URL with transformations.
     */
    public String getOptimizedUrl(String originalUrl, int width, int height, boolean isCircle) {
        if (originalUrl == null || !originalUrl.contains("cloudinary.com")) return originalUrl;

        String transformation = "w_" + width + ",h_" + height + ",c_fill,g_auto,f_auto,q_auto";
        if (isCircle) {
            transformation += ",r_max";
        }

        if (originalUrl.contains("/upload/")) {
            return originalUrl.replace("/upload/", "/upload/" + transformation + "/");
        }
        
        return originalUrl;
    }
}
