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
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.utils.DatabaseConstants;

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

                View btnAccept = itemView.findViewById(R.id.btnAccept);
                View btnReject = itemView.findViewById(R.id.btnReject);

                tvName.setText(item.getStudentName() != null ? item.getStudentName() : "Mahasiswa");
                tvStatus.setText(status.toUpperCase());
                
                String kosName = item.getKosName() != null ? item.getKosName() : "Kos";
                String roomName = item.getRoomName() != null ? item.getRoomName() : "Antrean";
                tvKosRoom.setText(kosName + " • " + roomName);
                
                tvPrice.setText("Rp " + item.getTotalPrice());

                if (DatabaseConstants.BOOKING_PENDING.equals(status)) {
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnAccept.setOnClickListener(v -> handleAccept(item));
                    btnReject.setOnClickListener(v -> handleReject(item));
                } else {
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
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

    private void openChatFromBooking(Booking b) {
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
}
