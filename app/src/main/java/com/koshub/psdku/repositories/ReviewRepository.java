package com.koshub.psdku.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Review;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewRepository {
    private static final String TAG = "KosHubReview";
    private static ReviewRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private ReviewRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized ReviewRepository getInstance() {
        if (instance == null) {
            instance = new ReviewRepository();
        }
        return instance;
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ReviewListCallback {
        void onSuccess(List<Review> reviews);
        void onError(String message);
    }

    public void canReviewBooking(String bookingId, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("Kamu harus login.");
            return;
        }

        // Check if review already exists
        db.collection(DatabaseConstants.COLLECTION_REVIEWS).document(bookingId).get()
                .addOnSuccessListener(reviewDoc -> {
                    if (reviewDoc.exists()) {
                        callback.onError("Kamu sudah memberi review untuk booking ini.");
                        return;
                    }

                    // Check booking status
                    db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                            .addOnSuccessListener(bookingDoc -> {
                                if (!bookingDoc.exists()) {
                                    callback.onError("Data booking tidak ditemukan.");
                                    return;
                                }

                                Booking booking = bookingDoc.toObject(Booking.class);
                                if (booking == null || !uid.equals(booking.getStudentId())) {
                                    callback.onError("Kamu tidak berhak memberi review untuk ini.");
                                    return;
                                }

                                if (!DatabaseConstants.BOOKING_COMPLETED.equals(booking.getStatus())) {
                                    callback.onError("Review hanya bisa dibuat setelah masa sewa selesai.");
                                    return;
                                }

                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onError("Gagal mengecek status booking."));
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengecek review."));
    }

    public void createReview(Review review, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("Sesi berakhir.");
            return;
        }

        review.setStudentId(uid);
        review.setId(review.getBookingId());
        review.setCreatedAt(System.currentTimeMillis());
        review.setUpdatedAt(System.currentTimeMillis());

        db.collection(DatabaseConstants.COLLECTION_REVIEWS).document(review.getBookingId()).set(review)
                .addOnSuccessListener(aVoid -> recalculateKosRating(review.getKosId(), callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createReview error: " + e.getMessage());
                    callback.onError("Gagal mengirim review.");
                });
    }

    public void getReviewsByKos(String kosId, ReviewListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Review> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Review r = doc.toObject(Review.class);
                        list.add(r);
                    }
                    Collections.sort(list, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getReviewsByKos error: " + e.getMessage());
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk melihat ulasan ini.");
                    } else {
                        callback.onError("Gagal memuat ulasan.");
                    }
                });
    }

    public void recalculateKosRating(String kosId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    double total = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double rating = doc.getDouble(DatabaseConstants.FIELD_RATING_VALUE);
                        if (rating != null) total += rating;
                    }

                    double average = count > 0 ? total / count : 0;

                    WriteBatch batch = db.batch();
                    DocumentReference kosRef = db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId);
                    batch.update(kosRef, DatabaseConstants.FIELD_RATING_AVERAGE, average);
                    batch.update(kosRef, DatabaseConstants.FIELD_RATING_COUNT, count);
                    batch.update(kosRef, DatabaseConstants.FIELD_RATING, average); // for compatibility

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "recalculateKosRating update error: " + e.getMessage());
                                callback.onError("Gagal memperbarui rating kos.");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "recalculateKosRating fetch error: " + e.getMessage());
                    callback.onError("Gagal menghitung rating.");
                });
    }

    public void getReviewsByStudent(String studentId, ReviewListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .whereEqualTo(DatabaseConstants.FIELD_STUDENT_ID, studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Review> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Review r = doc.toObject(Review.class);
                        list.add(r);
                    }
                    Collections.sort(list, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getReviewsByStudent error: " + e.getMessage());
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk melihat riwayat ulasan ini.");
                    } else {
                        callback.onError("Gagal memuat riwayat ulasan.");
                    }
                });
    }
}
