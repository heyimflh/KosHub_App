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
import com.google.firebase.firestore.FirebaseFirestore;
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

    private com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
    private KosRepository kosRepository;
    private CloudinaryRepository cloudinaryRepository;
    private BookingRepository bookingRepository;
    private FirebaseAuth auth;
    private List<Kos> ownerKosList;
    private Uri selectedImageUri;
    private ImageView imgPreview;

    private android.widget.LinearLayout roomListContainer;
    private android.widget.LinearLayout bookingListContainer;
    private android.widget.LinearLayout tenantListContainer;

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
        findViewById(R.id.actionMgmtKelolaKamar).setOnClickListener(v -> showRoomManagementDialog());
        findViewById(R.id.actionMgmtKelolaPenyewa).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            intent.putExtra("TAB", "active");
            startActivity(intent);
        });

        findViewById(R.id.actionMgmtFasilitas).setOnClickListener(v -> {
            if (ownerKosList == null || ownerKosList.isEmpty()) {
                showToast("Belum ada kos");
                return;
            }
            String[] names = new String[ownerKosList.size()];
            for (int i = 0; i < ownerKosList.size(); i++) names[i] = ownerKosList.get(i).getName();
            new AlertDialog.Builder(this)
                    .setTitle("Pilih Kos untuk Diedit")
                    .setItems(names, (dialog, which) -> showEditKosDialog(ownerKosList.get(which)))
                    .show();
        });

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
                        case 3: showConfirmMoveToMaintenance(room); break;
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

                // Pisahkan kamar maintenance dan non-maintenance
                List<Room> maintenanceRooms = new ArrayList<>();
                List<Room> availableRooms = new ArrayList<>();
                for (Room r : rooms) {
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(r.getStatus())) {
                        maintenanceRooms.add(r);
                    } else if (DatabaseConstants.ROOM_AVAILABLE.equals(r.getStatus())) {
                        availableRooms.add(r);
                    }
                }

                // Build display list: maintenance rooms first, then available ones
                List<Room> displayList = new ArrayList<>();
                displayList.addAll(maintenanceRooms);
                displayList.addAll(availableRooms);

                if (displayList.isEmpty()) {
                    showToast("Tidak ada kamar yang bisa dikelola maintenancenya.");
                    return;
                }

                String[] items = new String[displayList.size()];
                for (int i = 0; i < displayList.size(); i++) {
                    Room r = displayList.get(i);
                    if (DatabaseConstants.ROOM_MAINTENANCE.equals(r.getStatus())) {
                        items[i] = "🔧 " + r.getRoomName() + " [Sedang Maintenance]";
                    } else {
                        items[i] = "✅ " + r.getRoomName() + " [" + getStatusLabel(r.getStatus()) + "]";
                    }
                }

                new AlertDialog.Builder(OwnerManagementActivity.this)
                        .setTitle("Kelola Maintenance")
                        .setMessage("Kamar 🔧 = sedang maintenance. Pilih kamar untuk dikelola.")
                        .setItems(items, (dialog, which) -> {
                            Room selected = displayList.get(which);
                            if (DatabaseConstants.ROOM_MAINTENANCE.equals(selected.getStatus())) {
                                showMaintenanceActionDialog(selected);
                            } else {
                                showConfirmMoveToMaintenance(selected);
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

    private void showMaintenanceActionDialog(Room room) {
        String[] options = {
                "✅  Selesaikan Maintenance (ubah ke Tersedia)",
                "📝  Lihat / Edit Catatan Maintenance",
                "❌  Batalkan Maintenance"
        };

        new AlertDialog.Builder(this)
                .setTitle("🔧 " + room.getRoomName() + " — Maintenance")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Selesaikan maintenance, set ke available
                            new AlertDialog.Builder(this)
                                    .setTitle("Selesaikan Maintenance?")
                                    .setMessage("Kamar \"" + room.getRoomName() + "\" akan diubah menjadi Tersedia (available). Lanjutkan?")
                                    .setPositiveButton("Ya, Selesaikan", (d, w) -> {
                                        kosRepository.updateRoomStatus(room.getId(), DatabaseConstants.ROOM_AVAILABLE, room.getKosId(), new KosRepository.SimpleCallback() {
                                            @Override public void onSuccess() { showToast("Maintenance selesai! Kamar kini tersedia."); loadData(); }
                                            @Override public void onError(String message) { showToast("Gagal: " + message); }
                                        });
                                    })
                                    .setNegativeButton("Batal", null).show();
                            break;
                        case 1:
                            showEditMaintenanceNoteDialog(room);
                            break;
                        case 2:
                            // Batalkan maintenance
                            new AlertDialog.Builder(this)
                                    .setTitle("Batalkan Maintenance?")
                                    .setMessage("Status kamar akan dikembalikan ke Tersedia.")
                                    .setPositiveButton("Ya, Batalkan", (d, w) -> {
                                        kosRepository.updateRoomStatus(room.getId(), DatabaseConstants.ROOM_AVAILABLE, room.getKosId(), new KosRepository.SimpleCallback() {
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

    private void showConfirmMoveToMaintenance(Room room) {
        EditText etNote = new EditText(this);
        etNote.setHint("Catatan kerusakan (opsional, contoh: AC rusak, perlu perbaikan)");
        etNote.setPadding(48, 32, 48, 32);
        etNote.setMaxLines(3);

        new AlertDialog.Builder(this)
                .setTitle("Pindahkan ke Maintenance?")
                .setMessage("Kamar \"" + room.getRoomName() + "\" akan diubah statusnya menjadi Maintenance dan tidak bisa dibooking.")
                .setView(etNote)
                .setPositiveButton("Konfirmasi", (dialog, which) -> {
                    String note = etNote.getText().toString().trim();
                    kosRepository.updateRoomStatus(room.getId(), DatabaseConstants.ROOM_MAINTENANCE, room.getKosId(), new KosRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            // Simpan catatan maintenance ke Firestore jika ada
                            if (!note.isEmpty()) {
                                db.collection("rooms").document(room.getId())
                                        .update("maintenanceNote", note, "updatedAt", System.currentTimeMillis());
                            }
                            showToast("Kamar dipindahkan ke Maintenance.");
                            loadData();
                        }
                        @Override public void onError(String message) { showToast("Gagal: " + message); }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showEditMaintenanceNoteDialog(Room room) {
        // Load current note from Firestore first
        db.collection("rooms").document(room.getId()).get()
                .addOnSuccessListener(doc -> {
                    String currentNote = doc.getString("maintenanceNote");

                    EditText etNote = new EditText(this);
                    etNote.setHint("Catatan kerusakan");
                    etNote.setPadding(48, 32, 48, 32);
                    etNote.setMaxLines(4);
                    if (currentNote != null) etNote.setText(currentNote);

                    new AlertDialog.Builder(this)
                            .setTitle("Catatan Maintenance: " + room.getRoomName())
                            .setView(etNote)
                            .setPositiveButton("Simpan", (dialog, which) -> {
                                String note = etNote.getText().toString().trim();
                                db.collection("rooms").document(room.getId())
                                        .update("maintenanceNote", note, "updatedAt", System.currentTimeMillis())
                                        .addOnSuccessListener(a -> showToast("Catatan disimpan."))
                                        .addOnFailureListener(e -> showToast("Gagal: " + e.getMessage()));
                            })
                            .setNegativeButton("Batal", null)
                            .show();
                });
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
                    db.collection("rooms").document(room.getId())
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
                        db.collection("rooms").document(room.getId())
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
                    db.collection("rooms").document(room.getId()).delete()
                            .addOnSuccessListener(a -> { showToast("Kamar berhasil dihapus"); loadData(); })
                            .addOnFailureListener(e -> showToast("Gagal: " + e.getMessage()));
                })
                .setNegativeButton("Batal", null).show();
    }

    private String getStatusLabel(String status) {
        if (status == null) return "Tidak Diketahui";
        switch (status) {
            case "available": return "Tersedia";
            case "booked": return "Dibooking";
            case "occupied": return "Ditempati";
            case "maintenance": return "Maintenance";
            default: return status;
        }
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
                            new AlertDialog.Builder(OwnerManagementActivity.this)
                                    .setTitle(rooms.get(which).getRoomName())
                                    .setItems(new String[]{"Update Status", "Hapus Kamar"}, (d2, action) -> {
                                        if (action == 0) {
                                            showUpdateRoomStatusDialog(rooms.get(which));
                                        } else {
                                            new AlertDialog.Builder(OwnerManagementActivity.this)
                                                    .setTitle("Hapus Kamar")
                                                    .setMessage("Yakin hapus kamar \"" + rooms.get(which).getRoomName() + "\"?")
                                                    .setPositiveButton("Hapus", (d3, w3) -> {
                                                        Room r = rooms.get(which);
                                                        db.collection("rooms").document(r.getId()).delete()
                                                                .addOnSuccessListener(a -> {
                                                                    showToast("Kamar dihapus");
                                                                    loadData();
                                                                })
                                                                .addOnFailureListener(e -> showToast("Gagal: " + e.getMessage()));
                                                    })
                                                    .setNegativeButton("Batal", null).show();
                                        }
                                    }).show();
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

    private void showEditKosDialog(Kos kos) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_kos, null);
        imgPreview = dialogView.findViewById(R.id.imgKosPreview);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        EditText etName = dialogView.findViewById(R.id.etKosName);
        EditText etAddress = dialogView.findViewById(R.id.etKosAddress);
        EditText etPrice = dialogView.findViewById(R.id.etKosPrice);
        Spinner spCategory = dialogView.findViewById(R.id.spKosCategory);

        // Pre-fill existing data
        etName.setText(kos.getName());
        etAddress.setText(kos.getAddress());
        etPrice.setText(String.valueOf((long) kos.getPrice()));

        // Load existing image preview
        if (kos.getImageUrls() != null && !kos.getImageUrls().isEmpty()) {
            Glide.with(this).load(kos.getImageUrls().get(0)).into(imgPreview);
        }

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
                String name = etName.getText().toString().trim();
                String address = etAddress.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String category = spCategory.getSelectedItem().toString().toLowerCase();

                if (name.isEmpty() || address.isEmpty() || priceStr.isEmpty()) {
                    showToast("Harap isi semua field wajib");
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

                kosRepository.updateKos(kos, new KosRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (selectedImageUri != null) {
                            showToast("Menyimpan foto...");
                            cloudinaryRepository.uploadKosImage(OwnerManagementActivity.this, selectedImageUri, kos.getId(), new CloudinaryRepository.SimpleUploadCallback() {
                                @Override
                                public void onSuccess(String imageUrl) {
                                    runOnUiThread(() -> {
                                        showToast("Kos & foto berhasil diperbarui");
                                        selectedImageUri = null;
                                        loadData();
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    runOnUiThread(() -> {
                                        showToast("Data disimpan, tapi update foto gagal");
                                        selectedImageUri = null;
                                        loadData();
                                    });
                                }
                            });
                        } else {
                            showToast("Kos berhasil diperbarui");
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

            // Hapus kos dengan konfirmasi
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(OwnerManagementActivity.this)
                        .setTitle("Hapus Kos")
                        .setMessage("Yakin ingin menghapus kos \"" + kos.getName() + "\"? Semua kamar di kos ini juga akan dihapus. Tindakan ini tidak dapat dibatalkan.")
                        .setPositiveButton("Hapus", (confirmDialog, which) -> {
                            kosRepository.deleteKos(kos.getId(), new KosRepository.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    showToast("Kos berhasil dihapus");
                                    dialog.dismiss();
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
                        detail += " • ⚠️ Dalam Maintenance";
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

                    // Click = open room detail options
                    final Room finalRoom = room;
                    row.setOnClickListener(v -> showRoomDetailOptionsDialog(finalRoom));

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

        // Sembunyikan dummy booking items (tidak ada ID spesifik, hapus seluruh isi dan rebuild)
        // Cari tombol lihat semua
        findViewById(R.id.btnSeeAllBooking).setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });

        // Tambah dynamic container
        bookingListContainer = new android.widget.LinearLayout(this);
        bookingListContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        sectionBooking.addView(bookingListContainer);

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
                    String name = booking.getStudentName() != null ? booking.getStudentName() : "Penyewa";
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
}
