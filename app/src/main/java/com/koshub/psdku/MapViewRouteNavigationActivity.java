package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
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
import android.net.Uri;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.Manifest;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.slider.RangeSlider;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.util.BoundingBox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MapViewRouteNavigationActivity extends AppCompatActivity {

    private MapView mapView;
    private List<KosItem> allKosList;
    private List<KosItem> currentFilteredList;
    private KosItem selectedKos;

    // UI Components
    private AutoCompleteTextView etSearchLocation;
    private FrameLayout btnSearch;
    private LinearLayout routeCard;
    private ImageView imgKosCard;
    private TextView tvKosName, tvKosAddress, tvDistance, tvPrice, btnViewDetail, btnNavigate;
    private LinearLayout navHome, navWaitingList, navProfile;
    private FrameLayout btnNotification;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Coordinates for UNS Kampus 6 PGSD Kebumen
    private static final double CAMPUS_LAT = -7.68307;
    private static final double CAMPUS_LNG = 109.6645;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_map_view_route_navigation);

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
        btnViewDetail = findViewById(R.id.btnViewDetail);
        btnNavigate = findViewById(R.id.btnNavigate);
        navHome = findViewById(R.id.navHome);
        navWaitingList = findViewById(R.id.navWaitlist);
        navProfile = findViewById(R.id.navProfile);
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
        btnViewDetail.setText("Lihat Info Kampus");
    }

    private void setupSearchAutoComplete() {
        if (allKosList == null || allKosList.isEmpty()) return;
        List<String> names = allKosList.stream().map(KosItem::getName).collect(Collectors.toList());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        etSearchLocation.setAdapter(adapter);
    }

    private void setupMap() {
        FrameLayout mapContainer = findViewById(R.id.mapContainer);
        if (mapContainer == null) return;

        try {
            mapView = new MapView(this);
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            
            IMapController mapController = mapView.getController();
            mapController.setZoom(15.5);
            GeoPoint startPoint = new GeoPoint(CAMPUS_LAT, CAMPUS_LNG);
            mapController.setCenter(startPoint);

            mapContainer.addView(mapView);
            
            addKosMarkers();
        } catch (Exception e) {
            android.util.Log.e("OSMDroid", "Failed to initialize MapView", e);
            showMapFallback(mapContainer);
        }
    }

    private void addKosMarkers() {
        if (mapView == null) return;
        
        mapView.getOverlays().clear();

        // Add Campus Marker
        Marker campusMarker = new Marker(mapView);
        campusMarker.setPosition(new GeoPoint(CAMPUS_LAT, CAMPUS_LNG));
        campusMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        campusMarker.setTitle("UNS Kampus 6");
        campusMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_school));
        campusMarker.setOnMarkerClickListener((marker, mv) -> {
            showCampusCard();
            return true;
        });
        mapView.getOverlays().add(campusMarker);

        // Add Kos Markers
        if (currentFilteredList != null) {
            for (KosItem item : currentFilteredList) {
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(item.getLatitude(), item.getLongitude()));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(item.getName());
                marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_home));
                marker.setOnMarkerClickListener((m, mv) -> {
                    updatePropertyCard(item);
                    return true;
                });
                mapView.getOverlays().add(marker);
            }
        }
        
        mapView.invalidate();
    }

    private void showMapFallback(FrameLayout container) {
        if (container == null) return;
        container.removeAllViews();
        View fallbackView = LayoutInflater.from(this).inflate(R.layout.layout_map_fallback, container, false);
        container.addView(fallbackView);
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
                if (mapView != null) {
                    mapView.getController().animateTo(new GeoPoint(item.getLatitude(), item.getLongitude()));
                    mapView.getController().setZoom(17.0);
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
            if (mapView != null) {
                mapView.getController().animateTo(new GeoPoint(CAMPUS_LAT, CAMPUS_LNG));
                mapView.getController().setZoom(15.5);
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
        if (items == null || items.isEmpty() || mapView == null) return;

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        // Include campus
        minLat = Math.min(minLat, CAMPUS_LAT);
        maxLat = Math.max(maxLat, CAMPUS_LAT);
        minLng = Math.min(minLng, CAMPUS_LNG);
        maxLng = Math.max(maxLng, CAMPUS_LNG);

        for (KosItem item : items) {
            minLat = Math.min(minLat, item.getLatitude());
            maxLat = Math.max(maxLat, item.getLatitude());
            minLng = Math.min(minLng, item.getLongitude());
            maxLng = Math.max(maxLng, item.getLongitude());
        }

        BoundingBox boundingBox = new BoundingBox(maxLat, maxLng, minLat, minLng);
        mapView.zoomToBoundingBox(boundingBox, true, 100);
    }

    private void startNavigation(KosItem destination) {
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

    private void openGoogleMapsNavigation(double lat, double lng) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            showCustomToast("Google Maps tidak ditemukan.");
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
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
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
