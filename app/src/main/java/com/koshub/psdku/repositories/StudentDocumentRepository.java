package com.koshub.psdku.repositories;

import android.content.Context;
import android.net.Uri;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.koshub.psdku.models.StudentDocument;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.List;

public class StudentDocumentRepository {
    private static StudentDocumentRepository instance;
    private final FirebaseFirestore db;
    private final CloudinaryRepository cloudinaryRepository;

    private StudentDocumentRepository() {
        this.db = FirebaseService.getFirestore();
        this.cloudinaryRepository = CloudinaryRepository.getInstance();
    }

    public static synchronized StudentDocumentRepository getInstance() {
        if (instance == null) {
            instance = new StudentDocumentRepository();
        }
        return instance;
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface DocumentListCallback {
        void onSuccess(List<StudentDocument> documents);
        void onError(String message);
    }

    public ListenerRegistration listenDocuments(String userId, DocumentListCallback callback) {
        return db.collection(DatabaseConstants.COLLECTION_STUDENT_DOCUMENTS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("StudentDocRepo", "listenDocuments error: " + error.getMessage());
                        if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            callback.onError("Akses ditolak. Pastikan data dokumen Anda sudah benar.");
                        } else if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            callback.onError("Gagal memuat metode pembayaran. Silakan coba lagi.");
                        } else {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    List<StudentDocument> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            StudentDocument d = doc.toObject(StudentDocument.class);
                            if (d != null) {
                                d.setId(doc.getId());
                                list.add(d);
                            }
                        }

                        // Java-side sorting to avoid composite index requirements
                        java.util.Collections.sort(list, (a, b) -> {
                            long aTime = a.getUpdatedAt() > 0 ? a.getUpdatedAt() : a.getUploadedAt();
                            long bTime = b.getUpdatedAt() > 0 ? b.getUpdatedAt() : b.getUploadedAt();
                            return Long.compare(bTime, aTime);
                        });
                    }
                    callback.onSuccess(list);
                });
    }

    public void uploadDocument(Context context, Uri fileUri, String type, String title, SimpleCallback callback) {
        String uid = FirebaseService.getAuth().getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        // For simplicity, we can use a generic upload in CloudinaryRepository if we add one, 
        // or just use uploadImage directly. But CloudinaryRepository is private initialized.
        // Let's add a generic method to CloudinaryRepository or use uploadLegalDoc if it fits.
        // Actually I'll use the existing uploadImage logic or add a new one to CloudinaryRepository.
        
        // I will add uploadStudentDocument to CloudinaryRepository for cleaner access.
        cloudinaryRepository.uploadImage(context, fileUri, "documents/" + uid, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                saveDocumentToFirestore(uid, type, title, imageUrl, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void saveDocumentToFirestore(String userId, String type, String title, String imageUrl, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_STUDENT_DOCUMENTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StudentDocument doc;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        doc = queryDocumentSnapshots.getDocuments().get(0).toObject(StudentDocument.class);
                        doc.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                    } else {
                        doc = new StudentDocument();
                        doc.setUserId(userId);
                        doc.setType(type);
                    }
                    
                    doc.setTitle(title);
                    doc.setFileUrl(imageUrl);
                    doc.setStatus(DatabaseConstants.DOC_STATUS_PENDING);
                    doc.setUploadedAt(System.currentTimeMillis());
                    doc.setUpdatedAt(System.currentTimeMillis());

                    if (doc.getId() != null) {
                        db.collection(DatabaseConstants.COLLECTION_STUDENT_DOCUMENTS).document(doc.getId()).set(doc)
                                .addOnSuccessListener(aVoid -> {
                                    updateUserDocumentCache(userId, type, imageUrl);
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        callback.onError("Gagal mengupdate: Akses ditolak.");
                                    } else {
                                        callback.onError(e.getMessage());
                                    }
                                });
                    } else {
                        db.collection(DatabaseConstants.COLLECTION_STUDENT_DOCUMENTS).add(doc)
                                .addOnSuccessListener(dr -> {
                                    updateUserDocumentCache(userId, type, imageUrl);
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        callback.onError("Gagal menambah: Akses ditolak.");
                                    } else {
                                        callback.onError(e.getMessage());
                                    }
                                });
                    }
                });
    }

    public void deleteDocument(String documentId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_STUDENT_DOCUMENTS).document(documentId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateUserDocumentCache(String userId, String type, String fileUrl) {
        String field = null;
        if (DatabaseConstants.DOC_TYPE_KTP.equals(type)) {
            field = DatabaseConstants.FIELD_DOC_KTP;
        } else if (DatabaseConstants.DOC_TYPE_KTM.equals(type)) {
            field = DatabaseConstants.FIELD_DOC_KTM;
        }

        if (field != null) {
            db.collection(DatabaseConstants.COLLECTION_USERS).document(userId)
                    .update(field, fileUrl, DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());
        }
    }
}
