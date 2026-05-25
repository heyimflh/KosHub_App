package com.koshub.psdku.utils;

import com.koshub.psdku.KosItem;
import com.koshub.psdku.models.Kos;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper utility to convert between Kos and KosItem.
 */
public class KosMapper {

    public static KosItem toKosItem(Kos kos) {
        if (kos == null) return null;

        // Map price text and value
        String priceText = kos.getPriceText() != null ? kos.getPriceText() : "Rp " + (int)kos.getPrice();
        int priceValue = (int)kos.getPrice();

        // Map distance text and minutes
        String distanceText = kos.getDistanceText() != null ? kos.getDistanceText() : "5 mnt";
        int distanceMinutes = kos.getDistanceMinutes();

        // Map rating
        String ratingText = kos.getRatingText() != null ? kos.getRatingText() : String.valueOf(kos.getRating());

        // Map category (ensure proper casing)
        String category = kos.getCategory();
        if (category != null && !category.isEmpty()) {
            category = category.substring(0, 1).toUpperCase() + category.substring(1);
        }

        KosItem item = new KosItem(
                kos.getName(),
                kos.getAddress(),
                priceText,
                priceValue,
                distanceText,
                distanceMinutes,
                ratingText,
                category,
                kos.getFacilities() != null ? kos.getFacilities() : new ArrayList<>(),
                kos.getImageRes(),
                kos.isPremium(),
                kos.getSisaKamar() != null ? kos.getSisaKamar() : (kos.getAvailableRooms() > 0 ? "Sisa " + kos.getAvailableRooms() + " Kamar" : "Penuh"),
                kos.getLatitude(),
                kos.getLongitude()
        );
        
        if (kos.getImageUrls() != null && !kos.getImageUrls().isEmpty()) {
            item.setImageUrl(kos.getImageUrls().get(0));
        }

        return item;
    }

    public static List<KosItem> toKosItemList(List<Kos> kosList) {
        List<KosItem> items = new ArrayList<>();
        if (kosList == null) return items;
        for (Kos kos : kosList) {
            items.add(toKosItem(kos));
        }
        return items;
    }
}
