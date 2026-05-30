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
import com.koshub.psdku.models.Promo;
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

    private KosRepository() {
        this.db = FirebaseService.getFirestore();
        this.storage = FirebaseService.getStorage();
        this.auth = FirebaseService.getAuth();
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

    public interface StatsCallback {
        void onSuccess(com.koshub.psdku.models.OwnerKosStats stats);
        void onError(String message);
    }

    /**
     * Fetch all kos items from Firestore with basic pagination (limit).
     */
    public void getAllKos(KosListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_KOS)
                .orderBy(DatabaseConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(20) // Pro: Limit result to 20 for initial load
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void getAllKosItems(KosItemListCallback callback) {
        getAllKos(new KosListCallback() {
            @Override
            public void onSuccess(List<Kos> kosList) {
                if (kosList.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                } else {
                    callback.onSuccess(KosMapper.toKosItemList(kosList));
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void updateKos(Kos kos, SimpleCallback callback) {
        kos.setUpdatedAt(System.currentTimeMillis());
        db.collection(DatabaseConstants.COLLECTION_KOS).document(kos.getId()).set(kos)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void deleteKos(String kosId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void createPromo(Promo promo, SimpleCallback callback) {
        DocumentReference promoRef = db.collection("promos").document();
        promo.setId(promoRef.getId());

        promoRef.set(promo)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void getRoomsByOwner(String ownerId, RoomListCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_ROOMS)
                .whereEqualTo(DatabaseConstants.FIELD_OWNER_ID, ownerId)
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
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void calculateOwnerKosStats(String ownerId, StatsCallback callback) {
        com.koshub.psdku.models.OwnerKosStats stats = new com.koshub.psdku.models.OwnerKosStats();
        
        // 1. Get Kos Count
        db.collection(DatabaseConstants.COLLECTION_KOS)
                .whereEqualTo(DatabaseConstants.FIELD_OWNER_ID, ownerId)
                .get()
                .addOnSuccessListener(kosSnapshots -> {
                    stats.setTotalKos(kosSnapshots.size());
                    
                    // 2. Get Room Stats
                    db.collection(DatabaseConstants.COLLECTION_ROOMS)
                            .whereEqualTo(DatabaseConstants.FIELD_OWNER_ID, ownerId)
                            .get()
                            .addOnSuccessListener(roomSnapshots -> {
                                int totalRooms = roomSnapshots.size();
                                int occupied = 0;
                                int available = 0;
                                int maintenance = 0;
                                
                                for (QueryDocumentSnapshot doc : roomSnapshots) {
                                    String status = doc.getString(DatabaseConstants.FIELD_STATUS);
                                    if (status == null) status = DatabaseConstants.ROOM_AVAILABLE;
                                    
                                    switch (status) {
                                        case DatabaseConstants.ROOM_OCCUPIED:
                                        case DatabaseConstants.ROOM_BOOKED:
                                            occupied++;
                                            break;
                                        case DatabaseConstants.ROOM_AVAILABLE:
                                            available++;
                                            break;
                                        case DatabaseConstants.ROOM_MAINTENANCE:
                                            maintenance++;
                                            break;
                                    }
                                }
                                
                                stats.setTotalRooms(totalRooms);
                                stats.setOccupiedRooms(occupied);
                                stats.setAvailableRooms(available);
                                stats.setMaintenanceRooms(maintenance);
                                if (totalRooms > 0) {
                                    stats.setOccupancyRate((double) occupied / totalRooms * 100);
                                }
                                
                                callback.onSuccess(stats);
                            })
                            .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void updateRoomStatus(String roomId, String status, String kosId, SimpleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_ROOMS).document(roomId)
                .update(DatabaseConstants.FIELD_STATUS, status, DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    updateAvailableRoomsCount(kosId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        callback.onError("Kamu tidak memiliki izin untuk mengakses data ini.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
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
}
