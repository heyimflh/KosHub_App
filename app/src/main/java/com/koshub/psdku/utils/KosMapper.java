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
        String ratingText;
        if (kos.getRatingAverage() > 0) {
            ratingText = String.format(java.util.Locale.getDefault(), "%.1f", kos.getRatingAverage());
        } else {
            ratingText = "—";
        }

        // Map category (ensure proper casing)
        String category = kos.getCategory();
        if (category != null && !category.isEmpty()) {
            category = category.substring(0, 1).toUpperCase() + category.substring(1);
        }

        // Use RoomAvailabilityHelper for sisaKamar text
        String sisaKamar = RoomAvailabilityHelper.formatAvailabilityDetail(kos.getAvailableRooms());

        // UNIFIED MAPPING: Merge all feature lists into facilities for consistency
        List<String> combinedFacilities = new ArrayList<>();
        if (kos.getFacilities() != null) combinedFacilities.addAll(kos.getFacilities());
        if (kos.getRoomFeatures() != null) combinedFacilities.addAll(kos.getRoomFeatures());
        if (kos.getAccessFeatures() != null) combinedFacilities.addAll(kos.getAccessFeatures());
        if (kos.getSecurityFeatures() != null) combinedFacilities.addAll(kos.getSecurityFeatures());

        KosItem item = new KosItem(
                kos.getName(),
                kos.getAddress(),
                priceText,
                priceValue,
                distanceText,
                distanceMinutes,
                ratingText,
                category,
                combinedFacilities,
                kos.getImageRes(),
                kos.isPremium(),
                sisaKamar,
                kos.getAvailableRooms(),
                kos.getLatitude(),
                kos.getLongitude()
        );
        
        item.setId(kos.getId());
        item.setOwnerId(kos.getOwnerId());
        item.setPlaceId(kos.getPlaceId());
        item.setRatingAverage(kos.getRatingAverage());
        item.setRatingCount(kos.getRatingCount());
        item.setSecurityFeatures(kos.getSecurityFeatures());
        item.setAccessFeatures(kos.getAccessFeatures());
        item.setRoomFeatures(kos.getRoomFeatures());
        item.setRules(kos.getRules());

        // Dynamic Image System: Handle Gallery and Cover
        List<String> gallery = new ArrayList<>();
        if (kos.getImageUrls() != null && !kos.getImageUrls().isEmpty()) {
            gallery.addAll(kos.getImageUrls());
        } else if (kos.getImageUrl() != null && !kos.getImageUrl().isEmpty()) {
            gallery.add(kos.getImageUrl());
        }
        item.setImageUrls(gallery);

        // Set main cover image for cards
        if (!gallery.isEmpty()) {
            item.setImageUrl(gallery.get(0));
        } else {
            item.setImageUrl(kos.getImageUrl());
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
