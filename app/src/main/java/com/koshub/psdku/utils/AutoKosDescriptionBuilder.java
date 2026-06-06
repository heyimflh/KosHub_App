package com.koshub.psdku.utils;

import com.koshub.psdku.models.Kos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class to build automatic, factual descriptions for Kos properties.
 */
public class AutoKosDescriptionBuilder {

    public static String build(Kos kos, Double distanceKm, Integer etaMinutes) {
        if (kos == null) return "";

        StringBuilder sb = new StringBuilder();

        // --- Paragraph 1: Basic Info & Distance ---
        String name = isValidText(kos.getName()) ? kos.getName() : "Kos ini";
        String type = isValidText(kos.getCategory()) ? "kos " + kos.getCategory().toLowerCase() : "kos";
        String address = isValidText(kos.getAddress()) ? " yang berlokasi di " + safeTrim(kos.getAddress()) : "";

        sb.append(name).append(" merupakan ").append(type).append(address).append(".");

        if (distanceKm != null && distanceKm > 0 && etaMinutes != null && etaMinutes > 0) {
            String distStr = formatDistance(distanceKm);
            String etaStr = formatEta(etaMinutes);
            sb.append(" Lokasinya berada sekitar ").append(distStr)
              .append(" dari kampus dengan estimasi ").append(etaStr).append(".");
        }

        // --- Paragraph 2: Detailed Features (Room, Access, Security) ---
        StringBuilder p2 = new StringBuilder();

        String roomF = joinFeatures(kos.getRoomFeatures());
        if (isValidText(roomF)) {
            p2.append("Kos ini dilengkapi fasilitas kamar seperti ").append(roomF).append(".");
        }

        String accessF = joinFeatures(kos.getAccessFeatures());
        if (isValidText(accessF)) {
            if (p2.length() > 0) p2.append(" ");
            p2.append("Dari sisi akses, lokasi kos mendukung ").append(accessF).append(".");
        }

        String securityF = joinFeatures(kos.getSecurityFeatures());
        if (isValidText(securityF)) {
            if (p2.length() > 0) p2.append(" ");
            p2.append("Untuk keamanan, tersedia ").append(securityF).append(".");
        }

        if (p2.length() > 0) {
            sb.append("\n\n").append(p2.toString());
        }

        // --- Paragraph 3: Rules, Price & Urgency ---
        StringBuilder p3 = new StringBuilder();

        String rules = joinFeatures(kos.getRules());
        if (isValidText(rules)) {
            p3.append("Beberapa aturan yang berlaku yaitu ").append(rules).append(".");
        }

        boolean hasPrice = kos.getPrice() > 0;
        boolean hasRooms = kos.getAvailableRooms() > 0;

        if (hasPrice || hasRooms) {
            if (p3.length() > 0) p3.append(" ");

            if (hasPrice && hasRooms) {
                p3.append("Dengan harga mulai dari ").append(formatPrice(kos.getPrice()))
                  .append(" per bulan dan tersisa ").append(kos.getAvailableRooms())
                  .append(" kamar, ");
            } else if (hasPrice) {
                p3.append("Dengan harga mulai dari ").append(formatPrice(kos.getPrice()))
                  .append(" per bulan, ");
            } else if (hasRooms) {
                p3.append("Dengan sisa ").append(kos.getAvailableRooms()).append(" kamar, ");
            }

            p3.append("kos ini dapat menjadi pilihan bagi mahasiswa yang mencari hunian praktis berdasarkan data yang tersedia.");
        }

        if (p3.length() > 0) {
            sb.append("\n\n").append(p3.toString());
        }

        return sb.toString().trim().replace("  ", " ");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isValidText(String value) {
        return value != null && !value.trim().isEmpty() && !value.equalsIgnoreCase("null");
    }

    private static String formatPrice(double price) {
        return String.format(Locale.getDefault(), "Rp%,.0f", price).replace(",", ".");
    }

    private static String formatDistance(double distanceKm) {
        return String.format(Locale.US, "±%.1f km", distanceKm);
    }

    private static String formatEta(int etaMinutes) {
        return "±" + etaMinutes + " menit jalan kaki";
    }

    private static String joinFeatures(List<String> features) {
        if (features == null || features.isEmpty()) return "";

        List<String> cleanList = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String f : features) {
            if (isValidText(f)) {
                String trimmed = f.trim();
                if (seen.add(trimmed.toLowerCase())) {
                    cleanList.add(trimmed);
                }
            }
        }

        if (cleanList.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleanList.size(); i++) {
            sb.append(cleanList.get(i));
            if (i < cleanList.size() - 2) {
                sb.append(", ");
            } else if (i == cleanList.size() - 2) {
                if (cleanList.size() > 2) {
                    sb.append(", dan ");
                } else {
                    sb.append(" dan ");
                }
            }
        }
        return sb.toString();
    }
}
