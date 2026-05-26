package com.koshub.psdku;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.List;

public class WaitingListQueueActivity extends AppCompatActivity {

    private FrameLayout btnNotification;
    
    // Queue Card
    private LinearLayout cardQueueStatus;
    private TextView tvQueueStatus, tvPropertyName, tvPropertyAddress;

    // Available Card
    private LinearLayout cardAvailableStatus;
    private TextView tvAvailableStatus, tvAvailablePropertyName, tvAvailablePropertyAddress;
    private TextView btnPayAvailable;

    private View btnPrimaryAction;
    private TextView btnSecondaryAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list_queue);

        initViews();
        handleWindowInsets();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRealBookings();
    }

    private void initViews() {
        btnNotification = findViewById(R.id.btnNotification);

        cardQueueStatus = findViewById(R.id.cardQueueStatus);
        tvQueueStatus = findViewById(R.id.tvQueueStatus);
        tvPropertyName = findViewById(R.id.tvPropertyName);
        tvPropertyAddress = findViewById(R.id.tvPropertyAddress);

        cardAvailableStatus = findViewById(R.id.cardAvailableStatus);
        tvAvailableStatus = findViewById(R.id.tvAvailableStatus);
        tvAvailablePropertyName = findViewById(R.id.tvAvailablePropertyName);
        tvAvailablePropertyAddress = findViewById(R.id.tvAvailablePropertyAddress);
        btnPayAvailable = findViewById(R.id.btnPayAvailable);

        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnSecondaryAction = findViewById(R.id.btnSecondaryAction);
    }

    private void handleWindowInsets() {
        View root = findViewById(R.id.navbar);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void setupListeners() {
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.WAITLIST);

        btnNotification.setOnClickListener(v -> showCustomToast("Belum ada notifikasi baru"));

        if (btnPrimaryAction != null) {
            btnPrimaryAction.setOnClickListener(v -> showCustomToast("Fitur Chat segera hadir!"));
        }
    }

    private void loadRealBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        BookingRepository.getInstance().getBookingsByStudent(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                if (bookings.isEmpty()) {
                    cardQueueStatus.setVisibility(View.GONE);
                    cardAvailableStatus.setVisibility(View.GONE);
                    showCustomToast("Belum ada booking aktif.");
                } else {
                    Booking b = bookings.get(0); // Show most recent
                    updateUIWithBooking(b);
                }
            }

            @Override
            public void onError(String message) {
                // User friendly message already mapped in Repository
                showCustomToast(message);
            }
        });
    }

    private void updateUIWithBooking(Booking b) {
        String status = b.getStatus();
        
        // Populate standard fields
        tvPropertyName.setText(b.getKosName());
        tvPropertyAddress.setText(b.getKosAddress());
        tvQueueStatus.setText("Status: " + status.toUpperCase());

        if (DatabaseConstants.BOOKING_ACCEPTED.equals(status)) {
            cardQueueStatus.setVisibility(View.GONE);
            cardAvailableStatus.setVisibility(View.VISIBLE);
            tvAvailablePropertyName.setText(b.getKosName());
            tvAvailablePropertyAddress.setText(b.getKosAddress());
            tvAvailableStatus.setText("BOOKING DITERIMA");
            btnPayAvailable.setVisibility(View.VISIBLE);
            btnPayAvailable.setEnabled(true);
            btnPayAvailable.setText("Bayar Sekarang");
            btnPayAvailable.setOnClickListener(v -> handlePayment(b));
            
            btnSecondaryAction.setText("Batalkan Booking");
            btnSecondaryAction.setOnClickListener(v -> showCancelDialog(b));
        } else if (DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(status)) {
            cardQueueStatus.setVisibility(View.GONE);
            cardAvailableStatus.setVisibility(View.VISIBLE);
            tvAvailableStatus.setText("SIAP AMBIL KUNCI");
            btnPayAvailable.setText("Sudah Bayar");
            btnPayAvailable.setEnabled(false);
            
            btnSecondaryAction.setText("Sudah Ambil Kunci");
            btnSecondaryAction.setOnClickListener(v -> handleKeyTaken(b));
        } else if (DatabaseConstants.BOOKING_PENDING.equals(status)) {
            cardQueueStatus.setVisibility(View.VISIBLE);
            cardAvailableStatus.setVisibility(View.GONE);
            tvQueueStatus.setText("MENUNGGU KONFIRMASI");
            btnSecondaryAction.setText("Batalkan Antrean");
            btnSecondaryAction.setOnClickListener(v -> showCancelDialog(b));
        } else {
            // Rejected, Cancelled, Active, etc.
            cardQueueStatus.setVisibility(View.VISIBLE);
            cardAvailableStatus.setVisibility(View.GONE);
            btnSecondaryAction.setVisibility(View.GONE);
        }
    }

    private void handlePayment(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Simulasi Pembayaran")
                .setMessage("Lakukan pembayaran simulasi sebesar Rp " + b.getTotalPrice() + "?")
                .setPositiveButton("Bayar Sekarang", (dialog, which) -> {
                    BookingRepository.getInstance().simulatePayment(b.getId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showCustomToast("Pembayaran Berhasil!");
                            loadRealBookings();
                        }

                        @Override
                        public void onError(String message) {
                            showCustomToast(message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void handleKeyTaken(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Ambil Kunci")
                .setMessage("Apakah kamu sudah menerima kunci dari pemilik?")
                .setPositiveButton("Ya, Sudah", (dialog, which) -> {
                    BookingRepository.getInstance().markKeyTaken(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showCustomToast("Selamat! Sewa kamu kini Aktif.");
                            loadRealBookings();
                        }

                        @Override
                        public void onError(String message) {
                            showCustomToast(message);
                        }
                    });
                })
                .setNegativeButton("Belum", null)
                .show();
    }

    private void showCancelDialog(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Batalkan Booking?")
                .setMessage("Yakin ingin membatalkan booking/antrean ini?")
                .setPositiveButton("Ya, Batalkan", (dialog, which) -> {
                    BookingRepository.getInstance().cancelBooking(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showCustomToast("Berhasil dibatalkan");
                            loadRealBookings();
                        }

                        @Override
                        public void onError(String message) {
                            showCustomToast(message);
                        }
                    });
                })
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void showCustomToast(String message) {
        TextView toastView = new TextView(this);
        toastView.setText(message);
        toastView.setTextColor(Color.WHITE);
        toastView.setTextSize(14f);
        toastView.setBackgroundResource(R.drawable.bg_toast);
        int paddingH = dpToPx(24);
        int paddingV = dpToPx(12);
        toastView.setPadding(paddingH, paddingV, paddingH, paddingV);
        toastView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(this);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dpToPx(100));
        toast.show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
