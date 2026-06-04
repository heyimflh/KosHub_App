package com.koshub.psdku.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper to filter and prioritize facilities for UI display.
 */
public class KosFacilityHelper {

    private static final List<String> PRIORITY_FACILITIES = Arrays.asList(
            "WiFi",
            "Kamar Mandi Dalam",
            "AC",
            "Kipas",
            "Kasur",
            "Lemari",
            "Meja Belajar",
            "Parkir Motor",
            "CCTV",
            "Dapur Bersama",
            "Laundry",
            "Dekat Kampus"
    );

    /**
     * Returns up to 3 most relevant facilities for card display.
     */
    public static List<String> getFeaturedFacilities(List<String> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Clean data: Trim, remove empty, and remove duplicates (preserving order)
        Set<String> cleanSet = new LinkedHashSet<>();
        for (String f : facilities) {
            if (f != null) {
                String trimmed = f.trim();
                if (!trimmed.isEmpty()) {
                    // Try to match priority case-insensitively but keep original if not found
                    cleanSet.add(normalizeFacilityName(trimmed));
                }
            }
        }

        List<String> cleanedList = new ArrayList<>(cleanSet);
        List<String> result = new ArrayList<>();

        // 2. Add priority items first
        for (String priority : PRIORITY_FACILITIES) {
            for (String f : cleanedList) {
                if (f.equalsIgnoreCase(priority)) {
                    if (!result.contains(f)) {
                        result.add(f);
                    }
                    break;
                }
            }
            if (result.size() >= 3) break;
        }

        // 3. If less than 3, add non-priority items
        if (result.size() < 3) {
            for (String f : cleanedList) {
                if (!result.contains(f)) {
                    result.add(f);
                }
                if (result.size() >= 3) break;
            }
        }

        return result;
    }

    private static String normalizeFacilityName(String name) {
        for (String priority : PRIORITY_FACILITIES) {
            if (name.equalsIgnoreCase(priority)) {
                return priority; // Return the exact casing from priority list
            }
        }
        return name;
    }
}
