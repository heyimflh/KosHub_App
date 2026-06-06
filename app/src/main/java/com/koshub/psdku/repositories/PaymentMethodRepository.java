package com.koshub.psdku.repositories;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.koshub.psdku.models.PaymentMethod;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentMethodRepository {
    private static final String TAG = "PaymentMethodRepo";
    private static PaymentMethodRepository instance;
    private final FirebaseFirestore db;

    private PaymentMethodRepository() {
        this.db = FirebaseService.getFirestore();
    }

    public static synchronized PaymentMethodRepository getInstance() {
        if (instance == null) {
            instance = new PaymentMethodRepository();
        }
        return instance;
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface PaymentMethodListCallback {
        void onSuccess(List<PaymentMethod> methods);
        void onError(String message);
    }

    public ListenerRegistration listenPaymentMethods(String userId, PaymentMethodListCallback callback) {
        return db.collection(DatabaseConstants.COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenPaymentMethods error: " + error.getMessage());
                        if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            callback.onError("Akses ditolak. Pastikan Anda memiliki izin.");
                        } else if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            callback.onError("Gagal memuat metode pembayaran. Silakan coba lagi.");
                        } else {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    List<PaymentMethod> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PaymentMethod m = doc.toObject(PaymentMethod.class);
                            if (m != null) {
                                m.setId(doc.getId());
                                list.add(m);
                            }
                        }

                        // Java-side sorting to avoid composite index requirements
                        Collections.sort(list, (a, b) -> {
                            if (a.isDefault() && !b.isDefault()) return -1;
                            if (!a.isDefault() && b.isDefault()) return 1;

                            if (a.isActive() && !b.isActive()) return -1;
                            if (!a.isActive() && b.isActive()) return 1;

                            long aTime = a.getUpdatedAt() > 0 ? a.getUpdatedAt() : a.getCreatedAt();
                            long bTime = b.getUpdatedAt() > 0 ? b.getUpdatedAt() : b.getCreatedAt();

                            return Long.compare(bTime, aTime);
                        });
                    }
                    callback.onSuccess(list);
                });
    }

    public void addPaymentMethod(PaymentMethod method, SimpleCallback callback) {
        if (method.getUserId() == null) {
            callback.onError("User ID is required");
            return;
        }
        method.setCreatedAt(System.currentTimeMillis());
        method.setUpdatedAt(System.currentTimeMillis());
        db.collection(DatabaseConstants.COLLECTION_PAYMENT_METHODS).add(method)
                .addOnSuccessListener(doc -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Akses database belum diizinkan. Periksa Firestore Rules.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void updatePaymentMethod(PaymentMethod method, SimpleCallback callback) {
        method.setUpdatedAt(System.currentTimeMillis());
        db.collection(DatabaseConstants.COLLECTION_PAYMENT_METHODS).document(method.getId()).set(method)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deletePaymentMethod(String methodId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_PAYMENT_METHODS).document(methodId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void setDefaultMethod(String userId, String methodId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        boolean isTarget = doc.getId().equals(methodId);
                        batch.update(doc.getReference(), "isDefault", isTarget);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void ensureDefaultQrisMethod(String userId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_PAYMENT_METHODS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        PaymentMethod qris = new PaymentMethod();
                        qris.setUserId(userId);
                        qris.setType(DatabaseConstants.PAYMENT_METHOD_QRIS);
                        qris.setProviderName("QRIS");
                        qris.setAccountName("Default QRIS");
                        qris.setDefault(true);
                        qris.setActive(true);
                        addPaymentMethod(qris, callback);
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
