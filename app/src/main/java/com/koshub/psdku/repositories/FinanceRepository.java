package com.koshub.psdku.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.FinanceSummary;
import com.koshub.psdku.models.Transaction;
import com.koshub.psdku.models.Withdrawal;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository for Finance and Withdrawal data.
 */
public class FinanceRepository {
    private static final String TAG = "KosHubFinance";
    private static FinanceRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FinanceRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized FinanceRepository getInstance() {
        if (instance == null) {
            instance = new FinanceRepository();
        }
        return instance;
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface TransactionListCallback {
        void onSuccess(List<Transaction> transactions);
        void onError(String message);
    }

    public interface WithdrawalListCallback {
        void onSuccess(List<Withdrawal> withdrawals);
        void onError(String message);
    }

    public interface FinanceSummaryCallback {
        void onSuccess(FinanceSummary summary);
        void onError(String message);
    }

    public void createTransactionAfterPayment(Booking booking, long idTransaksi, double totalBayar, SimpleCallback callback) {
        String transactionId = booking.getId() + "_" + idTransaksi;
        
        db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS).document(transactionId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        if (callback != null) callback.onSuccess(); // Idempotent
                        return;
                    }

                    Transaction transaction = new Transaction();
                    transaction.setId(transactionId);
                    transaction.setOwnerId(booking.getOwnerId());
                    transaction.setStudentId(booking.getStudentId());
                    transaction.setBookingId(booking.getId());
                    transaction.setKosId(booking.getKosId());
                    transaction.setKosName(booking.getKosName());
                    transaction.setAmount(totalBayar);
                    transaction.setType("rent_payment");
                    transaction.setStatus(DatabaseConstants.TRANSACTION_AVAILABLE);
                    transaction.setCreatedAt(System.currentTimeMillis());
                    transaction.setUpdatedAt(System.currentTimeMillis());
                    
                    db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS).document(transactionId)
                            .set(transaction)
                            .addOnSuccessListener(aVoid -> {
                                // 1. Update with custom fields
                                db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS).document(transactionId)
                                        .update(
                                                "gateway", "custom_qris_alwaysdata",
                                                "gatewayTransactionId", idTransaksi,
                                                "paidAt", FieldValue.serverTimestamp()
                                        );
                                
                                // 2. Update Payment document status to paid
                                String paymentDocId = booking.getId() + "_" + idTransaksi;
                                db.collection(DatabaseConstants.COLLECTION_PAYMENTS).document(paymentDocId)
                                        .update(
                                                "status", DatabaseConstants.PAYMENT_PAID,
                                                "paidAt", FieldValue.serverTimestamp(),
                                                "updatedAt", FieldValue.serverTimestamp()
                                        );

                                // 3. Update Room status to booked
                                if (booking.getRoomId() != null && !booking.getRoomId().isEmpty()) {
                                    db.collection(DatabaseConstants.COLLECTION_ROOMS).document(booking.getRoomId())
                                            .update("status", DatabaseConstants.ROOM_BOOKED);
                                }

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

    public void createPendingTransactionFromBooking(String bookingId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        if (callback != null) callback.onError("Booking tidak ditemukan.");
                        return;
                    }

                    Booking booking = documentSnapshot.toObject(Booking.class);
                    if (booking == null) {
                        if (callback != null) callback.onError("Gagal memproses data booking.");
                        return;
                    }

                    // Check if transaction already exists for this booking
                    db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS)
                            .whereEqualTo("bookingId", bookingId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    Log.d(TAG, "Transaction already exists for booking: " + bookingId);
                                    if (callback != null) callback.onSuccess(); // Already exists, consider success
                                    return;
                                }

                                String transactionId = db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS).document().getId();
                                Transaction transaction = new Transaction();
                                transaction.setId(transactionId);
                                transaction.setOwnerId(booking.getOwnerId());
                                transaction.setStudentId(booking.getStudentId());
                                transaction.setBookingId(bookingId);
                                transaction.setKosId(booking.getKosId());
                                transaction.setKosName(booking.getKosName());
                                transaction.setAmount(booking.getTotalPrice());
                                transaction.setType(DatabaseConstants.TRANSACTION_TYPE_BOOKING_PAYMENT);
                                transaction.setStatus(DatabaseConstants.TRANSACTION_PENDING);
                                transaction.setCreatedAt(System.currentTimeMillis());
                                transaction.setUpdatedAt(System.currentTimeMillis());
                                transaction.setAvailableAt(0);
                                transaction.setWithdrawalId("");

                                db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS).document(transactionId)
                                        .set(transaction)
                                        .addOnSuccessListener(aVoid -> {
                                            if (callback != null) callback.onSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            if (callback != null) callback.onError("Gagal membuat transaksi: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError("Gagal mengecek duplikasi transaksi.");
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Gagal memuat booking.");
                });
    }

    public void markTransactionAvailableByBooking(String bookingId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        if (callback != null) callback.onError("Transaksi tidak ditemukan untuk booking ini.");
                        return;
                    }

                    WriteBatch batch = db.batch();
                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentReference ref = doc.getReference();
                        batch.update(ref,
                                "status", DatabaseConstants.TRANSACTION_AVAILABLE,
                                "updatedAt", now,
                                "availableAt", now);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError("Gagal mengupdate status transaksi.");
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Gagal mencari transaksi.");
                });
    }

    public void cancelTransactionByBooking(String bookingId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        if (callback != null) callback.onSuccess(); // Nothing to cancel
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");
                        // Only cancel if it's still pending
                        if (DatabaseConstants.TRANSACTION_PENDING.equals(status)) {
                            batch.update(doc.getReference(), 
                                    "status", DatabaseConstants.TRANSACTION_CANCELLED,
                                    "updatedAt", System.currentTimeMillis());
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError("Gagal membatalkan transaksi.");
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Gagal mencari transaksi untuk pembatalan.");
                });
    }

    public void getTransactionsByOwner(String ownerId, TransactionListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Transaction> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Transaction t = doc.toObject(Transaction.class);
                        list.add(t);
                    }
                    // Sort manual by createdAt desc
                    Collections.sort(list, (t1, t2) -> Long.compare(t2.getCreatedAt(), t1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data keuangan ini.");
                    } else {
                        callback.onError("Gagal memuat daftar transaksi: " + e.getMessage());
                    }
                });
    }

    public void getFinanceSummary(String ownerId, FinanceSummaryCallback callback) {
        // We need both transactions and withdrawals to calculate summary
        db.collection(DatabaseConstants.COLLECTION_TRANSACTIONS)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(transSnapshot -> {
                    db.collection(DatabaseConstants.COLLECTION_WITHDRAWALS)
                            .whereEqualTo("ownerId", ownerId)
                            .get()
                            .addOnSuccessListener(withSnapshot -> {
                                FinanceSummary summary = calculateSummary(transSnapshot, withSnapshot);
                                callback.onSuccess(summary);
                            })
                            .addOnFailureListener(e -> callback.onError("Gagal memuat data withdraw untuk ringkasan."));
                })
                .addOnFailureListener(e -> callback.onError("Gagal memuat data transaksi untuk ringkasan."));
    }

    private FinanceSummary calculateSummary(com.google.firebase.firestore.QuerySnapshot transSnapshot, com.google.firebase.firestore.QuerySnapshot withSnapshot) {
        FinanceSummary summary = new FinanceSummary();

        double totalIncome = 0;
        double availableBalance = 0;
        double pendingBalance = 0;
        double totalWithdrawn = 0;
        double totalPendingWithdraw = 0;
        int transactionCount = transSnapshot.size();
        int withdrawalCount = withSnapshot.size();

        int lunasCount = 0;
        int pendingCount = 0;
        int lateCount = 0;
        int cancelledCount = 0;

        long now = System.currentTimeMillis();
        long sevenDaysMillis = 7L * 24 * 60 * 60 * 1000;

        for (QueryDocumentSnapshot doc : transSnapshot) {
            Transaction t = doc.toObject(Transaction.class);
            if (DatabaseConstants.TRANSACTION_PENDING.equals(t.getStatus())) {
                pendingBalance += t.getAmount();
                pendingCount++;
                // Simple logic for late: pending for more than 7 days
                if (now - t.getCreatedAt() > sevenDaysMillis) {
                    lateCount++;
                }
            } else if (DatabaseConstants.TRANSACTION_AVAILABLE.equals(t.getStatus())) {
                availableBalance += t.getAmount();
                totalIncome += t.getAmount();
                lunasCount++;
            } else if (DatabaseConstants.TRANSACTION_WITHDRAWN.equals(t.getStatus())) {
                totalIncome += t.getAmount();
                lunasCount++;
            } else if (DatabaseConstants.TRANSACTION_CANCELLED.equals(t.getStatus())) {
                cancelledCount++;
            }
        }

        for (QueryDocumentSnapshot doc : withSnapshot) {
            Withdrawal w = doc.toObject(Withdrawal.class);
            if (DatabaseConstants.WITHDRAWAL_SUCCESS.equals(w.getStatus())) {
                totalWithdrawn += w.getAmount();
            } else if (DatabaseConstants.WITHDRAWAL_PENDING.equals(w.getStatus()) || 
                       DatabaseConstants.WITHDRAWAL_PROCESSING.equals(w.getStatus())) {
                totalPendingWithdraw += w.getAmount();
            }
        }

        // Available balance logic: transactions available - (pending/processing withdrawals)
        // This prevents double withdrawing
        availableBalance = availableBalance - totalPendingWithdraw;
        if (availableBalance < 0) availableBalance = 0;

        summary.setTotalIncome(totalIncome);
        summary.setAvailableBalance(availableBalance);
        summary.setPendingBalance(pendingBalance);
        summary.setTotalWithdrawn(totalWithdrawn);
        summary.setTotalPendingWithdraw(totalPendingWithdraw);
        summary.setTransactionCount(transactionCount);
        summary.setWithdrawalCount(withdrawalCount);
        summary.setLunasCount(lunasCount);
        summary.setPendingCount(pendingCount);
        summary.setLateCount(lateCount);
        summary.setCancelledCount(cancelledCount);

        return summary;
    }

    public void requestWithdraw(String bankName, String accountNumber, String accountHolder, double amount, SimpleCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User tidak terautentikasi.");
            return;
        }

        String ownerId = user.getUid();

        // 1. Validate amount > 0
        if (amount <= 0) {
            callback.onError("Nominal withdraw harus lebih besar dari 0.");
            return;
        }

        // 2. Fetch summary to validate balance
        getFinanceSummary(ownerId, new FinanceSummaryCallback() {
            @Override
            public void onSuccess(FinanceSummary summary) {
                if (amount > summary.getAvailableBalance()) {
                    callback.onError("Saldo tersedia tidak mencukupi.");
                    return;
                }

                // 3. Create Withdrawal
                String withdrawalId = db.collection(DatabaseConstants.COLLECTION_WITHDRAWALS).document().getId();
                Withdrawal withdrawal = new Withdrawal();
                withdrawal.setId(withdrawalId);
                withdrawal.setOwnerId(ownerId);
                withdrawal.setAmount(amount);
                withdrawal.setBankName(bankName);
                withdrawal.setAccountNumber(accountNumber);
                withdrawal.setAccountHolder(accountHolder);
                withdrawal.setStatus(DatabaseConstants.WITHDRAWAL_PENDING);
                withdrawal.setNote("");
                withdrawal.setCreatedAt(System.currentTimeMillis());
                withdrawal.setUpdatedAt(System.currentTimeMillis());
                withdrawal.setProcessedAt(0);

                db.collection(DatabaseConstants.COLLECTION_WITHDRAWALS).document(withdrawalId)
                        .set(withdrawal)
                        .addOnSuccessListener(aVoid -> {
                            // Trigger Notification for Owner
                            NotificationRepository.getInstance().createNotification(
                                    ownerId,
                                    "system",
                                    DatabaseConstants.NOTIF_WITHDRAW_REQUESTED,
                                    "Withdraw berhasil diajukan",
                                    "Permintaan withdraw sebesar Rp" + amount + " sedang menunggu proses.",
                                    DatabaseConstants.TARGET_WITHDRAW,
                                    withdrawalId
                            );
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onError("Gagal mengirim permintaan withdraw: " + e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError("Gagal memvalidasi saldo: " + message);
            }
        });
    }

    public void getWithdrawalsByOwner(String ownerId, WithdrawalListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_WITHDRAWALS)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Withdrawal> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Withdrawal w = doc.toObject(Withdrawal.class);
                        list.add(w);
                    }
                    // Sort manual by createdAt desc
                    Collections.sort(list, (w1, w2) -> Long.compare(w2.getCreatedAt(), w1.getCreatedAt()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Akses ditolak. Kamu tidak memiliki izin melihat riwayat withdraw.");
                    } else {
                        callback.onError("Gagal memuat riwayat withdraw: " + e.getMessage());
                    }
                });
    }

    public void updateWithdrawalStatus(String withdrawalId, String status, String note, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_WITHDRAWALS).document(withdrawalId)
                .update("status", status, 
                        "note", note, 
                        "updatedAt", System.currentTimeMillis(),
                        "processedAt", DatabaseConstants.WITHDRAWAL_SUCCESS.equals(status) ? System.currentTimeMillis() : 0)
                .addOnSuccessListener(aVoid -> {
                    // Trigger Notification for Owner
                    db.collection(DatabaseConstants.COLLECTION_WITHDRAWALS).document(withdrawalId).get()
                            .addOnSuccessListener(doc -> {
                                String ownerId = doc.getString("ownerId");
                                Double amount = doc.getDouble("amount");
                                String title = "Update Withdraw";
                                String body = "Status penarikan saldo kamu telah diperbarui.";
                                String type = DatabaseConstants.NOTIF_WITHDRAW_PROCESSING;

                                if (DatabaseConstants.WITHDRAWAL_PROCESSING.equals(status)) {
                                    title = "Withdraw sedang diproses";
                                    body = "Withdraw sebesar Rp" + amount + " sedang diproses.";
                                    type = DatabaseConstants.NOTIF_WITHDRAW_PROCESSING;
                                } else if (DatabaseConstants.WITHDRAWAL_SUCCESS.equals(status)) {
                                    title = "Withdraw berhasil";
                                    body = "Withdraw sebesar Rp" + amount + " berhasil.";
                                    type = DatabaseConstants.NOTIF_WITHDRAW_SUCCESS;
                                } else if (DatabaseConstants.WITHDRAWAL_FAILED.equals(status)) {
                                    title = "Withdraw gagal";
                                    body = "Withdraw sebesar Rp" + amount + " gagal diproses.";
                                    type = DatabaseConstants.NOTIF_WITHDRAW_FAILED;
                                }

                                NotificationRepository.getInstance().createNotification(
                                        ownerId,
                                        "system",
                                        type,
                                        title,
                                        body,
                                        DatabaseConstants.TARGET_FINANCE,
                                        withdrawalId
                                );
                            });
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Gagal update status withdraw: " + e.getMessage());
                });
    }
}
