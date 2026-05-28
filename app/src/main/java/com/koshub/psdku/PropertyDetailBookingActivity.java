package com.koshub.psdku;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Chat;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.ChatRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.FavoriteRepository;
import com.koshub.psdku.repositories.ReviewRepository;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

public class PropertyDetailBookingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnFavorite;
    private ImageButton btnShare;
    private ImageButton btnChat;
    private TextView btnWaitlistBottom;
    private TextView btnBookingBottom;
    private ImageView imgHero;
    
    private TextView tvDetailTitle, tvDetailLocation, tvDetailDescription;
    private TextView tvDetailPriceValue, tvDetailBadgeCategory, tvDetailBadgeSisa;
    private TextView tvDetailRouteDistance, tvDetailRouteTime;
    private TextView tvDetailRating, tvDetailRatingCount;
    private ChipGroup amenityChipGroup;
    private LinearLayout reviewContainer;
    private View layoutReviewPrompt;
    private MapView mapView;

    private KosItem currentItem;
    private boolean isFavorited = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_property_detail_booking);

        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentItem = (KosItem) intent.getSerializableExtra("kos_item");
        
        if (currentItem == null) {
            Toast.makeText(this, "Detail kos tidak tersedia", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        handler = new Handler(Looper.getMainLooper());

        initViews();
        setupListeners();
        populateData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnShare = findViewById(R.id.btnShare);
        btnChat = findViewById(R.id.btnChat);
        btnWaitlistBottom = findViewById(R.id.btnWaitlistBottom);
        btnBookingBottom = findViewById(R.id.btnBookingBottom);
        imgHero = findViewById(R.id.imgHero);
        
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailPriceValue = findViewById(R.id.tvDetailPriceValue);
        tvDetailBadgeCategory = findViewById(R.id.tvDetailBadgeCategory);
        tvDetailBadgeSisa = findViewById(R.id.tvDetailBadgeSisa);
        tvDetailRouteDistance = findViewById(R.id.tvDetailRouteDistance);
        tvDetailRouteTime = findViewById(R.id.tvDetailRouteTime);
        tvDetailRating = findViewById(R.id.tvDetailRating);
        tvDetailRatingCount = findViewById(R.id.tvDetailRatingCount);
        
        amenityChipGroup = findViewById(R.id.amenityChipGroup);
        reviewContainer = findViewById(R.id.reviewContainer);
        layoutReviewPrompt = findViewById(R.id.layoutReviewPrompt);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnShare.setOnClickListener(v -> {
            copyToClipboard();
            showCustomToast("📤 Link kos disalin ke clipboard!");
        });

        btnChat.setOnClickListener(v -> openChatWithOwner());

        btnBookingBottom.setOnClickListener(v -> showBookingConfirmationDialog());
        btnWaitlistBottom.setOnClickListener(v -> showBookingConfirmationDialog());
    }

    private void showBookingConfirmationDialog() {
        if (currentItem == null) return;
        
        String message = "Kos: " + currentItem.getName() + "\n" +
                "Harga: " + currentItem.getPrice() + "\n" +
                "Durasi: 1 Bulan\n\n" +
                "Lanjutkan booking?";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi Booking")
                .setMessage(message)
                .setPositiveButton("Booking Sekarang", (dialog, which) -> handleBooking())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void handleBooking() {
        if (currentItem == null) return;

        Booking booking = new Booking();
        booking.setKosId(currentItem.getId());
        booking.setOwnerId(currentItem.getOwnerId());
        booking.setKosName(currentItem.getName());
        booking.setKosAddress(currentItem.getAddress());
        booking.setTotalPrice(currentItem.getPriceValue());
        booking.setDurationMonth(1); // Default for simulation
        booking.setCheckInDate(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)); // Default 1 week from now

        BookingRepository.getInstance().createBooking(booking, new BookingRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                showCustomToast("Booking berhasil dikirim! Menunggu konfirmasi owner.");
                // Redirect to Waiting List
                Intent intent = new Intent(PropertyDetailBookingActivity.this, WaitingListQueueActivity.class);
                NavigationTransitionHelper.navigateMainWithIntent(PropertyDetailBookingActivity.this, intent);
            }

            @Override
            public void onError(String message) {
                showCustomToast(message);
            }
        });
    }

    private void toggleFavorite() {
        if (currentItem == null) return;
        FavoriteRepository.getInstance().toggleFavorite(currentItem, new FavoriteRepository.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                showCustomToast(message);
                checkFavoriteStatus();
            }

            @Override
            public void onError(String message) {
                showCustomToast(message);
            }
        });
    }

    private void checkFavoriteStatus() {
        if (currentItem == null) return;
        FavoriteRepository.getInstance().isFavorite(currentItem.getId(), new FavoriteRepository.FavoriteCallback() {
            @Override
            public void onSuccess(boolean status) {
                isFavorited = status;
                if (isFavorited) {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                }
            }

            @Override
            public void onError(String message) {
                // Ignore
            }
        });
    }

    private void openChatWithOwner() {
        if (currentItem == null || currentItem.getId() == null) return;

        // Disable button during process
        btnChat.setEnabled(false);
        btnChat.setAlpha(0.5f);

        ChatRepository.getInstance().getOrCreateChatFromKos(currentItem.getId(), new ChatRepository.ChatCallback() {
            @Override
            public void onSuccess(Chat chat) {
                btnChat.setEnabled(true);
                btnChat.setAlpha(1.0f);

                Intent intent = new Intent(PropertyDetailBookingActivity.this, OwnerChatRoomActivity.class);
                intent.putExtra("CHAT_ID", chat.getId());
                intent.putExtra("KOS_NAME", currentItem.getName());
                intent.putExtra("USER_NAME", "Pemilik Kos");
                intent.putExtra("INITIAL", "P");
                NavigationTransitionHelper.navigateDetailWithIntent(PropertyDetailBookingActivity.this, intent);
            }

            @Override
            public void onError(String message) {
                btnChat.setEnabled(true);
                btnChat.setAlpha(1.0f);
                showCustomToast(message);
            }
        });
    }

    private void populateData() {
        if (currentItem == null) return;

        // Fetch latest data from Firestore to ensure accurate rating/stats
        com.koshub.psdku.repositories.KosRepository.getInstance().getKosById(currentItem.getId(), new com.koshub.psdku.repositories.KosRepository.KosCallback() {
            @Override
            public void onSuccess(com.koshub.psdku.models.Kos kos) {
                com.koshub.psdku.KosItem updatedItem = com.koshub.psdku.utils.KosMapper.toKosItem(kos);
                if (updatedItem != null) {
                    currentItem.setRatingAverage(updatedItem.getRatingAverage());
                    currentItem.setRatingCount(updatedItem.getRatingCount());
                    updateRatingViews();
                }
            }

            @Override
            public void onError(String message) {
                updateRatingViews(); // Fallback to current
            }
        });

        // Basic Info
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            String optimizedUrl = CloudinaryRepository.getInstance().getOptimizedUrl(currentItem.getImageUrl(), 800, 500, false);
            Glide.with(this)
                    .load(optimizedUrl)
                    .placeholder(currentItem.getImageRes() != 0 ? currentItem.getImageRes() : R.drawable.bg_map_placeholder)
                    .error(currentItem.getImageRes() != 0 ? currentItem.getImageRes() : R.drawable.bg_map_placeholder)
                    .into(imgHero);
        } else {
            imgHero.setImageResource(currentItem.getImageRes());
        }

        tvDetailTitle.setText(currentItem.getName());
        tvDetailLocation.setText(currentItem.getAddress());
        tvDetailBadgeCategory.setText(currentItem.getCategory());
        
        if (currentItem.getSisaKamar() != null) {
            tvDetailBadgeSisa.setText(currentItem.getSisaKamar());
            tvDetailBadgeSisa.setVisibility(View.VISIBLE);
        } else {
            tvDetailBadgeSisa.setVisibility(View.GONE);
        }

        // Price formatting
        tvDetailPriceValue.setText(formatPrice(currentItem.getPriceValue()));

        // Rating
        updateRatingViews();

        // Description
        tvDetailDescription.setText(generateDescription(currentItem));

        // Route Info
        tvDetailRouteDistance.setText(currentItem.getDistance());
        tvDetailRouteTime.setText(String.format(Locale.getDefault(), "%d mnt", currentItem.getDistanceMinutes()));

        // Amenities
        populateAmenities(currentItem.getFacilities());

        // Reviews
        loadRealReviews();

        // Check if user can write review
        checkAndShowReviewButton();

        // Favorite status
        checkFavoriteStatus();

        // Map
        setupMap();
    }

    private void updateRatingViews() {
        if (tvDetailRating != null) {
            double avg = currentItem.getRatingAverage();
            tvDetailRating.setText(avg > 0
                    ? String.format(Locale.getDefault(), "%.1f", avg)
                    : "—");
        }
        if (tvDetailRatingCount != null) {
            int count = currentItem.getRatingCount();
            tvDetailRatingCount.setText(count > 0
                    ? String.format(Locale.getDefault(), "(%d Ulasan)", count)
                    : "(Belum ada ulasan)");
        }
    }

    private void loadRealReviews() {
        if (currentItem == null || currentItem.getId() == null) return;
        
        ReviewRepository.getInstance().getReviewsByKos(currentItem.getId(), new ReviewRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<com.koshub.psdku.models.Review> reviews) {
                populateReviewsReal(reviews);
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("KosHubReview", "Load reviews failed: " + message);
                // Fallback or empty state
            }
        });
    }

    private void checkAndShowReviewButton() {
        if (currentItem == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (layoutReviewPrompt != null) layoutReviewPrompt.setVisibility(View.GONE);
            return;
        }

        // Simplification: Show review prompt to any logged-in user
        if (layoutReviewPrompt != null) {
            layoutReviewPrompt.setVisibility(View.VISIBLE);
            View btn = layoutReviewPrompt.findViewById(R.id.btnWriteReviewPrompt);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(PropertyDetailBookingActivity.this, ReviewFormActivity.class);
                    intent.putExtra("KOS_ID", currentItem.getId());
                    intent.putExtra("KOS_NAME", currentItem.getName());
                    NavigationTransitionHelper.navigateDetailWithIntent(PropertyDetailBookingActivity.this, intent);
                });
            }
        }
    }

    private void populateReviewsReal(List<com.koshub.psdku.models.Review> reviews) {
        reviewContainer.removeAllViews();
        if (reviews.isEmpty()) {
            View emptyView = LayoutInflater.from(this).inflate(R.layout.layout_review_empty, reviewContainer, false);
            reviewContainer.addView(emptyView);
            return;
        }

        for (com.koshub.psdku.models.Review r : reviews) {
            View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review_dynamic, reviewContainer, false);
            
            TextView tvAvatar = reviewView.findViewById(R.id.tvReviewAvatar);
            TextView tvName = reviewView.findViewById(R.id.tvReviewName);
            TextView tvSub = reviewView.findViewById(R.id.tvReviewSub);
            TextView tvText = reviewView.findViewById(R.id.tvReviewText);
            
            String initial = r.getStudentName() != null && !r.getStudentName().isEmpty() ? 
                             r.getStudentName().substring(0, 1) : "?";
            tvAvatar.setText(initial);
            tvName.setText(r.getStudentName());
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String dateStr = sdf.format(new java.util.Date(r.getCreatedAt()));
            tvSub.setText("Mahasiswa • " + dateStr + " • ⭐ " + r.getRating());
            tvText.setText(r.getComment());
            
            reviewContainer.addView(reviewView);
        }
    }

    private String formatPrice(int value) {
        if (value >= 1000000) {
            double million = value / 1000000.0;
            if (million == (long) million)
                return String.format(Locale.getDefault(), "Rp %djt", (long) million);
            else
                return String.format(Locale.getDefault(), "Rp %.1fjt", million);
        } else if (value >= 1000) {
            return String.format(Locale.getDefault(), "Rp %dk", value / 1000);
        }
        return "Rp " + value;
    }

    private String generateDescription(KosItem item) {
        StringBuilder facilitiesStr = new StringBuilder();
        List<String> facilities = item.getFacilities();
        for (int i = 0; i < facilities.size(); i++) {
            facilitiesStr.append(facilities.get(i));
            if (i < facilities.size() - 1) {
                facilitiesStr.append(", ");
            }
        }

        return "Kos " + item.getCategory().toLowerCase() + " dengan fasilitas lengkap di " + item.getAddress() + 
               ". Sangat strategis, hanya berjarak " + item.getDistance() + " dari kampus. " +
               "Dilengkapi dengan " + facilitiesStr.toString() + ". " +
               "Lingkungan bersih, aman dengan CCTV, dan sangat kondusif untuk belajar.";
    }

    private void populateAmenities(List<String> facilities) {
        amenityChipGroup.removeAllViews();
        if (facilities == null) return;
        for (String facility : facilities) {
            Chip chip = new Chip(this);
            chip.setText(facility);
            chip.setChipBackgroundColorResource(R.color.md_surface_container_low);
            chip.setTextColor(getResources().getColor(R.color.md_on_surface_variant));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColorResource(R.color.md_outline_variant);
            
            int iconRes = getIconForFacility(facility);
            if (iconRes != 0) {
                chip.setChipIconResource(iconRes);
                chip.setChipIconTintResource(R.color.md_primary);
                chip.setChipIconSize(dpToPx(18));
            }
            
            amenityChipGroup.addView(chip);
        }
    }

    private int getIconForFacility(String facility) {
        String f = facility.toLowerCase();
        if (f.contains("ac")) return R.drawable.ic_ac_unit;
        if (f.contains("wifi")) return R.drawable.ic_wifi;
        if (f.contains("kamar mandi") || f.contains("shower")) return R.drawable.ic_shower;
        if (f.contains("kasur") || f.contains("bed")) return R.drawable.ic_bed;
        if (f.contains("meja") || f.contains("desk")) return R.drawable.ic_desk;
        if (f.contains("lemari") || f.contains("closet") || f.contains("wardrobe")) return R.drawable.ic_closet;
        if (f.contains("parkir")) return R.drawable.ic_parking;
        if (f.contains("dapur") || f.contains("kitchen")) return R.drawable.ic_kitchen;
        if (f.contains("laundry")) return R.drawable.ic_laundry;
        if (f.contains("water heater") || f.contains("air panas")) return R.drawable.ic_water_heater;
        return 0;
    }

    private void setupMap() {
        FrameLayout mapContainer = findViewById(R.id.mapContainer);
        if (mapContainer == null) return;

        try {
            mapView = new MapView(this);
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            
            double lat = currentItem != null ? currentItem.getLatitude() : -7.6298;
            double lng = currentItem != null ? currentItem.getLongitude() : 111.5231;
            
            IMapController mapController = mapView.getController();
            mapController.setZoom(15.0);
            GeoPoint startPoint = new GeoPoint(lat, lng);
            mapController.setCenter(startPoint);

            // Add Marker
            Marker marker = new Marker(mapView);
            marker.setPosition(startPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            if (currentItem != null) {
                marker.setTitle(currentItem.getName());
            }
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_home));
            mapView.getOverlays().add(marker);

            mapContainer.addView(mapView);
        } catch (Exception e) {
            android.util.Log.e("OSMDroid", "Failed to initialize MapView", e);
            showMapFallback(mapContainer);
        }
    }

    private void showMapFallback(FrameLayout container) {
        if (container == null) return;
        container.removeAllViews();
        View fallbackView = LayoutInflater.from(this).inflate(R.layout.layout_map_fallback, container, false);
        
        TextView tvSub = fallbackView.findViewById(R.id.tvMapFallbackSub);
        if (tvSub != null) {
            tvSub.setText("Lokasi kos tidak dapat ditampilkan saat ini.");
        }
        
        container.addView(fallbackView);
    }

    private void showCustomToast(String message) {
        TextView toastView = new TextView(this);
        toastView.setText(message);
        toastView.setTextColor(Color.WHITE);
        toastView.setTextSize(14f);
        toastView.setBackgroundResource(R.drawable.bg_toast);
        int paddingH = dpToPx(24);
        int paddingV = dpToPx(12);
        toastView.setPadding(paddingH, paddingV, paddingH, paddingV);
        toastView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(this);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dpToPx(100));
        toast.show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void copyToClipboard() {
        if (currentItem == null) return;
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("KosHub Link", "https://koshub.com/kos/" + currentItem.getId());
        if (clipboard != null) clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
