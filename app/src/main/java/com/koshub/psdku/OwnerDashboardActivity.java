package com.koshub.psdku;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
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

import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.repositories.FCMTokenRepository;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.utils.NotificationHelper;
import com.koshub.psdku.utils.NotificationPermissionHelper;
import com.bumptech.glide.Glide;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.adapters.RecentBookingAdapter;
import com.koshub.psdku.adapters.RecentPropertyAdapter;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.FinanceSummary;
import com.koshub.psdku.models.Kos;
import com.koshub.psdku.models.OwnerKosStats;
import com.koshub.psdku.models.Promo;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.repositories.StorageRepository;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class OwnerDashboardActivity extends AppCompatActivity {

    private KosRepository kosRepository;
    private CloudinaryRepository cloudinaryRepository;
    private StorageRepository storageRepository;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;
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
    private TextView tvStatValuePendapatan, tvSaldoTersedia, tvSaldoPending;

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
    private RecyclerView rvRecentBookings;

    // Property
    private LinearLayout sectionProperty;
    private TextView btnSeeAllProperty;
    private RecyclerView rvRecentProperty;

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

        // Notification & Permissions
        NotificationHelper.createNotificationChannels(this);
        NotificationPermissionHelper.askNotificationPermission(this);
        FCMTokenRepository.getInstance().saveCurrentToken();
        setupNotificationBadge();
    }

    private void setupNotificationBadge() {
        TextView tvBadge = findViewById(R.id.tvNotifBadge);
        notificationListener = NotificationRepository.getInstance().listenUnreadCount(new NotificationRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                if (count > 0) {
                    tvBadge.setVisibility(View.VISIBLE);
                    tvBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                } else {
                    tvBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("KosHubNotification", "Badge error: " + message);
            }
        });
    }

    private void initViews() {
        // Header
        btnOwnerNotification = findViewById(R.id.btnOwnerTopNotification);

        // Stats
        statTotalKos = findViewById(R.id.statTotalKos);
        statKamarTerisi = findViewById(R.id.statKamarTerisi);
        statBookingMasuk = findViewById(R.id.statBookingMasuk);
        statPendapatan = findViewById(R.id.statPendapatan);
        tvStatValuePendapatan = findViewById(R.id.tvStatValuePendapatan);
        tvSaldoTersedia = findViewById(R.id.tvSaldoTersedia);
        tvSaldoPending = findViewById(R.id.tvSaldoPending);

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
        rvRecentBookings = findViewById(R.id.rvRecentBookings);
        rvRecentBookings.setLayoutManager(new LinearLayoutManager(this));

        // Property
        sectionProperty = findViewById(R.id.sectionProperty);
        btnSeeAllProperty = findViewById(R.id.btnSeeAllProperty);
        rvRecentProperty = findViewById(R.id.rvRecentProperty);
        rvRecentProperty.setLayoutManager(new LinearLayoutManager(this));

        // Revenue
        cardRevenue = findViewById(R.id.cardRevenue);

        // Bottom Nav
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.DASHBOARD);
    }

    private void setupHeader() {
        btnOwnerNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationActivity.class);
            NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
        });
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

        // Load real booking count for "Booking Masuk"
        BookingRepository.getInstance().getBookingsByOwner(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                int pendingCount = 0;
                for (Booking b : bookings) {
                    if (DatabaseConstants.BOOKING_PENDING.equals(b.getStatus())) pendingCount++;
                }
                ((TextView) statBookingMasuk.findViewById(R.id.tvStatValueBooking)).setText(String.valueOf(pendingCount));
                updateRecentBookingsUI(bookings);
            }

            @Override
            public void onError(String message) {
                // Keep dummy
            }
        });

        // Load finance summary
        FinanceRepository.getInstance().getFinanceSummary(uid, new FinanceRepository.FinanceSummaryCallback() {
            @Override
            public void onSuccess(FinanceSummary summary) {
                if (tvStatValuePendapatan != null) {
                    tvStatValuePendapatan.setText(CurrencyHelper.formatRupiah(summary.getTotalIncome()));
                }
                if (tvSaldoTersedia != null) {
                    tvSaldoTersedia.setText(CurrencyHelper.formatRupiah(summary.getAvailableBalance()));
                }
                if (tvSaldoPending != null) {
                    tvSaldoPending.setText(CurrencyHelper.formatRupiah(summary.getPendingBalance()));
                }
            }

            @Override
            public void onError(String message) {
                // Keep dummy or zero
            }
        });

        statTotalKos.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerManagementActivity.class);
            startActivity(intent);
        });
        
        statKamarTerisi.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerManagementActivity.class);
            startActivity(intent);
        });
        
        statBookingMasuk.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });
        
        statPendapatan.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });

        cardSaldoOwner.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerFinanceReportActivity.class);
        });

        btnTarikSaldo.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(OwnerDashboardActivity.this, OwnerWithdrawActivity.class);
        });

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
        actionKelolaPenyewa.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            intent.putExtra("TAB", "active");
            startActivity(intent);
        });
        actionLihatBooking.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });
        actionAturHarga.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerManagementActivity.class);
            startActivity(intent);
        });
        actionBuatPromo.setOnClickListener(v -> showCreatePromoDialog());
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

    private void showCreatePromoDialog() {
        String uid = auth.getUid();
        kosRepository.getKosByOwner(uid, new KosRepository.KosListCallback() {
            @Override
            public void onSuccess(List<Kos> kosList) {
                if (kosList.isEmpty()) {
                    showToast("Harap tambah kos terlebih dahulu");
                    return;
                }

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_buat_promo, null);
                Spinner spKos = dialogView.findViewById(R.id.spinnerKosPromo);
                EditText etName = dialogView.findViewById(R.id.etPromoName);
                EditText etDiscount = dialogView.findViewById(R.id.etPromoDiscount);
                EditText etStart = dialogView.findViewById(R.id.etPromoStartDate);
                EditText etEnd = dialogView.findViewById(R.id.etPromoEndDate);
                EditText etDesc = dialogView.findViewById(R.id.etPromoDescription);

                List<String> kosNames = new ArrayList<>();
                for (Kos k : kosList) kosNames.add(k.getName());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(OwnerDashboardActivity.this, android.R.layout.simple_spinner_dropdown_item, kosNames);
                spKos.setAdapter(adapter);

                Calendar startCal = Calendar.getInstance();
                Calendar endCal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                etStart.setOnClickListener(v -> {
                    new DatePickerDialog(OwnerDashboardActivity.this, (view, year, month, dayOfMonth) -> {
                        startCal.set(year, month, dayOfMonth);
                        etStart.setText(sdf.format(startCal.getTime()));
                    }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
                });

                etEnd.setOnClickListener(v -> {
                    new DatePickerDialog(OwnerDashboardActivity.this, (view, year, month, dayOfMonth) -> {
                        endCal.set(year, month, dayOfMonth);
                        etEnd.setText(sdf.format(endCal.getTime()));
                    }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH)).show();
                });

                AlertDialog dialog = new AlertDialog.Builder(OwnerDashboardActivity.this)
                        .setTitle("Buat Promo")
                        .setView(dialogView)
                        .setPositiveButton("Simpan", null)
                        .setNegativeButton("Batal", (d, w) -> d.dismiss())
                        .create();

                dialog.setOnShowListener(d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String name = etName.getText().toString().trim();
                        String discStr = etDiscount.getText().toString().trim();
                        String startStr = etStart.getText().toString().trim();
                        String endStr = etEnd.getText().toString().trim();

                        if (name.isEmpty() || discStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
                            showToast("Harap isi semua field wajib");
                            return;
                        }

                        int disc = Integer.parseInt(discStr);
                        if (disc < 1 || disc > 99) {
                            showToast("Diskon harus antara 1-99%");
                            return;
                        }

                        Kos selectedKos = kosList.get(spKos.getSelectedItemPosition());
                        Promo promo = new Promo("", selectedKos.getId(), uid, name, disc,
                                startCal.getTimeInMillis(), endCal.getTimeInMillis(),
                                etDesc.getText().toString().trim(), true);

                        kosRepository.createPromo(promo, new KosRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                showToast("Promo berhasil dibuat!");
                                dialog.dismiss();
                                refreshData();
                            }

                            @Override
                            public void onError(String message) {
                                showToast("Gagal membuat promo: " + message);
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

    private void updateRecentBookingsUI(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            rvRecentBookings.setVisibility(View.GONE);
            return;
        }

        rvRecentBookings.setVisibility(View.VISIBLE);
        List<Booking> displayList = bookings.subList(0, Math.min(3, bookings.size()));
        rvRecentBookings.setAdapter(new RecentBookingAdapter(displayList));
    }

    private void setupBookings() {
        btnSeeAllBooking.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerBookingActivity.class);
            startActivity(intent);
        });
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
                    rvRecentProperty.setVisibility(View.GONE);
                } else {
                    rvRecentProperty.setVisibility(View.VISIBLE);
                    List<Kos> displayList = kosList.subList(0, Math.min(2, kosList.size()));
                    rvRecentProperty.setAdapter(new RecentPropertyAdapter(displayList));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
