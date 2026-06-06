package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.DateHelper;
import com.koshub.psdku.utils.SystemInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OwnerBookingActivity extends AppCompatActivity {

    private LinearLayout bookingListContainer;
    private TextView tabAll, tabPending, tabActive, tabCompleted;
    private TextView tvStatMenunggu, tvStatDiterima, tvStatSelesai, tvStatDitolak, tvBookingBaruBadge, tvHeaderDate, tvPriorityAlertTitle;
    private View btnNotification, layoutEmptyState, layoutLoadingState, cardPriorityAlert;
    private EditText etSearch;

    private List<Booking> realBookings = new ArrayList<>();
    private String currentTab = "pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_booking);

        if (getIntent().hasExtra("TAB")) {
            currentTab = getIntent().getStringExtra("TAB");
        }

        initViews();
        setupClickListeners();
        loadRealBookings();

        SystemInsetsHelper.applySystemBars(
            this,
            findViewById(R.id.headerOwnerBooking),
            findViewById(R.id.ownerBottomNav),
            findViewById(R.id.scrollOwnerBooking),
            false,
            true
        );
    }

    private void initViews() {
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.BOOKING);

        bookingListContainer = findViewById(R.id.bookingListContainer);

        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabActive = findViewById(R.id.tabActive);
        tabCompleted = findViewById(R.id.tabCompleted);
        
        // Hide redundant tabs
        if (findViewById(R.id.tabAccepted) != null) findViewById(R.id.tabAccepted).setVisibility(View.GONE);
        if (findViewById(R.id.tabRejected) != null) findViewById(R.id.tabRejected).setVisibility(View.GONE);

        btnNotification = findViewById(R.id.btnNotification);
        etSearch = findViewById(R.id.etSearch);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutLoadingState = findViewById(R.id.layoutLoadingState);

        tvHeaderDate = findViewById(R.id.tvHeaderDate);
        tvStatMenunggu = findViewById(R.id.tvStatMenunggu);
        tvStatDiterima = findViewById(R.id.tvStatDiterima);
        tvStatSelesai = findViewById(R.id.tvStatSelesai);
        tvStatDitolak = findViewById(R.id.tvStatDitolak);
        tvBookingBaruBadge = findViewById(R.id.tvBookingBaruBadge);
        tvPriorityAlertTitle = findViewById(R.id.tvPriorityAlertTitle);
        cardPriorityAlert = findViewById(R.id.cardPriorityAlert);

        // Set Dynamic Date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        if (tvHeaderDate != null) {
            tvHeaderDate.setText(sdf.format(new Date()));
        }
    }

    private void loadRealBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (layoutLoadingState != null) layoutLoadingState.setVisibility(View.VISIBLE);
        BookingRepository.getInstance().getBookingsByOwner(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                if (layoutLoadingState != null) layoutLoadingState.setVisibility(View.GONE);
                realBookings = bookings;
                updateStatsCards(bookings);
                renderBookings(currentTab);
            }

            @Override
            public void onError(String message) {
                if (layoutLoadingState != null) layoutLoadingState.setVisibility(View.GONE);
                showToast("Gagal memuat: " + message);
            }
        });
    }

    private void setupClickListeners() {
        tabAll.setOnClickListener(v -> { currentTab = "all"; renderBookings("all"); });
        tabPending.setOnClickListener(v -> { currentTab = "pending"; renderBookings("pending"); });
        tabActive.setOnClickListener(v -> { currentTab = "active"; renderBookings("active"); });
        tabCompleted.setOnClickListener(v -> { currentTab = "completed"; renderBookings("completed"); });
        
        if (findViewById(R.id.btnManageKos) != null) {
            findViewById(R.id.btnManageKos).setOnClickListener(v -> {
                startActivity(new Intent(this, OwnerManagementActivity.class));
            });
        }

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationActivity.class);
                NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
            });
        }

        if (findViewById(R.id.btnPriorityBooking) != null) {
            findViewById(R.id.btnPriorityBooking).setOnClickListener(v -> {
                currentTab = "pending";
                renderBookings("pending");
            });
        }
    }

    private void updateStatsCards(List<Booking> bookings) {
        int pending = 0;
        int accepted = 0;
        int completed = 0;
        int rejected = 0;

        for (Booking b : bookings) {
            String status = b.getStatus();
            if (status == null) continue;

            if (status.equals(DatabaseConstants.BOOKING_PENDING)) {
                pending++;
            } else if (status.equals(DatabaseConstants.BOOKING_ACCEPTED) ||
                    status.equals(DatabaseConstants.BOOKING_WAITING_PAYMENT) ||
                    status.equals(DatabaseConstants.BOOKING_WAITING_CHECKIN) ||
                    status.equals(DatabaseConstants.BOOKING_ACTIVE)) {
                accepted++;
            } else if (status.equals(DatabaseConstants.BOOKING_COMPLETED)) {
                completed++;
            } else if (status.equals(DatabaseConstants.BOOKING_REJECTED) ||
                    status.equals(DatabaseConstants.BOOKING_CANCELLED)) {
                rejected++;
            }
        }

        if (tvStatMenunggu != null) tvStatMenunggu.setText(String.valueOf(pending));
        if (tvStatDiterima != null) tvStatDiterima.setText(String.valueOf(accepted));
        if (tvStatSelesai != null) tvStatSelesai.setText(String.valueOf(completed));
        if (tvStatDitolak != null) tvStatDitolak.setText(String.valueOf(rejected));

        if (tvBookingBaruBadge != null) {
            if (pending > 0) {
                tvBookingBaruBadge.setVisibility(View.VISIBLE);
                tvBookingBaruBadge.setText(pending + " Booking Baru");
            } else {
                tvBookingBaruBadge.setVisibility(View.GONE);
            }
        }

        if (cardPriorityAlert != null) {
            if (pending > 0) {
                cardPriorityAlert.setVisibility(View.VISIBLE);
                if (tvPriorityAlertTitle != null) {
                    tvPriorityAlertTitle.setText(pending + " booking perlu dikonfirmasi hari ini");
                }
            } else {
                cardPriorityAlert.setVisibility(View.GONE);
            }
        }
    }

    private void updateTabUI(String filterStatus) {
        resetTabStyles();
        TextView activeTab = null;
        if (filterStatus.equals("all")) activeTab = tabAll;
        else if (filterStatus.equals("pending")) activeTab = tabPending;
        else if (filterStatus.equals("active")) activeTab = tabActive;
        else if (filterStatus.equals("completed")) activeTab = tabCompleted;

        if (activeTab != null) {
            activeTab.setBackgroundResource(R.drawable.bg_chip_active);
            activeTab.setTextColor(ContextCompat.getColor(this, R.color.text_white));
            activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void resetTabStyles() {
        TextView[] tabs = {tabAll, tabPending, tabActive, tabCompleted};
        for (TextView t : tabs) {
            if (t != null) {
                t.setBackgroundResource(R.drawable.bg_chip_inactive_premium);
                t.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                t.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void renderBookings(String filterStatus) {
        if (bookingListContainer == null) return;
        bookingListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        updateTabUI(filterStatus);

        int count = 0;
        for (Booking item : realBookings) {
            String status = item.getStatus() != null ? item.getStatus() : "";
            boolean matches = false;
            
            if (filterStatus.equals("all")) matches = true;
            else if (filterStatus.equals("pending")) matches = status.equals(DatabaseConstants.BOOKING_PENDING);
            else if (filterStatus.equals("active")) {
                matches = status.equals(DatabaseConstants.BOOKING_ACCEPTED) || 
                          status.equals(DatabaseConstants.BOOKING_WAITING_PAYMENT) ||
                          status.equals(DatabaseConstants.BOOKING_WAITING_CHECKIN) || 
                          status.equals(DatabaseConstants.BOOKING_ACTIVE);
            }
            else if (filterStatus.equals("completed")) {
                matches = status.equals(DatabaseConstants.BOOKING_COMPLETED) || 
                          status.equals(DatabaseConstants.BOOKING_REJECTED) || 
                          status.equals(DatabaseConstants.BOOKING_CANCELLED);
            }

            if (matches) {
                count++;
                View itemView = inflater.inflate(R.layout.item_owner_booking, bookingListContainer, false);

                TextView tvName = itemView.findViewById(R.id.tvTenantName);
                TextView tvStatus = itemView.findViewById(R.id.tvBookingStatus);
                TextView tvKosRoom = itemView.findViewById(R.id.tvKosName);
                TextView tvPrice = itemView.findViewById(R.id.tvPrice);
                TextView tvInitial = itemView.findViewById(R.id.tvInitial);
                TextView tvTenantStatus = itemView.findViewById(R.id.tvTenantStatus);
                TextView tvCheckInDate = itemView.findViewById(R.id.tvCheckInDate);
                TextView tvDuration = itemView.findViewById(R.id.tvDuration);

                View btnAccept = itemView.findViewById(R.id.btnAccept);
                View btnReject = itemView.findViewById(R.id.btnReject);
                View btnDetail = itemView.findViewById(R.id.btnDetail);

                String studentName = item.getStudentName() != null ? item.getStudentName() : "Mahasiswa";
                tvName.setText(studentName);
                tvStatus.setText(status.toUpperCase());

                // Set Initial
                if (tvInitial != null) {
                    tvInitial.setText(studentName.substring(0, 1).toUpperCase());
                }

                // Set Tenant Status (default if not available)
                if (tvTenantStatus != null) {
                    tvTenantStatus.setText("Mahasiswa UNS");
                }
                
                String kosName = item.getKosName() != null ? item.getKosName() : "Kos";
                String roomName = item.getRoomName() != null ? item.getRoomName() : "Antrean";
                tvKosRoom.setText(kosName + " • " + roomName);
                
                tvPrice.setText(CurrencyHelper.formatRupiah(getEffectiveBookingAmount(item)));

                // Set Dates and Duration
                if (tvCheckInDate != null) {
                    tvCheckInDate.setText(item.getCheckInDate() > 0 ? DateHelper.formatDate(item.getCheckInDate()) : "-");
                }
                if (tvDuration != null) {
                    tvDuration.setText(item.getDurationMonth() > 0 ? item.getDurationMonth() + " bulan" : "-");
                }

                if (DatabaseConstants.BOOKING_PENDING.equals(status)) {
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnAccept.setOnClickListener(v -> handleAccept(item));
                    btnReject.setOnClickListener(v -> handleReject(item));
                } else {
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                }

                if (btnDetail != null) {
                    btnDetail.setOnClickListener(v -> {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            showToast("ID Booking tidak ditemukan.");
                            return;
                        }
                        showToast("Memuat detail booking...");
                        BookingRepository.getInstance().getBookingById(item.getId(), new BookingRepository.BookingCallback() {
                            @Override
                            public void onSuccess(Booking freshBooking) {
                                showBookingDetailDialog(freshBooking);
                            }

                            @Override
                            public void onError(String message) {
                                showToast("Gagal memuat detail: " + message);
                            }
                        });
                    });
                }

                itemView.setOnClickListener(v -> openChatFromBooking(item));

                bookingListContainer.addView(itemView);
            }
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            bookingListContainer.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private void showBookingDetailDialog(Booking b) {
        if (b == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView tvTenantName = dialogView.findViewById(R.id.tvDetailTenantName);
        TextView tvTenantContact = dialogView.findViewById(R.id.tvDetailTenantContact);
        TextView tvKosName = dialogView.findViewById(R.id.tvDetailKosName);
        TextView tvRoom = dialogView.findViewById(R.id.tvDetailRoom);
        TextView tvCheckIn = dialogView.findViewById(R.id.tvDetailCheckIn);
        TextView tvDuration = dialogView.findViewById(R.id.tvDetailDuration);
        TextView tvTotal = dialogView.findViewById(R.id.tvDetailTotal);
        TextView tvPaymentStatus = dialogView.findViewById(R.id.tvDetailPaymentStatus);
        TextView tvBookingStatus = dialogView.findViewById(R.id.tvDetailBookingStatus);

        View btnAccept = dialogView.findViewById(R.id.btnDialogAccept);
        View btnChat = dialogView.findViewById(R.id.btnDialogChat);
        View btnClose = dialogView.findViewById(R.id.btnDialogClose);

        tvTenantName.setText(b.getStudentName() != null ? b.getStudentName() : "Mahasiswa");
        
        String contact = (b.getStudentEmail() != null ? b.getStudentEmail() : "-");
        tvTenantContact.setText(contact);
        
        tvKosName.setText(b.getKosName() != null ? b.getKosName() : "Kos");
        
        // Accurate Room Info Resolve
        resolveRoomForBooking(b, tvRoom);
        
        tvCheckIn.setText(b.getCheckInDate() > 0 ? DateHelper.formatDate(b.getCheckInDate()) : "-");
        tvDuration.setText(b.getDurationMonth() > 0 ? b.getDurationMonth() + " bulan" : "-");
        
        // Effective Amount
        tvTotal.setText(CurrencyHelper.formatRupiah(getEffectiveBookingAmount(b)));

        // Status Booking Label
        tvBookingStatus.setText(getBookingStatusLabel(b.getStatus()));

        // Status Pembayaran Label & Color
        String pStatusLabel = getPaymentStatusLabel(b);
        tvPaymentStatus.setText(pStatusLabel);
        
        if ("Sudah Bayar".equals(pStatusLabel)) {
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        } else if ("Menunggu Pembayaran".equals(pStatusLabel)) {
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text));
        } else if ("Refund".equals(pStatusLabel)) {
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            tvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.profile_logout_text));
        }

        if (DatabaseConstants.BOOKING_PENDING.equals(b.getStatus())) {
            btnAccept.setVisibility(View.VISIBLE);
            btnAccept.setOnClickListener(v -> {
                dialog.dismiss();
                handleAccept(b);
            });
        } else {
            btnAccept.setVisibility(View.GONE);
        }

        btnChat.setOnClickListener(v -> {
            dialog.dismiss();
            openChatFromBooking(b);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String getBookingStatusLabel(String status) {
        if (status == null) return "Menunggu";
        switch (status) {
            case DatabaseConstants.BOOKING_PENDING:
                return "Menunggu Konfirmasi";
            case DatabaseConstants.BOOKING_ACCEPTED:
                return "Diterima";
            case DatabaseConstants.BOOKING_WAITING_PAYMENT:
                return "Menunggu Pembayaran";
            case DatabaseConstants.BOOKING_WAITING_CHECKIN:
                return "Menunggu Check-in";
            case DatabaseConstants.BOOKING_ACTIVE:
                return "Aktif";
            case DatabaseConstants.BOOKING_COMPLETED:
                return "Selesai";
            case DatabaseConstants.BOOKING_REJECTED:
                return "Ditolak";
            case DatabaseConstants.BOOKING_CANCELLED:
                return "Dibatalkan";
            default:
                return status;
        }
    }

    private String getPaymentStatusLabel(Booking b) {
        if (b == null) return "Belum Bayar";

        String bookingStatus = b.getStatus() == null ? "" : b.getStatus();
        String paymentStatus = b.getPaymentStatus() == null ? "" : b.getPaymentStatus();

        if (DatabaseConstants.BOOKING_REJECTED.equals(bookingStatus)) {
            return "Tidak Dibayar / Ditolak";
        }

        if (DatabaseConstants.BOOKING_CANCELLED.equals(bookingStatus)) {
            return "Dibatalkan";
        }

        if (DatabaseConstants.PAYMENT_PAID.equals(paymentStatus)) {
            return "Sudah Bayar";
        }

        if (DatabaseConstants.PAYMENT_PENDING.equals(paymentStatus)
                || DatabaseConstants.BOOKING_WAITING_PAYMENT.equals(bookingStatus)) {
            return "Menunggu Pembayaran";
        }

        if (DatabaseConstants.PAYMENT_REFUNDED.equals(paymentStatus)) {
            return "Refund";
        }

        return "Belum Bayar";
    }

    private double getEffectiveBookingAmount(Booking b) {
        if (b == null) return 0.0;

        if (b.getTotalBayar() != null && b.getTotalBayar() > 0) {
            return b.getTotalBayar();
        }

        if (b.getTotalPrice() > 0) {
            return b.getTotalPrice();
        }

        if (b.getPrice() != null) {
            try {
                String cleaned = b.getPrice()
                        .replace("Rp", "")
                        .replace(".", "")
                        .replace(",", "")
                        .trim();
                return Double.parseDouble(cleaned);
            } catch (Exception ignored) {}
        }

        return 0.0;
    }

    private void openChatFromBooking(Booking b) {
        if (b == null || b.getId() == null || b.getId().isEmpty()) {
            showToast("Data booking tidak valid.");
            return;
        }

        Intent intent = new Intent(this, OwnerChatRoomActivity.class);
        intent.putExtra("BOOKING_ID", b.getId());
        intent.putExtra("USER_NAME", b.getStudentName());
        intent.putExtra("KOS_NAME", b.getKosName());
        intent.putExtra("STATUS", b.getStatus());
        intent.putExtra("INITIAL", b.getStudentName() != null && !b.getStudentName().isEmpty() ? 
                b.getStudentName().substring(0, 1).toUpperCase() : "M");
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    private void handleAccept(Booking b) {
        if (b == null || b.getId() == null) {
            showToast("Data booking tidak valid.");
            return;
        }

        if (b.getKosId() == null) {
            showToast("ID Kos tidak ditemukan pada data booking.");
            return;
        }

        showToast("Memuat daftar kamar...");

        KosRepository.getInstance().getRoomsByKos(b.getKosId(), new KosRepository.RoomListCallback() {
            @Override
            public void onSuccess(List<Room> rooms) {
                if (isFinishing() || isDestroyed()) return;

                List<Room> availableRooms = new ArrayList<>();
                for (Room r : rooms) {
                    if (DatabaseConstants.ROOM_AVAILABLE.equals(r.getStatus())) {
                        availableRooms.add(r);
                    }
                }

                if (availableRooms.isEmpty()) {
                    showToast("Tidak ada kamar yang tersedia saat ini.");
                    return;
                }

                showRoomSelectionDialog(b, availableRooms);
            }

            @Override
            public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) {
                    showToast("Gagal memuat kamar: " + message);
                }
            }
        });
    }

    private void showRoomSelectionDialog(Booking b, List<Room> rooms) {
        String[] roomNames = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            roomNames[i] = rooms.get(i).getRoomName();
        }

        final int[] selectedIndex = {-1};

        new AlertDialog.Builder(this)
                .setTitle("Pilih Kamar")
                .setSingleChoiceItems(roomNames, -1, (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton("Terima Booking", (dialog, which) -> {
                    if (selectedIndex[0] != -1) {
                        Room selectedRoom = rooms.get(selectedIndex[0]);
                        processAcceptBooking(b.getId(), selectedRoom.getId());
                    } else {
                        showToast("Pilih kamar terlebih dahulu.");
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void processAcceptBooking(String bookingId, String roomId) {
        showToast("Memproses...");

        BookingRepository.getInstance().acceptBooking(bookingId, roomId, new BookingRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isFinishing() && !isDestroyed()) {
                    showToast("Booking Diterima");
                    loadRealBookings();
                }
            }

            @Override
            public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) {
                    showToast(message);
                }
            }
        });
    }

    private void handleReject(Booking b) {
        if (b == null || b.getId() == null) {
            showToast("Data booking tidak valid.");
            return;
        }

        showToast("Memproses...");

        BookingRepository.getInstance().rejectBooking(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isFinishing() && !isDestroyed()) {
                    showToast("Booking Ditolak");
                    loadRealBookings();
                }
            }

            @Override
            public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) {
                    showToast(message);
                }
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Resolves room information for a booking with fallback and backfill.
     */
    private void resolveRoomForBooking(Booking b, TextView tvDetailRoom) {
        if (b == null || tvDetailRoom == null) return;

        String status = b.getSafeStatus();

        // 1. Existing Room Name
        if (b.getRoomName() != null && !b.getRoomName().trim().isEmpty()) {
            tvDetailRoom.setText("Kamar: " + b.getRoomName());
            return;
        }

        // 2. Status check for non-accepted bookings
        if (DatabaseConstants.BOOKING_PENDING.equals(status)) {
            tvDetailRoom.setText("Kamar: Antrean / belum ditentukan");
            return;
        }
        if (DatabaseConstants.BOOKING_REJECTED.equals(status) || DatabaseConstants.BOOKING_CANCELLED.equals(status)) {
            tvDetailRoom.setText("Kamar: Belum ditentukan");
            return;
        }

        // 3. Resolve from RoomId if available
        if (b.getRoomId() != null && !b.getRoomId().trim().isEmpty()) {
            tvDetailRoom.setText("Kamar: Memuat...");
            FirebaseFirestore.getInstance().collection(DatabaseConstants.COLLECTION_ROOMS)
                    .document(b.getRoomId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String resolvedName = documentSnapshot.getString("roomName");
                            if (resolvedName == null) resolvedName = documentSnapshot.getString("name");
                            if (resolvedName == null) resolvedName = documentSnapshot.getString("code");
                            if (resolvedName == null) resolvedName = documentSnapshot.getString("roomCode");
                            if (resolvedName == null) resolvedName = "Kamar Terpilih";

                            tvDetailRoom.setText("Kamar: " + resolvedName);
                            // Backfill
                            backfillBookingRoomInfo(b.getId(), b.getRoomId(), resolvedName);
                        } else {
                            tvDetailRoom.setText("Kamar: Belum tercatat");
                        }
                    })
                    .addOnFailureListener(e -> tvDetailRoom.setText("Kamar: Belum tercatat"));
            return;
        }

        // 4. Resolve from Room query (bookingId relation)
        tvDetailRoom.setText("Kamar: Mencari...");
        FirebaseFirestore.getInstance().collection(DatabaseConstants.COLLECTION_ROOMS)
                .whereEqualTo("bookingId", b.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String resolvedName = doc.getString("roomName");
                        if (resolvedName == null) resolvedName = doc.getString("name");
                        if (resolvedName == null) resolvedName = doc.getString("code");
                        if (resolvedName == null) resolvedName = doc.getString("roomCode");
                        if (resolvedName == null) resolvedName = "Kamar Terpilih";

                        tvDetailRoom.setText("Kamar: " + resolvedName);
                        // Backfill
                        backfillBookingRoomInfo(b.getId(), doc.getId(), resolvedName);
                    } else {
                        // 5. Final Fallback: StudentId + KosId (only for active/paid)
                        resolveRoomByStudentRelation(b, tvDetailRoom);
                    }
                })
                .addOnFailureListener(e -> tvDetailRoom.setText("Kamar: Belum tercatat"));
    }

    private void resolveRoomByStudentRelation(Booking b, TextView tvDetailRoom) {
        if (b.getStudentId() == null || b.getKosId() == null) {
            tvDetailRoom.setText("Kamar: Belum tercatat");
            return;
        }

        FirebaseFirestore.getInstance().collection(DatabaseConstants.COLLECTION_ROOMS)
                .whereEqualTo("kosId", b.getKosId())
                .whereEqualTo("studentId", b.getStudentId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.size() == 1) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String resolvedName = doc.getString("roomName");
                        if (resolvedName == null) resolvedName = doc.getString("name");
                        if (resolvedName == null) resolvedName = doc.getString("code");
                        if (resolvedName == null) resolvedName = doc.getString("roomCode");
                        if (resolvedName == null) resolvedName = "Kamar Terpilih";

                        tvDetailRoom.setText("Kamar: " + resolvedName);
                        // Backfill
                        backfillBookingRoomInfo(b.getId(), doc.getId(), resolvedName);
                    } else if (querySnapshot.size() > 1) {
                        tvDetailRoom.setText("Kamar: Perlu verifikasi");
                    } else {
                        tvDetailRoom.setText("Kamar: Belum tercatat");
                    }
                })
                .addOnFailureListener(e -> tvDetailRoom.setText("Kamar: Belum tercatat"));
    }

    private void backfillBookingRoomInfo(String bookingId, String roomId, String roomName) {
        if (bookingId == null || roomId == null || roomName == null) return;
        
        FirebaseFirestore.getInstance().collection(DatabaseConstants.COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(
                        "roomId", roomId,
                        "roomName", roomName,
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> android.util.Log.d("Backfill", "Successfully backfilled booking " + bookingId))
                .addOnFailureListener(e -> android.util.Log.e("Backfill", "Failed to backfill: " + e.getMessage()));
    }
}
