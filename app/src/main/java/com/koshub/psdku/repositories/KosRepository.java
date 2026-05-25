package com.koshub.psdku.repositories;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.koshub.psdku.KosItem;
import com.koshub.psdku.R;
import com.koshub.psdku.models.Kos;
import com.koshub.psdku.models.Room;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.KosMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Repository for Kos data.
 * Handles Firestore and Firebase Storage interactions.
 */
public class KosRepository {
    private static KosRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;
    private List<KosItem> dummyKosList;

    private KosRepository() {
        this.db = FirebaseService.getFirestore();
        this.storage = FirebaseService.getStorage();
        this.auth = FirebaseService.getAuth();
        initDummyData();
    }

    public static synchronized KosRepository getInstance() {
        if (instance == null) {
            instance = new KosRepository();
        }
        return instance;
    }

    public interface KosListCallback {
        void onSuccess(List<Kos> kosList);
        void onError(String message);
    }

    public interface KosItemListCallback {
        void onSuccess(List<KosItem> items);
        void onError(String message);
    }

    public interface KosCallback {
        void onSuccess(Kos kos);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String message);
    }

    public interface RoomListCallback {
        void onSuccess(List<Room> rooms);
        void onError(String message);
    }

    /**
     * Fetch all kos items from Firestore.
     */
    public void getAllKos(KosListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_KOS)
                .orderBy(DatabaseConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Kos> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Kos kos = doc.toObject(Kos.class);
                        kos.setId(doc.getId());
                        list.add(kos);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Fetch all kos items and map to legacy KosItem.
     */
    public void getAllKosItems(KosItemListCallback callback) {
        getAllKos(new KosListCallback() {
            @Override
            public void onSuccess(List<Kos> kosList) {
                if (kosList.isEmpty()) {
                    // Fallback to dummy if empty for dev
                    callback.onSuccess(new ArrayList<>(dummyKosList));
                } else {
                    callback.onSuccess(KosMapper.toKosItemList(kosList));
                }
            }

            @Override
            public void onError(String message) {
                // Fallback to dummy on error
                callback.onSuccess(new ArrayList<>(dummyKosList));
            }
        });
    }

    public void getKosById(String kosId, KosCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Kos kos = documentSnapshot.toObject(Kos.class);
                        if (kos != null) {
                            kos.setId(documentSnapshot.getId());
                            callback.onSuccess(kos);
                        } else {
                            callback.onError("Failed to parse data");
                        }
                    } else {
                        callback.onError("Kos not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getKosByOwner(String ownerId, KosListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_KOS)
                .whereEqualTo(DatabaseConstants.FIELD_OWNER_ID, ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Kos> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Kos kos = doc.toObject(Kos.class);
                        kos.setId(doc.getId());
                        list.add(kos);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createKos(Kos kos, SimpleCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Not authenticated");
            return;
        }
        kos.setOwnerId(auth.getUid());
        kos.setCreatedAt(System.currentTimeMillis());
        kos.setUpdatedAt(System.currentTimeMillis());

        db.collection(DatabaseConstants.COLLECTION_KOS).add(kos)
                .addOnSuccessListener(documentReference -> {
                    kos.setId(documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateKos(Kos kos, SimpleCallback callback) {
        kos.setUpdatedAt(System.currentTimeMillis());
        db.collection(DatabaseConstants.COLLECTION_KOS).document(kos.getId()).set(kos)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteKos(String kosId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        // TODO: Handle related rooms and images in Storage
    }

    public void uploadKosImage(Uri imageUri, String kosId, UploadCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("Not authenticated");
            return;
        }
        StorageReference ref = storage.getReference().child("kos/" + uid + "/" + kosId + "/" + System.currentTimeMillis() + ".jpg");
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addRoom(Room room, SimpleCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Not authenticated");
            return;
        }
        room.setOwnerId(auth.getUid());
        room.setCreatedAt(System.currentTimeMillis());
        room.setUpdatedAt(System.currentTimeMillis());

        db.collection(DatabaseConstants.COLLECTION_ROOMS).add(room)
                .addOnSuccessListener(documentReference -> {
                    room.setId(documentReference.getId());
                    // Update availableRooms count on Kos
                    updateAvailableRoomsCount(room.getKosId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getRoomsByKos(String kosId, RoomListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_ROOMS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Room> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        room.setId(doc.getId());
                        list.add(room);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateRoomStatus(String roomId, String status, String kosId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                .update(DatabaseConstants.FIELD_STATUS, status, DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    updateAvailableRoomsCount(kosId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void updateAvailableRoomsCount(String kosId) {
        db.collection(DatabaseConstants.COLLECTION_ROOMS)
                .whereEqualTo(DatabaseConstants.FIELD_KOS_ID, kosId)
                .whereEqualTo(DatabaseConstants.FIELD_STATUS, DatabaseConstants.ROOM_AVAILABLE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId)
                            .update(DatabaseConstants.FIELD_AVAILABLE_ROOMS, count);
                });
    }

    private void initDummyData() {
        dummyKosList = new ArrayList<>();
        double baseLat = -7.68307;
        double baseLng = 109.6645;

        dummyKosList.add(new KosItem("Kos Putri Premium Sakura", "Jl. Mawar No. 17, Kebumen", "Rp 1.2jt", 1200000, "8 mnt", 8, "4.9", "Putri", Arrays.asList("AC", "WiFi", "K. Mandi Dalam"), R.drawable.kos_01, true, null, baseLat + 0.001, baseLng - 0.001));
        dummyKosList.add(new KosItem("Kos Campur Nusantara", "Jl. Sungai Lukulo No. 21, Kebumen", "Rp 550rb", 550000, "7 mnt", 7, "4.3", "Campur", Arrays.asList("WiFi", "Dapur", "Parkir Motor"), R.drawable.kos_02, false, "Sisa 2 Kamar", baseLat - 0.001, baseLng + 0.001));
        dummyKosList.add(new KosItem("Kos Putra Harmoni", "Jl. Pendidikan No. 12, Kebumen", "Rp 750rb", 750000, "5 mnt", 5, "4.8", "Putra", Arrays.asList("WiFi", "K. Mandi Dalam", "Laundry"), R.drawable.kos_03, false, null, baseLat + 0.0005, baseLng + 0.0005));
    }

    public List<KosItem> getAllKosSync() {
        return new ArrayList<>(dummyKosList);
    }
}
