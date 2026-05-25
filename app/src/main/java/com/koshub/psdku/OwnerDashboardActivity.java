package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Kos;
import com.koshub.psdku.models.OwnerKosStats;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.repositories.StorageRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OwnerDashboardActivity extends AppCompatActivity {

    private KosRepository kosRepository;
    private CloudinaryRepository cloudinaryRepository;
    private StorageRepository storageRepository;
    private FirebaseAuth auth;
    private Uri selectedImageUri;
    private ImageView imgPreview;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (imgPreview != null) {
                        // For profile, it should be circular, but this launcher is shared.
                        // We need to know if we are picking for profile or kos.
                        // Currently, this activity only handles Kos upload in quick actions.
                        // If we add profile upload here later, we should distinguish.
                        Glide.with(this).load(uri).into(imgPreview);
                    }
                }
            }
    );

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

        kosRepository = KosRepository.getInstance();
        cloudinaryRepository = CloudinaryRepository.getInstance();
        storageRepository = StorageRepository.getInstance();
        auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

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
        String uid = auth.getUid();
        kosRepository.calculateOwnerKosStats(uid, new KosRepository.StatsCallback() {
            @Override
            public void onSuccess(OwnerKosStats stats) {
                ((TextView) statTotalKos.findViewById(R.id.tvStatValueKos)).setText(String.valueOf(stats.getTotalKos()));
                ((TextView) statKamarTerisi.findViewById(R.id.tvStatValueTerisi)).setText(String.valueOf(stats.getOccupiedRooms()));
                
                progressOccupancy.setProgress((int) stats.getOccupancyRate());
                ((TextView) findViewById(R.id.tvOccupancyPercent)).setText(String.format(Locale.getDefault(), "%.1f%%", stats.getOccupancyRate()));
                ((TextView) findViewById(R.id.tvOccupancyDetail)).setText(stats.getOccupiedRooms() + "/" + stats.getTotalRooms() + " Kamar Terisi");
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat statistik: " + message);
            }
        });

        statTotalKos.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerManagementActivity.class);
            startActivity(intent);
        });
        
        statKamarTerisi.setOnClickListener(v ->
                showToast("🛏️ Menampilkan rincian hunian kamar..."));
        
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
        // Handled within setupStats callback for synchronization
    }

    private void setupQuickActions() {
        actionTambahKos.setOnClickListener(v -> showAddKosDialog());
        actionTambahKamar.setOnClickListener(v -> showAddRoomDialog());
        actionKelolaPenyewa.setOnClickListener(v ->
                showToast("👥 Fitur Kelola Penyewa tersedia di phase berikutnya"));
        actionLihatBooking.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });
        actionAturHarga.setOnClickListener(v ->
                showToast("💲 Fitur Atur Harga tersedia di phase berikutnya"));
        actionBuatPromo.setOnClickListener(v ->
                showToast("🏷️ Fitur Buat Promo tersedia di phase berikutnya"));
    }

    private void showAddKosDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_kos, null);
        imgPreview = dialogView.findViewById(R.id.imgKosPreview);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        
        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tambah Kos Baru")
                .setView(dialogView)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                EditText etName = dialogView.findViewById(R.id.etKosName);
                EditText etAddress = dialogView.findViewById(R.id.etKosAddress);
                EditText etPrice = dialogView.findViewById(R.id.etKosPrice);
                Spinner spCategory = dialogView.findViewById(R.id.spKosCategory);

                String name = etName.getText().toString().trim();
                String address = etAddress.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String category = spCategory.getSelectedItem().toString().toLowerCase();

                if (name.isEmpty() || address.isEmpty() || priceStr.isEmpty()) {
                    showToast("Harap isi semua field wajib");
                    return;
                }

                double price = Double.parseDouble(priceStr);
                Kos newKos = new Kos("", name, address, "Rp " + priceStr, (int)price, "0 mnt", 0, "0.0", category, new ArrayList<>(), 0, false, "0 Kamar", 0.0, 0.0);
                newKos.setPrice(price);

                kosRepository.createKos(newKos, new KosRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (selectedImageUri != null) {
                            uploadKosImage(newKos.getId());
                        } else {
                            showToast("Kos berhasil ditambahkan");
                            refreshData();
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Gagal: " + message);
                    }
                });
            });
        });
        dialog.show();
    }

    private void uploadKosImage(String kosId) {
        showToast("Sedang mengupload foto...");
        cloudinaryRepository.uploadKosImage(this, selectedImageUri, kosId, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                showToast("Kos & Foto berhasil disimpan");
                selectedImageUri = null;
                refreshData();
            }

            @Override
            public void onError(String message) {
                showToast("Kos disimpan, tapi upload foto gagal: " + message);
                selectedImageUri = null;
                refreshData();
            }
        });
    }

    private void showAddRoomDialog() {
        String uid = auth.getUid();
        kosRepository.getKosByOwner(uid, new KosRepository.KosListCallback() {
            @Override
            public void onSuccess(List<Kos> kosList) {
                if (kosList.isEmpty()) {
                    showToast("Harap tambah kos terlebih dahulu");
                    return;
                }
                
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
                Spinner spKos = dialogView.findViewById(R.id.spSelectKos);
                EditText etRoomName = dialogView.findViewById(R.id.etRoomName);
                EditText etRoomPrice = dialogView.findViewById(R.id.etRoomPrice);

                List<String> kosNames = new ArrayList<>();
                for (Kos k : kosList) kosNames.add(k.getName());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(OwnerDashboardActivity.this, android.R.layout.simple_spinner_item, kosNames);
                spKos.setAdapter(adapter);

                AlertDialog dialog = new AlertDialog.Builder(OwnerDashboardActivity.this)
                        .setTitle("Tambah Kamar")
                        .setView(dialogView)
                        .setPositiveButton("Simpan", null)
                        .setNegativeButton("Batal", null)
                        .create();

                dialog.setOnShowListener(d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        int selectedIdx = spKos.getSelectedItemPosition();
                        String roomName = etRoomName.getText().toString().trim();
                        String priceStr = etRoomPrice.getText().toString().trim();

                        if (roomName.isEmpty() || priceStr.isEmpty()) {
                            showToast("Harap isi semua field");
                            return;
                        }

                        Kos selectedKos = kosList.get(selectedIdx);
                        Room newRoom = new Room("", selectedKos.getId(), uid, roomName, Double.parseDouble(priceStr), DatabaseConstants.ROOM_AVAILABLE);

                        kosRepository.addRoom(newRoom, new KosRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                showToast("Kamar berhasil ditambahkan");
                                dialog.dismiss();
                                refreshData();
                            }

                            @Override
                            public void onError(String message) {
                                showToast("Gagal: " + message);
                            }
                        });
                    });
                });
                dialog.show();
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat data kos: " + message);
            }
        });
    }

    private void refreshData() {
        setupStats();
        setupProperty();
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
        btnSeeAllProperty.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerManagementActivity.class);
            startActivity(intent);
        });

        String uid = auth.getUid();
        kosRepository.getKosByOwner(uid, new KosRepository.KosListCallback() {
            @Override
            public void onSuccess(List<Kos> kosList) {
                if (kosList.isEmpty()) {
                    propertyItem1.setVisibility(View.GONE);
                    propertyItem2.setVisibility(View.GONE);
                } else {
                    propertyItem1.setVisibility(View.VISIBLE);
                    Kos k1 = kosList.get(0);
                    ((TextView) propertyItem1.findViewById(R.id.tvPropertyName1)).setText(k1.getName());
                    ((TextView) propertyItem1.findViewById(R.id.tvPropertyRooms1)).setText(k1.getAvailableRooms() + " Kamar Tersedia");
                    
                    if (kosList.size() > 1) {
                        propertyItem2.setVisibility(View.VISIBLE);
                        Kos k2 = kosList.get(1);
                        ((TextView) propertyItem2.findViewById(R.id.tvPropertyName2)).setText(k2.getName());
                        ((TextView) propertyItem2.findViewById(R.id.tvPropertyRooms2)).setText(k2.getAvailableRooms() + " Kamar Tersedia");
                    } else {
                        propertyItem2.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat daftar kos: " + message);
            }
        });
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
