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
 * OwnerDashboardActivity - Dashboard Pemilik Kos
 *
 * Menampilkan ringkasan statistik, tingkat hunian, aksi cepat,
 * booking terbaru, daftar properti, pendapatan, dan notifikasi penting.
 */
public class OwnerDashboardActivity extends AppCompatActivity {

    // Header
    private View btnOwnerNotification;

    // Stats
    private LinearLayout statTotalKos;
    private LinearLayout statKamarTerisi;
    private LinearLayout statBookingMasuk;
    private LinearLayout statPendapatan;

    // Finance & Complaint Detail
    private LinearLayout cardSaldoOwner;
    private TextView btnTarikSaldo;
    private LinearLayout cardSaldoTersedia;
    private LinearLayout cardSaldoPending;
    private LinearLayout cardKomplainMasuk;
    private LinearLayout cardSiapCheckin;

    // Occupancy
    private ProgressBar progressOccupancy;

    // Quick Actions
    private LinearLayout actionTambahKos;
    private LinearLayout actionTambahKamar;
    private LinearLayout actionKelolaPenyewa;
    private LinearLayout actionLihatBooking;
    private LinearLayout actionAturHarga;
    private LinearLayout actionBuatPromo;

    // Bookings
    private LinearLayout sectionBookings;
    private TextView btnSeeAllBooking;
    private LinearLayout bookingItem1;
    private LinearLayout bookingItem2;

    // Property
    private LinearLayout sectionProperty;
    private TextView btnSeeAllProperty;
    private LinearLayout propertyItem1;
    private LinearLayout propertyItem2;

    // Revenue
    private LinearLayout cardRevenue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        initViews();
        setupHeader();
        setupStats();
        setupOccupancy();
        setupQuickActions();
        setupBookings();
        setupProperty();
        setupRevenue();
        setupBottomNav();
    }

    private void initViews() {
        // Header
        btnOwnerNotification = findViewById(R.id.btnOwnerTopNotification);

        // Stats
        statTotalKos = findViewById(R.id.statTotalKos);
        statKamarTerisi = findViewById(R.id.statKamarTerisi);
        statBookingMasuk = findViewById(R.id.statBookingMasuk);
        statPendapatan = findViewById(R.id.statPendapatan);

        // Finance & Complaint Detail
        cardSaldoOwner = findViewById(R.id.cardSaldoOwner);
        btnTarikSaldo = findViewById(R.id.btnTarikSaldo);
        cardSaldoTersedia = findViewById(R.id.cardSaldoTersedia);
        cardSaldoPending = findViewById(R.id.cardSaldoPending);
        cardKomplainMasuk = findViewById(R.id.cardKomplainMasuk);
        cardSiapCheckin = findViewById(R.id.cardSiapCheckin);

        // Occupancy
        progressOccupancy = findViewById(R.id.progressOccupancy);

        // Quick Actions
        actionTambahKos = findViewById(R.id.actionTambahKos);
        actionTambahKamar = findViewById(R.id.actionTambahKamar);
        actionKelolaPenyewa = findViewById(R.id.actionKelolaPenyewa);
        actionLihatBooking = findViewById(R.id.actionLihatBooking);
        actionAturHarga = findViewById(R.id.actionAturHarga);
        actionBuatPromo = findViewById(R.id.actionBuatPromo);

        // Bookings
        sectionBookings = findViewById(R.id.sectionBookings);
        btnSeeAllBooking = findViewById(R.id.btnSeeAllBooking);
        bookingItem1 = findViewById(R.id.bookingItem1);
        bookingItem2 = findViewById(R.id.bookingItem2);

        // Property
        sectionProperty = findViewById(R.id.sectionProperty);
        btnSeeAllProperty = findViewById(R.id.btnSeeAllProperty);
        propertyItem1 = findViewById(R.id.propertyItem1);
        propertyItem2 = findViewById(R.id.propertyItem2);

        // Revenue
        cardRevenue = findViewById(R.id.cardRevenue);

        // Bottom Nav
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.DASHBOARD);
    }

    private void setupHeader() {
        btnOwnerNotification.setOnClickListener(v ->
                showToast("🔔 3 notifikasi baru"));
    }

    private void setupStats() {
        statTotalKos.setOnClickListener(v ->
                showToast("🏠 Anda memiliki 2 kos terdaftar"));
        statKamarTerisi.setOnClickListener(v ->
                showToast("🛏️ 15 dari 20 kamar terisi"));
        statBookingMasuk.setOnClickListener(v ->
                showToast("📅 4 booking masuk, 3 baru hari ini"));
        statPendapatan.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });

        cardSaldoOwner.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });

        btnTarikSaldo.setOnClickListener(v ->
                showToast("🏧 Membuka fitur tarik saldo..."));

        cardSaldoTersedia.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });

        cardSaldoPending.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });

        cardKomplainMasuk.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerComplaintActivity.class);
        });

        cardSiapCheckin.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateMain(OwnerDashboardActivity.this, OwnerBookingActivity.class);
        });
    }

    private void setupOccupancy() {
        progressOccupancy.setProgress(75);
    }

    private void setupQuickActions() {
        actionTambahKos.setOnClickListener(v ->
                showToast("➕ Membuka form Tambah Kos..."));
        actionTambahKamar.setOnClickListener(v ->
                showToast("🚪 Membuka form Tambah Kamar..."));
        actionKelolaPenyewa.setOnClickListener(v ->
                showToast("👥 Membuka daftar Penyewa..."));
        actionLihatBooking.setOnClickListener(v ->
                showToast("📋 Membuka daftar Booking..."));
        actionAturHarga.setOnClickListener(v ->
                showToast("💲 Membuka pengaturan Harga..."));
        actionBuatPromo.setOnClickListener(v ->
                showToast("🏷️ Membuka form Buat Promo..."));
    }

    private void setupBookings() {
        btnSeeAllBooking.setOnClickListener(v ->
                showToast("📋 Memuat semua booking..."));
        bookingItem1.setOnClickListener(v ->
                showToast("📅 Muhammad Fakhri - Menunggu Konfirmasi"));
        bookingItem2.setOnClickListener(v ->
                showToast("✅ Siti Nurhaliza - Booking Diterima"));
    }

    private void setupProperty() {
        btnSeeAllProperty.setOnClickListener(v ->
                showToast("🏠 Memuat semua properti..."));
        propertyItem1.setOnClickListener(v ->
                showToast("🏠 Kos Melati Indah - 12 kamar, 3 tersedia"));
        propertyItem2.setOnClickListener(v ->
                showToast("🏠 Kos Putra Harmoni - 8 kamar, 2 tersedia"));
    }

    private void setupRevenue() {
        cardRevenue.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });
    }

    private void setupBottomNav() {
        // Handled by OwnerBottomNavHelper
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
