package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.repositories.FCMTokenRepository;
import com.koshub.psdku.repositories.FavoriteRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.utils.NotificationHelper;
import com.koshub.psdku.utils.NotificationPermissionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StudentHomeActivity extends AppCompatActivity implements KosAdapter.OnKosClickListener {

    private RecyclerView rvKosList;
    private KosAdapter adapter;
    private TextView tvResultCount;
    private View layoutEmptyState;
    private View layoutErrorState;
    private List<KosItem> allKosList = new ArrayList<>();
    private List<KosItem> filteredList = new ArrayList<>();
    private EditText etSearch;
    private ProgressBar progressBar;
    private KosRepository kosRepository;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        kosRepository = KosRepository.getInstance();
        progressBar = findViewById(R.id.progressBar);

        // Handle window insets for status bar
        View navbar = findViewById(R.id.navbar);
        if (navbar != null) {
            int initialPaddingTop = navbar.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(navbar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + initialPaddingTop, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Handle window insets for bottom navigation
        View bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            int initialPaddingBottom = bottomNav.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + initialPaddingBottom);
                return insets;
            });
        }

        setupViews();
        initData();
        NavigationHelper.cachedKosList = allKosList;
        setupSearch();
        setupQuickChips();
        setupToggle();
        setupBottomNav();
        
        // Notification & Permissions
        NotificationHelper.createNotificationChannels(this);
        NotificationPermissionHelper.askNotificationPermission(this);
        FCMTokenRepository.getInstance().saveCurrentToken();
        setupNotificationBadge();

        // Initial count update
        updateResultCount();
    }

    private void setupNotificationBadge() {
        TextView tvBadge = findViewById(R.id.tvNotifBadge);
        if (tvBadge == null) return;
        
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
                Log.e("KosHubNotification", "Badge error: " + message);
            }
        });
    }

    private void initData() {
        setLoading(true);
        if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
        
        kosRepository.getAllKosItems(new KosRepository.KosItemListCallback() {
            @Override
            public void onSuccess(List<KosItem> items) {
                setLoading(false);
                if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
                
                allKosList.clear();
                allKosList.addAll(items);
                filteredList.clear();
                filteredList.addAll(items);
                NavigationHelper.cachedKosList = new ArrayList<>(allKosList);
                if (adapter != null) adapter.notifyDataSetChanged();
                updateResultCount();
                
                if (allKosList.isEmpty()) {
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                } else {
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                if (layoutErrorState != null) {
                    layoutErrorState.setVisibility(View.VISIBLE);
                    View btnRetry = layoutErrorState.findViewById(R.id.btnRetry);
                    if (btnRetry != null) btnRetry.setOnClickListener(v -> initData());
                }
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);

                showToast("Gagal memuat data: " + message);
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (rvKosList != null) rvKosList.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void setupViews() {
        rvKosList = findViewById(R.id.rvKosList);
        tvResultCount = findViewById(R.id.tvResultCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyStateHome);
        layoutErrorState = findViewById(R.id.layoutErrorState);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);

        if (rvKosList != null) {
            rvKosList.setLayoutManager(new LinearLayoutManager(this));
            adapter = new KosAdapter(filteredList, this);
            rvKosList.setAdapter(adapter);
        }

        // Notification button
        findViewById(R.id.btnNotification).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationActivity.class);
            NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
        });
    }

    private void setupSearch() {
        etSearch = findViewById(R.id.etSearch);
        tvResultCount = findViewById(R.id.tvResultCount);

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    filterBySearch(s.toString());
                }
            });
        }

        // Filter button
        View btnFilter = findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterSheet());
        }
    }

    private void filterBySearch(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allKosList);
        } else {
            String q = query.toLowerCase();
            for (KosItem item : allKosList) {
                String searchable = (item.getName() + " " + item.getAddress() + " " +
                        item.getCategory() + " " + item.getFacilities().toString()).toLowerCase();
                if (searchable.contains(q)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateResultCount();
    }

    private void setupQuickChips() {
        TextView chipSemua = findViewById(R.id.chipSemua);
        TextView chipPutra = findViewById(R.id.chipPutra);
        TextView chipPutri = findViewById(R.id.chipPutri);
        TextView chipCampur = findViewById(R.id.chipCampur);

        View.OnClickListener chipListener = v -> {
            // Reset all chips
            chipSemua.setBackgroundResource(R.drawable.bg_quick_chip_inactive);
            chipSemua.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));
            chipPutra.setBackgroundResource(R.drawable.bg_quick_chip_inactive);
            chipPutra.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));
            chipPutri.setBackgroundResource(R.drawable.bg_quick_chip_inactive);
            chipPutri.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));
            chipCampur.setBackgroundResource(R.drawable.bg_quick_chip_inactive);
            chipCampur.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));

            // Set active chip
            v.setBackgroundResource(R.drawable.bg_quick_chip_active);
            ((TextView) v).setTextColor(ContextCompat.getColor(this, R.color.text_white));

            // Filter logic
            String category = "";
            if (v.getId() == R.id.chipPutra) category = "Putra";
            else if (v.getId() == R.id.chipPutri) category = "Putri";
            else if (v.getId() == R.id.chipCampur) category = "Campur";

            filterByCategory(category);
        };

        chipSemua.setOnClickListener(chipListener);
        chipPutra.setOnClickListener(chipListener);
        chipPutri.setOnClickListener(chipListener);
        chipCampur.setOnClickListener(chipListener);
    }

    private void filterByCategory(String category) {
        filteredList.clear();
        if (category.isEmpty()) {
            filteredList.addAll(allKosList);
        } else {
            for (KosItem item : allKosList) {
                if (item.getCategory().equalsIgnoreCase(category)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateResultCount();
    }

    private void setupToggle() {
        TextView btnFeed = findViewById(R.id.btnFeedView);
        TextView btnMap = findViewById(R.id.btnMapView);

        if (btnFeed != null && btnMap != null) {
            btnFeed.setOnClickListener(v -> {
                btnFeed.setBackgroundResource(R.drawable.bg_toggle_active);
                btnFeed.setTextColor(ContextCompat.getColor(this, R.color.text_white));
                btnMap.setBackgroundResource(R.drawable.bg_toggle_inactive);
                btnMap.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));
                rvKosList.setVisibility(View.VISIBLE);
            });

            btnMap.setOnClickListener(v -> {
                btnMap.setBackgroundResource(R.drawable.bg_toggle_active);
                btnMap.setTextColor(ContextCompat.getColor(this, R.color.text_white));
                btnFeed.setBackgroundResource(R.drawable.bg_toggle_inactive);
                btnFeed.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));

                try {
                    Intent intent = new Intent(this, MapViewRouteNavigationActivity.class);
                    // Ensure we pass a serializable ArrayList
                    intent.putExtra("kos_list", new ArrayList<>(allKosList));
                    NavigationTransitionHelper.navigateMainWithIntent(this, intent);
                } catch (Exception e) {
                    showToast("Gagal membuka peta: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    private void setupBottomNav() {
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.HOME);
    }

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_filter, null);
        dialog.setContentView(sheetView);

        RangeSlider rangeSlider = sheetView.findViewById(R.id.rangeSliderHarga);
        TextView tvHargaMin = sheetView.findViewById(R.id.tvHargaMin);
        TextView tvHargaMax = sheetView.findViewById(R.id.tvHargaMax);

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            tvHargaMin.setText(String.format(Locale.GERMANY, "Rp %,.0f", values.get(0)).replace(',', '.'));
            tvHargaMax.setText(String.format(Locale.GERMANY, "Rp %,.0f", values.get(1)).replace(',', '.'));
        });

        // Setup Chips
        int[] chipIds = {
                R.id.chipFilterPutra, R.id.chipFilterPutri, R.id.chipFilterCampur,
                R.id.chipFilterWifi, R.id.chipFilterAC, R.id.chipFilterKamarMandi,
                R.id.chipFilterParkir, R.id.chipFilterLaundry
        };

        for (int id : chipIds) {
            TextView chip = sheetView.findViewById(id);
            if (chip != null) {
                chip.setOnClickListener(v -> toggleChipState(chip));
            }
        }

        // Apply filter button
        sheetView.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            List<Float> values = rangeSlider.getValues();
            int minPrice = values.get(0).intValue();
            int maxPrice = values.get(1).intValue();

            String selectedCategory = "";
            if (isChipActive(sheetView, R.id.chipFilterPutra)) selectedCategory = "Putra";
            else if (isChipActive(sheetView, R.id.chipFilterPutri)) selectedCategory = "Putri";
            else if (isChipActive(sheetView, R.id.chipFilterCampur)) selectedCategory = "Campur";

            List<String> selectedFacilities = new ArrayList<>();
            if (isChipActive(sheetView, R.id.chipFilterWifi)) selectedFacilities.add("WiFi");
            if (isChipActive(sheetView, R.id.chipFilterAC)) selectedFacilities.add("AC");
            if (isChipActive(sheetView, R.id.chipFilterKamarMandi)) selectedFacilities.add("K. Mandi Dalam");
            if (isChipActive(sheetView, R.id.chipFilterParkir)) selectedFacilities.add("Parkir");
            if (isChipActive(sheetView, R.id.chipFilterLaundry)) selectedFacilities.add("Laundry");

            filterByAdvanced(minPrice, maxPrice, selectedCategory, selectedFacilities);
            dialog.dismiss();
        });

        // Reset filter button
        sheetView.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            etSearch.setText("");
            filterByCategory("");
            showToast("Filter direset");
            dialog.dismiss();
        });

        // Close button
        sheetView.findViewById(R.id.btnCloseFilter).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void toggleChipState(TextView chip) {
        boolean isActive = chip.getTag() != null && (boolean) chip.getTag();
        isActive = !isActive;
        chip.setTag(isActive);

        if (isActive) {
            chip.setBackgroundResource(R.drawable.bg_quick_chip_active);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_quick_chip_inactive);
            chip.setTextColor(ContextCompat.getColor(this, R.color.home_text_secondary));
        }
    }

    private boolean isChipActive(View parent, int resId) {
        View chip = parent.findViewById(resId);
        return chip != null && chip.getTag() != null && (boolean) chip.getTag();
    }

    private void filterByAdvanced(int minPrice, int maxPrice, String category, List<String> selectedFacilities) {
        filteredList.clear();
        for (KosItem item : allKosList) {
            boolean matchesPrice = item.getPriceValue() >= minPrice && item.getPriceValue() <= maxPrice;
            boolean matchesCategory = category.isEmpty() || item.getCategory().equalsIgnoreCase(category);
            boolean matchesFacilities = true;
            if (!selectedFacilities.isEmpty()) {
                if (item.getFacilities() == null) {
                    matchesFacilities = false;
                } else {
                    for (String facility : selectedFacilities) {
                        if (!item.getFacilities().contains(facility)) {
                            matchesFacilities = false;
                            break;
                        }
                    }
                }
            }

            if (matchesPrice && matchesCategory && matchesFacilities) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        updateResultCount();
        
        if (filteredList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateResultCount() {
        if (tvResultCount != null && filteredList != null) {
            tvResultCount.setText(filteredList.size() + " kos");
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                rvKosList.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }
    }

    public List<KosItem> getAllKosList() {
        return allKosList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Override
    public void onKosClick(KosItem item, int position) {
        Intent intent = new Intent(this, PropertyDetailBookingActivity.class);
        intent.putExtra("kos_item", item);
        intent.putExtra("kos_id", item.getId());
        intent.putExtra("owner_id", item.getOwnerId());
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    @Override
    public void onFavoriteClick(KosItem item, int position) {
        FavoriteRepository.getInstance().toggleFavorite(item, new FavoriteRepository.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                showToast(message);
            }

            @Override
            public void onError(String message) {
                showToast(message);
                // Revert state on error
                item.setFavorite(!item.isFavorite());
                adapter.notifyItemChanged(position);
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
