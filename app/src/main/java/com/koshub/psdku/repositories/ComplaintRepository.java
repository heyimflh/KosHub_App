package com.koshub.psdku.repositories;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Complaint;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Complaint data.
 * Handles Firestore logic for tenant complaints.
 */
public class ComplaintRepository {
    private static final String TAG = "KosHubComplaint";
    private static ComplaintRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private ComplaintRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized ComplaintRepository getInstance() {
        if (instance == null) {
            instance = new ComplaintRepository();
        }
        return instance;
    }

    public interface ComplaintCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ComplaintListCallback {
        void onSuccess(List<Complaint> complaints);
        void onError(String message);
    }

    /**
     * Create a new complaint from an active booking.
     */
    public void createComplaintFromBooking(Context context, String bookingId, String title, String description, Uri imageUri, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("User not authenticated");
            return;
        }

        // 1. Fetch Booking Details
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onError("Booking tidak ditemukan");
                        return;
                    }

                    Booking booking = documentSnapshot.toObject(Booking.class);
                    if (booking == null) {
                        callback.onError("Gagal memproses data booking");
                        return;
                    }

                    // 2. Validate Student Ownership & Active Status
                    if (!uid.equals(booking.getStudentId())) {
                        callback.onError("Akses ditolak. Ini bukan booking kamu.");
                        return;
                    }

                    if (!DatabaseConstants.BOOKING_ACTIVE.equals(booking.getStatus())) {
                        callback.onError("Komplain hanya bisa dibuat setelah sewa aktif.");
                        return;
                    }

                    // 3. Create Complaint Object
                    Complaint complaint = new Complaint();
                    complaint.setStudentId(uid);
                    complaint.setStudentName(booking.getStudentName());
                    complaint.setStudentEmail(booking.getStudentEmail());
                    complaint.setOwnerId(booking.getOwnerId());
                    complaint.setKosId(booking.getKosId());
                    complaint.setKosName(booking.getKosName());
                    complaint.setBookingId(bookingId);
                    complaint.setRoomId(booking.getRoomId());
                    complaint.setRoomName(booking.getRoomName());
                    complaint.setTitle(title);
                    complaint.setDescription(description);
                    complaint.setStatus(DatabaseConstants.COMPLAINT_NEW);
                    complaint.setCreatedAt(System.currentTimeMillis());
                    complaint.setUpdatedAt(System.currentTimeMillis());

                    // 4. Save to Firestore
                    db.collection(DatabaseConstants.COLLECTION_COMPLAINTS).add(complaint)
                            .addOnSuccessListener(docRef -> {
                                String complaintId = docRef.getId();
                                docRef.update(DatabaseConstants.FIELD_ID, complaintId);

                                // Trigger Notification for Owner
                                NotificationRepository.getInstance().createNotification(
                                        complaint.getOwnerId(),
                                        uid,
                                        DatabaseConstants.NOTIF_COMPLAINT_NEW,
                                        "Komplain baru",
                                        complaint.getStudentName() + " mengirim komplain untuk " + complaint.getKosName(),
                                        DatabaseConstants.TARGET_COMPLAINT,
                                        complaintId
                                );

                                // 5. Handle Image Upload if exists
                                if (imageUri != null) {
                                    CloudinaryRepository.getInstance().uploadComplaintEvidence(context, imageUri, complaintId, new CloudinaryRepository.SimpleUploadCallback() {
                                        @Override
                                        public void onSuccess(String imageUrl) {
                                            callback.onSuccess();
                                        }

                                        @Override
                                        public void onError(String message) {
                                            // Complaint still created, but image failed
                                            Log.e(TAG, "Image upload failed: " + message);
                                            callback.onSuccess(); 
                                        }
                                    });
                                } else {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                                    ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                    callback.onError("Kamu tidak memiliki izin untuk membuat komplain.");
                                } else {
                                    callback.onError("Gagal menyimpan komplain: " + e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengambil data booking: " + e.getMessage()));
    }

    public void getComplaintsByStudent(String studentId, ComplaintListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_COMPLAINTS)
                .whereEqualTo(DatabaseConstants.FIELD_STUDENT_ID, studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Complaint> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Complaint c = doc.toObject(Complaint.class);
                        c.setId(doc.getId());
                        list.add(c);
                    }
                    // Sort manual to avoid index requirement
                    Collections.sort(list, (c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Akses ditolak. Kamu tidak memiliki izin melihat data ini.");
                    } else {
                        callback.onError("Gagal memuat komplain: " + e.getMessage());
                    }
                });
    }

    public void getComplaintsByOwner(String ownerId, ComplaintListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_COMPLAINTS)
                .whereEqualTo(DatabaseConstants.FIELD_OWNER_ID, ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Complaint> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Complaint c = doc.toObject(Complaint.class);
                        c.setId(doc.getId());
                        list.add(c);
                    }
                    Collections.sort(list, (c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Akses ditolak. Kamu tidak memiliki izin melihat data ini.");
                    } else {
                        callback.onError("Gagal memuat komplain: " + e.getMessage());
                    }
                });
    }

    public void updateComplaintStatus(String complaintId, String newStatus, String ownerResponse, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabaseConstants.FIELD_STATUS, newStatus);
        updates.put(DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());
        if (ownerResponse != null) {
            updates.put(DatabaseConstants.FIELD_OWNER_RESPONSE, ownerResponse);
        }

        if (DatabaseConstants.COMPLAINT_DONE.equals(newStatus) || DatabaseConstants.COMPLAINT_REJECTED.equals(newStatus)) {
            updates.put(DatabaseConstants.FIELD_RESOLVED_AT, System.currentTimeMillis());
        }

        db.collection(DatabaseConstants.COLLECTION_COMPLAINTS).document(complaintId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Trigger Notification for Student
                    db.collection(DatabaseConstants.COLLECTION_COMPLAINTS).document(complaintId).get()
                            .addOnSuccessListener(doc -> {
                                String studentId = doc.getString("studentId");
                                String ownerId = doc.getString("ownerId");
                                String kosName = doc.getString("kosName");
                                String title = "Update Komplain";
                                String body = "Status komplain kamu untuk " + kosName + " telah diperbarui.";
                                String type = DatabaseConstants.NOTIF_COMPLAINT_PROCESS;

                                if (DatabaseConstants.COMPLAINT_PROCESS.equals(newStatus)) {
                                    title = "Komplain sedang diproses";
                                    body = "Komplain kamu untuk " + kosName + " sedang diproses.";
                                    type = DatabaseConstants.NOTIF_COMPLAINT_PROCESS;
                                } else if (DatabaseConstants.COMPLAINT_DONE.equals(newStatus)) {
                                    title = "Komplain selesai";
                                    body = "Komplain kamu untuk " + kosName + " telah selesai.";
                                    type = DatabaseConstants.NOTIF_COMPLAINT_DONE;
                                } else if (DatabaseConstants.COMPLAINT_REJECTED.equals(newStatus)) {
                                    title = "Komplain ditolak";
                                    body = "Komplain kamu untuk " + kosName + " ditolak.";
                                    type = DatabaseConstants.NOTIF_COMPLAINT_REJECTED;
                                }

                                NotificationRepository.getInstance().createNotification(
                                        studentId,
                                        ownerId,
                                        type,
                                        title,
                                        body,
                                        DatabaseConstants.TARGET_COMPLAINT,
                                        complaintId
                                );
                            });
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Gagal update status: " + e.getMessage()));
    }

    public void markComplaintProcess(String complaintId, SimpleCallback callback) {
        updateComplaintStatus(complaintId, DatabaseConstants.COMPLAINT_PROCESS, null, callback);
    }

    public void markComplaintDone(String complaintId, String ownerResponse, SimpleCallback callback) {
        updateComplaintStatus(complaintId, DatabaseConstants.COMPLAINT_DONE, ownerResponse, callback);
    }

    public void rejectComplaint(String complaintId, String ownerResponse, SimpleCallback callback) {
        updateComplaintStatus(complaintId, DatabaseConstants.COMPLAINT_REJECTED, ownerResponse, callback);
    }
}
