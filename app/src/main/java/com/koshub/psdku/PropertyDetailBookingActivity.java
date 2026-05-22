package com.koshub.psdku;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PropertyDetailBookingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnFavorite;
    private ImageButton btnShare;
    private TextView btnWaitlistBottom;
    private TextView btnBookingBottom;
    private ImageView imgHero;
    
    private TextView tvDetailTitle, tvDetailLocation, tvDetailDescription;
    private TextView tvDetailPriceValue, tvDetailBadgeCategory, tvDetailBadgeSisa;
    private TextView tvDetailRouteDistance, tvDetailRouteTime;
    private ChipGroup amenityChipGroup;
    private LinearLayout reviewContainer;
    private MapView mapView;

    private KosItem currentItem;
    private boolean isFavorited = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail_booking);

        currentItem = (KosItem) getIntent().getSerializableExtra("kos_item");
        if (currentItem == null) {
            // Default "Real" Data if none passed
            currentItem = new KosItem(
                    "Kos Putra Harmoni", "Jl. Pendidikan No. 12, Kebumen",
                    "Rp 750rb", 750000, "5 mnt", 5, "4.8", "Putra",
                    java.util.Arrays.asList("WiFi", "K. Mandi Dalam", "Laundry"),
                    R.drawable.kos_03, false, null,
                    -7.68307 + 0.0005, 109.6645 + 0.0005);
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
        
        amenityChipGroup = findViewById(R.id.amenityChipGroup);
        reviewContainer = findViewById(R.id.reviewContainer);
        mapView = findViewById(R.id.mapView);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> {
            isFavorited = !isFavorited;
            if (isFavorited) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                showCustomToast("❤️ Ditambahkan ke favorit");
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                showCustomToast("Dihapus dari favorit");
            }
        });

        btnShare.setOnClickListener(v ->
            showCustomToast("📤 Link kos disalin!")
        );

        btnBookingBottom.setOnClickListener(v -> {
            btnBookingBottom.setEnabled(false);
            btnBookingBottom.setAlpha(0.7f);
            String originalText = btnBookingBottom.getText().toString();
            btnBookingBottom.setText("...");

            handler.postDelayed(() -> {
                btnBookingBottom.setText(originalText);
                btnBookingBottom.setEnabled(true);
                btnBookingBottom.setAlpha(1.0f);
                showCustomToast("📩 Permintaan booking dikirim! Pemilik akan menghubungi Anda.");
            }, 1500);
        });

        btnWaitlistBottom.setOnClickListener(v -> {
            btnWaitlistBottom.setText("✅ Terdaftar di Waiting List");
            btnWaitlistBottom.setEnabled(false);
            btnWaitlistBottom.setAlpha(0.7f);
            showCustomToast("📋 Anda masuk antrean! Kami kabari jika kamar tersedia.");
        });
    }

    private void populateData() {
        if (currentItem == null) return;

        // Basic Info
        imgHero.setImageResource(currentItem.getImageRes());
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

        // Description
        tvDetailDescription.setText(generateDescription(currentItem));

        // Route Info
        tvDetailRouteDistance.setText(currentItem.getDistance());
        tvDetailRouteTime.setText(String.format(Locale.getDefault(), "%d mnt", currentItem.getDistanceMinutes()));

        // Amenities
        populateAmenities(currentItem.getFacilities());

        // Reviews
        populateReviews(currentItem.getName());

        // Map
        setupMap();
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
        for (String facility : facilities) {
            Chip chip = new Chip(this);
            chip.setText(facility);
            chip.setChipBackgroundColorResource(R.color.md_surface_container_low);
            chip.setTextColor(getResources().getColor(R.color.md_on_surface_variant));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColorResource(R.color.md_outline_variant);
            
            // Set Lucide-style icons based on facility name
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

    private void populateReviews(String kosName) {
        reviewContainer.removeAllViews();
        List<Review> reviews = getDummyReviews(kosName);
        for (Review r : reviews) {
            View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review_dynamic, reviewContainer, false);
            
            TextView tvAvatar = reviewView.findViewById(R.id.tvReviewAvatar);
            TextView tvName = reviewView.findViewById(R.id.tvReviewName);
            TextView tvSub = reviewView.findViewById(R.id.tvReviewSub);
            TextView tvText = reviewView.findViewById(R.id.tvReviewText);
            
            tvAvatar.setText(r.name.substring(0, 1));
            tvName.setText(r.name);
            tvSub.setText(r.sub);
            tvText.setText(r.text);
            
            reviewContainer.addView(reviewView);
        }
    }

    private void setupMap() {
        if (mapView != null) {
            mapView.getMapboxMap().loadStyle(Style.STANDARD, style -> {
                // Set initial camera position (dummy point near Madiun/Kebumen area)
                Point madiun = Point.fromLngLat(111.5231, -7.6298);
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(madiun)
                        .zoom(14.0)
                        .build());
            });
        }
    }

    private List<Review> getDummyReviews(String kosName) {
        List<Review> list = new ArrayList<>();
        int hash = kosName.hashCode();
        
        if (hash % 3 == 0) {
            list.add(new Review("Rina Amanda", "Mahasiswa PSDKU • 1 bulan lalu", "Tempatnya tenang banget, cocok buat yang butuh fokus belajar. Fasilitas " + currentItem.getFacilities().get(0) + " oke banget."));
            list.add(new Review("Fajar Kurniawan", "Mahasiswa PSDKU • 3 bulan lalu", "Suka sama lokasinya yang cuma " + currentItem.getDistance() + " ke kampus. Gak pernah telat lagi masuk kelas."));
        } else if (hash % 3 == 1) {
            StringBuilder facilitiesStr = new StringBuilder();
            int limit = Math.min(2, currentItem.getFacilities().size());
            for (int i = 0; i < limit; i++) {
                facilitiesStr.append(currentItem.getFacilities().get(i));
                if (i < limit - 1) facilitiesStr.append(" & ");
            }
            list.add(new Review("Siti Zulaikha", "Mahasiswa PSDKU • 2 minggu lalu", "Penjaga kosnya ramah pol! Kamar mandi dalam juga bersih dan air lancar. Sangat recommended!"));
            list.add(new Review("Dedi Pratama", "Mahasiswa PSDKU • 4 bulan lalu", "Harga segini dapet fasilitas " + facilitiesStr.toString() + " mah worth it banget."));
        } else {
            list.add(new Review("Agus Setiawan", "Mahasiswa PSDKU • 5 bulan lalu", "Lingkungan kosnya aman, parkiran luas. Gak nyesel milih kos " + kosName + " ini."));
            list.add(new Review("Maya Putri", "Mahasiswa PSDKU • 2 bulan lalu", "Wifinya kenceng, pas banget buat ngerjain tugas akhir. Kamar juga gak pengap."));
        }
        return list;
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

    private static class Review {
        String name;
        String sub;
        String text;
        Review(String n, String s, String t) {
            this.name = n; this.sub = s; this.text = t;
        }
    }
}
