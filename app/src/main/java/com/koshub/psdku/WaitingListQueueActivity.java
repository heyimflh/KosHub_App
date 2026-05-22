package com.koshub.psdku;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;

public class WaitingListQueueActivity extends AppCompatActivity {

    // Top Bar
    private FrameLayout btnNotification;

    // Bottom Nav
    private LinearLayout navHome, navMap, navWaitlist, navProfile;

    // Status Card (Waiting - Card 1)
    private LinearLayout cardQueueStatus;
    private TextView tvQueueStatus;
    private TextView tvPropertyName;
    private TextView tvPropertyAddress;
    private TextView tvQueuePosition;
    private TextView tvEstimatedAvailability;
    private TextView tvQueueMicrocopy;

    // Available Card (Card 2 - Action Required)
    private LinearLayout cardAvailableStatus;
    private TextView tvAvailableStatus;
    private TextView tvAvailablePropertyName;
    private TextView tvAvailablePropertyAddress;
    private ImageView imgAvailableProperty;
    private TextView btnPayAvailable;
    private TextView btnViewDetailAvailable;

    // Next Step
    private View btnPrimaryAction;
    private TextView btnSecondaryAction;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list_queue);

        handler = new Handler(Looper.getMainLooper());

        initViews();
        handleWindowInsets();
        setupListeners();
        loadData();
    }

    private void initViews() {
        // Top Bar
        btnNotification = findViewById(R.id.btnNotification);

        // Bottom Nav
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navWaitlist = findViewById(R.id.navWaitlist);
        navProfile = findViewById(R.id.navProfile);

        // Status Card
        cardQueueStatus = findViewById(R.id.cardQueueStatus);
        tvQueueStatus = findViewById(R.id.tvQueueStatus);
        tvPropertyName = findViewById(R.id.tvPropertyName);
        tvPropertyAddress = findViewById(R.id.tvPropertyAddress);
        tvQueuePosition = findViewById(R.id.tvQueuePosition);
        tvEstimatedAvailability = findViewById(R.id.tvEstimatedAvailability);
        tvQueueMicrocopy = findViewById(R.id.tvQueueMicrocopy);

        // Available Card
        cardAvailableStatus = findViewById(R.id.cardAvailableStatus);
        tvAvailableStatus = findViewById(R.id.tvAvailableStatus);
        tvAvailablePropertyName = findViewById(R.id.tvAvailablePropertyName);
        tvAvailablePropertyAddress = findViewById(R.id.tvAvailablePropertyAddress);
        imgAvailableProperty = findViewById(R.id.imgAvailableProperty);
        btnPayAvailable = findViewById(R.id.btnPayAvailable);
        btnViewDetailAvailable = findViewById(R.id.btnViewDetailAvailable);

        // Next Step
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnSecondaryAction = findViewById(R.id.btnSecondaryAction);
    }

    private void handleWindowInsets() {
        View navbar = findViewById(R.id.navbar);
        if (navbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navbar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + dpToPx(12), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        View bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }
    }

    private void setupListeners() {
        // Notification
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> showCustomToast("🔔 Tidak ada notifikasi baru"));
        }

        // Bottom Nav
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.WAITLIST);

        // Actions
        if (btnPayAvailable != null) btnPayAvailable.setOnClickListener(v -> handlePayment(btnPayAvailable));

        if (btnViewDetailAvailable != null) btnViewDetailAvailable.setOnClickListener(v -> navigateToPropertyDetail("harmoni"));

        if (btnPrimaryAction != null) btnPrimaryAction.setOnClickListener(v -> showCustomToast("📞 Menghubungi pemilik kos..."));
        if (btnSecondaryAction != null) btnSecondaryAction.setOnClickListener(v -> showCancelDialog());

        if (cardQueueStatus != null) cardQueueStatus.setOnClickListener(v -> navigateToPropertyDetail("sakura"));
        if (cardAvailableStatus != null) cardAvailableStatus.setOnClickListener(v -> navigateToPropertyDetail("harmoni"));
    }

    private void loadData() {
        // Mock "Real" Data (matching StudentHomeActivity/PropertyDetail)
        
        // Card 1: Waiting List (Sakura)
        tvPropertyName.setText("Kos Putri Premium Sakura");
        tvPropertyAddress.setText("Jl. Mawar No. 17, Kebumen");
        tvQueuePosition.setText("#3");
        tvQueueStatus.setText("⏳ Dalam Antrean");
        tvEstimatedAvailability.setText("Est. 2-3 Minggu");
        tvQueueMicrocopy.setText("Kamu berada di posisi antrean ke-3. Kami akan memberitahumu segera setelah ada slot kosong.");

        // Card 2: Action Required (Harmoni)
        tvAvailablePropertyName.setText("Kos Putra Harmoni");
        tvAvailablePropertyAddress.setText("Jl. Pendidikan No. 12, Kebumen");
        tvAvailableStatus.setText("✅ Tersedia! Amankan Sekarang");
        imgAvailableProperty.setImageResource(R.drawable.kos_03);
    }

    private void handlePayment(TextView payButton) {
        payButton.setEnabled(false);
        payButton.setAlpha(0.7f);
        payButton.setText("⏳ Memproses...");

        handler.postDelayed(() -> {
            payButton.setText("✅ Terkonfirmasi");
            payButton.setBackgroundResource(R.drawable.bg_waiting_btn_pay_confirmed);
            payButton.setAlpha(1.0f);
            showCustomToast("💳 Pembayaran berhasil! Kos Anda telah dipesan.");
        }, 1500);
    }

    private void navigateToPropertyDetail(String kosType) {
        Intent intent = new Intent(this, PropertyDetailBookingActivity.class);
        KosItem selectedKos;

        if ("harmoni".equals(kosType)) {
            selectedKos = new KosItem(
                    "Kos Putra Harmoni", "Jl. Pendidikan No. 12, Kebumen",
                    "Rp 750rb", 750000, "5 mnt", 5, "4.8", "Putra",
                    java.util.Arrays.asList("WiFi", "K. Mandi Dalam", "Laundry"),
                    R.drawable.kos_03, false, null,
                    -7.68307 + 0.0005, 109.6645 + 0.0005);
        } else {
            selectedKos = new KosItem(
                    "Kos Putri Premium Sakura", "Jl. Mawar No. 17, Kebumen",
                    "Rp 1.2jt", 1200000, "8 mnt", 8, "4.9", "Putri",
                    java.util.Arrays.asList("AC", "WiFi", "K. Mandi Dalam"),
                    R.drawable.kos_01, true, null,
                    -7.68307 + 0.001, 109.6645 - 0.001);
        }
        
        intent.putExtra("kos_item", selectedKos);
        startActivity(intent);
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Batalkan Antrean?")
                .setMessage("Kamu akan keluar dari waiting list kos ini. Posisi antreanmu tidak bisa dikembalikan.")
                .setPositiveButton("Ya, Batalkan", (dialog, which) -> {
                    tvQueueStatus.setText("❌ Dibatalkan");
                    tvQueueStatus.setTextColor(Color.RED);
                    btnSecondaryAction.setEnabled(false);
                    btnSecondaryAction.setAlpha(0.5f);
                    showCustomToast("Antrean berhasil dibatalkan");
                })
                .setNegativeButton("Kembali", null)
                .show();
    }

    private void showCustomToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
