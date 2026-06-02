package com.koshub.psdku.utils;

import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.models.Kos;
import java.util.Arrays;

/**
 * Utility to seed initial data to Firestore.
 * Use for testing purposes only.
 * This class is located in the debug source set and will not be included in release builds.
 */
public class DummyKosSeeder {

    public static void seed(KosRepository repository) {
        // Kos 1: Kos Putri Bu Sari
        Kos kos1 = new Kos(
                "", "Kos Putri Bu Sari", "Jl. Kepodang No. 45, Panjer, Kebumen",
                "Rp 600rb", 600000, "...", 0, "4.5", "putri",
                Arrays.asList("WiFi", "K. Mandi Dalam", "Lemari"),
                0, false, "Sisa 3 Kamar",
                -7.682500, 109.663800
        );
        kos1.setDescription("Kos nyaman untuk mahasiswi, lokasi sangat strategis dekat kampus.");
        kos1.setAvailableRooms(3);
        kos1.setPlaceId("");

        // Kos 2: Kos Putra Pak Hadi
        Kos kos2 = new Kos(
                "", "Kos Putra Pak Hadi", "Jl. Cendrawasih No. 12, Kebumen",
                "Rp 450rb", 450000, "...", 0, "4.2", "putra",
                Arrays.asList("WiFi", "Parkir Motor", "Dapur Bersama"),
                0, false, "Sisa 2 Kamar",
                -7.684200, 109.665100
        );
        kos2.setDescription("Lingkungan tenang, cocok untuk mahasiswa yang ingin fokus belajar.");
        kos2.setAvailableRooms(2);
        kos2.setPlaceId("");

        // Kos 3: Kos Campur Pak Ridwan
        Kos kos3 = new Kos(
                "", "Kos Campur Pak Ridwan", "Jl. Sungai Lukulo No. 8, Kebumen",
                "Rp 500rb", 500000, "...", 0, "4.0", "campur",
                Arrays.asList("WiFi", "Parkir Motor"),
                0, false, "Sisa 5 Kamar",
                -7.680500, 109.666200
        );
        kos3.setDescription("Akses mudah, dekat dengan fasilitas umum dan sungai Lukulo.");
        kos3.setAvailableRooms(5);
        kos3.setPlaceId("");

        // Kos 4: Kos Putri Melati
        Kos kos4 = new Kos(
                "", "Kos Putri Melati", "Jl. Masjid No. 3, Panjer, Kebumen",
                "Rp 750rb", 750000, "...", 0, "4.8", "putri",
                Arrays.asList("AC", "WiFi", "K. Mandi Dalam", "CCTV"),
                0, true, "Sisa 1 Kamar",
                -7.679800, 109.663200
        );
        kos4.setDescription("Fasilitas eksklusif dengan keamanan CCTV 24 jam.");
        kos4.setAvailableRooms(1);
        kos4.setPlaceId("");

        KosRepository.SimpleCallback callback = new KosRepository.SimpleCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(String message) {}
        };

        repository.createKos(kos1, callback);
        repository.createKos(kos2, callback);
        repository.createKos(kos3, callback);
        repository.createKos(kos4, callback);
    }
}
