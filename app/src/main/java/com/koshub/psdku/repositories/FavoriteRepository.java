package com.koshub.psdku.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.KosItem;
import com.koshub.psdku.models.Favorite;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteRepository {
    private static final String TAG = "KosHubFavorite";
    private static FavoriteRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FavoriteRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized FavoriteRepository getInstance() {
        if (instance == null) {
            instance = new FavoriteRepository();
        }
        return instance;
    }

    public interface FavoriteCallback {
        void onSuccess(boolean status);
        void onError(String message);
    }

    public interface FavoriteListCallback {
        void onSuccess(List<Favorite> favorites);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public String generateFavoriteId(String userId, String kosId) {
        return userId + "_" + kosId;
    }

    public void isFavorite(String kosId, FavoriteCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onSuccess(false);
            return;
        }

        String favId = generateFavoriteId(uid, kosId);
        db.collection(DatabaseConstants.COLLECTION_FAVORITES).document(favId).get()
                .addOnSuccessListener(documentSnapshot -> callback.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "isFavorite error: " + e.getMessage());
                    callback.onSuccess(false);
                });
    }

    public void toggleFavorite(KosItem kos, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("Kamu harus login terlebih dahulu.");
            return;
        }

        String favId = generateFavoriteId(uid, kos.getId());
        db.collection(DatabaseConstants.COLLECTION_FAVORITES).document(favId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        removeFavorite(favId, callback);
                    } else {
                        addFavorite(uid, kos, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "toggleFavorite check error: " + e.getMessage());
                    callback.onError("Gagal memperbarui favorit.");
                });
    }

    private void addFavorite(String uid, KosItem kos, SimpleCallback callback) {
        String favId = generateFavoriteId(uid, kos.getId());
        Favorite favorite = new Favorite(
                favId,
                uid,
                kos.getId(),
                kos.getName(),
                kos.getAddress(),
                kos.getImageUrl() != null ? kos.getImageUrl() : "",
                System.currentTimeMillis()
        );

        db.collection(DatabaseConstants.COLLECTION_FAVORITES).document(favId).set(favorite)
                .addOnSuccessListener(aVoid -> callback.onSuccess("Ditambahkan ke favorit"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addFavorite error: " + e.getMessage());
                    callback.onError("Gagal menambahkan ke favorit.");
                });
    }

    private void removeFavorite(String favId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_FAVORITES).document(favId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Dihapus dari favorit"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "removeFavorite error: " + e.getMessage());
                    callback.onError("Gagal menghapus dari favorit.");
                });
    }

    public void getFavoritesByUser(String userId, FavoriteListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_FAVORITES)
                .whereEqualTo(DatabaseConstants.FIELD_USER_ID, userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Favorite> favorites = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Favorite fav = doc.toObject(Favorite.class);
                        if (fav != null) favorites.add(fav);
                    }
                    // Sort manual by createdAt desc
                    Collections.sort(favorites, (f1, f2) -> Long.compare(f2.getCreatedAt(), f1.getCreatedAt()));
                    callback.onSuccess(favorites);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getFavoritesByUser error: " + e.getMessage());
                    callback.onError("Gagal memuat favorit. Silakan coba lagi.");
                });
    }
}
