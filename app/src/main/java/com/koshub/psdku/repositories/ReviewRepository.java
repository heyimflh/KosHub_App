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

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public interface ReviewCallback {
        void onSuccess(Review review);
        void onError(String message);
    }

    public void getUserReviewForKos(String kosId, String studentId, ReviewCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .whereEqualTo(DatabaseConstants.FIELD_STUDENT_ID, studentId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Review r = doc.toObject(Review.class);
                        if (r != null && (r.getId() == null || r.getId().isEmpty())) {
                            r.setId(doc.getId());
                        }
                        callback.onSuccess(r);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserReviewForKos error: " + e.getMessage());
                    callback.onError("Gagal mengecek ulasan anda.");
                });
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

                                Booking booking = BookingRepository.getInstance().mapBookingSafely(bookingDoc);
                                if (booking == null || !uid.equals(booking.getStudentId())) {
                                    callback.onError("Kamu tidak berhak memberi review untuk ini.");
                                    return;
                                }

                                if (!isReviewableBookingStatus(booking.getStatus())) {
                                    callback.onError("Review hanya bisa dibuat saat masa sewa aktif atau sudah selesai.");
                                    return;
                                }

                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onError("Gagal mengecek status booking."));
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengecek review."));
    }

    private boolean isReviewableBookingStatus(String status) {
        return DatabaseConstants.BOOKING_ACTIVE.equals(status)
                || DatabaseConstants.BOOKING_COMPLETED.equals(status);
    }

    public void getReviewableBookingForKos(String studentId, String kosId, BookingRepository.BookingCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("kosId", kosId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> candidates = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Booking b = BookingRepository.getInstance().mapBookingSafely(doc);
                        if (b != null && isReviewableBookingStatus(b.getStatus())) {
                            candidates.add(b);
                        }
                    }

                    if (candidates.isEmpty()) {
                        callback.onError("Ulasan hanya tersedia untuk penghuni aktif atau riwayat sewa yang sudah selesai.");
                        return;
                    }

                    // Prioritize ACTIVE, then COMPLETED (latest by date)
                    Collections.sort(candidates, (b1, b2) -> {
                        if (DatabaseConstants.BOOKING_ACTIVE.equals(b1.getStatus()) && !DatabaseConstants.BOOKING_ACTIVE.equals(b2.getStatus())) return -1;
                        if (!DatabaseConstants.BOOKING_ACTIVE.equals(b1.getStatus()) && DatabaseConstants.BOOKING_ACTIVE.equals(b2.getStatus())) return 1;
                        return Long.compare(b2.getCreatedAt(), b1.getCreatedAt());
                    });

                    callback.onSuccess(candidates.get(0));
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengecek riwayat sewa: " + e.getMessage()));
    }

    public void createReview(Review review, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            if (callback != null) callback.onError("Sesi berakhir.");
            return;
        }

        review.setStudentId(uid);
        
        // Handle ID: priority review.getId() > review.getBookingId()
        String docId = review.getId();
        if (docId == null || docId.isEmpty()) {
            docId = review.getBookingId();
        }
        
        if (docId == null || docId.isEmpty()) {
            if (callback != null) callback.onError("Data booking tidak valid.");
            return;
        }
        
        review.setId(docId);
        
        long now = System.currentTimeMillis();
        if (review.getCreatedAt() <= 0) {
            review.setCreatedAt(now);
        }
        review.setUpdatedAt(now);

        // Ensure studentName is set from current user if missing
        if (review.getStudentName() == null || review.getStudentName().isEmpty()) {
            if (auth.getCurrentUser() != null) {
                String name = auth.getCurrentUser().getDisplayName();
                if (name == null || name.isEmpty()) name = auth.getCurrentUser().getEmail();
                review.setStudentName(name);
            }
        }

        db.collection(DatabaseConstants.COLLECTION_REVIEWS).document(docId).set(review)
                .addOnSuccessListener(aVoid -> {
                    // Review is considered successful once the document is saved.
                    if (callback != null) callback.onSuccess();

                    // Aggregate rating update is best-effort.
                    recalculateKosRatingBestEffort(review.getKosId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createReview error: " + e.getMessage());
                    if (callback != null) callback.onError("Gagal mengirim review.");
                });
    }

    private void recalculateKosRatingBestEffort(String kosId) {
        if (kosId == null || kosId.isEmpty()) {
            Log.w(TAG, "recalculateKosRatingBestEffort skipped: kosId is empty");
            return;
        }

        db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    double total = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double rating = doc.getDouble(DatabaseConstants.FIELD_RATING_VALUE);
                        if (rating != null && rating >= 1 && rating <= 5) {
                            total += rating;
                            count++;
                        }
                    }

                    double average = count > 0 ? total / count : 0;
                    final int finalCount = count;
                    final double finalAverage = average;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put(DatabaseConstants.FIELD_RATING_AVERAGE, average);
                    updates.put(DatabaseConstants.FIELD_RATING_COUNT, count);
                    updates.put(DatabaseConstants.FIELD_RATING, average);
                    updates.put(DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());

                    db.collection(DatabaseConstants.COLLECTION_KOS)
                            .document(kosId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Aggregate rating kos berhasil diupdate: avg=" + finalAverage + ", count=" + finalCount))
                            .addOnFailureListener(e -> {
                                // We don't notify user about this failure as it might be due to Firestore Rules
                                Log.w(TAG, "Aggregate rating kos gagal diupdate. Review tetap tersimpan. Error: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Gagal membaca reviews untuk hitung aggregate. Error: " + e.getMessage());
                });
    }

    public static class RatingSummary {
        public double average;
        public int count;

        public RatingSummary(double average, int count) {
            this.average = average;
            this.count = count;
        }
    }

    public interface RatingSummaryCallback {
        void onSuccess(Map<String, RatingSummary> summaries);
        void onError(String message);
    }

    public ListenerRegistration listenRatingSummaries(RatingSummaryCallback callback) {
        return db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenRatingSummaries error: " + error.getMessage(), error);
                        callback.onError("Gagal memuat ringkasan rating.");
                        return;
                    }

                    Map<String, double[]> buckets = new HashMap<>();

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Review r = doc.toObject(Review.class);
                            if (r == null) continue;

                            String kosId = r.getKosId();
                            double rating = r.getRating();

                            if (kosId == null || kosId.isEmpty()) continue;
                            if (rating < 1 || rating > 5) continue;

                            double[] bucket = buckets.get(kosId);
                            if (bucket == null) {
                                bucket = new double[]{0, 0};
                                buckets.put(kosId, bucket);
                            }

                            bucket[0] += rating;
                            bucket[1] += 1;
                        }
                    }

                    Map<String, RatingSummary> result = new HashMap<>();
                    for (Map.Entry<String, double[]> entry : buckets.entrySet()) {
                        double total = entry.getValue()[0];
                        int count = (int) entry.getValue()[1];
                        double average = count > 0 ? total / count : 0;
                        result.put(entry.getKey(), new RatingSummary(average, count));
                    }

                    callback.onSuccess(result);
                });
    }

    public ListenerRegistration listenReviewsByKos(String kosId, ReviewListCallback callback) {
        return db.collection(DatabaseConstants.COLLECTION_REVIEWS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenReviewsByKos error: " + error.getMessage());
                        callback.onError("Gagal memuat ulasan.");
                        return;
                    }

                    List<Review> list = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Review r = doc.toObject(Review.class);
                            if (r != null) {
                                if (r.getId() == null || r.getId().isEmpty()) {
                                    r.setId(doc.getId());
                                }
                                list.add(r);
                            }
                        }
                    }

                    Collections.sort(list, (r1, r2) -> Long.compare(getSafeTime(r2), getSafeTime(r1)));
                    callback.onSuccess(list);
                });
    }

    private long getSafeTime(Review r) {
        if (r == null) return 0;
        if (r.getCreatedAt() > 0) return r.getCreatedAt();
        return r.getUpdatedAt();
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
