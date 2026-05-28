package com.koshub.psdku;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.RenderedQueryGeometry;
import com.mapbox.maps.RenderedQueryOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver;

import com.koshub.psdku.repositories.CloudinaryRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.slider.RangeSlider;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.CameraBoundsOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapViewRouteNavigationActivity extends AppCompatActivity {

    private MapView mapView;
    private List<KosItem> allKosList;
    private List<KosItem> currentFilteredList;
    private KosItem selectedKos;
    private FusedLocationProviderClient fusedLocationClient;
    private MapboxNavigation mapboxNavigation;

    // UI Components
    private AutoCompleteTextView etSearchLocation;
    private FrameLayout btnSearch;
    private LinearLayout routeCard;
    private ImageView imgKosCard;
    private TextView tvKosName, tvKosAddress, tvDistance, tvPrice, btnViewDetail, btnNavigate;
    private LinearLayout navHome, navWaitingList, navProfile;
    private FrameLayout btnNotification;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final String KOS_SOURCE_ID = "kos-source";
    private static final String KOS_LAYER_ID = "kos-layer";
    private static final String CAMPUS_SOURCE_ID = "campus-source";
    private static final String CAMPUS_LAYER_ID = "campus-layer";
    private static final String KOS_ICON_ID = "kos-icon";
    private static final String CAMPUS_ICON_ID = "campus-icon";

    // Coordinates for UNS Kampus 6 PGSD Kebumen
    private static final double CAMPUS_LAT = -7.68307;
    private static final double CAMPUS_LNG = 109.6645;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initNavigation();
    }

    private void initNavigation() {
        if (!MapboxNavigationApp.isSetup()) {
            NavigationOptions navigationOptions = new NavigationOptions.Builder(this)
                    .build();
            MapboxNavigationApp.setup(navigationOptions);
        }

        MapboxNavigationApp.registerObserver(new MapboxNavigationObserver() {
            @Override
            public void onAttached(@NonNull MapboxNavigation mapboxNavigation) {
                MapViewRouteNavigationActivity.this.mapboxNavigation = mapboxNavigation;
            }

            @Override
            public void onDetached(@NonNull MapboxNavigation mapboxNavigation) {
                MapViewRouteNavigationActivity.this.mapboxNavigation = null;
            }
        });
    }

    private void initViews() {
        // MapView is now handled programmatically in setupMap()
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

        // Map Click Listener will be added in setupMap() after initialization
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

        if (!MapboxTokenHelper.hasValidMapboxToken(this)) {
            showMapFallback(mapContainer);
            return;
        }

        try {
            mapView = new MapView(this);
            mapContainer.addView(mapView);

            mapView.getMapboxMap().loadStyle(Style.STANDARD, style -> {
                runOnUiThread(() -> {
                    try {
                        addMapResources(style);
                        addMapLayers(style);

                        Point campusPoint = Point.fromLngLat(CAMPUS_LNG, CAMPUS_LAT);
                        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                                .center(campusPoint)
                                .zoom(15.5)
                                .build());
                        
                        setupMapClickListener();
                    } catch (Exception e) {
                        android.util.Log.e("MapboxSafety", "Error in style loaded callback", e);
                    }
                });
            });
        } catch (Exception e) {
            android.util.Log.e("MapboxSafety", "Failed to initialize MapView", e);
            showMapFallback(mapContainer);
        }
    }

    private void setupMapClickListener() {
        if (mapView == null) return;
        
        GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
            RenderedQueryGeometry geometry = new RenderedQueryGeometry(mapView.getMapboxMap().pixelForCoordinate(point));
            RenderedQueryOptions options = new RenderedQueryOptions(Arrays.asList(KOS_LAYER_ID, CAMPUS_LAYER_ID), null);

            mapView.getMapboxMap().queryRenderedFeatures(geometry, options, expected -> {
                if (expected.isValue() && !expected.getValue().isEmpty()) {
                    Feature feature = expected.getValue().get(0).getQueriedFeature().getFeature();
                    String layerId = expected.getValue().get(0).getLayers().get(0);

                    if (KOS_LAYER_ID.equals(layerId)) {
                        com.google.gson.JsonElement indexProp = feature.getProperty("index");
                        if (indexProp != null && !indexProp.isJsonNull()) {
                            int index = indexProp.getAsInt();
                            if (allKosList != null && index >= 0 && index < allKosList.size()) {
                                runOnUiThread(() -> updatePropertyCard(allKosList.get(index)));
                            }
                        }
                    } else if (CAMPUS_LAYER_ID.equals(layerId)) {
                        runOnUiThread(this::showCampusCard);
                    }
                } else {
                    runOnUiThread(() -> routeCard.setVisibility(View.GONE));
                }
            });
            return true;
        });
    }

    private void showMapFallback(FrameLayout container) {
        if (container == null) return;
        container.removeAllViews();
        View fallbackView = LayoutInflater.from(this).inflate(R.layout.layout_map_fallback, container, false);
        container.addView(fallbackView);
    }

    private void addMapResources(Style style) {
        // Add Icons
        style.addImage(KOS_ICON_ID, drawableToBitmap(ContextCompat.getDrawable(this, R.drawable.ic_map_home)));
        style.addImage(CAMPUS_ICON_ID, drawableToBitmap(ContextCompat.getDrawable(this, R.drawable.ic_map_school)));

        updateMapData(style);

        // Add Campus Source
        Feature campusFeature = Feature.fromGeometry(Point.fromLngLat(CAMPUS_LNG, CAMPUS_LAT));
        String json = "{\"type\": \"geojson\", \"data\": " + campusFeature.toJson() + "}";
        mapView.getMapboxMap().addStyleSource(CAMPUS_SOURCE_ID, com.mapbox.bindgen.Value.fromJson(json).getValue());
    }

    private void updateMapData(Style style) {
        List<Feature> kosFeatures = new ArrayList<>();
        if (allKosList != null && !allKosList.isEmpty()) {
            for (int i = 0; i < allKosList.size(); i++) {
                KosItem item = allKosList.get(i);
                if (currentFilteredList != null && currentFilteredList.contains(item)) {
                    Feature feature = Feature.fromGeometry(Point.fromLngLat(item.getLongitude(), item.getLatitude()));
                    feature.addNumberProperty("index", i);
                    kosFeatures.add(feature);
                }
            }
        }
        
        String geoJson = FeatureCollection.fromFeatures(kosFeatures).toJson();
        String json = "{\"type\": \"geojson\", \"data\": " + geoJson + "}";
        
        // Use a safer way to update or add source
        try {
            style.removeStyleSource(KOS_SOURCE_ID);
        } catch (Exception ignored) {}
        style.addStyleSource(KOS_SOURCE_ID, com.mapbox.bindgen.Value.fromJson(json).getValue());
    }

    private void addMapLayers(Style style) {
        // Kos Layer
        String kosLayerJson = "{\"id\": \"" + KOS_LAYER_ID + "\", \"type\": \"symbol\", \"source\": \"" + KOS_SOURCE_ID + "\", \"layout\": {\"icon-image\": \"" + KOS_ICON_ID + "\", \"icon-size\": 1.0, \"icon-allow-overlap\": true}}";
        mapView.getMapboxMap().addStyleLayer(com.mapbox.bindgen.Value.fromJson(kosLayerJson).getValue(), null);

        // Campus Layer
        String campusLayerJson = "{\"id\": \"" + CAMPUS_LAYER_ID + "\", \"type\": \"symbol\", \"source\": \"" + CAMPUS_SOURCE_ID + "\", \"layout\": {\"icon-image\": \"" + CAMPUS_ICON_ID + "\", \"icon-size\": 1.3, \"icon-allow-overlap\": true}}";
        mapView.getMapboxMap().addStyleLayer(com.mapbox.bindgen.Value.fromJson(campusLayerJson).getValue(), null);
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
                    mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                            .center(Point.fromLngLat(item.getLongitude(), item.getLatitude()))
                            .zoom(17.0)
                            .build());
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

            if (mapView != null) {
                mapView.getMapboxMap().getStyle(style -> {
                    updateMapData(style);
                    zoomToFitMarkers(currentFilteredList);
                });
            }
            
            dialog.dismiss();
            showCustomToast("✅ Filter diterapkan");
        });

        sheetView.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            if (allKosList == null) return;
            selectedFilterCategory = "";
            currentFilteredList = new ArrayList<>(allKosList);
            if (mapView != null) {
                mapView.getMapboxMap().getStyle(style -> {
                    updateMapData(style);
                    // Reset to campus view
                    Point campusPoint = Point.fromLngLat(CAMPUS_LNG, CAMPUS_LAT);
                    mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                            .center(campusPoint)
                            .zoom(15.5)
                            .build());
                });
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

        List<Point> points = items.stream()
                .map(item -> Point.fromLngLat(item.getLongitude(), item.getLatitude()))
                .collect(Collectors.toList());
        
        // Add campus to bounds to keep context
        points.add(Point.fromLngLat(CAMPUS_LNG, CAMPUS_LAT));

        CameraOptions cameraOptions = mapView.getMapboxMap().cameraForCoordinates(
                points,
                new EdgeInsets(200.0, 100.0, 200.0, 100.0),
                null,
                null
        );
        
        mapView.getMapboxMap().setCamera(cameraOptions);
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

        showCustomToast("Menyiapkan navigasi...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                Point dest = Point.fromLngLat(destination.getLongitude(), destination.getLatitude());
                
                if (mapboxNavigation != null) {
                    requestMapboxRoute(origin, dest);
                } else {
                    openGoogleMapsNavigation(destination.getLatitude(), destination.getLongitude());
                }
            } else {
                showCustomToast("Gagal mendapatkan lokasi. Pastikan GPS aktif.");
                openGoogleMapsNavigation(destination.getLatitude(), destination.getLongitude());
            }
        });
    }

    private void requestMapboxRoute(Point origin, Point destination) {
        RouteOptions routeOptions = RouteOptions.builder()
                .coordinatesList(Arrays.asList(origin, destination))
                .build();

        mapboxNavigation.requestRoutes(routeOptions, new NavigationRouterCallback() {
            @Override
            public void onRoutesReady(@NonNull List<NavigationRoute> routes, @NonNull String routerOrigin) {
                if (!routes.isEmpty()) {
                    showCustomToast("Rute ditemukan. Memulai navigasi...");
                    openGoogleMapsNavigation(destination.latitude(), destination.longitude());
                }
            }

            @Override
            public void onFailure(@NonNull List<RouterFailure> failures, @NonNull RouteOptions routeOptions) {
                showCustomToast("Mapbox routing gagal, membuka Google Maps...");
                openGoogleMapsNavigation(destination.latitude(), destination.longitude());
            }

            @Override
            public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull String routerOrigin) {
            }
        });
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

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
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
