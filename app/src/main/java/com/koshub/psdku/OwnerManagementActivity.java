package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * OwnerManagementActivity - Halaman Manajemen Kos
 *
 * Menampilkan ringkasan properti, statistik kamar, aksi cepat,
 * manajemen kamar, booking request, penyewa aktif, dan alert.
 */
public class OwnerManagementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_management);

        setupPropertySelector();
        setupQuickActions();
        setupRoomSection();
        setupBookingSection();
        setupTenantSection();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.KOS);
    }

    private void setupPropertySelector() {
        findViewById(R.id.cardPropertySelector).setOnClickListener(v ->
                showToast("🏠 Ganti kos yang dikelola..."));
    }

    private void setupQuickActions() {
        findViewById(R.id.actionMgmtTambahKamar).setOnClickListener(v ->
                showToast("➕ Membuka form Tambah Kamar..."));
        findViewById(R.id.actionMgmtKelolaKamar).setOnClickListener(v ->
                showToast("🚪 Membuka daftar Kelola Kamar..."));
        findViewById(R.id.actionMgmtKelolaPenyewa).setOnClickListener(v ->
                showToast("👥 Membuka daftar Kelola Penyewa..."));
        findViewById(R.id.actionMgmtKonfirmasi).setOnClickListener(v ->
                showToast("✅ Membuka Konfirmasi Booking..."));
        findViewById(R.id.actionMgmtFasilitas).setOnClickListener(v ->
                showToast("🔧 Membuka Edit Fasilitas..."));
        findViewById(R.id.actionMgmtMaintenance).setOnClickListener(v ->
                showToast("🛠️ Membuka Laporan Maintenance..."));
    }

    private void setupRoomSection() {
        findViewById(R.id.btnSeeAllRooms).setOnClickListener(v ->
                showToast("🚪 Memuat semua kamar..."));
        findViewById(R.id.roomItem1).setOnClickListener(v ->
                showToast("🚪 Kamar A-01 - Terisi - Muhammad Fakhri"));
        findViewById(R.id.roomItem2).setOnClickListener(v ->
                showToast("🚪 Kamar A-02 - Kosong - Tersedia"));
        findViewById(R.id.roomItem3).setOnClickListener(v ->
                showToast("🛠️ Kamar B-04 - Maintenance - Perbaikan AC"));
    }

    private void setupBookingSection() {
        findViewById(R.id.btnSeeAllBooking).setOnClickListener(v ->
                showToast("📋 Memuat semua booking..."));
        findViewById(R.id.btnAcceptBooking1).setOnClickListener(v ->
                showToast("✅ Booking Raka Pratama diterima!"));
        findViewById(R.id.btnRejectBooking1).setOnClickListener(v ->
                showToast("❌ Booking Raka Pratama ditolak"));
        findViewById(R.id.btnAcceptBooking2).setOnClickListener(v ->
                showToast("✅ Booking Dinda Ayu diterima!"));
        findViewById(R.id.btnRejectBooking2).setOnClickListener(v ->
                showToast("❌ Booking Dinda Ayu ditolak"));
    }

    private void setupTenantSection() {
        findViewById(R.id.btnSeeAllTenants).setOnClickListener(v ->
                showToast("👥 Memuat semua penyewa..."));
        findViewById(R.id.tenantItem1).setOnClickListener(v ->
                showToast("👤 Muhammad Fakhri - Kamar A-01 - Lunas"));
        findViewById(R.id.tenantItem2).setOnClickListener(v ->
                showToast("👤 Siti Nurhaliza - Kamar A-05 - Terlambat"));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
