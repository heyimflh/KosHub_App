package com.koshub.psdku;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.utils.KosLocationUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.slider.RangeSlider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapViewRouteNavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private ListenerRegistration kosListener;
    private boolean isInitialLoad = true;
    private List<KosItem> allKosList;
    private List<KosItem> currentFilteredList;
    private KosItem selectedKos;
    private Map<String, KosItem> markerKosMap = new HashMap<>();

    // UI Components
    private AutoCompleteTextView etSearchLocation;
    private FrameLayout btnSearch;
    private LinearLayout routeCard;
    private ImageView imgKosCard;
    private TextView tvKosName, tvKosAddress, tvDistance, tvPrice, tvPricePeriod, btnViewDetail, btnNavigate;
    private FrameLayout btnNotification;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view_route_navigation);

        mapView = findViewById(R.id.mapViewMain);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        // Safely get data
        try {
            Serializable serializable = getIntent().getSerializableExtra("kos_list");
            if (serializable instanceof ArrayList) {
                allKosList = (ArrayList<KosItem>) serializable;
            }
        } catch (Exception e) {
            allKosList = null;
        }
        if (allKosList == null) allKosList = new ArrayList<>();
        currentFilteredList = new ArrayList<>(allKosList);

        initViews();
        handleWindowInsets();
        setupListeners();
        setupSearchAutoComplete();
        setupMap();
    }

    private void initViews() {
        etSearchLocation = findViewById(R.id.etSearchLocation);
        btnSearch = findViewById(R.id.btnSearch);
        routeCard = findViewById(R.id.routeCard);
        imgKosCard = findViewById(R.id.imgKosCard);
        tvKosName = findViewById(R.id.tvKosName);
        tvKosAddress = findViewById(R.id.tvKosAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvPrice = findViewById(R.id.tvPrice);
        tvPricePeriod = findViewById(R.id.tvPricePeriod);
        btnViewDetail = findViewById(R.id.btnViewDetail);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnNotification = findViewById(R.id.btnNotification);

        routeCard.setVisibility(View.GONE);
    }

    private void handleWindowInsets() {
        View navbar = findViewById(R.id.navbar);
        if (navbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navbar, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void setupListeners() {
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch(etSearchLocation.getText().toString());
                return true;
            }
            return false;
        });

        etSearchLocation.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            performSearch(selection);
        });

        btnSearch.setOnClickListener(v -> showFilterSheet());
        
        routeCard.setOnClickListener(v -> navigateToPropertyDetail());
        btnViewDetail.setOnClickListener(v -> navigateToPropertyDetail());
        btnNavigate.setOnClickListener(v -> {
            if (selectedKos != null) {
                startNavigation(selectedKos);
            }
        });
        
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.MAP);
        btnNotification.setOnClickListener(v -> showCustomToast("🔔 Tidak ada notifikasi baru"));
    }

    private void showCampusCard() {
        selectedKos = null; // Mark that it's campus, not a specific kos
        routeCard.setVisibility(View.VISIBLE);
        routeCard.setAlpha(0f);
        routeCard.animate().alpha(1f).setDuration(300).start();

        imgKosCard.setImageResource(R.drawable.ic_map_school);
        tvKosName.setText("UNS Kampus 6 Kebumen");
        tvKosAddress.setText("Jl. Pendidikan No. 5, Panjer");
        tvDistance.setText("Pusat Pendidikan PSDKU");
        tvPrice.setText("Fasilitas Publik");
        if (tvPricePeriod != null) tvPricePeriod.setVisibility(View.GONE);
        btnViewDetail.setText("Lihat Info Kampus");
    }

    private void setupSearchAutoComplete() {
        if (allKosList == null || allKosList.isEmpty()) return;
        List<String> names = allKosList.stream().map(KosItem::getName).collect(Collectors.toList());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        etSearchLocation.setAdapter(adapter);
    }

    private void setupMap() {
        // Now handled via getMapAsync and onMapReady
    }

    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        this.googleMap = gMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Tambahkan marker kampus
        LatLng campusLatLng = new LatLng(KosLocationUtils.CAMPUS_LAT, KosLocationUtils.CAMPUS_LNG);
        googleMap.addMarker(new MarkerOptions()
                .position(campusLatLng)
                .title(KosLocationUtils.CAMPUS_NAME)
                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE))
        );

        // Pindahkan kamera ke kampus atau kos spesifik jika ada
        double targetLat = getIntent().getDoubleExtra("kos_lat", -1);
        double targetLng = getIntent().getDoubleExtra("kos_lng", -1);

        if (targetLat != -1 && targetLng != -1) {
            LatLng targetLatLng = new LatLng(targetLat, targetLng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 17f));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLatLng, 15f));
        }

        // Load all markers
        loadKosMarkers();

        // Marker Click Listener
        googleMap.setOnMarkerClickListener(marker -> {
            String markerId = marker.getId();
            KosItem item = markerKosMap.get(markerId);
            if (item != null) {
                selectedKos = item;
                updatePropertyCard(item);
                
                // Fetch dynamic duration
                tvDistance.setText("Menghitung...");
                KosLocationUtils.fetchWalkingDuration(this, item.getLatitude(), item.getLongitude(), new KosLocationUtils.DurationCallback() {
                    @Override
                    public void onSuccess(String durationText, int durationMinutes, String distanceText) {
                        tvDistance.setText(distanceText + " • " + durationText + " ke kampus");
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        tvDistance.setText(errorMessage); // "Estimasi belum tersedia"
                    }
                });
                return true; // We handled the click
            } else if (KosLocationUtils.CAMPUS_NAME.equals(marker.getTitle())) {
                showCampusCard();
                return true;
            }
            return false;
        });
    }

    private void loadKosMarkers() {
        if (kosListener != null) {
            kosListener.remove();
        }

        kosListener = KosRepository.getInstance().listenAllKosItems(new KosRepository.KosItemListCallback() {
            @Override
            public void onSuccess(List<KosItem> items) {
                allKosList = items;
                NavigationHelper.cachedKosList = new ArrayList<>(items);
                currentFilteredList = new ArrayList<>(items);
                
                // Jika sedang ada kos yang terpilih di card, pastikan datanya sinkron
                if (selectedKos != null) {
                    for (KosItem item : items) {
                        if (item.getId().equals(selectedKos.getId())) {
                            selectedKos = item;
                            // Update UI card jika sedang terlihat
                            if (routeCard != null && routeCard.getVisibility() == View.VISIBLE) {
                                updatePropertyCard(item);
                            }
                            break;
                        }
                    }
                }

                addKosMarkers();
                
                if (isInitialLoad) {
                    highlightTargetKos();
                    isInitialLoad = false;
                }
            }

            @Override
            public void onError(String message) {
                showCustomToast("Gagal memuat data kos: " + message);
            }
        });
    }

    private void highlightTargetKos() {
        double targetLat = getIntent().getDoubleExtra("kos_lat", -1);
        double targetLng = getIntent().getDoubleExtra("kos_lng", -1);

        if (targetLat != -1 && targetLng != -1) {
            for (Map.Entry<String, KosItem> entry : markerKosMap.entrySet()) {
                KosItem item = entry.getValue();
                if (Math.abs(item.getLatitude() - targetLat) < 0.0001 &&
                        Math.abs(item.getLongitude() - targetLng) < 0.0001) {
                    
                    selectedKos = item;
                    updatePropertyCard(item);
                    
                    // Fetch dynamic duration
                    tvDistance.setText("Menghitung...");
                    KosLocationUtils.fetchWalkingDuration(this, item.getLatitude(), item.getLongitude(), new KosLocationUtils.DurationCallback() {
                        @Override
                        public void onSuccess(String durationText, int durationMinutes, String distanceText) {
                            tvDistance.setText(distanceText + " • " + durationText + " ke kampus");
                        }
                        @Override
                        public void onFailure(String errorMessage) {
                            tvDistance.setText(errorMessage); // "Estimasi belum tersedia"
                        }
                    });
                    break;
                }
            }
        }
    }

    private void addKosMarkers() {
        if (googleMap == null) return;
        
        // Simpan marker saat ini atau clear
        googleMap.clear();
        markerKosMap.clear();

        // Re-add campus marker
        LatLng campusLatLng = new LatLng(KosLocationUtils.CAMPUS_LAT, KosLocationUtils.CAMPUS_LNG);
        googleMap.addMarker(new MarkerOptions()
                .position(campusLatLng)
                .title(KosLocationUtils.CAMPUS_NAME)
                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE))
        );

        for (KosItem item : currentFilteredList) {
            LatLng pos = new LatLng(item.getLatitude(), item.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(item.getName())
                    .snippet(item.getPrice())
            );
            if (marker != null) {
                markerKosMap.put(marker.getId(), item);
            }
        }
    }

    private void showMapFallback(FrameLayout container) {
        // Not used with Google Maps SDK
    }

    private void updatePropertyCard(KosItem item) {
        selectedKos = item;
        routeCard.setVisibility(View.VISIBLE);
        routeCard.setAlpha(0f);
        routeCard.animate().alpha(1f).setDuration(300).start();

        // Image (Optimized via Cloudinary) - Consistent with KosAdapter
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            String optimizedUrl = CloudinaryRepository.getInstance().getOptimizedUrl(item.getImageUrl(), 300, 200, false);
            Glide.with(this)
                    .load(optimizedUrl)
                    .placeholder(R.drawable.bg_map_placeholder)
                    .error(R.drawable.bg_map_placeholder)
                    .into(imgKosCard);
        } else {
            imgKosCard.setImageResource(item.getImageRes());
        }

        tvKosName.setText(item.getName());
        tvKosAddress.setText(item.getAddress());
        tvDistance.setText(item.getDistance() + " ke Kampus");
        tvPrice.setText(item.getPrice());
        if (tvPricePeriod != null) tvPricePeriod.setVisibility(View.VISIBLE);
        btnViewDetail.setText("Lihat Detail");
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            showCustomToast("Ketik lokasi atau nama kos terlebih dahulu");
            return;
        }

        if (allKosList == null) return;

        for (KosItem item : allKosList) {
            if (item.getName().toLowerCase().contains(query.toLowerCase().trim())) {
                updatePropertyCard(item);
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(item.getLatitude(), item.getLongitude()), 17.0f));
                }
                return;
            }
        }
        showCustomToast("🔍 Lokasi tidak ditemukan: " + query);
    }

    private String selectedFilterCategory = ""; // Empty means all

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_filter, null);
        dialog.setContentView(sheetView);

        TextView chipPutra = sheetView.findViewById(R.id.chipFilterPutra);
        TextView chipPutri = sheetView.findViewById(R.id.chipFilterPutri);
        TextView chipCampur = sheetView.findViewById(R.id.chipFilterCampur);
        RangeSlider priceSlider = sheetView.findViewById(R.id.rangeSliderHarga);

        // Set initial UI state
        updateFilterChipsUI(chipPutra, chipPutri, chipCampur);

        chipPutra.setOnClickListener(v -> {
            selectedFilterCategory = selectedFilterCategory.equals("Putra") ? "" : "Putra";
            updateFilterChipsUI(chipPutra, chipPutri, chipCampur);
        });
        chipPutri.setOnClickListener(v -> {
            selectedFilterCategory = selectedFilterCategory.equals("Putri") ? "" : "Putri";
            updateFilterChipsUI(chipPutra, chipPutri, chipCampur);
        });
        chipCampur.setOnClickListener(v -> {
            selectedFilterCategory = selectedFilterCategory.equals("Campur") ? "" : "Campur";
            updateFilterChipsUI(chipPutra, chipPutri, chipCampur);
        });

        sheetView.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            if (allKosList == null) return;

            List<Float> prices = priceSlider.getValues();
            float minPrice = prices.get(0);
            float maxPrice = prices.get(1);

            filterKosListForMap(selectedFilterCategory, minPrice, maxPrice);
            addKosMarkers();
            zoomToFitMarkers(currentFilteredList);
            
            dialog.dismiss();
            showCustomToast("✅ Filter diterapkan");
        });

        sheetView.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            if (allKosList == null) return;
            selectedFilterCategory = "";
            currentFilteredList = new ArrayList<>(allKosList);
            
            addKosMarkers();
            if (googleMap != null) {
                LatLng campusLatLng = new LatLng(KosLocationUtils.CAMPUS_LAT, KosLocationUtils.CAMPUS_LNG);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campusLatLng, 15.5f));
            }
            
            dialog.dismiss();
            showCustomToast("🔄 Filter direset");
        });

        sheetView.findViewById(R.id.btnCloseFilter).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateFilterChipsUI(TextView putra, TextView putri, TextView campur) {
        putra.setBackgroundResource(selectedFilterCategory.equals("Putra") ? R.drawable.bg_quick_chip_active : R.drawable.bg_quick_chip_inactive);
        putra.setTextColor(ContextCompat.getColor(this, selectedFilterCategory.equals("Putra") ? R.color.text_white : R.color.home_text_secondary));

        putri.setBackgroundResource(selectedFilterCategory.equals("Putri") ? R.drawable.bg_quick_chip_active : R.drawable.bg_quick_chip_inactive);
        putri.setTextColor(ContextCompat.getColor(this, selectedFilterCategory.equals("Putri") ? R.color.text_white : R.color.home_text_secondary));

        campur.setBackgroundResource(selectedFilterCategory.equals("Campur") ? R.drawable.bg_quick_chip_active : R.drawable.bg_quick_chip_inactive);
        campur.setTextColor(ContextCompat.getColor(this, selectedFilterCategory.equals("Campur") ? R.color.text_white : R.color.home_text_secondary));
    }

    private void filterKosListForMap(String category, float minPrice, float maxPrice) {
        currentFilteredList = allKosList.stream()
            .filter(item -> {
                boolean matchCategory = category.isEmpty() || item.getCategory().equalsIgnoreCase(category);
                
                // Parse price string to long (e.g., "Rp 850.000" -> 850000)
                long priceVal = parsePrice(item.getPrice());
                boolean matchPrice = priceVal >= minPrice && priceVal <= maxPrice;
                
                return matchCategory && matchPrice;
            })
            .collect(Collectors.toList());

        addKosMarkers();
        zoomToFitMarkers(currentFilteredList);
    }

    private long parsePrice(String priceStr) {
        try {
            if (priceStr == null) return 0;
            String clean = priceStr.replaceAll("[^0-9]", "");
            return clean.isEmpty() ? 0 : Long.parseLong(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    private void zoomToFitMarkers(List<KosItem> items) {
        if (googleMap == null || items == null || items.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        // Include Campus
        builder.include(new LatLng(KosLocationUtils.CAMPUS_LAT, KosLocationUtils.CAMPUS_LNG));
        
        for (KosItem item : items) {
            builder.include(new LatLng(item.getLatitude(), item.getLongitude()));
        }

        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
    }

    private void startNavigation(KosItem destination) {
        if (destination == null) return;
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (!isGpsEnabled()) {
            showGpsDisabledDialog();
            return;
        }

        openGoogleMapsNavigation(destination.getLatitude(), destination.getLongitude());
    }

    private void openGoogleMapsNavigation(double kosLat, double kosLng) {
        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + kosLat + "," + kosLng + "&destination=" + KosLocationUtils.CAMPUS_LAT + "," + KosLocationUtils.CAMPUS_LNG + "&travelmode=walking");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser
            startActivity(new Intent(Intent.ACTION_VIEW, gmmIntentUri));
        }
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void showGpsDisabledDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS Tidak Aktif")
                .setMessage("Silakan aktifkan GPS untuk menggunakan fitur navigasi.")
                .setPositiveButton("Pengaturan", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (selectedKos != null) startNavigation(selectedKos);
            } else {
                showCustomToast("Izin lokasi diperlukan untuk navigasi.");
            }
        }
    }

    private void navigateToPropertyDetail() {
        if (selectedKos == null) {
            showCustomToast("🏫 Info Kampus UNS 6 segera hadir!");
            return;
        }
        Intent intent = new Intent(this, PropertyDetailBookingActivity.class);
        intent.putExtra("kos_item", selectedKos);
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
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

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        if (kosListener != null) {
            kosListener.remove();
        }
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_map_custom, null);
        TextView tvToast = layout.findViewById(R.id.tvToastMessage);
        tvToast.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 250);
        toast.show();
    }
}
