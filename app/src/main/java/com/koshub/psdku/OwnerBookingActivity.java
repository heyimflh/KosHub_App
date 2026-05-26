package com.koshub.psdku;

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
            boolean matches = filterStatus.equals("all") || item.getStatus().equals(filterStatus);

            if (matches) {
                View itemView = inflater.inflate(R.layout.item_owner_booking, bookingListContainer, false);

                TextView tvName = itemView.findViewById(R.id.tvTenantName);
                TextView tvStatus = itemView.findViewById(R.id.tvBookingStatus);
                TextView tvKosRoom = itemView.findViewById(R.id.tvKosName);
                TextView tvPrice = itemView.findViewById(R.id.tvPrice);

                View btnAccept = itemView.findViewById(R.id.btnAccept);
                View btnReject = itemView.findViewById(R.id.btnReject);

                tvName.setText(item.getStudentName() != null ? item.getStudentName() : "Mahasiswa");
                tvStatus.setText(item.getStatus().toUpperCase());
                tvKosRoom.setText(item.getKosName() + " • " + (item.getRoomName() != null ? item.getRoomName() : "Antrean"));
                tvPrice.setText("Rp " + item.getTotalPrice());

                if (DatabaseConstants.BOOKING_PENDING.equals(item.getStatus())) {
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnAccept.setOnClickListener(v -> handleAccept(item));
                    btnReject.setOnClickListener(v -> handleReject(item));
                } else {
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                }

                bookingListContainer.addView(itemView);
            }
        }
    }

    private void handleAccept(Booking b) {
        BookingRepository.getInstance().acceptBooking(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                showToast("Booking Diterima");
                loadRealBookings();
            }

            @Override
            public void onError(String message) {
                showToast(message);
            }
        });
    }

    private void handleReject(Booking b) {
        BookingRepository.getInstance().rejectBooking(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                showToast("Booking Ditolak");
                loadRealBookings();
            }

            @Override
            public void onError(String message) {
                showToast(message);
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
