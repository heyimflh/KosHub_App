package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Favorite;
import com.koshub.psdku.models.Kos;
import com.koshub.psdku.repositories.FavoriteRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.utils.KosMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FavoriteListActivity extends AppCompatActivity implements KosAdapter.OnKosClickListener {

    private RecyclerView rvFavorites;
    private View layoutEmpty;
    private ProgressBar pbLoading;
    private KosAdapter adapter;
    private List<KosItem> favoriteItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_list);

        initViews();
        loadFavorites();
    }

    private void initViews() {
        rvFavorites = findViewById(R.id.rvFavorites);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        pbLoading = findViewById(R.id.pbLoading);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KosAdapter(favoriteItems, this);
        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        rvFavorites.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        FavoriteRepository.getInstance().getFavoritesByUser(uid, new FavoriteRepository.FavoriteListCallback() {
            @Override
            public void onSuccess(List<Favorite> favorites) {
                if (favorites.isEmpty()) {
                    showEmptyState();
                    return;
                }
                fetchKosDetails(favorites);
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(FavoriteListActivity.this, message, Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void fetchKosDetails(List<Favorite> favorites) {
        favoriteItems.clear();
        AtomicInteger counter = new AtomicInteger(0);
        int total = favorites.size();

        for (Favorite fav : favorites) {
            KosRepository.getInstance().getKosById(fav.getKosId(), new KosRepository.KosCallback() {
                @Override
                public void onSuccess(Kos kos) {
                    KosItem item = KosMapper.toKosItem(kos);
                    item.setFavorite(true);
                    favoriteItems.add(item);
                    checkFinished(counter, total);
                }

                @Override
                public void onError(String message) {
                    checkFinished(counter, total);
                }
            });
        }
    }

    private void checkFinished(AtomicInteger counter, int total) {
        if (counter.incrementAndGet() == total) {
            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                if (favoriteItems.isEmpty()) {
                    showEmptyState();
                } else {
                    rvFavorites.setVisibility(View.VISIBLE);
                    adapter.updateList(new ArrayList<>(favoriteItems));
                }
            });
        }
    }

    private void showEmptyState() {
        pbLoading.setVisibility(View.GONE);
        rvFavorites.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onKosClick(KosItem item, int position) {
        Intent intent = new Intent(this, PropertyDetailBookingActivity.class);
        intent.putExtra("kos_item", item);
        intent.putExtra("kos_id", item.getId());
        intent.putExtra("owner_id", item.getOwnerId());
        
        // Add coordinates and basic info
        intent.putExtra("kos_lat", item.getLatitude());
        intent.putExtra("kos_lng", item.getLongitude());
        intent.putExtra("kos_name", item.getName());
        intent.putExtra("kos_address", item.getAddress());
        
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    @Override
    public void onFavoriteClick(KosItem item, int position) {
        FavoriteRepository.getInstance().toggleFavorite(item, new FavoriteRepository.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(FavoriteListActivity.this, message, Toast.LENGTH_SHORT).show();
                if (!item.isFavorite()) {
                    // Item was unfavorited, remove from list
                    favoriteItems.remove(item);
                    adapter.updateList(new ArrayList<>(favoriteItems));
                    if (favoriteItems.isEmpty()) {
                        showEmptyState();
                    }
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(FavoriteListActivity.this, message, Toast.LENGTH_SHORT).show();
                // Revert UI state
                item.setFavorite(!item.isFavorite());
                adapter.notifyItemChanged(position);
            }
        });
    }

    @Override
    public void onNavigateClick(KosItem item, int position) {
        Intent navIntent = new Intent(this, MapViewRouteNavigationActivity.class);
        navIntent.putExtra("kos_lat", item.getLatitude());
        navIntent.putExtra("kos_lng", item.getLongitude());
        navIntent.putExtra("kos_name", item.getName());
        // Pass the favorite list as well if needed
        navIntent.putExtra("kos_list", new ArrayList<>(favoriteItems));
        NavigationTransitionHelper.navigateMainWithIntent(this, navIntent);
    }
}
