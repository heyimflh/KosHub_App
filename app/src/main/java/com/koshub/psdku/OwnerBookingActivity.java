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

import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.NavigationTransitionHelper;

import java.util.ArrayList;
import java.util.List;

public class OwnerBookingActivity extends AppCompatActivity {

    private LinearLayout bookingListContainer;
    private TextView tabAll, tabPending, tabAccepted, tabActive;
    private View btnFilter, btnNotification;
    private EditText etSearch;

    private List<Booking> realBookings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_booking);

        initViews();
        setupClickListeners();
        loadRealBookings();
    }

    private void initViews() {
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.BOOKING);

        bookingListContainer = findViewById(R.id.bookingListContainer);

        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabAccepted = findViewById(R.id.tabAccepted);
        tabActive = findViewById(R.id.tabActive);

        btnFilter = findViewById(R.id.btnFilter);
        btnNotification = findViewById(R.id.btnNotification);
        etSearch = findViewById(R.id.etSearch);
    }

    private void loadRealBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        BookingRepository.getInstance().getBookingsByOwner(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                realBookings = bookings;
                renderBookings("pending"); // Default view
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat: " + message);
            }
        });
    }

    private void setupClickListeners() {
        tabAll.setOnClickListener(v -> renderBookings("all"));
        tabPending.setOnClickListener(v -> renderBookings("pending"));
        tabAccepted.setOnClickListener(v -> renderBookings("accepted"));
        tabActive.setOnClickListener(v -> renderBookings("active"));
    }

    private void renderBookings(String filterStatus) {
        bookingListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Booking item : realBookings) {
            String status = item.getStatus() != null ? item.getStatus() : "";
            boolean matches = filterStatus.equals("all") || status.equals(filterStatus);

            if (matches) {
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

        android.util.Log.d("KosHubBooking", "Owner accepting booking: " + b.getId());
        
        // Find and disable the button in the container to prevent double clicks
        // Since we are using a dynamic list, we can just show a toast or a simple progress
        showToast("Memproses...");

        BookingRepository.getInstance().acceptBooking(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
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

        android.util.Log.d("KosHubBooking", "Owner rejecting booking: " + b.getId());
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
