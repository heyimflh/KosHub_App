package com.koshub.psdku.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public void getBookingById(String bookingId, BookingCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Booking b = mapBookingSafely(documentSnapshot);
                            callback.onSuccess(b);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error mapping booking: " + e.getMessage());
                            callback.onError("Gagal memproses data booking.");
                        }
                    } else {
                        callback.onError("Booking not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public Booking mapBookingSafely(DocumentSnapshot doc) {
        Booking b = doc.toObject(Booking.class);
        if (b == null) return null;
        b.setId(doc.getId());

        // Extra precautions for specific fields that might crash due to type mismatch
        b.setPaymentStatus(getStringSafe(doc, "paymentStatus", "unpaid"));
        b.setStatus(getStringSafe(doc, "status", "pending"));
        b.setGatewayTransactionId(getLongSafe(doc, "gatewayTransactionId", 0L));
        b.setTotalBayar(getDoubleSafe(doc, "totalBayar", 0.0));
        b.setQrisString(getStringSafe(doc, "qrisString", null));
        
        // Timestamps
        if (doc.contains("paymentCreatedAt")) b.setPaymentCreatedAt(doc.getTimestamp("paymentCreatedAt"));
        if (doc.contains("paidAt")) b.setPaidAt(doc.getTimestamp("paidAt"));

        return b;
    }

    private String getStringSafe(DocumentSnapshot doc, String field, String defaultValue) {
        if (!doc.contains(field)) return defaultValue;
        Object val = doc.get(field);
        return val == null ? defaultValue : String.valueOf(val);
    }

    private Long getLongSafe(DocumentSnapshot doc, String field, Long defaultValue) {
        if (!doc.contains(field)) return defaultValue;
        Object val = doc.get(field);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Double getDoubleSafe(DocumentSnapshot doc, String field, Double defaultValue) {
        if (!doc.contains(field)) return defaultValue;
        Object val = doc.get(field);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public void savePaymentDraft(String bookingId, long idTransaksi, double totalBayar, String qrisString, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        if (callback != null) callback.onError("Booking not found");
                        return;
                    }

                    Booking b = doc.toObject(Booking.class);
                    if (b == null) {
                        if (callback != null) callback.onError("Failed to parse booking");
                        return;
                    }

                    // 1. Update Booking
                    db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                            .update(
                                    "paymentStatus", DatabaseConstants.PAYMENT_PENDING,
                                    "status", DatabaseConstants.BOOKING_WAITING_PAYMENT,
                                    "gatewayTransactionId", idTransaksi,
                                    "totalBayar", totalBayar,
                                    "qrisString", qrisString,
                                    "paymentCreatedAt", FieldValue.serverTimestamp(),
                                    "updatedAt", FieldValue.serverTimestamp()
                            );

                    // 2. Create/Update Payment Document
                    String paymentDocId = bookingId + "_" + idTransaksi;
                    java.util.Map<String, Object> paymentData = new java.util.HashMap<>();
                    paymentData.put("bookingId", bookingId);
                    paymentData.put("studentId", b.getStudentId());
                    paymentData.put("ownerId", b.getOwnerId());
                    paymentData.put("kosId", b.getKosId());
                    paymentData.put("roomId", b.getRoomId());
                    paymentData.put("amount", totalBayar);
                    paymentData.put("status", DatabaseConstants.PAYMENT_PENDING);
                    paymentData.put("gateway", "custom_qris_alwaysdata");
                    paymentData.put("gatewayTransactionId", idTransaksi);
                    paymentData.put("qrisString", qrisString);
                    paymentData.put("createdAt", FieldValue.serverTimestamp());
                    paymentData.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection(DatabaseConstants.COLLECTION_PAYMENTS).document(paymentDocId)
                            .set(paymentData)
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void updateBookingToPaid(String bookingId, long idTransaksi, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update(
                        "paymentStatus", DatabaseConstants.PAYMENT_PAID,
                        "status", DatabaseConstants.BOOKING_WAITING_CHECKIN,
                        "gatewayTransactionId", idTransaksi,
                        "paidAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp(),
                        DatabaseConstants.FIELD_STATUS_HISTORY, FieldValue.arrayUnion("PAID via AlwaysData ID " + idTransaksi + " at " + System.currentTimeMillis())
                )
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
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
                                                
                                                // Trigger Notification for Owner
                                                NotificationRepository.getInstance().createNotification(
                                                        booking.getOwnerId(),
                                                        studentId,
                                                        DatabaseConstants.NOTIF_BOOKING_NEW,
                                                        "Ada booking baru masuk",
                                                        booking.getStudentName() + " mengajukan booking untuk " + booking.getKosName(),
                                                        DatabaseConstants.TARGET_OWNER_BOOKING,
                                                        booking.getId()
                                                );
                                                
                                                callback.onSuccess();
                                            })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Save booking failed: " + e.getMessage());
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        android.util.Log.e(DatabaseConstants.LOG_TAG_SECURITY, "Permission denied creating booking: " + e.getMessage());
                        callback.onError("Kamu tidak memiliki izin untuk membuat booking ini.");
                    } else {
                        callback.onError("Gagal menyimpan booking. Silakan coba lagi.");
                    }
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
                        try {
                            Booking b = mapBookingSafely(doc);
                            if (b != null) list.add(b);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error mapping booking list item Student: " + e.getMessage());
                        }
                    }
                    // Manual sorting to avoid composite index error
                    Collections.sort(list, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "getBookingsByStudent error: " + e.getMessage());
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        android.util.Log.e(DatabaseConstants.LOG_TAG_SECURITY, "Permission denied fetching student bookings: " + e.getMessage());
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError("Gagal memuat data booking. Silakan coba lagi.");
                    }
                });
    }

    public void getBookingsByOwner(String ownerId, BookingListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Booking b = mapBookingSafely(doc);
                            if (b != null) list.add(b);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error mapping booking list item Owner: " + e.getMessage());
                        }
                    }
                    // Manual sorting to avoid composite index error
                    Collections.sort(list, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "getBookingsByOwner error: " + e.getMessage());
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError("Gagal memuat data booking masuk. Silakan coba lagi.");
                    }
                });
    }

    public void acceptBooking(String bookingId, String roomId, SimpleCallback callback) {
        android.util.Log.d(TAG, "acceptBooking called: id=" + bookingId + ", roomId=" + roomId);
        
        if (bookingId == null || bookingId.trim().isEmpty()) {
            android.util.Log.e(TAG, "acceptBooking: bookingId is null or empty");
            if (callback != null) callback.onError("ID Booking tidak valid.");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("Sesi berakhir. Silakan login ulang.");
            return;
        }

        DocumentReference bookingRef = db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId);
        
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 1. Fetch booking to verify owner and status
            com.google.firebase.firestore.DocumentSnapshot bookingSnapshot = transaction.get(bookingRef);
            if (!bookingSnapshot.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Booking tidak ditemukan.", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String currentStatus = bookingSnapshot.getString("status");
            String ownerId = bookingSnapshot.getString("ownerId");

            if (!user.getUid().equals(ownerId)) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Kamu tidak memiliki akses ke booking ini.", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            if (!DatabaseConstants.BOOKING_PENDING.equals(currentStatus)) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Booking sudah diproses.", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            // 2. Update Booking Status and Audit
            transaction.update(bookingRef, "status", DatabaseConstants.BOOKING_ACCEPTED);
            transaction.update(bookingRef, "updatedAt", System.currentTimeMillis());
            transaction.update(bookingRef, DatabaseConstants.FIELD_UPDATED_BY, user.getUid());
            transaction.update(bookingRef, DatabaseConstants.FIELD_STATUS_HISTORY, FieldValue.arrayUnion("ACCEPTED at " + System.currentTimeMillis()));

            // 3. Update Room Status
            if (roomId != null && !roomId.trim().isEmpty()) {
                android.util.Log.d(TAG, "acceptBooking: updating room " + roomId);
                DocumentReference roomRef = db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId);
                transaction.update(roomRef, DatabaseConstants.FIELD_STATUS, DatabaseConstants.ROOM_BOOKED);
            } else {
                android.util.Log.d(TAG, "acceptBooking: no roomId provided, skipping room update");
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            android.util.Log.d(TAG, "acceptBooking: successfully accepted " + bookingId);
            
            // Trigger Notification for Student
            db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                    .addOnSuccessListener(doc -> {
                        String studentId = doc.getString("studentId");
                        String kosName = doc.getString("kosName");
                        NotificationRepository.getInstance().createNotification(
                                studentId,
                                user.getUid(),
                                DatabaseConstants.NOTIF_BOOKING_ACCEPTED,
                                "Booking kamu diterima",
                                "Booking untuk " + kosName + " telah diterima owner.",
                                DatabaseConstants.TARGET_WAITING_LIST,
                                bookingId
                        );
                    });

            FinanceRepository.getInstance().cancelTransactionByBooking(bookingId, null);
            if (callback != null) callback.onSuccess();
        })
          .addOnFailureListener(e -> {
              android.util.Log.e(TAG, "acceptBooking failed: " + e.getMessage());
              if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                  ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                  if (callback != null) callback.onError("Akses ditolak. Kamu bukan pemilik data ini.");
              } else {
                  if (callback != null) callback.onError(e.getMessage());
              }
          });
    }

    public void rejectBooking(String bookingId, String roomId, SimpleCallback callback) {
        android.util.Log.d(TAG, "rejectBooking called: id=" + bookingId + ", roomId=" + roomId);

        if (bookingId == null || bookingId.trim().isEmpty()) {
            if (callback != null) callback.onError("ID Booking tidak valid.");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("Sesi berakhir. Silakan login ulang.");
            return;
        }

        DocumentReference bookingRef = db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot bookingSnapshot = transaction.get(bookingRef);
            if (!bookingSnapshot.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Booking tidak ditemukan.", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String currentStatus = bookingSnapshot.getString("status");
            String ownerId = bookingSnapshot.getString("ownerId");

            if (!user.getUid().equals(ownerId)) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Kamu tidak memiliki akses ke booking ini.", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            transaction.update(bookingRef, "status", DatabaseConstants.BOOKING_REJECTED);
            transaction.update(bookingRef, "updatedAt", System.currentTimeMillis());
            transaction.update(bookingRef, DatabaseConstants.FIELD_UPDATED_BY, user.getUid());
            transaction.update(bookingRef, DatabaseConstants.FIELD_STATUS_HISTORY, FieldValue.arrayUnion("REJECTED at " + System.currentTimeMillis()));

            if (roomId != null && !roomId.trim().isEmpty()) {
                android.util.Log.d(TAG, "rejectBooking: updating room " + roomId);
                DocumentReference roomRef = db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId);
                transaction.update(roomRef, DatabaseConstants.FIELD_STATUS, DatabaseConstants.ROOM_AVAILABLE);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            android.util.Log.d(TAG, "rejectBooking: successfully rejected " + bookingId);
            
            // Trigger Notification for Student
            db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                    .addOnSuccessListener(doc -> {
                        String studentId = doc.getString("studentId");
                        String kosName = doc.getString("kosName");
                        NotificationRepository.getInstance().createNotification(
                                studentId,
                                user.getUid(),
                                DatabaseConstants.NOTIF_BOOKING_REJECTED,
                                "Booking kamu ditolak",
                                "Booking untuk " + kosName + " ditolak owner.",
                                DatabaseConstants.TARGET_WAITING_LIST,
                                bookingId
                        );
                    });

            FinanceRepository.getInstance().cancelTransactionByBooking(bookingId, null);
            if (callback != null) callback.onSuccess();
        })
          .addOnFailureListener(e -> {
              android.util.Log.e(TAG, "rejectBooking failed: " + e.getMessage());
              if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                  ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                  if (callback != null) callback.onError("Akses ditolak. Kamu bukan pemilik data ini.");
              } else {
                  if (callback != null) callback.onError(e.getMessage());
              }
          });
    }

    public void cancelBooking(String bookingId, String roomId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update("status", DatabaseConstants.BOOKING_CANCELLED, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    FinanceRepository.getInstance().cancelTransactionByBooking(bookingId, null);
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

    public void markKeyTaken(String bookingId, String roomId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update("status", DatabaseConstants.BOOKING_ACTIVE, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    // Trigger Notification for Owner
                    db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                            .addOnSuccessListener(doc -> {
                                String ownerId = doc.getString("ownerId");
                                String studentId = doc.getString("studentId");
                                String kosName = doc.getString("kosName");
                                NotificationRepository.getInstance().createNotification(
                                        ownerId,
                                        studentId,
                                        DatabaseConstants.NOTIF_FINANCE_AVAILABLE,
                                        "Penyewa sudah mengambil kunci",
                                        "Saldo booking " + kosName + " kini tersedia.",
                                        DatabaseConstants.TARGET_FINANCE,
                                        bookingId
                                );
                            });

                    FinanceRepository.getInstance().markTransactionAvailableByBooking(bookingId, null);
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

    public void completeBooking(String bookingId, String roomId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId)
                .update("status", DatabaseConstants.BOOKING_COMPLETED, "updatedAt", System.currentTimeMillis())
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

    public void getCompletedBookingForKos(String studentId, String kosId, BookingCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("kosId", kosId)
                .whereEqualTo("status", DatabaseConstants.BOOKING_COMPLETED)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.QueryDocumentSnapshot doc = (com.google.firebase.firestore.QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Booking b = doc.toObject(Booking.class);
                        b.setId(doc.getId());
                        callback.onSuccess(b);
                    } else {
                        callback.onError("No completed booking found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
