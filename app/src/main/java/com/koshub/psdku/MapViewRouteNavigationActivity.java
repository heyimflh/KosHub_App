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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.RenderedQueryGeometry;
import com.mapbox.maps.RenderedQueryOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.GesturesUtils;

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

    // UI Components
    private AutoCompleteTextView etSearchLocation;
    private FrameLayout btnSearch;
    private LinearLayout routeCard;
    private ImageView imgKosCard;
    private TextView tvKosName, tvKosAddress, tvDistance, tvPrice, btnViewDetail;
    private LinearLayout navHome, navWaitingList, navProfile;
    private FrameLayout btnNotification;

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
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        etSearchLocation = findViewById(R.id.etSearchLocation);
        btnSearch = findViewById(R.id.btnSearch);
        routeCard = findViewById(R.id.routeCard);
        imgKosCard = findViewById(R.id.imgKosCard);
        tvKosName = findViewById(R.id.tvKosName);
        tvKosAddress = findViewById(R.id.tvKosAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvPrice = findViewById(R.id.tvPrice);
        btnViewDetail = findViewById(R.id.btnViewDetail);
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
        
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.MAP);
        btnNotification.setOnClickListener(v -> showCustomToast("🔔 Tidak ada notifikasi baru"));

        // Map Click Listener
        GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
            RenderedQueryGeometry geometry = new RenderedQueryGeometry(mapView.getMapboxMap().pixelForCoordinate(point));
            RenderedQueryOptions options = new RenderedQueryOptions(Arrays.asList(KOS_LAYER_ID, CAMPUS_LAYER_ID), null);

            mapView.getMapboxMap().queryRenderedFeatures(geometry, options, expected -> {
                if (expected.isValue() && !expected.getValue().isEmpty()) {
                    Feature feature = expected.getValue().get(0).getQueriedFeature().getFeature();
                    String layerId = expected.getValue().get(0).getLayers().get(0);

                    if (KOS_LAYER_ID.equals(layerId)) {
                        int index = feature.getNumberProperty("index").intValue();
                        runOnUiThread(() -> updatePropertyCard(allKosList.get(index)));
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
        mapView.getMapboxMap().loadStyle(Style.STANDARD, style -> {
            addMapResources(style);
            addMapLayers(style);

            Point campusPoint = Point.fromLngLat(CAMPUS_LNG, CAMPUS_LAT);
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(campusPoint)
                    .zoom(15.5)
                    .build());
        });
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
        if (allKosList == null || allKosList.isEmpty()) return;
        List<Feature> kosFeatures = new ArrayList<>();
        for (int i = 0; i < allKosList.size(); i++) {
            KosItem item = allKosList.get(i);
            if (currentFilteredList.contains(item)) {
                Feature feature = Feature.fromGeometry(Point.fromLngLat(item.getLongitude(), item.getLatitude()));
                feature.addNumberProperty("index", i);
                kosFeatures.add(feature);
            }
        }
        
        String geoJson = FeatureCollection.fromFeatures(kosFeatures).toJson();
        String json = "{\"type\": \"geojson\", \"data\": " + geoJson + "}";
        mapView.getMapboxMap().addStyleSource(KOS_SOURCE_ID, com.mapbox.bindgen.Value.fromJson(json).getValue());
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

        imgKosCard.setImageResource(item.getImageRes());
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

        for (KosItem item : allKosList) {
            if (item.getName().toLowerCase().contains(query.toLowerCase().trim())) {
                updatePropertyCard(item);
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(Point.fromLngLat(item.getLongitude(), item.getLatitude()))
                        .zoom(17.0)
                        .build());
                return;
            }
        }
        showCustomToast("🔍 Lokasi tidak ditemukan: " + query);
    }

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_filter, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            // Real filter logic: filter by category "Putri" if a checkbox is selected (dummy checkbox check)
            // For now, let's just simulate a filter by taking half the list
            currentFilteredList = allKosList.stream()
                .filter(item -> item.getCategory().equals("Putri"))
                .collect(Collectors.toList());

            mapView.getMapboxMap().getStyle(style -> {
                // Remove old source before adding new one with filtered data
                // In Mapbox v11, we update data via Source update or re-adding
                updateMapData(style);
            });
            
            dialog.dismiss();
            showCustomToast("✅ Filter diterapkan: Menampilkan Kos Putri");
        });

        sheetView.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            currentFilteredList = new ArrayList<>(allKosList);
            mapView.getMapboxMap().getStyle(this::updateMapData);
            dialog.dismiss();
            showCustomToast("🔄 Filter direset: Menampilkan Semua");
        });

        sheetView.findViewById(R.id.btnCloseFilter).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void navigateToPropertyDetail() {
        if (selectedKos == null) {
            showCustomToast("🏫 Info Kampus UNS 6 segera hadir!");
            return;
        }
        Intent intent = new Intent(this, PropertyDetailBookingActivity.class);
        intent.putExtra("kos_item", selectedKos);
        startActivity(intent);
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
