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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.adapters.PropertyImageAdapter;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.Chat;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.ChatRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.FavoriteRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.repositories.ReviewRepository;
import com.koshub.psdku.utils.AutoKosDescriptionBuilder;
import com.koshub.psdku.utils.KosLocationUtils;
import com.koshub.psdku.utils.KosMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PropertyDetailBookingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnFavorite;
    private ImageButton btnShare;
    private ImageButton btnChat;
    private TextView btnWaitlistBottom;
    private TextView btnBookingBottom;
    private ViewPager2 vpGallery;
    private LinearLayout layoutDots;
    
    private TextView tvDetailTitle, tvDetailLocation, tvDetailDescription;
    private TextView tvDetailPriceValue, tvDetailBadgeCategory, tvDetailBadgeSisa, tvDetailBottomSisa;
    private TextView tvDetailRouteDistance, tvDetailRouteTime;
    private View btnOpenMaps;
    private TextView tvTitleRating, tvTitleRatingCount;
    private TextView tvReviewsAverage, tvReviewsCount;
    private ChipGroup amenityChipGroup;
    private LinearLayout reviewContainer;
    private View layoutReviewPrompt;
    private MapView mapViewDetail;
    private GoogleMap googleMap;

    private KosItem currentItem;
    private com.koshub.psdku.models.Kos latestKosData;
    private ListenerRegistration kosListener;
    private ListenerRegistration reviewsListener;
    private Double currentDistanceKm;
    private Integer currentEtaMinutes;
    private boolean isFavorited = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail_booking);

        mapViewDetail = findViewById(R.id.mapViewDetail);
        mapViewDetail.onCreate(savedInstanceState);

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

        applyPropertyDetailInsets();
    }

    private void applyPropertyDetailInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        // Hero image berada di belakang status bar, jadi icon status bar harus light/putih.
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        View root = findViewById(R.id.rootPropertyDetail);
        View topNav = findViewById(R.id.topPropertyNav);
        View bottomBar = findViewById(R.id.bottomBookingBar);
        View scrollContent = findViewById(R.id.scrollPropertyDetail);

        final int topNavBasePaddingTop = topNav.getPaddingTop();
        final int topNavBasePaddingBottom = topNav.getPaddingBottom();
        final int bottomBarBasePaddingBottom = bottomBar.getPaddingBottom();
        final int scrollBasePaddingBottom = scrollContent.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            topNav.setPadding(
                    topNav.getPaddingLeft(),
                    bars.top + topNavBasePaddingTop,
                    topNav.getPaddingRight(),
                    topNavBasePaddingBottom
            );

            bottomBar.setPadding(
                    bottomBar.getPaddingLeft(),
                    bottomBar.getPaddingTop(),
                    bottomBar.getPaddingRight(),
                    bottomBarBasePaddingBottom + bars.bottom
            );

            scrollContent.setPadding(
                    scrollContent.getPaddingLeft(),
                    scrollContent.getPaddingTop(),
                    scrollContent.getPaddingRight(),
                    scrollBasePaddingBottom + bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnShare = findViewById(R.id.btnShare);
        btnChat = findViewById(R.id.btnChat);
        btnWaitlistBottom = findViewById(R.id.btnWaitlistBottom);
        btnBookingBottom = findViewById(R.id.btnBookingBottom);
        vpGallery = findViewById(R.id.vpGallery);
        layoutDots = findViewById(R.id.layoutDots);
        
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailPriceValue = findViewById(R.id.tvDetailPriceValue);
        tvDetailBadgeCategory = findViewById(R.id.tvDetailBadgeCategory);
        tvDetailBadgeSisa = findViewById(R.id.tvDetailBadgeSisa);
        tvDetailBottomSisa = findViewById(R.id.tvDetailBottomSisa);
        tvDetailRouteDistance = findViewById(R.id.tvDetailRouteDistance);
        tvDetailRouteTime = findViewById(R.id.tvDetailRouteTime);
        btnOpenMaps = findViewById(R.id.btnOpenMaps);
        
        tvTitleRating = findViewById(R.id.tvTitleRating);
        tvTitleRatingCount = findViewById(R.id.tvTitleRatingCount);
        tvReviewsAverage = findViewById(R.id.tvReviewsAverage);
        tvReviewsCount = findViewById(R.id.tvReviewsCount);
        
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

        if (btnOpenMaps != null) {
            btnOpenMaps.setOnClickListener(v -> openGoogleMaps());
        }

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

        boolean target = !isFavorited;
        isFavorited = target;
        currentItem.setFavorite(target);

        // Update icon directly
        if (isFavorited) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
        }

        FavoriteRepository.getInstance().setFavorite(currentItem, target, new FavoriteRepository.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                showCustomToast(message);
            }

            @Override
            public void onError(String message) {
                showCustomToast(message);
                // Revert on error
                isFavorited = !target;
                currentItem.setFavorite(!target);
                if (isFavorited) {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                }
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

        // Fetch latest data from Firestore realtime
        kosListener = KosRepository.getInstance().listenKosById(currentItem.getId(), new KosRepository.KosCallback() {
            @Override
            public void onSuccess(com.koshub.psdku.models.Kos kos) {
                latestKosData = kos;
                bindKosDetail(kos);
            }

            @Override
            public void onError(String message) {
                // Fallback UI if listening fails
                android.util.Log.e("PropertyDetail", "Failed to listen kos: " + message);
            }
        });

        // Basic Info (from Intent while waiting for Firestore)
        setupImageGallery(currentItem.getImageUrls(), currentItem.getImageRes());

        // Amenities
        populateAmenities(currentItem.getFacilities(), currentItem.getRoomFeatures(), currentItem.getAccessFeatures(), currentItem.getSecurityFeatures());

        // Reviews
        loadRealReviews();

        // Check if user can write review
        checkAndShowReviewButton();

        // Favorite status
        checkFavoriteStatus();

        // Route Info & Map
        updateLocationAndRoute();
    }

    private void bindKosDetail(com.koshub.psdku.models.Kos kos) {
        if (kos == null) return;
        
        // Map Kos to gallery images list
        List<String> gallery = new ArrayList<>();
        if (kos.getImageUrls() != null && !kos.getImageUrls().isEmpty()) {
            gallery.addAll(kos.getImageUrls());
        } else if (kos.getImageUrl() != null && !kos.getImageUrl().isEmpty()) {
            gallery.add(kos.getImageUrl());
        }

        boolean locationChanged = false;
        if (latestKosData != null) {
            locationChanged = kos.getLatitude() != latestKosData.getLatitude() || 
                             kos.getLongitude() != latestKosData.getLongitude();
        }
        
        latestKosData = kos;

        // Update Title, Address, Category
        tvDetailTitle.setText(kos.getName());
        tvDetailLocation.setText(kos.getAddress());
        
        String category = kos.getCategory();
        if (category != null && !category.isEmpty()) {
            category = category.substring(0, 1).toUpperCase() + category.substring(1);
        }
        tvDetailBadgeCategory.setText(category);

        // Availability UI
        updateAvailabilityUI(kos.getAvailableRooms());

        // Price
        tvDetailPriceValue.setText(formatPrice((int) kos.getPrice()));

        // Gallery Update
        setupImageGallery(gallery, kos.getImageRes());

        // Rating Fallback
        double avg = kos.getRatingAverage() > 0 ? kos.getRatingAverage() : kos.getRating();
        int count = kos.getRatingCount();
        updateRatingViews(avg, count);

        // Amenities
        populateAmenities(kos.getFacilities(), kos.getRoomFeatures(), kos.getAccessFeatures(), kos.getSecurityFeatures());

        // Map refresh if coordinates changed or first time
        if (KosLocationUtils.isValidCoordinate(kos.getLatitude(), kos.getLongitude())) {
            if (mapViewDetail != null && (mapViewDetail.getVisibility() == View.GONE || locationChanged)) {
                mapViewDetail.setVisibility(View.VISIBLE);
                initDetailMap(kos.getLatitude(), kos.getLongitude(), kos.getName());
                
                if (locationChanged) {
                    updateLocationAndRoute();
                }
            }
        } else {
            if (mapViewDetail != null) mapViewDetail.setVisibility(View.GONE);
        }

        // Trigger Auto Description
        updateAutoDescription();
    }

    private void updateAvailabilityUI(int availableRooms) {
        // Top Badge
        String detailText = com.koshub.psdku.utils.RoomAvailabilityHelper.formatAvailabilityDetail(availableRooms);
        tvDetailBadgeSisa.setText(detailText);
        tvDetailBadgeSisa.setVisibility(View.VISIBLE);

        if (availableRooms == 0) {
            tvDetailBadgeSisa.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            tvDetailBadgeSisa.setBackgroundResource(R.drawable.bg_badge_sisa);
        }

        // Bottom Bar
        if (tvDetailBottomSisa != null) {
            String bottomText = com.koshub.psdku.utils.RoomAvailabilityHelper.formatAvailabilityBottom(availableRooms);
            tvDetailBottomSisa.setText(bottomText);
        }

        // Button States
        if (availableRooms == 0) {
            btnBookingBottom.setEnabled(false);
            btnBookingBottom.setAlpha(0.5f);
            btnBookingBottom.setBackgroundResource(R.drawable.bg_outline_button);
            btnBookingBottom.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface_variant));
            
            btnWaitlistBottom.setVisibility(View.VISIBLE);
            btnWaitlistBottom.setText("Kabari Saya (Waitlist)");
        } else {
            btnBookingBottom.setEnabled(true);
            btnBookingBottom.setAlpha(1.0f);
            btnBookingBottom.setBackgroundResource(R.drawable.bg_primary_button);
            btnBookingBottom.setTextColor(ContextCompat.getColor(this, R.color.md_on_primary));
            
            // If rooms available, waitlist is optional or can be hidden if preferred
            btnWaitlistBottom.setText(R.string.detail_btn_waitlist);
        }
    }

    private void updateLocationAndRoute() {
        if (latestKosData == null && currentItem == null) return;

        double lat = latestKosData != null ? latestKosData.getLatitude() : currentItem.getLatitude();
        double lng = latestKosData != null ? latestKosData.getLongitude() : currentItem.getLongitude();
        String name = latestKosData != null ? latestKosData.getName() : currentItem.getName();

        // Route Info
        if (tvDetailRouteDistance != null) tvDetailRouteDistance.setText(R.string.eta_calculating);
        if (tvDetailRouteTime != null) tvDetailRouteTime.setText("...");

        // Map
        if (KosLocationUtils.isValidCoordinate(lat, lng)) {
            initDetailMap(lat, lng, name);
        } else {
            if (mapViewDetail != null) mapViewDetail.setVisibility(View.GONE);
        }
        
        KosLocationUtils.fetchWalkingDuration(this, lat, lng, new KosLocationUtils.DurationCallback() {
            @Override
            public void onSuccess(String durationText, int durationMinutes, String distanceText) {
                currentEtaMinutes = durationMinutes;
                // Extract km from distanceText "0.5 km" -> 0.5
                try {
                    String clean = distanceText.replace("km", "").replace(",", ".").trim();
                    currentDistanceKm = Double.parseDouble(clean);
                } catch (Exception e) {
                    currentDistanceKm = KosLocationUtils.calculateDistanceKm(lat, lng, KosLocationUtils.CAMPUS_LAT, KosLocationUtils.CAMPUS_LNG);
                }

                String detailEta = KosLocationUtils.formatEtaDetail(PropertyDetailBookingActivity.this, durationMinutes);
                if (tvDetailRouteTime != null) tvDetailRouteTime.setText(detailEta);
                if (tvDetailRouteDistance != null) tvDetailRouteDistance.setText(distanceText + " ke Kampus UNS PSDKU");
                
                updateAutoDescription();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (tvDetailRouteTime != null) tvDetailRouteTime.setText(errorMessage); // "Cek Maps"
                if (tvDetailRouteDistance != null) tvDetailRouteDistance.setText("Lokasi tidak valid");
                updateAutoDescription();
            }
        });
    }

    private void updateAutoDescription() {
        if (latestKosData == null) return;
        
        String autoDescription = AutoKosDescriptionBuilder.build(latestKosData, currentDistanceKm, currentEtaMinutes);
        if (tvDetailDescription != null) {
            tvDetailDescription.setText(autoDescription);
        }
    }

    private void openGoogleMaps() {
        if (currentItem == null) return;
        double lat = currentItem.getLatitude();
        double lng = currentItem.getLongitude();
        
        if (!KosLocationUtils.isValidCoordinate(lat, lng)) {
            showCustomToast("Lokasi tidak valid");
            return;
        }

        String uri = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f&mode=w", lat, lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser
            String browserUri = String.format(Locale.ENGLISH, "https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=walking", lat, lng);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(browserUri));
            startActivity(browserIntent);
        }
    }

    private void initDetailMap(double kosLat, double kosLng, String kosName) {
        mapViewDetail.getMapAsync(gMap -> {
            googleMap = gMap;
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            googleMap.getUiSettings().setScrollGesturesEnabled(false);

            LatLng kosLocation = new LatLng(kosLat, kosLng);
            LatLng campusLocation = new LatLng(
                    KosLocationUtils.CAMPUS_LAT,
                    KosLocationUtils.CAMPUS_LNG
            );

            // Marker kos
            googleMap.addMarker(new MarkerOptions()
                    .position(kosLocation)
                    .title(kosName)
            );

            // Marker kampus
            googleMap.addMarker(new MarkerOptions()
                    .position(campusLocation)
                    .title(KosLocationUtils.CAMPUS_NAME)
            );

            // Zoom agar kedua marker kelihatan
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boundsBuilder.include(kosLocation);
            boundsBuilder.include(campusLocation);
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
        });
    }



    private void loadRealReviews() {
        if (currentItem == null || currentItem.getId() == null) return;
        
        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
        }

        reviewsListener = ReviewRepository.getInstance().listenReviewsByKos(
                currentItem.getId(),
                new ReviewRepository.ReviewListCallback() {
                    @Override
                    public void onSuccess(List<com.koshub.psdku.models.Review> reviews) {
                        populateReviewsReal(reviews);
                        updateRatingFromReviews(reviews);
                    }

                    @Override
                    public void onError(String message) {
                        android.util.Log.e("KosHubReview", "Load reviews failed: " + message);
                    }
                }
        );
    }

    private void updateRatingFromReviews(List<com.koshub.psdku.models.Review> reviews) {
        double total = 0;
        int count = 0;

        if (reviews != null) {
            for (com.koshub.psdku.models.Review r : reviews) {
                if (r != null && r.getRating() >= 1 && r.getRating() <= 5) {
                    total += r.getRating();
                    count++;
                }
            }
        }

        double average = count > 0 ? total / count : 0;
        updateRatingViews(average, count);
    }

    private void updateRatingViews(double average, int count) {
        String ratingText = count > 0
                ? String.format(Locale.getDefault(), "%.1f", average)
                : "—";

        String countText = count > 0
                ? String.format(Locale.getDefault(), "(%d Ulasan)", count)
                : "(Belum ada ulasan)";

        if (tvTitleRating != null) tvTitleRating.setText(ratingText);
        if (tvTitleRatingCount != null) tvTitleRatingCount.setText(countText);
        if (tvReviewsAverage != null) tvReviewsAverage.setText(ratingText);
        if (tvReviewsCount != null) tvReviewsCount.setText(countText);
    }


    private void checkAndShowReviewButton() {
        if (currentItem == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (layoutReviewPrompt != null) layoutReviewPrompt.setVisibility(View.GONE);
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        
        // Use getReviewableBookingForKos to check if user is eligible (active or completed)
        ReviewRepository.getInstance().getReviewableBookingForKos(uid, currentItem.getId(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(Booking reviewableBooking) {
                if (layoutReviewPrompt == null) return;
                
                // Now check if a review already exists for THIS specific booking
                ReviewRepository.getInstance().getUserReviewForKos(currentItem.getId(), uid, new ReviewRepository.ReviewCallback() {
                    @Override
                    public void onSuccess(com.koshub.psdku.models.Review myReview) {
                        layoutReviewPrompt.setVisibility(View.VISIBLE);

                        TextView tvTitle = layoutReviewPrompt.findViewById(R.id.tvReviewPromptTitle);
                        TextView tvSub = layoutReviewPrompt.findViewById(R.id.tvReviewPromptSub);
                        TextView btn = layoutReviewPrompt.findViewById(R.id.btnWriteReviewPrompt);
                        ImageView ivIcon = layoutReviewPrompt.findViewById(R.id.ivReviewPromptIcon);

                        if (myReview != null) {
                            // User already reviewed
                            if (tvTitle != null) tvTitle.setText("Ulasanmu dikirim");
                            if (tvSub != null) tvSub.setText("Kamu memberi rating " + (int)myReview.getRating() + " bintang");
                            if (btn != null) btn.setText("Edit Ulasan");
                            if (ivIcon != null) ivIcon.setImageResource(R.drawable.ic_star_filled);
                        } else {
                            // No review yet
                            if (tvTitle != null) tvTitle.setText("Bagikan pengalamanmu");
                            if (tvSub != null) tvSub.setText("Bantu mahasiswa lain memilih kos yang tepat.");
                            if (btn != null) btn.setText("Tulis Ulasan");
                            if (ivIcon != null) ivIcon.setImageResource(R.drawable.ic_chat_review);
                        }

                        if (btn != null) {
                            btn.setOnClickListener(v -> {
                                Intent intent = new Intent(PropertyDetailBookingActivity.this, ReviewFormActivity.class);
                                intent.putExtra("KOS_ID", currentItem.getId());
                                intent.putExtra("KOS_NAME", currentItem.getName());
                                intent.putExtra("BOOKING_ID", reviewableBooking.getId());
                                if (myReview != null) {
                                    intent.putExtra("REVIEW_ID", myReview.getId());
                                }
                                NavigationTransitionHelper.navigateDetailWithIntent(PropertyDetailBookingActivity.this, intent);
                            });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        layoutReviewPrompt.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                // Not eligible for review
                if (layoutReviewPrompt != null) layoutReviewPrompt.setVisibility(View.GONE);
            }
        });
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
            long displayTime = r.getCreatedAt() > 0 ? r.getCreatedAt() : r.getUpdatedAt();
            if (displayTime <= 0) displayTime = System.currentTimeMillis();
            String dateStr = sdf.format(new java.util.Date(displayTime));

            tvSub.setText("Mahasiswa • " + dateStr + " • ⭐ " + r.getRating());
            tvText.setText(r.getComment());
            
            reviewContainer.addView(reviewView);
        }
    }

    private void setupImageGallery(List<String> images, int fallbackRes) {
        if (vpGallery == null) return;

        List<String> galleryImages = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            galleryImages.addAll(images);
        } else {
            // Add empty string to trigger placeholder in adapter if no images at all
            galleryImages.add("");
        }

        PropertyImageAdapter adapter = new PropertyImageAdapter(galleryImages, fallbackRes);
        vpGallery.setAdapter(adapter);

        // Dynamic Indicators
        if (layoutDots != null) {
            layoutDots.removeAllViews();
            if (galleryImages.size() > 1) {
                layoutDots.setVisibility(View.VISIBLE);
                vpGallery.setUserInputEnabled(true);
                
                ImageView[] dots = new ImageView[galleryImages.size()];
                for (int i = 0; i < galleryImages.size(); i++) {
                    dots[i] = new ImageView(this);
                    dots[i].setImageResource(i == 0 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
                    
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            dpToPx(8), dpToPx(8)
                    );
                    params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
                    layoutDots.addView(dots[i], params);
                }

                vpGallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        for (int i = 0; i < galleryImages.size(); i++) {
                            dots[i].setImageResource(i == position ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
                        }
                    }
                });
            } else {
                layoutDots.setVisibility(View.GONE);
                vpGallery.setUserInputEnabled(false);
            }
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



    private void populateAmenities(List<String> facilities, List<String> roomFeatures, List<String> accessFeatures, List<String> securityFeatures) {
        amenityChipGroup.removeAllViews();
        
        List<String> allFeatures = new ArrayList<>();
        if (facilities != null) allFeatures.addAll(facilities);
        if (roomFeatures != null) allFeatures.addAll(roomFeatures);
        if (accessFeatures != null) allFeatures.addAll(accessFeatures);
        if (securityFeatures != null) allFeatures.addAll(securityFeatures);

        if (allFeatures.isEmpty()) return;
        
        // Remove duplicates and empty strings
        List<String> cleanFeatures = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String f : allFeatures) {
            if (f != null && !f.trim().isEmpty()) {
                if (seen.add(f.toLowerCase())) {
                    cleanFeatures.add(f);
                }
            }
        }

        for (String feature : cleanFeatures) {
            Chip chip = new Chip(this);
            chip.setText(feature);
            chip.setChipBackgroundColorResource(R.color.md_surface_container_low);
            chip.setTextColor(getResources().getColor(R.color.md_on_surface_variant));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColorResource(R.color.md_outline_variant);
            
            int iconRes = getIconForFacility(feature);
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
        if (f.contains("parkir") || f.contains("motor")) return R.drawable.ic_parking;
        if (f.contains("dapur") || f.contains("kitchen")) return R.drawable.ic_kitchen;
        if (f.contains("laundry")) return R.drawable.ic_laundry;
        if (f.contains("water heater") || f.contains("air panas")) return R.drawable.ic_water_heater;

        // New mappings for structured data
        if (f.contains("cctv") || f.contains("keamanan") || f.contains("penjaga") || f.contains("gerbang")) return R.drawable.ic_owp_security;
        if (f.contains("kunci")) return R.drawable.ic_key;
        if (f.contains("kampus") || f.contains("mahasiswa") || f.contains("sekolah")) return R.drawable.ic_school;
        if (f.contains("makan") || f.contains("warung")) return R.drawable.ic_kitchen;
        if (f.contains("masjid")) return R.drawable.ic_map_school;
        if (f.contains("jam malam") || f.contains("akses 24 jam")) return R.drawable.ic_lock;

        return 0;
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
        if (mapViewDetail != null) mapViewDetail.onResume();
        checkAndShowReviewButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewDetail != null) mapViewDetail.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (kosListener != null) {
            kosListener.remove();
        }
        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
        }
        if (mapViewDetail != null) mapViewDetail.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewDetail != null) mapViewDetail.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapViewDetail != null) mapViewDetail.onSaveInstanceState(outState);
    }
}
