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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Kos;
import com.koshub.psdku.models.OwnerKosStats;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OwnerManagementActivity extends AppCompatActivity {

    private KosRepository kosRepository;
    private CloudinaryRepository cloudinaryRepository;
    private BookingRepository bookingRepository;
    private FirebaseAuth auth;
    private List<Kos> ownerKosList;
    private Uri selectedImageUri;
    private ImageView imgPreview;

    private int pendingBookingsCount = 0;
    private int maintenanceRoomsCount = 0;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (imgPreview != null) Glide.with(this).load(uri).into(imgPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_management);

        kosRepository = KosRepository.getInstance();
        cloudinaryRepository = CloudinaryRepository.getInstance();
        bookingRepository = BookingRepository.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        loadData();
        setupPropertySelector();
        setupQuickActions();
        setupRoomSection();
        setupBookingSection();
        setupTenantSection();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.KOS);
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
                if (kosList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    propertyCard.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    propertyCard.setVisibility(View.VISIBLE);
                    updateKosUI(kosList);
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
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat booking: " + message);
            }
        });
    }

    private void updateKosUI(List<Kos> kosList) {
        // Find existing list container or individual items if they have IDs
        // For Phase 4, we'll try to find common IDs from activity_owner_management.xml
        TextView tvCurrentKos = findViewById(R.id.tvCurrentKosName);
        if (tvCurrentKos != null && !kosList.isEmpty()) {
            tvCurrentKos.setText(kosList.get(0).getName());
        }
        
        // TODO: Populate kos cards if IDs are available, or use a RecyclerView in next step
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
            String[] names = new String[ownerKosList.size()];
            for (int i = 0; i < ownerKosList.size(); i++) names[i] = ownerKosList.get(i).getName();
            
            new AlertDialog.Builder(this)
                    .setTitle("Pilih Kos")
                    .setItems(names, (dialog, which) -> {
                        Kos selected = ownerKosList.get(which);
                        ((TextView) findViewById(R.id.tvCurrentKosName)).setText(selected.getName());
                        showToast("Beralih ke " + selected.getName());
                    })
                    .show();
        });
    }

    private void setupQuickActions() {
        findViewById(R.id.actionMgmtTambahKos).setOnClickListener(v -> showAddKosDialog());
        findViewById(R.id.actionMgmtTambahKamar).setOnClickListener(v -> showAddRoomDialog());
        findViewById(R.id.actionMgmtKelolaKamar).setOnClickListener(v -> showManageRoomsDialog());
        findViewById(R.id.actionMgmtKelolaPenyewa).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            intent.putExtra("TAB", "active");
            startActivity(intent);
        });
        findViewById(R.id.actionMgmtFasilitas).setOnClickListener(v -> showEditFacilitiesDialog());
        findViewById(R.id.actionMgmtMaintenance).setOnClickListener(v -> showManageRoomsDialog());
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

    private void uploadKosImage(String kosId) {
        showToast("Sedang mengupload foto kos...");
        cloudinaryRepository.uploadKosImage(this, selectedImageUri, kosId, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    showToast("Kos & Foto berhasil disimpan");
                    selectedImageUri = null;
                    loadData();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showToast("Kos disimpan, tapi upload foto gagal: " + message);
                    selectedImageUri = null;
                    loadData();
                });
            }
        });
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

    private void showManageRoomsDialog() {
        String uid = auth.getUid();
        kosRepository.getRoomsByOwner(uid, new KosRepository.RoomListCallback() {
            @Override
            public void onSuccess(List<Room> rooms) {
                if (rooms.isEmpty()) {
                    showToast("Belum ada kamar terdaftar");
                    return;
                }

                String[] roomNames = new String[rooms.size()];
                for (int i = 0; i < rooms.size(); i++) {
                    roomNames[i] = rooms.get(i).getRoomName() + " - " + rooms.get(i).getStatus();
                }

                new AlertDialog.Builder(OwnerManagementActivity.this)
                        .setTitle("Daftar Kamar")
                        .setItems(roomNames, (dialog, which) -> {
                            Room selectedRoom = rooms.get(which);
                            showUpdateRoomStatusDialog(selectedRoom);
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

    private void showUpdateRoomStatusDialog(Room room) {
        String[] statuses = {DatabaseConstants.ROOM_AVAILABLE, DatabaseConstants.ROOM_BOOKED, DatabaseConstants.ROOM_OCCUPIED, DatabaseConstants.ROOM_MAINTENANCE};
        new AlertDialog.Builder(this)
                .setTitle("Update Status Kamar: " + room.getRoomName())
                .setItems(statuses, (dialog, which) -> {
                    String newStatus = statuses[which];
                    kosRepository.updateRoomStatus(room.getId(), newStatus, room.getKosId(), new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Status kamar diperbarui");
                            loadData();
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal: " + message);
                        }
                    });
                })
                .show();
    }

    private void showEditFacilitiesDialog() {
        if (ownerKosList == null || ownerKosList.isEmpty()) {
            showToast("Belum ada kos untuk diedit");
            return;
        }

        String[] kosNames = new String[ownerKosList.size()];
        for (int i = 0; i < ownerKosList.size(); i++) kosNames[i] = ownerKosList.get(i).getName();

        new AlertDialog.Builder(this)
                .setTitle("Pilih Kos untuk Edit Fasilitas")
                .setItems(kosNames, (dialog, which) -> {
                    Kos selectedKos = ownerKosList.get(which);
                    promptFacilitiesInput(selectedKos);
                })
                .show();
    }

    private void promptFacilitiesInput(Kos kos) {
        EditText etFacilities = new EditText(this);
        etFacilities.setHint("Fasilitas (pisahkan dengan koma)");
        StringBuilder current = new StringBuilder();
        if (kos.getFacilities() != null) {
            for (int i = 0; i < kos.getFacilities().size(); i++) {
                current.append(kos.getFacilities().get(i));
                if (i < kos.getFacilities().size() - 1) current.append(", ");
            }
        }
        etFacilities.setText(current.toString());

        new AlertDialog.Builder(this)
                .setTitle("Edit Fasilitas: " + kos.getName())
                .setView(etFacilities)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String input = etFacilities.getText().toString().trim();
                    List<String> facilities = new ArrayList<>();
                    if (!input.isEmpty()) {
                        String[] parts = input.split(",");
                        for (String p : parts) facilities.add(p.trim());
                    }
                    kos.setFacilities(facilities);
                    kosRepository.updateKos(kos, new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Fasilitas diperbarui");
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

    private void setupRoomSection() {
        findViewById(R.id.btnSeeAllRooms).setOnClickListener(v -> showManageRoomsDialog());
        // Hide dummy items to avoid confusion with real stats
        findViewById(R.id.roomItem1).setVisibility(View.GONE);
        findViewById(R.id.roomItem2).setVisibility(View.GONE);
        findViewById(R.id.roomItem3).setVisibility(View.GONE);
    }

    private void setupBookingSection() {
        findViewById(R.id.btnSeeAllBooking).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });
        // Hide dummy items
        findViewById(R.id.sectionBooking).setVisibility(View.GONE);
    }

    private void setupTenantSection() {
        findViewById(R.id.btnSeeAllTenants).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            intent.putExtra("TAB", "active");
            startActivity(intent);
        });
        // Hide dummy items
        findViewById(R.id.sectionTenants).setVisibility(View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
