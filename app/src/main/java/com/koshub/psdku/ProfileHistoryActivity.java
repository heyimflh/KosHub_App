package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.net.Uri;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import com.bumptech.glide.Glide;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.AuthRepository;
import com.koshub.psdku.repositories.FavoriteRepository;
import com.koshub.psdku.repositories.ReviewRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.services.FirebaseService;

public class ProfileHistoryActivity extends AppCompatActivity {

    private CloudinaryRepository cloudinaryRepository;
    private View btnEditProfile;
    private ImageView imgProfile;

    private final ActivityResultLauncher<String> profilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (imgProfile != null) Glide.with(this).load(uri).circleCrop().into(imgProfile);
                    uploadProfileImage(uri);
                }
            }
    );

    private ProgressBar progressProfileCompletion;
    private TextView tvCompletionPercent;
    private TextView btnCompleteProfile;

    private LinearLayout statBooking;
    private LinearLayout statFavorite;
    private LinearLayout statReview;
    private LinearLayout statTransaction;

    private View menuPersonal;
    private View menuHistory;
    private View menuWishlist;
    private View menuPayment;
    private View menuDocument;
    private View menuHelp;
    private View menuSettings;

    private LinearLayout sectionHistory;
    private LinearLayout emptyStateHistory;
    private TextView btnSeeAllHistory;
    private View historyItem1;
    private View historyItem2;
    private View historyItem3;
    private TextView tvHistoryStatus1, tvCheckInDate1;
    private android.widget.Button btnAmbilKunci, btnLaporkanKomplain;
    private LinearLayout layoutTenantActions;

    private LinearLayout btnLogout;
    private TextView btnEmptySearch;

    private boolean hasBookingHistory = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_history);

        cloudinaryRepository = CloudinaryRepository.getInstance();
        initViews();
        loadProfileData();
        setupProfileCompletion();
        setupQuickStats();
        setupMenuListeners();
        setupHistorySection();
        setupLogout();
        setupBottomNav();
    }

    private void initViews() {
        btnEditProfile = findViewById(R.id.btnEditProfile);
        imgProfile = findViewById(R.id.imgProfileAvatar);

        progressProfileCompletion = findViewById(R.id.progressProfileCompletion);
        tvCompletionPercent = findViewById(R.id.tvCompletionPercent);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);

        statBooking = findViewById(R.id.statBooking);
        statFavorite = findViewById(R.id.statFavorite);
        statReview = findViewById(R.id.statReview);
        statTransaction = findViewById(R.id.statTransaction);

        menuPersonal = findViewById(R.id.menuPersonal);
        menuHistory = findViewById(R.id.menuHistory);
        menuWishlist = findViewById(R.id.menuWishlist);
        menuPayment = findViewById(R.id.menuPayment);
        menuDocument = findViewById(R.id.menuDocument);
        menuHelp = findViewById(R.id.menuHelp);
        menuSettings = findViewById(R.id.menuSettings);

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

        btnLogout = findViewById(R.id.btnLogout);
        btnEmptySearch = findViewById(R.id.btnEmptySearch);
    }

    private void setupProfileCompletion() {
        progressProfileCompletion.setProgress(85);
        btnEditProfile.setOnClickListener(v -> profilePickerLauncher.launch("image/*"));
        btnCompleteProfile.setOnClickListener(v ->
                showToast("📋 Lengkapi data profil untuk verifikasi"));
    }

    private void setupQuickStats() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            BookingRepository.getInstance().getBookingsByStudent(uid, new BookingRepository.BookingListCallback() {
                @Override
                public void onSuccess(List<Booking> bookings) {
                    TextView tvBookingValue = findViewById(R.id.tvStatBookingValue);
                    if (tvBookingValue != null) tvBookingValue.setText(String.valueOf(bookings.size()));
                    updateRentalHistoryUI(bookings);
                }

                @Override
                public void onError(String message) {
                    showToast("Gagal memuat statistik booking");
                }
            });

            FavoriteRepository.getInstance().getFavoritesByUser(uid, new FavoriteRepository.FavoriteListCallback() {
                @Override
                public void onSuccess(List<com.koshub.psdku.models.Favorite> favorites) {
                    TextView tvFavoriteValue = findViewById(R.id.tvStatFavoriteValue);
                    if (tvFavoriteValue != null) tvFavoriteValue.setText(String.valueOf(favorites.size()));
                }

                @Override
                public void onError(String message) {
                }
            });

            ReviewRepository.getInstance().getReviewsByStudent(uid, new ReviewRepository.ReviewListCallback() {
                @Override
                public void onSuccess(List<com.koshub.psdku.models.Review> reviews) {
                    TextView tvReviewValue = findViewById(R.id.tvStatReviewValue);
                    if (tvReviewValue != null) {
                        tvReviewValue.setText(String.valueOf(reviews.size()));
                    }
                }

                @Override
                public void onError(String message) {
                }
            });
        }
    }

    private void setupMenuListeners() {
        View.OnClickListener comingSoon = v -> showToast("Fitur ini akan segera hadir");
        menuPersonal.setOnClickListener(comingSoon);
        menuHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, WaitingListQueueActivity.class);
            startActivity(intent);
        });
        menuWishlist.setOnClickListener(comingSoon);
        menuPayment.setOnClickListener(comingSoon);
        menuDocument.setOnClickListener(comingSoon);
        menuHelp.setOnClickListener(comingSoon);
        menuSettings.setOnClickListener(comingSoon);
    }

    private void setupHistorySection() {
        if (hasBookingHistory) {
            sectionHistory.setVisibility(View.VISIBLE);
            emptyStateHistory.setVisibility(View.GONE);

            btnSeeAllHistory.setOnClickListener(v -> {
                Intent intent = new Intent(this, WaitingListQueueActivity.class);
                startActivity(intent);
            });

            if (historyItem1 != null) historyItem1.setVisibility(View.VISIBLE);
            if (historyItem2 != null) historyItem2.setVisibility(View.GONE);
            if (historyItem3 != null) historyItem3.setVisibility(View.GONE);
            
            View div1 = findViewById(R.id.dividerHistory1);
            View div2 = findViewById(R.id.dividerHistory2);
            if (div1 != null) div1.setVisibility(View.GONE);
            if (div2 != null) div2.setVisibility(View.GONE);
        } else {
            sectionHistory.setVisibility(View.GONE);
            emptyStateHistory.setVisibility(View.VISIBLE);

            btnEmptySearch.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }
    }

    private void setupLogout() {
        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Yakin ingin keluar dari akun?")
                    .setPositiveButton("Ya", (dialog, which) -> {
                        AuthRepository.getInstance().logout(this, new AuthRepository.AuthCallback<Void>() {
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

    private void uploadProfileImage(Uri uri) {
        showToast("Sedang mengupdate foto profil...");
        cloudinaryRepository.uploadProfileImage(this, uri, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                showToast("Foto profil diperbarui");
                String optimizedUrl = cloudinaryRepository.getOptimizedUrl(downloadUrl, 200, 200, true);
                Glide.with(ProfileHistoryActivity.this)
                        .load(optimizedUrl)
                        .placeholder(R.drawable.bg_avatar_circle)
                        .circleCrop()
                        .into(imgProfile);
            }

            @Override
            public void onError(String message) {
                showToast("Gagal upload: " + message);
            }
        });
    }

    private void loadProfileData() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString(DatabaseConstants.FIELD_NAME);
                        String email = documentSnapshot.getString(DatabaseConstants.FIELD_EMAIL);
                        String photo = documentSnapshot.getString(DatabaseConstants.FIELD_PROFILE_IMAGE_URL);

                        if (name != null) ((TextView)findViewById(R.id.tvProfileName)).setText(name);
                        if (email != null) ((TextView)findViewById(R.id.tvProfileEmail)).setText(email);
                        if (photo != null && !photo.isEmpty()) {
                            String optimizedUrl = cloudinaryRepository.getOptimizedUrl(photo, 200, 200, true);
                            Glide.with(this).load(optimizedUrl).placeholder(R.drawable.bg_avatar_circle).circleCrop().into(imgProfile);
                        } else {
                            Glide.with(this).load(R.drawable.bg_avatar_circle).circleCrop().into(imgProfile);
                        }
                    });
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

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

    private void updateRentalHistoryUI(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            toggleEmptyState(true);
            return;
        }
        
        toggleEmptyState(false);
        Booking latest = bookings.get(0);
        
        TextView tvTitle1 = findViewById(R.id.tvHistoryTitle1);
        TextView tvSub1 = findViewById(R.id.tvHistorySub1);
        if (tvTitle1 != null) tvTitle1.setText(latest.getKosName());
        if (tvSub1 != null) tvSub1.setText(latest.getKosAddress());
        
        tvHistoryStatus1.setText(latest.getStatus().toUpperCase());
        
        if (DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(latest.getStatus())) {
            layoutTenantActions.setVisibility(View.VISIBLE);
            btnAmbilKunci.setVisibility(View.VISIBLE);
            btnLaporkanKomplain.setVisibility(View.GONE);
            btnAmbilKunci.setOnClickListener(v -> showAmbilKunciDialog(latest));
        } else if (DatabaseConstants.BOOKING_ACTIVE.equals(latest.getStatus())) {
            layoutTenantActions.setVisibility(View.VISIBLE);
            btnAmbilKunci.setVisibility(View.GONE);
            btnLaporkanKomplain.setVisibility(View.VISIBLE);
            btnLaporkanKomplain.setText("Komplain");
            btnLaporkanKomplain.setOnClickListener(v -> {
                Intent intent = new Intent(this, TenantComplaintFormActivity.class);
                intent.putExtra("bookingId", latest.getId());
                NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
            });
        } else if (DatabaseConstants.BOOKING_COMPLETED.equals(latest.getStatus())) {
            layoutTenantActions.setVisibility(View.VISIBLE);
            btnAmbilKunci.setVisibility(View.GONE);
            btnLaporkanKomplain.setVisibility(View.VISIBLE);
            btnLaporkanKomplain.setText("Beri Review");
            btnLaporkanKomplain.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReviewFormActivity.class);
                intent.putExtra("BOOKING_ID", latest.getId());
                NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
            });
        } else {
            layoutTenantActions.setVisibility(View.GONE);
        }
    }

    private void showAmbilKunciDialog(Booking b) {
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Konfirmasi Ambil Kunci?")
                .setMessage("Pastikan kamu benar-benar sudah menerima kunci kos dari pemilik.")
                .setPositiveButton("Ya, Sudah Ambil Kunci", (dialog, which) -> {
                    BookingRepository.getInstance().markKeyTaken(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Status sewa berhasil diaktifkan");
                            loadProfileData();
                            setupQuickStats();
                        }

                        @Override
                        public void onError(String message) {
                            showToast(message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
