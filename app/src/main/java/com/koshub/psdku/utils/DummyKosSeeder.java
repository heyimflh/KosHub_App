package com.koshub.psdku.utils;

import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.models.Kos;
import java.util.Arrays;

/**
 * Utility to seed initial data to Firestore.
 * Use for testing purposes only.
 */
public class DummyKosSeeder {

    public static void seed(KosRepository repository) {
        // Base location for UNS Kampus 6 PGSD Kebumen
        double baseLat = -7.68307;
        double baseLng = 109.6645;

        Kos kos1 = new Kos(
                "", "Kos Putri Premium Sakura", "Jl. Mawar No. 17, Kebumen",
                "Rp 1.2jt", 1200000, "8 mnt", 8, "4.9", "putri",
                Arrays.asList("AC", "WiFi", "K. Mandi Dalam"),
                0, true, "Sisa 3 Kamar",
                baseLat + 0.001, baseLng - 0.001
        );
        kos1.setDescription("Kos premium eksklusif dekat kampus dengan fasilitas lengkap.");
        kos1.setAvailableRooms(3);

        Kos kos2 = new Kos(
                "", "Kos Campur Nusantara", "Jl. Sungai Lukulo No. 21, Kebumen",
                "Rp 550rb", 550000, "7 mnt", 7, "4.3", "campur",
                Arrays.asList("WiFi", "Dapur", "Parkir Motor"),
                0, false, "Sisa 2 Kamar",
                baseLat - 0.001, baseLng + 0.001
        );
        kos2.setDescription("Kos terjangkau dengan lingkungan asri dan aman.");
        kos2.setAvailableRooms(2);

        repository.createKos(kos1, new KosRepository.SimpleCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(String message) {}
        });

        repository.createKos(kos2, new KosRepository.SimpleCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(String message) {}
        });
    }
}
