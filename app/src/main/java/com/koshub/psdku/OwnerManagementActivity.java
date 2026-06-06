package com.koshub.psdku;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.adapters.ImagePreviewAdapter;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Kos;
import com.koshub.psdku.models.OwnerKosStats;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.SystemInsetsHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerManagementActivity extends AppCompatActivity {

    private com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
    private KosRepository kosRepository;
    private CloudinaryRepository cloudinaryRepository;
    private BookingRepository bookingRepository;
    private FirebaseAuth auth;
    private List<Kos> ownerKosList;
    private final List<Object> selectedImages = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;

    private android.widget.LinearLayout roomListContainer;
    private android.widget.LinearLayout bookingListContainer;
    private android.widget.LinearLayout tenantListContainer;

    private String selectedKosId;
    private int pendingBookingsCount = 0;
    private int maintenanceRoomsCount = 0;

    private static final int AUTOCOMPLETE_REQUEST_CODE = 100;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private String selectedPlaceId = "";
    private EditText etAddressRef;
    private TextView tvAddressHelperRef;

    private final String[] securityOptions = {"CCTV", "akses gerbang", "penjaga kos", "kunci pribadi"};
    private final String[] accessOptions = {"dekat kampus", "dekat warung makan", "dekat minimarket", "dekat masjid", "akses motor mudah"};
    private final String[] roomOptions = {"kamar mandi dalam", "kamar mandi luar", "kasur", "lemari", "meja belajar", "kipas", "AC"};
    private final String[] ruleOptions = {"khusus mahasiswa", "boleh bulanan", "tidak boleh membawa hewan", "jam malam", "bebas jam malam"};

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (selectedImages.size() < 5) {
                            selectedImages.add(uri);
                        } else {
                            showToast("Maksimal 5 gambar");
                            break;
                        }
                    }
                    if (imagePreviewAdapter != null) imagePreviewAdapter.notifyDataSetChanged();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_management);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_KEY);
        }

        kosRepository = KosRepository.getInstance();
        cloudinaryRepository = CloudinaryRepository.getInstance();
        bookingRepository = BookingRepository.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        loadData();
        // DEBUG: Clean old kos on first load
        cleanOldKosWithBadCoordinates();
        setupPropertySelector();
        setupQuickActions();
        setupRoomSection();
        setupBookingSection();
        setupTenantSection();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.KOS);

        // Check if redirected from dashboard to add new kos
        if (getIntent().getBooleanExtra("SHOW_ADD_DIALOG", false)) {
            getIntent().removeExtra("SHOW_ADD_DIALOG");
            showAddKosDialog();
        }

        SystemInsetsHelper.applySystemBars(
            this,
            findViewById(R.id.headerOwnerManagement),
            findViewById(R.id.ownerBottomNav),
            findViewById(R.id.scrollOwnerManagement),
            false,
            true
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        String uid = auth.getUid();
        
        View emptyState = findViewById(R.id.layoutEmptyStateMgmt);
        View propertyCard = findViewById(R.id.cardPropertySelector);
        findViewById(R.id.btnEmptyTambahKos).setOnClickListener(v -> showAddKosDialog());

        // 1. Load Kos List
        kosRepository.getKosByOwner(uid, new KosRepository.KosListCallback() {
            @Override
            public void onSuccess(List<Kos> kosList) {
                ownerKosList = kosList;
                
                // Update Badge
                TextView tvKosAktifBadge = findViewById(R.id.tvKosAktifBadge);
                if (tvKosAktifBadge != null) {
                    tvKosAktifBadge.setText(kosList.size() + " Kos Aktif");
                }

                if (kosList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    propertyCard.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    propertyCard.setVisibility(View.VISIBLE);
                    
                    // Logic: cari kos yang sedang terpilih atau default ke yang pertama
                    Kos currentKos = null;
                    if (selectedKosId != null) {
                        for (Kos k : kosList) {
                            if (k.getId().equals(selectedKosId)) {
                                currentKos = k;
                                break;
                            }
                        }
                    }
                    
                    if (currentKos == null) {
                        currentKos = kosList.get(0);
                        selectedKosId = currentKos.getId();
                    }
                    
                    updateCurrentKosCard(currentKos);
                }
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat kos: " + message);
            }
        });

        // 2. Load Stats
        kosRepository.calculateOwnerKosStats(uid, new KosRepository.StatsCallback() {
            @Override
            public void onSuccess(OwnerKosStats stats) {
                updateStatsUI(stats);
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat statistik: " + message);
            }
        });

        // 3. Load Pending Bookings
        bookingRepository.getBookingsByOwner(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                int pending = 0;
                for (Booking b : bookings) {
                    if ("pending".equalsIgnoreCase(b.getStatus())) {
                        pending++;
                    }
                }
                pendingBookingsCount = pending;
                TextView tvStatBookingMenunggu = findViewById(R.id.tvStatBookingMenunggu);
                if (tvStatBookingMenunggu != null) tvStatBookingMenunggu.setText(String.valueOf(pending));
                updateAlertsUI();

                // Refresh dynamic sections
                loadRoomSection();
                loadBookingSection();
                loadTenantSection();
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat booking: " + message);
            }
        });
    }

    private void updateCurrentKosCard(Kos kos) {
        TextView tvName = findViewById(R.id.tvCurrentKosName);
        TextView tvStatus = findViewById(R.id.tvCurrentKosStatus);
        
        if (tvName != null) tvName.setText(kos.getName());
        if (tvStatus != null) tvStatus.setText("Kelola ▾");
        
        selectedKosId = kos.getId();
        updateCurrentKosSubtitle(kos);
    }

    private void updateCurrentKosSubtitle(Kos kos) {
        TextView tvSubtitle = findViewById(R.id.tvCurrentKosSubtitle);
        if (tvSubtitle == null) return;

        String address = (kos.getAddress() != null && !kos.getAddress().isEmpty()) 
                ? kos.getAddress() : "Alamat belum diisi";

        kosRepository.getRoomsByKos(kos.getId(), new KosRepository.RoomListCallback() {
            @Override
            public void onSuccess(List<Room> rooms) {
                int roomCount = rooms.size();
                String subtitle = address + " • " + roomCount + " kamar • Ketuk untuk kelola";
                tvSubtitle.setText(subtitle);
            }

            @Override
            public void onError(String message) {
                tvSubtitle.setText(address + " • Ketuk untuk kelola");
            }
        });
    }

    private void updateStatsUI(OwnerKosStats stats) {
        // Update statistics cards
        TextView tvTotalKamar = findViewById(R.id.tvTotalKamar);
        TextView tvKamarTerisi = findViewById(R.id.tvKamarTerisi);
        TextView tvKamarKosong = findViewById(R.id.tvKamarKosong);
        TextView tvStatMaintenance = findViewById(R.id.tvStatMaintenance);

        if (tvTotalKamar != null) tvTotalKamar.setText(String.valueOf(stats.getTotalRooms()));
        if (tvKamarTerisi != null) tvKamarTerisi.setText(String.valueOf(stats.getOccupiedRooms()));
        if (tvKamarKosong != null) tvKamarKosong.setText(String.valueOf(stats.getAvailableRooms()));
        if (tvStatMaintenance != null) tvStatMaintenance.setText(String.valueOf(stats.getMaintenanceRooms()));
        
        this.maintenanceRoomsCount = stats.getMaintenanceRooms();
        updateAlertsUI();

        // Additional secondary stats
        TextView tvStatPenyewa = findViewById(R.id.tvStatPenyewa);
        if (tvStatPenyewa != null) tvStatPenyewa.setText(stats.getOccupiedRooms() + " Aktif");
    }

    private void updateAlertsUI() {
        View layoutAlert = findViewById(R.id.layoutPerluPerhatian);
        TextView tvAlertContent = findViewById(R.id.tvPerluPerhatianContent);
        
        if (layoutAlert == null || tvAlertContent == null) return;

        StringBuilder alerts = new StringBuilder();
        if (pendingBookingsCount > 0) {
            alerts.append("• ").append(pendingBookingsCount).append(" booking menunggu konfirmasi\n");
        }
        if (maintenanceRoomsCount > 0) {
            alerts.append("• ").append(maintenanceRoomsCount).append(" kamar dalam pemeliharaan (maintenance)");
        }

        String content = alerts.toString().trim();
        if (content.isEmpty()) {
            layoutAlert.setVisibility(View.GONE);
        } else {
            layoutAlert.setVisibility(View.VISIBLE);
            tvAlertContent.setText(content);
        }
    }

    private void setupPropertySelector() {
        findViewById(R.id.cardPropertySelector).setOnClickListener(v -> {
            if (ownerKosList == null || ownerKosList.isEmpty()) {
                showToast("Belum ada kos terdaftar.");
                return;
            }

            if (ownerKosList.size() == 1) {
                showKosActionDialog(ownerKosList.get(0));
            } else {
                String[] names = new String[ownerKosList.size()];
                for (int i = 0; i < ownerKosList.size(); i++) names[i] = ownerKosList.get(i).getName();
                
                new AlertDialog.Builder(this)
                        .setTitle("Pilih Kos")
                        .setItems(names, (dialog, which) -> {
                            Kos selected = ownerKosList.get(which);
                            updateCurrentKosCard(selected);
                            showKosActionDialog(selected);
                        })
                        .show();
            }
        });
    }

    private void showKosActionDialog(Kos kos) {
        String[] options = {
                "👁️  Tampilkan Kos Ini",
                "✏️  Edit Data Kos",
                "💰  Atur Harga",
                "🛠️  Edit Fasilitas",
                "🗑️  Hapus Kos"
        };

        new AlertDialog.Builder(this)
                .setTitle(kos.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectedKosId = kos.getId();
                            TextView tvCurrentKos = findViewById(R.id.tvCurrentKosName);
                            if (tvCurrentKos != null) tvCurrentKos.setText(kos.getName());
                            showToast("Menampilkan " + kos.getName());
                            // Optional: refresh specific sections if needed
                            break;
                        case 1:
                            showEditKosDialog(kos);
                            break;
                        case 2:
                            showEditPriceDialog(kos);
                            break;
                        case 3:
                            showEditFacilitiesDialog();
                            break;
                        case 4:
                            showDeleteKosConfirmation(kos);
                            break;
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void showDeleteKosConfirmation(Kos kos) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Kos?")
                .setMessage("Yakin ingin menghapus kos \"" + kos.getName() + "\"? Semua kamar di kos ini juga akan dihapus. Kos dengan booking aktif tidak bisa dihapus.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    kosRepository.deleteKos(kos.getId(), new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Kos berhasil dihapus");
                            if (kos.getId().equals(selectedKosId)) {
                                selectedKosId = null;
                            }
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal menghapus: " + message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void setupQuickActions() {
        findViewById(R.id.actionMgmtTambahKos).setOnClickListener(v -> showAddKosDialog());
        findViewById(R.id.actionMgmtTambahKamar).setOnClickListener(v -> showAddRoomDialog());
        findViewById(R.id.actionMgmtKelolaKamar).setOnClickListener(v -> showRoomManagementDialog());
        findViewById(R.id.actionMgmtKelolaPenyewa).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            intent.putExtra("TAB", "active");
            startActivity(intent);
        });

        findViewById(R.id.actionMgmtFasilitas).setOnClickListener(v -> showEditFacilitiesDialog());

        findViewById(R.id.actionMgmtMaintenance).setOnClickListener(v -> showMaintenanceDialog());

        // Entry point "Atur Harga"
        int resId = getResources().getIdentifier("actionMgmtAturHarga", "id", getPackageName());
        if (resId != 0) {
            View btnAturHarga = findViewById(resId);
            if (btnAturHarga != null) {
                btnAturHarga.setOnClickListener(v -> {
                    if (ownerKosList == null || ownerKosList.isEmpty()) {
                        showToast("Belum ada kos");
                        return;
                    }
                    String[] names = new String[ownerKosList.size()];
                    for (int i = 0; i < ownerKosList.size(); i++) names[i] = ownerKosList.get(i).getName();
                    new AlertDialog.Builder(this)
                            .setTitle("Pilih Kos untuk Atur Harga")
                            .setItems(names, (dialog, which) -> showEditPriceDialog(ownerKosList.get(which)))
                            .show();
                });
            }
        }
    }

    private void showAddKosDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_kos, null);
        RecyclerView rvPreviews = dialogView.findViewById(R.id.rvImagePreviews);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);

        selectedImages.clear();
        imagePreviewAdapter = new ImagePreviewAdapter(selectedImages, position -> {
            selectedImages.remove(position);
            imagePreviewAdapter.notifyDataSetChanged();
        });
        rvPreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPreviews.setAdapter(imagePreviewAdapter);

        List<String> roomFeatures = new ArrayList<>();
        List<String> accessFeatures = new ArrayList<>();
        List<String> securityFeatures = new ArrayList<>();
        List<String> rules = new ArrayList<>();

        dialogView.findViewById(R.id.btnEditRoomFeatures).setOnClickListener(v ->
                showFeatureSelectionDialog("Fasilitas Kamar", roomOptions, roomFeatures, selected -> {
                    roomFeatures.clear();
                    roomFeatures.addAll(selected);
                }));

        dialogView.findViewById(R.id.btnEditAccessFeatures).setOnClickListener(v ->
                showFeatureSelectionDialog("Akses Lokasi", accessOptions, accessFeatures, selected -> {
                    accessFeatures.clear();
                    accessFeatures.addAll(selected);
                }));

        dialogView.findViewById(R.id.btnEditSecurityFeatures).setOnClickListener(v ->
                showFeatureSelectionDialog("Keamanan", securityOptions, securityFeatures, selected -> {
                    securityFeatures.clear();
                    securityFeatures.addAll(selected);
                }));

        dialogView.findViewById(R.id.btnEditRules).setOnClickListener(v ->
                showFeatureSelectionDialog("Aturan Kos", ruleOptions, rules, selected -> {
                    rules.clear();
                    rules.addAll(selected);
                }));

        // Reset location fields for new entry
        selectedLatitude = 0.0;
        selectedLongitude = 0.0;
        selectedPlaceId = "";

        etAddressRef = dialogView.findViewById(R.id.etKosAddress);
        etAddressRef.setFocusable(false);
        etAddressRef.setFocusableInTouchMode(false);
        etAddressRef.setCursorVisible(false);
        etAddressRef.setOnClickListener(v -> startAutocompletePicker());

        tvAddressHelperRef = dialogView.findViewById(R.id.tvAddressHelper);

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
                EditText etPrice = dialogView.findViewById(R.id.etKosPrice);
                Spinner spCategory = dialogView.findViewById(R.id.spKosCategory);

                String name = etName.getText().toString().trim();
                String address = etAddressRef.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String category = spCategory.getSelectedItem().toString().toLowerCase();

                if (name.isEmpty() || address.isEmpty() || priceStr.isEmpty()) {
                    showToast("Harap isi semua field wajib");
                    return;
                }

                // Strict validation for Google Places data
                if (selectedLatitude == 0.0 || selectedLongitude == 0.0 || selectedPlaceId.isEmpty()) {
                    showToast("Silakan pilih alamat kos dari Google Maps terlebih dahulu.");
                    return;
                }

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    showToast("Harga tidak valid");
                    return;
                }

                Kos newKos = new Kos("", name, address, "Rp " + priceStr, (int)price, "...", 0, "0.0", category, new ArrayList<>(), 0, false, "0 Kamar", selectedLatitude, selectedLongitude);
                newKos.setPrice(price);
                newKos.setPlaceId(selectedPlaceId);
                newKos.setRoomFeatures(roomFeatures);
                newKos.setAccessFeatures(accessFeatures);
                newKos.setSecurityFeatures(securityFeatures);
                newKos.setRules(rules);
                
                // Consolidate features into primary facilities list for card consistency
                List<String> combined = new ArrayList<>();
                combined.addAll(roomFeatures);
                combined.addAll(accessFeatures);
                combined.addAll(securityFeatures);
                newKos.setFacilities(combined);

                kosRepository.createKos(newKos, new KosRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!selectedImages.isEmpty()) {
                            uploadMultipleKosImages(newKos.getId());
                        } else {
                            showToast("Kos berhasil ditambahkan");
                            loadData();
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

    private void uploadMultipleKosImages(String kosId) {
        showToast("Sedang mengupload foto-foto kos...");
        
        List<Uri> newUris = new ArrayList<>();
        List<String> existingUrls = new ArrayList<>();
        for (Object item : selectedImages) {
            if (item instanceof Uri) {
                newUris.add((Uri) item);
            } else if (item instanceof String) {
                existingUrls.add((String) item);
            }
        }

        cloudinaryRepository.uploadMultipleKosImages(this, newUris, existingUrls, kosId, new CloudinaryRepository.MultiUploadCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                runOnUiThread(() -> {
                    showToast("Kos & Foto berhasil disimpan");
                    selectedImages.clear();
                    loadData();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showToast("Kos disimpan, tapi upload foto gagal: " + message);
                    selectedImages.clear();
                    loadData();
                });
            }
        });
    }

    private void finishKosImageUpdate(String kosId, List<String> finalUrls) {
        // Not used anymore as Repository handles the update
    }

    private void showAddRoomDialog() {
        if (ownerKosList == null || ownerKosList.isEmpty()) {
            showToast("Harap tambah kos terlebih dahulu");
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        Spinner spKos = dialogView.findViewById(R.id.spSelectKos);
        EditText etRoomName = dialogView.findViewById(R.id.etRoomName);
        EditText etRoomPrice = dialogView.findViewById(R.id.etRoomPrice);

        List<String> kosNames = new ArrayList<>();
        for (Kos k : ownerKosList) kosNames.add(k.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, kosNames);
        spKos.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Tambah Kamar")
                .setView(dialogView)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    int selectedIdx = spKos.getSelectedItemPosition();
                    String roomName = etRoomName.getText().toString().trim();
                    String priceStr = etRoomPrice.getText().toString().trim();

                    if (roomName.isEmpty() || priceStr.isEmpty()) {
                        showToast("Harap isi semua field");
                        return;
                    }

                    Kos selectedKos = ownerKosList.get(selectedIdx);
                    Room newRoom = new Room("", selectedKos.getId(), auth.getUid(), roomName, Double.parseDouble(priceStr), DatabaseConstants.ROOM_AVAILABLE);

                    kosRepository.addRoom(newRoom, new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Kamar berhasil ditambahkan");
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal: " + message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showRoomManagementDialog() {
        String uid = auth.getUid();
        kosRepository.getRoomsByOwner(uid, new KosRepository.RoomListCallback() {
            @Override
            public void onSuccess(List<Room> rooms) {
                if (rooms.isEmpty()) {
                    showToast("Belum ada kamar. Tambah kamar terlebih dahulu.");
                    return;
                }

                // Filter hanya kamar NON-maintenance untuk kelola umum
                List<Room> nonMaintenance = new ArrayList<>();
                for (Room r : rooms) {
                    if (!DatabaseConstants.ROOM_MAINTENANCE.equals(r.getStatus())) {
                        nonMaintenance.add(r);
                    }
                }

                if (nonMaintenance.isEmpty()) {
                    showToast("Semua kamar sedang dalam maintenance. Selesaikan maintenance terlebih dahulu.");
                    return;
                }

                String[] items = new String[nonMaintenance.size()];
                for (int i = 0; i < nonMaintenance.size(); i++) {
                    Room r = nonMaintenance.get(i);
                    String statusLabel = getStatusLabel(r.getStatus());
                    items[i] = r.getRoomName() + " • " + statusLabel + " • " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(r.getPrice());
                }

                new AlertDialog.Builder(OwnerManagementActivity.this)
                        .setTitle("Kelola Kamar")
                        .setItems(items, (dialog, which) -> {
                            Room selected = nonMaintenance.get(which);
                            showRoomDetailOptionsDialog(selected);
                        })
                        .setPositiveButton("Tutup", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat kamar: " + message);
            }
        });
    }

    private void showRoomDetailOptionsDialog(Room room) {
        String[] options = {
                "✏️  Edit Nama Kamar",
                "💰  Ubah Harga Kamar",
                "🔄  Ubah Status Kamar",
                "🔧  Pindahkan ke Maintenance",
                "🗑️  Hapus Kamar"
        };

        new AlertDialog.Builder(this)
                .setTitle(room.getRoomName() + " • " + getStatusLabel(room.getStatus()))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showEditRoomNameDialog(room); break;
                        case 1: showEditRoomPriceDialog(room); break;
                        case 2: showUpdateRoomStatusOnlyDialog(room); break;
                        case 3: showStartMaintenanceDialog(room); break;
                        case 4: showConfirmDeleteRoom(room); break;
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showMaintenanceDialog() {
        String uid = auth.getUid();
        kosRepository.getRoomsByOwner(uid, new KosRepository.RoomListCallback() {
            @Override
            public void onSuccess(List<Room> rooms) {
                if (rooms.isEmpty()) {
                    showToast("Belum ada kamar terdaftar.");
                    return;
                }

                List<Room> maintenanceRooms = new ArrayList<>();
                List<Room> availableRooms = new ArrayList<>();
                List<Room> busyRooms = new ArrayList<>();

                for (Room r : rooms) {
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(r.getStatus())) {
                        maintenanceRooms.add(r);
                    } else if (DatabaseConstants.ROOM_AVAILABLE.equals(r.getStatus())) {
                        availableRooms.add(r);
                    } else {
                        busyRooms.add(r);
                    }
                }

                List<Room> displayList = new ArrayList<>();
                displayList.addAll(maintenanceRooms);
                displayList.addAll(availableRooms);
                displayList.addAll(busyRooms);

                String[] items = new String[displayList.size()];
                for (int i = 0; i < displayList.size(); i++) {
                    Room r = displayList.get(i);
                    String label = getStatusLabel(r.getStatus());
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(r.getStatus())) {
                        items[i] = "🔧 " + r.getRoomName() + " — Sedang Maintenance";
                    } else if (DatabaseConstants.ROOM_AVAILABLE.equals(r.getStatus())) {
                        items[i] = "✅ " + r.getRoomName() + " — Tersedia";
                    } else {
                        items[i] = "🔒 " + r.getRoomName() + " — Sedang ditempati (" + label + ")";
                    }
                }

                new AlertDialog.Builder(OwnerManagementActivity.this)
                        .setTitle("Kelola Maintenance")
                        .setItems(items, (dialog, which) -> {
                            Room selected = displayList.get(which);
                            if (DatabaseConstants.ROOM_MAINTENANCE.equals(selected.getStatus())) {
                                showMaintenanceActionDialog(selected);
                            } else if (DatabaseConstants.ROOM_AVAILABLE.equals(selected.getStatus())) {
                                showStartMaintenanceDialog(selected);
                            } else {
                                showToast("Kamar sedang ditempati/dibooking. Selesaikan atau ubah status kamar terlebih dahulu sebelum maintenance.");
                            }
                        })
                        .setPositiveButton("Tutup", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat kamar: " + message);
            }
        });
    }

    private void showStartMaintenanceDialog(Room room) {
        View view = getLayoutInflater().inflate(android.R.layout.simple_spinner_item, null); // Placeholder
        // Actually it's better to build a custom view programmatically or inflate a simple layout if exists.
        // Since I can't create new XML layouts easily, I'll build it programmatically.

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);

        TextView tvType = new TextView(this);
        tvType.setText("Jenis Masalah:");
        tvType.setPadding(0, 0, 0, 8);
        layout.addView(tvType);

        Spinner spType = new Spinner(this);
        String[] types = {"AC rusak", "Kamar mandi", "Listrik", "Air", "Pintu / kunci", "Plafon / atap", "Internet / WiFi", "Kasur / lemari", "Kebersihan", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapter);
        layout.addView(spType);

        TextView tvNote = new TextView(this);
        tvNote.setText("\nCatatan Kerusakan:");
        tvNote.setPadding(0, 16, 0, 8);
        layout.addView(tvNote);

        EditText etNote = new EditText(this);
        etNote.setHint("Isi catatan kerusakan...");
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle("Mulai Maintenance: " + room.getRoomName())
                .setMessage("Kamar tidak bisa dibooking selama maintenance.")
                .setView(layout)
                .setPositiveButton("Mulai Maintenance", (dialog, which) -> {
                    String type = spType.getSelectedItem().toString();
                    String note = etNote.getText().toString().trim();
                    kosRepository.startRoomMaintenance(room, type, note, new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Maintenance dimulai.");
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal: " + message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showMaintenanceActionDialog(Room room) {
        String[] options = {
                "🔍  Lihat Detail Maintenance",
                "📝  Edit Catatan Maintenance",
                "✅  Selesaikan Maintenance",
                "❌  Batalkan Maintenance"
        };

        new AlertDialog.Builder(this)
                .setTitle("🔧 " + room.getRoomName() + " — Maintenance")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showMaintenanceDetailDialog(room); break;
                        case 1: showEditMaintenanceNoteDialog(room); break;
                        case 2:
                            new AlertDialog.Builder(this)
                                    .setTitle("Selesaikan Maintenance?")
                                    .setMessage("Maintenance kamar ini selesai dan kamar akan tersedia kembali. Lanjutkan?")
                                    .setPositiveButton("Ya, Selesaikan", (d, w) -> {
                                        kosRepository.finishRoomMaintenance(room, new KosRepository.SimpleCallback() {
                                            @Override public void onSuccess() { showToast("Maintenance selesai!"); loadData(); }
                                            @Override public void onError(String message) { showToast("Gagal: " + message); }
                                        });
                                    })
                                    .setNegativeButton("Batal", null).show();
                            break;
                        case 3:
                            new AlertDialog.Builder(this)
                                    .setTitle("Batalkan Maintenance?")
                                    .setMessage("Maintenance akan dibatalkan dan kamar dikembalikan ke status sebelumnya.")
                                    .setPositiveButton("Ya, Batalkan", (d, w) -> {
                                        kosRepository.cancelRoomMaintenance(room, new KosRepository.SimpleCallback() {
                                            @Override public void onSuccess() { showToast("Maintenance dibatalkan."); loadData(); }
                                            @Override public void onError(String message) { showToast("Gagal: " + message); }
                                        });
                                    })
                                    .setNegativeButton("Kembali", null).show();
                            break;
                    }
                })
                .setNegativeButton("Kembali", null)
                .show();
    }

    private void showMaintenanceDetailDialog(Room room) {
        // Fetch latest data from Firestore to be sure
        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(room.getId()).get()
                .addOnSuccessListener(doc -> {
                    Room r = doc.toObject(Room.class);
                    if (r == null) return;
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Nama Kamar: ").append(r.getRoomName()).append("\n");
                    sb.append("Status: ").append(getStatusLabel(r.getStatus())).append("\n");
                    sb.append("Jenis Masalah: ").append(r.getMaintenanceType() != null ? r.getMaintenanceType() : "-").append("\n");
                    sb.append("Catatan: ").append(r.getMaintenanceNote() != null ? r.getMaintenanceNote() : "-").append("\n");
                    
                    if (r.getMaintenanceStartedAt() > 0) {
                        sb.append("Mulai Pada: ").append(com.koshub.psdku.utils.DateHelper.formatDate(r.getMaintenanceStartedAt())).append("\n");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Detail Maintenance")
                            .setMessage(sb.toString())
                            .setPositiveButton("Tutup", null)
                            .setNeutralButton("Edit Catatan", (dialog, which) -> showEditMaintenanceNoteDialog(r))
                            .show();
                });
    }

    private void showEditMaintenanceNoteDialog(Room room) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);

        TextView tvType = new TextView(this);
        tvType.setText("Jenis Masalah:");
        tvType.setPadding(0, 0, 0, 8);
        layout.addView(tvType);

        Spinner spType = new Spinner(this);
        String[] types = {"AC rusak", "Kamar mandi", "Listrik", "Air", "Pintu / kunci", "Plafon / atap", "Internet / WiFi", "Kasur / lemari", "Kebersihan", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapter);
        
        // Pre-select current type
        if (room.getMaintenanceType() != null) {
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(room.getMaintenanceType())) {
                    spType.setSelection(i);
                    break;
                }
            }
        }
        layout.addView(spType);

        TextView tvNote = new TextView(this);
        tvNote.setText("\nCatatan Kerusakan:");
        tvNote.setPadding(0, 16, 0, 8);
        layout.addView(tvNote);

        EditText etNote = new EditText(this);
        etNote.setHint("Isi catatan kerusakan...");
        etNote.setText(room.getMaintenanceNote());
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle("Edit Maintenance: " + room.getRoomName())
                .setView(layout)
                .setPositiveButton("Simpan Perubahan", (dialog, which) -> {
                    String type = spType.getSelectedItem().toString();
                    String note = etNote.getText().toString().trim();
                    kosRepository.updateRoomMaintenanceNote(room.getId(), type, note, new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Catatan maintenance diperbarui.");
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal: " + message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showEditRoomNameDialog(Room room) {
        EditText et = new EditText(this);
        et.setText(room.getRoomName());
        et.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("Edit Nama Kamar")
                .setView(et)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = et.getText().toString().trim();
                    if (newName.isEmpty()) { showToast("Nama tidak boleh kosong"); return; }
                    db.collection(DatabaseConstants.COLLECTION_ROOMS).document(room.getId())
                            .update("roomName", newName, "updatedAt", System.currentTimeMillis())
                            .addOnSuccessListener(a -> { showToast("Nama kamar diperbarui"); loadData(); })
                            .addOnFailureListener(e -> showToast("Gagal: " + e.getMessage()));
                })
                .setNegativeButton("Batal", null).show();
    }

    private void showEditRoomPriceDialog(Room room) {
        EditText et = new EditText(this);
        et.setHint("Harga per bulan (Rp)");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf((long) room.getPrice()));
        et.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("Ubah Harga: " + room.getRoomName())
                .setView(et)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String ps = et.getText().toString().trim();
                    if (ps.isEmpty()) { showToast("Harga tidak boleh kosong"); return; }
                    try {
                        double newPrice = Double.parseDouble(ps);
                        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(room.getId())
                                .update("price", newPrice, "updatedAt", System.currentTimeMillis())
                                .addOnSuccessListener(a -> { showToast("Harga kamar diperbarui"); loadData(); })
                                .addOnFailureListener(e -> showToast("Gagal: " + e.getMessage()));
                    } catch (NumberFormatException e) { showToast("Harga tidak valid"); }
                })
                .setNegativeButton("Batal", null).show();
    }

    private void showUpdateRoomStatusOnlyDialog(Room room) {
        // Hanya status non-maintenance
        String[] statuses = {"Tersedia (available)", "Sudah Dibooking (booked)", "Ditempati (occupied)"};
        String[] statusValues = {DatabaseConstants.ROOM_AVAILABLE, DatabaseConstants.ROOM_BOOKED, DatabaseConstants.ROOM_OCCUPIED};
        new AlertDialog.Builder(this)
                .setTitle("Ubah Status: " + room.getRoomName())
                .setItems(statuses, (dialog, which) -> {
                    kosRepository.updateRoomStatus(room.getId(), statusValues[which], room.getKosId(), new KosRepository.SimpleCallback() {
                        @Override public void onSuccess() { showToast("Status diperbarui ke: " + statuses[which]); loadData(); }
                        @Override public void onError(String message) { showToast("Gagal: " + message); }
                    });
                })
                .setNegativeButton("Batal", null).show();
    }

    private void showConfirmDeleteRoom(Room room) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Kamar")
                .setMessage("Yakin hapus kamar \"" + room.getRoomName() + "\"? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus", (d, w) -> {
                    db.collection(DatabaseConstants.COLLECTION_ROOMS).document(room.getId()).delete()
                            .addOnSuccessListener(a -> { showToast("Kamar berhasil dihapus"); loadData(); })
                            .addOnFailureListener(e -> showToast("Gagal: " + e.getMessage()));
                })
                .setNegativeButton("Batal", null).show();
    }

    private String getStatusLabel(String status) {
        if (status == null) return "Tidak Diketahui";
        switch (status) {
            case DatabaseConstants.ROOM_AVAILABLE: return "Tersedia";
            case DatabaseConstants.ROOM_BOOKED: return "Dibooking";
            case DatabaseConstants.ROOM_OCCUPIED: return "Ditempati";
            case DatabaseConstants.ROOM_MAINTENANCE: return "Maintenance";
            default: return status;
        }
    }

    private void showEditKosDialog(Kos kos) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_kos, null);
        RecyclerView rvPreviews = dialogView.findViewById(R.id.rvImagePreviews);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        EditText etName = dialogView.findViewById(R.id.etKosName);
        EditText etPrice = dialogView.findViewById(R.id.etKosPrice);
        Spinner spCategory = dialogView.findViewById(R.id.spKosCategory);

        selectedImages.clear();
        if (kos.getImageUrls() != null && !kos.getImageUrls().isEmpty()) {
            selectedImages.addAll(kos.getImageUrls());
        } else if (kos.getImageUrl() != null && !kos.getImageUrl().isEmpty()) {
            selectedImages.add(kos.getImageUrl());
        }

        imagePreviewAdapter = new ImagePreviewAdapter(selectedImages, position -> {
            selectedImages.remove(position);
            imagePreviewAdapter.notifyDataSetChanged();
        });
        rvPreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPreviews.setAdapter(imagePreviewAdapter);

        List<String> roomFeatures = new ArrayList<>(kos.getRoomFeatures() != null ? kos.getRoomFeatures() : new ArrayList<>());
        List<String> accessFeatures = new ArrayList<>(kos.getAccessFeatures() != null ? kos.getAccessFeatures() : new ArrayList<>());
        List<String> securityFeatures = new ArrayList<>(kos.getSecurityFeatures() != null ? kos.getSecurityFeatures() : new ArrayList<>());
        List<String> rules = new ArrayList<>(kos.getRules() != null ? kos.getRules() : new ArrayList<>());

        dialogView.findViewById(R.id.btnEditRoomFeatures).setOnClickListener(v ->
                showFeatureSelectionDialog("Fasilitas Kamar", roomOptions, roomFeatures, selected -> {
                    roomFeatures.clear();
                    roomFeatures.addAll(selected);
                }));

        dialogView.findViewById(R.id.btnEditAccessFeatures).setOnClickListener(v ->
                showFeatureSelectionDialog("Akses Lokasi", accessOptions, accessFeatures, selected -> {
                    accessFeatures.clear();
                    accessFeatures.addAll(selected);
                }));

        dialogView.findViewById(R.id.btnEditSecurityFeatures).setOnClickListener(v ->
                showFeatureSelectionDialog("Keamanan", securityOptions, securityFeatures, selected -> {
                    securityFeatures.clear();
                    securityFeatures.addAll(selected);
                }));

        dialogView.findViewById(R.id.btnEditRules).setOnClickListener(v ->
                showFeatureSelectionDialog("Aturan Kos", ruleOptions, rules, selected -> {
                    rules.clear();
                    rules.addAll(selected);
                }));

        // Pre-fill existing data
        etName.setText(kos.getName());

        // Initialize location fields from existing kos
        selectedLatitude = kos.getLatitude();
        selectedLongitude = kos.getLongitude();
        selectedPlaceId = kos.getPlaceId() != null ? kos.getPlaceId() : "";

        etAddressRef = dialogView.findViewById(R.id.etKosAddress);
        etAddressRef.setText(kos.getAddress());
        etAddressRef.setFocusable(false);
        etAddressRef.setFocusableInTouchMode(false);
        etAddressRef.setCursorVisible(false);
        etAddressRef.setOnClickListener(v -> startAutocompletePicker());

        tvAddressHelperRef = dialogView.findViewById(R.id.tvAddressHelper);
        if (!selectedPlaceId.isEmpty()) {
            tvAddressHelperRef.setText("Lokasi Google Maps berhasil dipilih.");
            tvAddressHelperRef.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        }

        etPrice.setText(String.valueOf((long) kos.getPrice()));

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Set category spinner selection
        String[] categories = {"Putra", "Putri", "Campur"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spCategory.setAdapter(catAdapter);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].toLowerCase().equals(kos.getCategory())) {
                spCategory.setSelection(i);
                break;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Kos: " + kos.getName())
                .setView(dialogView)
                .setPositiveButton("Simpan", null)
                .setNeutralButton("Hapus Kos", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                EditText etNameEdit = dialogView.findViewById(R.id.etKosName);
                EditText etPriceEdit = dialogView.findViewById(R.id.etKosPrice);
                Spinner spCategoryEdit = dialogView.findViewById(R.id.spKosCategory);

                String name = etNameEdit.getText().toString().trim();
                String address = etAddressRef.getText().toString().trim();
                String priceStr = etPriceEdit.getText().toString().trim();
                String category = spCategoryEdit.getSelectedItem().toString().toLowerCase();

                if (name.isEmpty() || address.isEmpty() || priceStr.isEmpty()) {
                    showToast("Harap isi semua field wajib");
                    return;
                }

                // Strict validation for Google Places data
                if (selectedLatitude == 0.0 || selectedLongitude == 0.0 || selectedPlaceId.isEmpty()) {
                    showToast("Silakan pilih alamat kos dari Google Maps terlebih dahulu.");
                    return;
                }

                double newPrice;
                try {
                    newPrice = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    showToast("Harga tidak valid");
                    return;
                }

                kos.setName(name);
                kos.setAddress(address);
                kos.setPrice(newPrice);
                kos.setPriceText("Rp " + priceStr);
                kos.setCategory(category);
                kos.setLatitude(selectedLatitude);
                kos.setLongitude(selectedLongitude);
                kos.setPlaceId(selectedPlaceId);
                kos.setRoomFeatures(roomFeatures);
                kos.setAccessFeatures(accessFeatures);
                kos.setSecurityFeatures(securityFeatures);
                kos.setRules(rules);

                // Consolidate features into primary facilities list for card consistency
                List<String> combined = new ArrayList<>();
                if (kos.getFacilities() != null) combined.addAll(kos.getFacilities());
                for (String f : roomFeatures) if (!combined.contains(f)) combined.add(f);
                for (String f : accessFeatures) if (!combined.contains(f)) combined.add(f);
                for (String f : securityFeatures) if (!combined.contains(f)) combined.add(f);
                kos.setFacilities(combined);

                kosRepository.updateKos(kos, new KosRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        uploadMultipleKosImages(kos.getId());
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Gagal: " + message);
                    }
                });
            });

            // Hapus kos dengan konfirmasi
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                showDeleteKosConfirmation(kos);
            });
        });
        dialog.show();
    }

    private void showEditPriceDialog(Kos kos) {
        EditText etNewPrice = new EditText(this);
        etNewPrice.setHint("Harga baru per bulan (Rp)");
        etNewPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etNewPrice.setText(String.valueOf((long) kos.getPrice()));
        etNewPrice.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Atur Harga: " + kos.getName())
                .setView(etNewPrice)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String priceStr = etNewPrice.getText().toString().trim();
                    if (priceStr.isEmpty()) {
                        showToast("Harga tidak boleh kosong");
                        return;
                    }
                    double newPrice;
                    try {
                        newPrice = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        showToast("Harga tidak valid");
                        return;
                    }
                    kos.setPrice(newPrice);
                    kos.setPriceText("Rp " + priceStr);
                    kosRepository.updateKos(kos, new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Harga kos \"" + kos.getName() + "\" berhasil diperbarui");
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal: " + message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showEditFacilitiesDialog() {
        if (ownerKosList == null || ownerKosList.isEmpty()) {
            showToast("Belum ada kos untuk diedit fasilitasnya.");
            return;
        }

        if (ownerKosList.size() == 1) {
            promptFacilitiesInput(ownerKosList.get(0));
        } else {
            String[] names = new String[ownerKosList.size()];
            for (int i = 0; i < ownerKosList.size(); i++) names[i] = ownerKosList.get(i).getName();

            new AlertDialog.Builder(this)
                    .setTitle("Pilih Kos untuk Edit Fasilitas")
                    .setItems(names, (dialog, which) -> promptFacilitiesInput(ownerKosList.get(which)))
                    .show();
        }
    }

    private void promptFacilitiesInput(Kos kos) {
        String[] defaultFacilities = {
                "WiFi", "AC", "Kasur", "Lemari", "Meja Belajar", "Kursi",
                "Kamar Mandi Dalam", "Kamar Mandi Luar", "Dapur", "Parkir Motor",
                "Parkir Mobil", "Listrik", "Air Bersih", "Laundry", "CCTV",
                "Keamanan 24 Jam", "Jemuran", "Kulkas", "Dispenser", "Akses 24 Jam"
        };

        boolean[] checkedItems = new boolean[defaultFacilities.length];
        List<String> currentFacilities = kos.getFacilities() != null ? kos.getFacilities() : new ArrayList<>();

        for (int i = 0; i < defaultFacilities.length; i++) {
            if (currentFacilities.contains(defaultFacilities[i])) {
                checkedItems[i] = true;
            }
        }

        // Custom facilities (not in default list)
        StringBuilder customBuilder = new StringBuilder();
        for (String f : currentFacilities) {
            boolean isDefault = false;
            for (String df : defaultFacilities) {
                if (df.equals(f)) {
                    isDefault = true;
                    break;
                }
            }
            if (!isDefault) {
                if (customBuilder.length() > 0) customBuilder.append(", ");
                customBuilder.append(f);
            }
        }

        EditText etCustom = new EditText(this);
        etCustom.setHint("Fasilitas tambahan, pisahkan dengan koma");
        etCustom.setText(customBuilder.toString());
        etCustom.setPadding(48, 16, 48, 16);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(etCustom);

        new AlertDialog.Builder(this)
                .setTitle("Edit Fasilitas: " + kos.getName())
                .setMultiChoiceItems(defaultFacilities, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setView(layout)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    List<String> newFacilities = new ArrayList<>();
                    for (int i = 0; i < defaultFacilities.length; i++) {
                        if (checkedItems[i]) {
                            newFacilities.add(defaultFacilities[i]);
                        }
                    }

                    String customInput = etCustom.getText().toString().trim();
                    if (!customInput.isEmpty()) {
                        String[] parts = customInput.split(",");
                        for (String p : parts) {
                            String trimmed = p.trim();
                            if (!trimmed.isEmpty() && !newFacilities.contains(trimmed)) {
                                newFacilities.add(trimmed);
                            }
                        }
                    }

                    kosRepository.updateKosFacilities(kos.getId(), newFacilities, new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Fasilitas kos berhasil diperbarui");
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal memperbarui fasilitas: " + message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void setupRoomSection() {
        // Sembunyikan dummy items
        View ri1 = findViewById(R.id.roomItem1);
        View ri2 = findViewById(R.id.roomItem2);
        View ri3 = findViewById(R.id.roomItem3);
        if (ri1 != null) ri1.setVisibility(View.GONE);
        if (ri2 != null) ri2.setVisibility(View.GONE);
        if (ri3 != null) ri3.setVisibility(View.GONE);

        // Setup "Lihat Semua" tombol
        findViewById(R.id.btnSeeAllRooms).setOnClickListener(v -> showRoomManagementDialog());

        // Buat dynamic container untuk room list
        android.widget.LinearLayout sectionRooms = findViewById(R.id.sectionRooms);
        if (sectionRooms == null) return;

        roomListContainer = new android.widget.LinearLayout(this);
        roomListContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        sectionRooms.addView(roomListContainer);

        // Load realtime room data
        loadRoomSection();
    }

    private void loadRoomSection() {
        if (roomListContainer == null) return;
        String uid = auth.getUid();
        kosRepository.getRoomsByOwner(uid, new KosRepository.RoomListCallback() {
            @Override
            public void onSuccess(List<Room> rooms) {
                roomListContainer.removeAllViews();

                if (rooms.isEmpty()) {
                    android.widget.TextView empty = new android.widget.TextView(OwnerManagementActivity.this);
                    empty.setText("Belum ada kamar. Tambah kamar melalui Aksi Cepat.");
                    empty.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                    empty.setTextSize(12);
                    empty.setPadding(0, 16, 0, 8);
                    roomListContainer.addView(empty);
                    return;
                }

                // Show max 4 rooms, prioritize maintenance rooms first
                List<Room> sortedRooms = new ArrayList<>(rooms);
                sortedRooms.sort((a, b) -> {
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(a.getStatus())) return -1;
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(b.getStatus())) return 1;
                    return 0;
                });

                int count = Math.min(sortedRooms.size(), 4);
                for (int i = 0; i < count; i++) {
                    Room room = sortedRooms.get(i);

                    // Divider before each item except first
                    if (i > 0) {
                        View divider = new View(OwnerManagementActivity.this);
                        android.widget.LinearLayout.LayoutParams divParams = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        divParams.topMargin = 10;
                        divParams.bottomMargin = 10;
                        divider.setLayoutParams(divParams);
                        divider.setBackgroundColor(getResources().getColor(R.color.mgmt_divider));
                        roomListContainer.addView(divider);
                    }

                    // Room row
                    android.widget.LinearLayout row = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.topMargin = i == 0 ? 14 : 0;
                    row.setLayoutParams(rowParams);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    row.setPadding(4, 4, 4, 4);
                    row.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_menu_ripple));
                    row.setClickable(true);
                    row.setFocusable(true);

                    // Left text section
                    android.widget.LinearLayout textSection = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    android.widget.LinearLayout.LayoutParams textParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    textSection.setLayoutParams(textParams);
                    textSection.setOrientation(android.widget.LinearLayout.VERTICAL);

                    android.widget.TextView tvName = new android.widget.TextView(OwnerManagementActivity.this);
                    tvName.setText(room.getRoomName());
                    tvName.setTextColor(getResources().getColor(R.color.mgmt_text_primary));
                    tvName.setTextSize(14);
                    tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                    android.widget.TextView tvDetail = new android.widget.TextView(OwnerManagementActivity.this);
                    String detail = com.koshub.psdku.utils.CurrencyHelper.formatRupiah(room.getPrice()) + "/bln";
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(room.getStatus())) {
                        String type = room.getMaintenanceType() != null ? room.getMaintenanceType() : "Dalam Maintenance";
                        detail += " • 🔧 " + type;
                    }
                    tvDetail.setText(detail);
                    tvDetail.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                    tvDetail.setTextSize(11);

                    textSection.addView(tvName);
                    textSection.addView(tvDetail);

                    // Status badge
                    android.widget.TextView tvStatus = new android.widget.TextView(OwnerManagementActivity.this);
                    tvStatus.setText(getStatusLabel(room.getStatus()));
                    tvStatus.setTextSize(10);
                    tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvStatus.setPadding(16, 6, 16, 6);

                    // Set status color
                    switch (room.getStatus() != null ? room.getStatus() : "") {
                        case "available":
                            tvStatus.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_status_empty));
                            tvStatus.setTextColor(getResources().getColor(R.color.mgmt_status_empty));
                            break;
                        case "occupied":
                        case "booked":
                            tvStatus.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_status_occupied));
                            tvStatus.setTextColor(getResources().getColor(R.color.mgmt_status_occupied));
                            break;
                        case "maintenance":
                            tvStatus.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_status_maintenance));
                            tvStatus.setTextColor(getResources().getColor(R.color.mgmt_status_maintenance));
                            break;
                        default:
                            tvStatus.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                            break;
                    }

                    row.addView(textSection);
                    row.addView(tvStatus);

                    // Click = open room detail options or maintenance options
                    final Room finalRoom = room;
                    row.setOnClickListener(v -> {
                        if (DatabaseConstants.ROOM_MAINTENANCE.equals(finalRoom.getStatus())) {
                            showMaintenanceActionDialog(finalRoom);
                        } else {
                            showRoomDetailOptionsDialog(finalRoom);
                        }
                    });

                    roomListContainer.addView(row);
                }

                // Show hint if there are more rooms
                if (rooms.size() > 4) {
                    android.widget.TextView tvMore = new android.widget.TextView(OwnerManagementActivity.this);
                    tvMore.setText("+ " + (rooms.size() - 4) + " kamar lainnya — Lihat Semua");
                    tvMore.setTextColor(getResources().getColor(R.color.brand_green));
                    tvMore.setTextSize(11);
                    tvMore.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvMore.setPadding(0, 12, 0, 4);
                    tvMore.setOnClickListener(v -> showRoomManagementDialog());
                    roomListContainer.addView(tvMore);
                }
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat kamar: " + message);
            }
        });
    }

    private void setupBookingSection() {
        android.widget.LinearLayout sectionBooking = findViewById(R.id.sectionBooking);
        if (sectionBooking == null) return;
        sectionBooking.setVisibility(View.VISIBLE);

        // Sembunyikan dummy booking items
        View dummy = findViewById(R.id.layoutDummyBookingItems);
        if (dummy != null) dummy.setVisibility(View.GONE);

        findViewById(R.id.btnSeeAllBooking).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });

        // Use dynamic container from XML
        bookingListContainer = findViewById(R.id.bookingListContainer);
        if (bookingListContainer == null) {
            // Fallback if ID not found for some reason
            bookingListContainer = new android.widget.LinearLayout(this);
            bookingListContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            sectionBooking.addView(bookingListContainer);
        }

        loadBookingSection();
    }

    private void loadBookingSection() {
        if (bookingListContainer == null) return;
        String uid = auth.getUid();
        bookingRepository.getBookingsByOwner(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                bookingListContainer.removeAllViews();

                List<Booking> pending = new ArrayList<>();
                for (Booking b : bookings) {
                    if ("pending".equalsIgnoreCase(b.getStatus())) pending.add(b);
                }

                if (pending.isEmpty()) {
                    android.widget.TextView empty = new android.widget.TextView(OwnerManagementActivity.this);
                    empty.setText("✅ Tidak ada booking yang perlu dikonfirmasi.");
                    empty.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                    empty.setTextSize(12);
                    empty.setPadding(0, 16, 0, 8);
                    bookingListContainer.addView(empty);
                    return;
                }

                int count = Math.min(pending.size(), 3);
                for (int i = 0; i < count; i++) {
                    final Booking booking = pending.get(i);

                    if (i > 0) {
                        View divider = new View(OwnerManagementActivity.this);
                        android.widget.LinearLayout.LayoutParams dp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        dp.topMargin = 10; dp.bottomMargin = 10;
                        divider.setLayoutParams(dp);
                        divider.setBackgroundColor(getResources().getColor(R.color.mgmt_divider));
                        bookingListContainer.addView(divider);
                    }

                    android.widget.LinearLayout row = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    android.widget.LinearLayout.LayoutParams rp = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    rp.topMargin = i == 0 ? 14 : 0;
                    row.setLayoutParams(rp);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    // Name + detail
                    android.widget.LinearLayout infoCol = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    infoCol.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    infoCol.setOrientation(android.widget.LinearLayout.VERTICAL);

                    android.widget.TextView tvName = new android.widget.TextView(OwnerManagementActivity.this);
                    String name = booking.getStudentName() != null && !booking.getStudentName().trim().isEmpty() 
                            ? booking.getStudentName() : "Mahasiswa";
                    tvName.setText(name);
                    tvName.setTextColor(getResources().getColor(R.color.mgmt_text_primary));
                    tvName.setTextSize(14);
                    tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                    android.widget.TextView tvDetail = new android.widget.TextView(OwnerManagementActivity.this);
                    String detail = (booking.getRoomName() != null ? booking.getRoomName() : "Kamar") + " • " + com.koshub.psdku.utils.DateHelper.formatDate(booking.getCreatedAt());
                    tvDetail.setText(detail);
                    tvDetail.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                    tvDetail.setTextSize(11);

                    infoCol.addView(tvName);
                    infoCol.addView(tvDetail);

                    // Accept / Reject buttons
                    android.widget.LinearLayout btnRow = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

                    android.widget.TextView btnAccept = new android.widget.TextView(OwnerManagementActivity.this);
                    btnAccept.setText("Terima");
                    btnAccept.setTextColor(getResources().getColor(R.color.mgmt_text_white));
                    btnAccept.setTextSize(11);
                    btnAccept.setTypeface(null, android.graphics.Typeface.BOLD);
                    btnAccept.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_btn_accept));
                    btnAccept.setPadding(28, 14, 28, 14);
                    btnAccept.setOnClickListener(v -> {
                        BookingRepository.getInstance().updateBookingStatus(booking.getId(), "accepted", new BookingRepository.SimpleCallback() {
                            @Override public void onSuccess() { showToast("Booking diterima!"); loadData(); loadBookingSection(); }
                            @Override public void onError(String message) { showToast("Gagal: " + message); }
                        });
                    });

                    android.widget.TextView btnReject = new android.widget.TextView(OwnerManagementActivity.this);
                    btnReject.setText("Tolak");
                    btnReject.setTextColor(getResources().getColor(R.color.mgmt_btn_reject_text));
                    btnReject.setTextSize(11);
                    btnReject.setTypeface(null, android.graphics.Typeface.BOLD);
                    btnReject.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_btn_reject));
                    android.widget.LinearLayout.LayoutParams rejectParams = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    rejectParams.leftMargin = 12;
                    btnReject.setLayoutParams(rejectParams);
                    btnReject.setPadding(28, 14, 28, 14);
                    btnReject.setOnClickListener(v -> {
                        new AlertDialog.Builder(OwnerManagementActivity.this)
                                .setTitle("Tolak Booking?")
                                .setMessage("Yakin ingin menolak booking dari " + name + "?")
                                .setPositiveButton("Ya, Tolak", (d, w) -> {
                                    BookingRepository.getInstance().updateBookingStatus(booking.getId(), "rejected", new BookingRepository.SimpleCallback() {
                                        @Override public void onSuccess() { showToast("Booking ditolak."); loadData(); loadBookingSection(); }
                                        @Override public void onError(String message) { showToast("Gagal: " + message); }
                                    });
                                })
                                .setNegativeButton("Batal", null).show();
                    });

                    btnRow.addView(btnAccept);
                    btnRow.addView(btnReject);
                    row.addView(infoCol);
                    row.addView(btnRow);
                    bookingListContainer.addView(row);
                }

                if (pending.size() > 3) {
                    android.widget.TextView tvMore = new android.widget.TextView(OwnerManagementActivity.this);
                    tvMore.setText("+ " + (pending.size() - 3) + " booking lagi — Lihat Semua");
                    tvMore.setTextColor(getResources().getColor(R.color.brand_green));
                    tvMore.setTextSize(11);
                    tvMore.setPadding(0, 12, 0, 4);
                    tvMore.setOnClickListener(v -> startActivity(new Intent(OwnerManagementActivity.this, OwnerBookingActivity.class)));
                    bookingListContainer.addView(tvMore);
                }
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat booking: " + message);
            }
        });
    }

    private void setupTenantSection() {
        android.widget.LinearLayout sectionTenants = findViewById(R.id.sectionTenants);
        if (sectionTenants == null) return;
        sectionTenants.setVisibility(View.VISIBLE);

        // Sembunyikan dummy items
        View t1 = findViewById(R.id.tenantItem1);
        View t2 = findViewById(R.id.tenantItem2);
        if (t1 != null) t1.setVisibility(View.GONE);
        if (t2 != null) t2.setVisibility(View.GONE);

        findViewById(R.id.btnSeeAllTenants).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            intent.putExtra("TAB", "active");
            startActivity(intent);
        });

        tenantListContainer = new android.widget.LinearLayout(this);
        tenantListContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        sectionTenants.addView(tenantListContainer);

        loadTenantSection();
    }

    private void loadTenantSection() {
        if (tenantListContainer == null) return;
        String uid = auth.getUid();
        bookingRepository.getBookingsByOwner(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                tenantListContainer.removeAllViews();

                List<Booking> activeBookings = new ArrayList<>();
                for (Booking b : bookings) {
                    if ("active".equalsIgnoreCase(b.getStatus()) || "waiting_checkin".equalsIgnoreCase(b.getStatus())) {
                        activeBookings.add(b);
                    }
                }

                if (activeBookings.isEmpty()) {
                    android.widget.TextView empty = new android.widget.TextView(OwnerManagementActivity.this);
                    empty.setText("Belum ada penyewa aktif saat ini.");
                    empty.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                    empty.setTextSize(12);
                    empty.setPadding(0, 16, 0, 8);
                    tenantListContainer.addView(empty);
                    return;
                }

                int count = Math.min(activeBookings.size(), 3);
                for (int i = 0; i < count; i++) {
                    Booking b = activeBookings.get(i);

                    if (i > 0) {
                        View divider = new View(OwnerManagementActivity.this);
                        android.widget.LinearLayout.LayoutParams dp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        dp.topMargin = 10; dp.bottomMargin = 10;
                        divider.setLayoutParams(dp);
                        divider.setBackgroundColor(getResources().getColor(R.color.mgmt_divider));
                        tenantListContainer.addView(divider);
                    }

                    android.widget.LinearLayout row = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    android.widget.LinearLayout.LayoutParams rp = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    rp.topMargin = i == 0 ? 14 : 0;
                    row.setLayoutParams(rp);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    android.widget.LinearLayout infoCol = new android.widget.LinearLayout(OwnerManagementActivity.this);
                    infoCol.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    infoCol.setOrientation(android.widget.LinearLayout.VERTICAL);

                    android.widget.TextView tvName = new android.widget.TextView(OwnerManagementActivity.this);
                    String name = b.getStudentName() != null ? b.getStudentName() : "Penyewa";
                    tvName.setText(name);
                    tvName.setTextColor(getResources().getColor(R.color.mgmt_text_primary));
                    tvName.setTextSize(14);
                    tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                    android.widget.TextView tvDetail = new android.widget.TextView(OwnerManagementActivity.this);
                    String detail = (b.getRoomName() != null ? b.getRoomName() : "Kamar") + " • Masuk: " + com.koshub.psdku.utils.DateHelper.formatDate(b.getCheckInDate());
                    tvDetail.setText(detail);
                    tvDetail.setTextColor(getResources().getColor(R.color.mgmt_text_muted));
                    tvDetail.setTextSize(11);

                    infoCol.addView(tvName);
                    infoCol.addView(tvDetail);

                    android.widget.TextView tvStatus = new android.widget.TextView(OwnerManagementActivity.this);
                    tvStatus.setText("waiting_checkin".equalsIgnoreCase(b.getStatus()) ? "Menunggu Check-in" : "Aktif");
                    tvStatus.setTextSize(10);
                    tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvStatus.setBackground(getResources().getDrawable(R.drawable.bg_mgmt_status_occupied));
                    tvStatus.setTextColor(getResources().getColor(R.color.mgmt_status_occupied));
                    tvStatus.setPadding(16, 6, 16, 6);

                    row.addView(infoCol);
                    row.addView(tvStatus);
                    tenantListContainer.addView(row);
                }

                if (activeBookings.size() > 3) {
                    android.widget.TextView tvMore = new android.widget.TextView(OwnerManagementActivity.this);
                    tvMore.setText("+ " + (activeBookings.size() - 3) + " penyewa lagi — Lihat Semua");
                    tvMore.setTextColor(getResources().getColor(R.color.brand_green));
                    tvMore.setTextSize(11);
                    tvMore.setPadding(0, 12, 0, 4);
                    Intent intent = new Intent(OwnerManagementActivity.this, OwnerBookingActivity.class);
                    intent.putExtra("TAB", "active");
                    tvMore.setOnClickListener(v -> startActivity(intent));
                    tenantListContainer.addView(tvMore);
                }
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat penyewa: " + message);
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void cleanOldKosWithBadCoordinates() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("kos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int deletedCount = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Kos kos = doc.toObject(Kos.class);
                        if (kos.getLatitude() == 0.0 && kos.getLongitude() == 0.0) {
                            doc.getReference().delete();
                            deletedCount++;
                        }
                    }
                    if (deletedCount > 0) {
                        int finalDeletedCount = deletedCount;
                        runOnUiThread(() -> {
                            Toast.makeText(this, "✓ Dihapus " + finalDeletedCount + " kos dengan koordinat invalid",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Silently fail to avoid disturbing UX if it's just a background clean
                });
    }

    private void startAutocompletePicker() {
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN,
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )
                .setCountry("ID")
                .setLocationBias(RectangularBounds.newInstance(
                        new LatLng(-7.72, 109.60),
                        new LatLng(-7.64, 109.72)
                ))
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                selectedLatitude = place.getLatLng().latitude;
                selectedLongitude = place.getLatLng().longitude;
                selectedPlaceId = place.getId();
                
                if (etAddressRef != null) {
                    etAddressRef.setText(place.getAddress());
                }

                if (tvAddressHelperRef != null) {
                    tvAddressHelperRef.setText("Lokasi Google Maps berhasil dipilih.");
                    tvAddressHelperRef.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                com.google.android.gms.common.api.Status status = Autocomplete.getStatusFromIntent(data);
                showToast("Error: " + status.getStatusMessage());
            }
        }
    }

    private void showFeatureSelectionDialog(String title, String[] options, List<String> currentSelections, java.util.function.Consumer<List<String>> callback) {
        boolean[] checkedItems = new boolean[options.length];
        for (int i = 0; i < options.length; i++) {
            if (currentSelections.contains(options[i])) {
                checkedItems[i] = true;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Simpan", (dialog, which) -> {
                    List<String> result = new ArrayList<>();
                    for (int i = 0; i < options.length; i++) {
                        if (checkedItems[i]) result.add(options[i]);
                    }
                    callback.accept(result);
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
