package com.koshub.psdku.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository for Booking data.
 * Handles room rental transactions via Firestore.
 */
public class BookingRepository {
    private static final String TAG = "KosHubBooking";
    private static BookingRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private BookingRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized BookingRepository getInstance() {
        if (instance == null) {
            instance = new BookingRepository();
        }
        return instance;
    }

    public interface BookingCallback {
        void onSuccess(Booking booking);
        void onError(String message);
    }

    public interface BookingListCallback {
        void onSuccess(List<Booking> bookings);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public void createBooking(Booking booking, SimpleCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String studentId = user.getUid();
        
        // 1. Fetch student profile to get Name/Email
        db.collection(DatabaseConstants.COLLECTION_USERS).document(studentId).get()
                .addOnSuccessListener(studentDoc -> {
                    if (!studentDoc.exists()) {
                        callback.onError("Student profile not found.");
                        return;
                    }

                    booking.setStudentId(studentId);
                    booking.setStudentName(studentDoc.getString(DatabaseConstants.FIELD_NAME));
                    booking.setStudentEmail(studentDoc.getString(DatabaseConstants.FIELD_EMAIL));
                    
                    booking.setStatus(DatabaseConstants.BOOKING_PENDING);
                    booking.setPaymentStatus(DatabaseConstants.PAYMENT_UNPAID);
                    booking.setBookingDate(System.currentTimeMillis());
                    booking.setCreatedAt(System.currentTimeMillis());
                    booking.setUpdatedAt(System.currentTimeMillis());

                    // 2. Check for existing active/pending booking for the same Kos
                    // Simple query to avoid composite index error
                    db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                            .whereEqualTo("studentId", studentId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                boolean hasActive = false;
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    String status = doc.getString("status");
                                    String kosId = doc.getString("kosId");
                                    
                                    if (booking.getKosId().equals(kosId) && (
                                        DatabaseConstants.BOOKING_PENDING.equals(status) || 
                                        DatabaseConstants.BOOKING_ACCEPTED.equals(status) ||
                                        DatabaseConstants.BOOKING_ACTIVE.equals(status) ||
                                        DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(status))) {
                                        hasActive = true;
                                        break;
                                    }
                                }

                                if (hasActive) {
                                    callback.onError("Kamu sudah memiliki booking/antrean aktif untuk kos ini.");
                                } else {
                                    // 3. Save the booking
                                    db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                                            .add(booking)
                                            .addOnSuccessListener(documentReference -> {
                                                booking.setId(documentReference.getId());
                                                callback.onSuccess();
                                            })
                                            .addOnFailureListener(e -> {
                                                android.util.Log.e(TAG, "Save booking failed: " + e.getMessage());
                                                callback.onError("Gagal menyimpan booking. Silakan coba lagi.");
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(TAG, "Duplicate check failed: " + e.getMessage());
                                callback.onError("Gagal mengecek antrean aktif.");
                            });
                })
                .addOnFailureListener(e -> callback.onError("Gagal memuat profil student."));
    }

    public void getBookingsByStudent(String studentId, BookingListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking b = doc.toObject(Booking.class);
                        b.setId(doc.getId());
                        list.add(b);
                    }
                    // Manual sorting to avoid composite index error
                    Collections.sort(list, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "getBookingsByStudent error: " + e.getMessage());
                    callback.onError("Gagal memuat data booking. Silakan coba lagi.");
                });
    }

    public void getBookingsByOwner(String ownerId, BookingListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking b = doc.toObject(Booking.class);
                        b.setId(doc.getId());
                        list.add(b);
                    }
                    // Manual sorting to avoid composite index error
                    Collections.sort(list, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "getBookingsByOwner error: " + e.getMessage());
                    callback.onError("Gagal memuat data booking masuk. Silakan coba lagi.");
                });
    }

    public void acceptBooking(String bookingId, String roomId, SimpleCallback callback) {
        DocumentReference bookingRef = db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId);
        
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 1. Update Booking Status and Audit
            transaction.update(bookingRef, "status", DatabaseConstants.BOOKING_ACCEPTED);
            transaction.update(bookingRef, "updatedAt", System.currentTimeMillis());
            transaction.update(bookingRef, DatabaseConstants.FIELD_UPDATED_BY, auth.getUid());
            transaction.update(bookingRef, DatabaseConstants.FIELD_STATUS_HISTORY, FieldValue.arrayUnion("ACCEPTED at " + System.currentTimeMillis()));

            // 2. Update Room Status
            if (roomId != null && !roomId.isEmpty()) {
                DocumentReference roomRef = db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId);
                transaction.update(roomRef, DatabaseConstants.FIELD_STATUS, DatabaseConstants.ROOM_BOOKED);
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> callback.onSuccess())
          .addOnFailureListener(e -> {
              android.util.Log.e(TAG, "Transaction failed: " + e.getMessage());
              callback.onError("Gagal menerima booking: " + e.getMessage());
          });
    }

    public void rejectBooking(String bookingId, String roomId, SimpleCallback callback) {
        DocumentReference bookingRef = db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.update(bookingRef, "status", DatabaseConstants.BOOKING_REJECTED);
            transaction.update(bookingRef, "updatedAt", System.currentTimeMillis());
            transaction.update(bookingRef, DatabaseConstants.FIELD_UPDATED_BY, auth.getUid());
            transaction.update(bookingRef, DatabaseConstants.FIELD_STATUS_HISTORY, FieldValue.arrayUnion("REJECTED at " + System.currentTimeMillis()));

            if (roomId != null && !roomId.isEmpty()) {
                DocumentReference roomRef = db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId);
                transaction.update(roomRef, DatabaseConstants.FIELD_STATUS, DatabaseConstants.ROOM_AVAILABLE);
            }
            return null;
        }).addOnSuccessListener(aVoid -> callback.onSuccess())
          .addOnFailureListener(e -> callback.onError("Gagal menolak booking: " + e.getMessage()));
    }

    public void cancelBooking(String bookingId, String roomId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update("status", DatabaseConstants.BOOKING_CANCELLED, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (roomId != null && !roomId.isEmpty()) {
                        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                                .update("status", DatabaseConstants.ROOM_AVAILABLE)
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onSuccess());
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void simulatePayment(String bookingId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update("status", DatabaseConstants.BOOKING_WAITING_CHECKIN, 
                        "paymentStatus", DatabaseConstants.PAYMENT_PAID,
                        "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markKeyTaken(String bookingId, String roomId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update("status", DatabaseConstants.BOOKING_ACTIVE, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (roomId != null && !roomId.isEmpty()) {
                        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                                .update("status", DatabaseConstants.ROOM_OCCUPIED)
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onSuccess());
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
