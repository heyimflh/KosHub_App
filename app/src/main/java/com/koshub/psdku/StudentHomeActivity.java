package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.koshub.psdku.repositories.KosRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StudentHomeActivity extends AppCompatActivity implements KosAdapter.OnKosClickListener {

    private RecyclerView rvKosList;
    private KosAdapter adapter;
    private TextView tvResultCount;
    private List<KosItem> allKosList = new ArrayList<>();
    private List<KosItem> filteredList = new ArrayList<>();
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

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

        initData();
        NavigationHelper.cachedKosList = allKosList;
        setupViews();
        setupSearch();
        setupQuickChips();
        setupToggle();
        setupBottomNav();
        
        // Initial count update
        updateResultCount();
    }

    private void initData() {
        // Fetch data from Repository (Stage 0: Dummy data from repository)
        // TODO: Replace dummy data with Firebase Firestore query in backend integration phase.
        allKosList = KosRepository.getInstance().getAllKosSync();
        filteredList = new ArrayList<>(allKosList);
        NavigationHelper.cachedKosList = new ArrayList<>(allKosList);
    }

    private void setupViews() {
        rvKosList = findViewById(R.id.rvKosList);
        rvKosList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KosAdapter(filteredList, this);
        rvKosList.setAdapter(adapter);

        // Notification button
        findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(this, "Belum ada notifikasi baru", Toast.LENGTH_SHORT).show());
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
                    Toast.makeText(this, "Gagal membuka peta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Apply filter button
        sheetView.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Filter diterapkan", Toast.LENGTH_SHORT).show();
        });

        // Reset filter button
        sheetView.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            Toast.makeText(this, "Filter direset", Toast.LENGTH_SHORT).show();
        });

        // Close button
        sheetView.findViewById(R.id.btnCloseFilter).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateResultCount() {
        if (tvResultCount != null && filteredList != null) {
            tvResultCount.setText(filteredList.size() + " kos");
        }
    }

    public List<KosItem> getAllKosList() {
        return allKosList;
    }

    @Override
    public void onKosClick(KosItem item, int position) {
        Intent intent = new Intent(this, PropertyDetailBookingActivity.class);
        intent.putExtra("kos_item", item);
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    @Override
    public void onFavoriteClick(KosItem item, int position) {
        String msg = item.isFavorite() ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
