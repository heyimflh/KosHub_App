package com.koshub.psdku.repositories;

import com.koshub.psdku.KosItem;
import com.koshub.psdku.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Repository for Kos data.
 * Initially returns dummy data.
 */
public class KosRepository {
    private static KosRepository instance;
    private List<KosItem> dummyKosList;

    private KosRepository() {
        initDummyData();
    }

    public static synchronized KosRepository getInstance() {
        if (instance == null) {
            instance = new KosRepository();
        }
        return instance;
    }

    /**
     * Callback interface for async-style operations.
     */
    public interface KosCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private void initDummyData() {
        dummyKosList = new ArrayList<>();

        // Base location for UNS Kampus 6 PGSD Kebumen
        double baseLat = -7.68307;
        double baseLng = 109.6645;

        dummyKosList.add(new KosItem(
                "Kos Putri Premium Sakura", "Jl. Mawar No. 17, Kebumen",
                "Rp 1.2jt", 1200000, "8 mnt", 8, "4.9", "Putri",
                Arrays.asList("AC", "WiFi", "K. Mandi Dalam"),
                R.drawable.kos_01, true, null,
                baseLat + 0.001, baseLng - 0.001));

        dummyKosList.add(new KosItem(
                "Kos Campur Nusantara", "Jl. Sungai Lukulo No. 21, Kebumen",
                "Rp 550rb", 550000, "7 mnt", 7, "4.3", "Campur",
                Arrays.asList("WiFi", "Dapur", "Parkir Motor"),
                R.drawable.kos_02, false, "Sisa 2 Kamar",
                baseLat - 0.001, baseLng + 0.001));

        dummyKosList.add(new KosItem(
                "Kos Putra Harmoni", "Jl. Pendidikan No. 12, Kebumen",
                "Rp 750rb", 750000, "5 mnt", 5, "4.8", "Putra",
                Arrays.asList("WiFi", "K. Mandi Dalam", "Laundry"),
                R.drawable.kos_03, false, null,
                baseLat + 0.0005, baseLng + 0.0005));

        dummyKosList.add(new KosItem(
                "Kos Putri Melati Eksklusif", "Jl. Melati No. 12, Kebumen",
                "Rp 900rb", 900000, "10 mnt", 10, "4.9", "Putri",
                Arrays.asList("AC", "WiFi", "Lemari"),
                R.drawable.kos_04, false, "Sisa 1 Kamar",
                baseLat - 0.0008, baseLng - 0.0008));

        dummyKosList.add(new KosItem(
                "Kos Putra Sederhana Jaya", "Jl. Kebumen Raya No. 5, Kebumen",
                "Rp 500rb", 500000, "3 mnt", 3, "4.2", "Putra",
                Arrays.asList("Parkir", "Laundry", "WiFi"),
                R.drawable.kos_05, false, null,
                baseLat + 0.0003, baseLng - 0.0003));

        dummyKosList.add(new KosItem(
                "Kos Putri Anggrek", "Jl. Anggrek No. 8, Kebumen",
                "Rp 650rb", 650000, "6 mnt", 6, "4.6", "Putri",
                Arrays.asList("AC", "WiFi", "K. Mandi Dalam"),
                R.drawable.kos_06, false, null,
                baseLat + 0.0007, baseLng + 0.0002));

        dummyKosList.add(new KosItem(
                "Kos Campur Perwira", "Jl. Perwira No. 15, Kebumen",
                "Rp 450rb", 450000, "4 mnt", 4, "4.1", "Campur",
                Arrays.asList("WiFi", "Parkir Motor", "Dapur"),
                R.drawable.kos_07, false, "Sisa 3 Kamar",
                baseLat - 0.0006, baseLng + 0.0004));

        dummyKosList.add(new KosItem(
                "Kos Putra Barokah", "Jl. Pahlawan No. 22, Kebumen",
                "Rp 600rb", 600000, "5 mnt", 5, "4.5", "Putra",
                Arrays.asList("WiFi", "K. Mandi Dalam", "Lemari"),
                R.drawable.kos_08, false, null,
                baseLat + 0.0012, baseLng - 0.0007));

        dummyKosList.add(new KosItem(
                "Kos Putri Cendana", "Jl. Cendana No. 3, Kebumen",
                "Rp 800rb", 800000, "7 mnt", 7, "4.7", "Putri",
                Arrays.asList("AC", "WiFi", "K. Mandi Dalam", "Lemari"),
                R.drawable.kos_09, true, null,
                baseLat - 0.0004, baseLng - 0.0012));

        dummyKosList.add(new KosItem(
                "Kos Campur Merdeka", "Jl. Merdeka No. 10, Kebumen",
                "Rp 475rb", 475000, "12 mnt", 12, "4.0", "Campur",
                Arrays.asList("WiFi", "Parkir"),
                R.drawable.kos_10, false, null,
                baseLat + 0.0015, baseLng + 0.001));
    }

    /**
     * Fetch all kos items.
     * TODO: Replace with Firebase query.
     */
    public void getAllKos(KosCallback<List<KosItem>> callback) {
        // Simulating async behavior
        if (callback != null) {
            callback.onSuccess(new ArrayList<>(dummyKosList));
        }
    }

    /**
     * Fetch kos items synchronously (for immediate use if needed).
     */
    public List<KosItem> getAllKosSync() {
        return new ArrayList<>(dummyKosList);
    }
}
