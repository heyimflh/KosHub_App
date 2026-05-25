package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileHistoryActivity - Halaman Profil Mahasiswa / Penyewa Kos
 *
 * Menampilkan informasi profil user, kelengkapan profil,
 * statistik aktivitas, menu navigasi profil, dan riwayat pemesanan terbaru.
 */
public class ProfileHistoryActivity extends AppCompatActivity {

    // Header
    private View btnEditProfile;

    // Profile Completion
    private ProgressBar progressProfileCompletion;
    private TextView tvCompletionPercent;
    private TextView btnCompleteProfile;

    // Quick Stats
    private LinearLayout statBooking;
    private LinearLayout statFavorite;
    private LinearLayout statReview;
    private LinearLayout statTransaction;

    // Menu Items
    private View menuPersonal;
    private View menuHistory;
    private View menuWishlist;
    private View menuPayment;
    private View menuDocument;
    private View menuHelp;
    private View menuSettings;

    // History Section
    private LinearLayout sectionHistory;
    private LinearLayout emptyStateHistory;
    private TextView btnSeeAllHistory;
    private View historyItem1;
    private View historyItem2;
    private View historyItem3;
    private TextView tvHistoryStatus1, tvCheckInDate1;
    private android.widget.Button btnAmbilKunci, btnLaporkanKomplain;
    private LinearLayout layoutTenantActions;

    // Logout
    private LinearLayout btnLogout;

    // Empty State
    private TextView btnEmptySearch;

    // Data flag - set to true to show empty state instead of history
    private boolean hasBookingHistory = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_history);

        initViews();
        setupProfileCompletion();
        setupQuickStats();
        setupMenuListeners();
        setupHistorySection();
        setupLogout();
        setupBottomNav();
    }

    private void initViews() {
        // Header
        btnEditProfile = findViewById(R.id.btnEditProfile);

        // Profile Completion
        progressProfileCompletion = findViewById(R.id.progressProfileCompletion);
        tvCompletionPercent = findViewById(R.id.tvCompletionPercent);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);

        // Quick Stats
        statBooking = findViewById(R.id.statBooking);
        statFavorite = findViewById(R.id.statFavorite);
        statReview = findViewById(R.id.statReview);
        statTransaction = findViewById(R.id.statTransaction);

        // Menu Items
        menuPersonal = findViewById(R.id.menuPersonal);
        menuHistory = findViewById(R.id.menuHistory);
        menuWishlist = findViewById(R.id.menuWishlist);
        menuPayment = findViewById(R.id.menuPayment);
        menuDocument = findViewById(R.id.menuDocument);
        menuHelp = findViewById(R.id.menuHelp);
        menuSettings = findViewById(R.id.menuSettings);

        // History Section
        sectionHistory = findViewById(R.id.sectionHistory);
        emptyStateHistory = findViewById(R.id.emptyStateHistory);
        btnSeeAllHistory = findViewById(R.id.btnSeeAllHistory);
        historyItem1 = findViewById(R.id.historyItem1);
        tvHistoryStatus1 = findViewById(R.id.tvHistoryStatus1);
        tvCheckInDate1 = findViewById(R.id.tvCheckInDate1);
        btnAmbilKunci = findViewById(R.id.btnAmbilKunci);
        btnLaporkanKomplain = findViewById(R.id.btnLaporkanKomplain);
        layoutTenantActions = findViewById(R.id.layoutTenantActions);
        historyItem2 = findViewById(R.id.historyItem2);
        historyItem3 = findViewById(R.id.historyItem3);

        // Logout
        btnLogout = findViewById(R.id.btnLogout);

        // Empty State
        btnEmptySearch = findViewById(R.id.btnEmptySearch);
    }

    private void setupProfileCompletion() {
        // Set progress
        progressProfileCompletion.setProgress(85);

        // Edit profile button
        btnEditProfile.setOnClickListener(v ->
                showToast("✏️ Membuka halaman edit profil..."));

        // Complete profile CTA
        btnCompleteProfile.setOnClickListener(v ->
                showToast("📋 Lengkapi data profil untuk verifikasi"));
    }

    private void setupQuickStats() {
        statBooking.setOnClickListener(v ->
                showToast("📅 Kamu memiliki 3 riwayat booking"));

        statFavorite.setOnClickListener(v ->
                showToast("❤️ 7 kos tersimpan di wishlist"));

        statReview.setOnClickListener(v ->
                showToast("⭐ 2 review telah kamu berikan"));

        statTransaction.setOnClickListener(v ->
                showToast("💳 5 transaksi tercatat"));
    }

    private void setupMenuListeners() {
        menuPersonal.setOnClickListener(v ->
                showToast("👤 Membuka Data Pribadi..."));

        menuHistory.setOnClickListener(v ->
                showToast("📜 Membuka Riwayat Pemesanan..."));

        menuWishlist.setOnClickListener(v ->
                showToast("❤️ Membuka Wishlist Kos..."));

        menuPayment.setOnClickListener(v ->
                showToast("💳 Membuka Pembayaran..."));

        menuDocument.setOnClickListener(v ->
                showToast("📄 Membuka Dokumen Mahasiswa..."));

        menuHelp.setOnClickListener(v ->
                showToast("❓ Membuka Bantuan & Layanan..."));

        menuSettings.setOnClickListener(v ->
                showToast("⚙️ Membuka Pengaturan Akun..."));
    }

    private void setupHistorySection() {
        if (hasBookingHistory) {
            sectionHistory.setVisibility(View.VISIBLE);
            emptyStateHistory.setVisibility(View.GONE);

            // See all button
            btnSeeAllHistory.setOnClickListener(v ->
                    showToast("📜 Memuat semua riwayat pemesanan..."));

            btnAmbilKunci.setOnClickListener(v -> showAmbilKunciDialog());
            btnLaporkanKomplain.setOnClickListener(v -> {
                NavigationTransitionHelper.navigateDetail(this, TenantComplaintFormActivity.class);
            });

            // History item click listeners
            historyItem1.setOnClickListener(v ->
                    showToast("🏠 Kos Putri Melati - Status: Aktif"));

            historyItem2.setOnClickListener(v ->
                    showToast("🏠 Kos Putra Harmoni - Status: Selesai"));

            historyItem3.setOnClickListener(v ->
                    showToast("🏠 Kos Campur Nusantara - Status: Dibatalkan"));
        } else {
            sectionHistory.setVisibility(View.GONE);
            emptyStateHistory.setVisibility(View.VISIBLE);

            btnEmptySearch.setOnClickListener(v ->
                    showToast("🔍 Membuka halaman pencarian kos..."));
        }
    }

    private void setupLogout() {
        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Yakin ingin keluar dari akun?")
                    .setPositiveButton("Ya", (dialog, which) -> {
                        showToast("👋 Keluar dari akun...");
                        com.koshub.psdku.repositories.AuthRepository.getInstance().logout(this, new com.koshub.psdku.repositories.AuthRepository.AuthCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Intent intent = new Intent(ProfileHistoryActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                showToast("Gagal logout: " + message);
                            }
                        });
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void setupBottomNav() {
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.PROFILE);
    }

    /**
     * Helper method to show toast messages.
     * Provides interactive feedback for all clickable elements.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Toggle between showing history items and empty state.
     * Can be called programmatically to switch views.
     */
    public void toggleEmptyState(boolean showEmpty) {
        hasBookingHistory = !showEmpty;
        if (showEmpty) {
            sectionHistory.setVisibility(View.GONE);
            emptyStateHistory.setVisibility(View.VISIBLE);
        } else {
            sectionHistory.setVisibility(View.VISIBLE);
            emptyStateHistory.setVisibility(View.GONE);
        }
    }

    private void showAmbilKunciDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Konfirmasi Ambil Kunci?")
                .setMessage("Pastikan kamu benar-benar sudah menerima kunci kos dari pemilik.")
                .setPositiveButton("Ya, Sudah Ambil Kunci", (dialog, which) -> {
                    tvHistoryStatus1.setText("Aktif Ngekos");
                    tvHistoryStatus1.setBackgroundResource(R.drawable.bg_profile_status_active);
                    tvHistoryStatus1.setTextColor(getResources().getColor(R.color.brand_green));
                    tvCheckInDate1.setText("Mulai ngekos: 20 Mei 2026");
                    layoutTenantActions.setVisibility(View.GONE);
                    showToast("Status sewa berhasil diaktifkan");
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
