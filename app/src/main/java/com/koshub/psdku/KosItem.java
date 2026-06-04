package com.koshub.psdku;

import java.io.Serializable;
import java.util.List;

public class KosItem implements Serializable {
    private String name;
    private String address;
    private String price;
    private int priceValue;
    private String distance;
    private int distanceMinutes;
    private String rating;
    private String category; // "Putra", "Putri", "Campur"
    private List<String> facilities;
    private List<String> securityFeatures;
    private List<String> accessFeatures;
    private List<String> roomFeatures;
    private List<String> rules;
    private int imageRes;
    private String imageUrl; // New field for remote images
    private List<String> imageUrls; // List of gallery images
    private boolean isFavorite;
    private boolean isPremium;
    private String sisaKamar; // null if not shown
    private int availableRooms;
    private double latitude;
    private double longitude;
    private double ratingAverage;
    private int ratingCount;
    private String id; // Real Firestore ID
    private String ownerId; // Owner ID
    private String placeId;
    private String durationText;
    private int durationMinutes;

    public KosItem(String name, String address, String price, int priceValue,
                   String distance, int distanceMinutes, String rating, String category,
                   List<String> facilities, int imageRes, boolean isPremium, String sisaKamar,
                   int availableRooms, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.price = price;
        this.priceValue = priceValue;
        this.distance = distance;
        this.distanceMinutes = distanceMinutes;
        this.rating = rating;
        this.category = category;
        this.facilities = (facilities != null) ? facilities : new java.util.ArrayList<>();
        this.imageRes = imageRes;
        this.imageUrl = null;
        this.isFavorite = false;
        this.isPremium = isPremium;
        this.sisaKamar = sisaKamar;
        this.availableRooms = availableRooms;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPrice() { return price; }
    public int getPriceValue() { return priceValue; }
    public String getDistance() { return distance; }
    public int getDistanceMinutes() { return distanceMinutes; }
    public String getRating() { return rating; }
    public String getCategory() { return category; }
    
    public List<String> getFacilities() { 
        return (facilities != null) ? facilities : new java.util.ArrayList<>(); 
    }
    
    public void setFacilities(List<String> facilities) {
        this.facilities = (facilities != null) ? facilities : new java.util.ArrayList<>();
    }

    public List<String> getSecurityFeatures() { return securityFeatures; }
    public void setSecurityFeatures(List<String> securityFeatures) { this.securityFeatures = securityFeatures; }

    public List<String> getAccessFeatures() { return accessFeatures; }
    public void setAccessFeatures(List<String> accessFeatures) { this.accessFeatures = accessFeatures; }

    public List<String> getRoomFeatures() { return roomFeatures; }
    public void setRoomFeatures(List<String> roomFeatures) { this.roomFeatures = roomFeatures; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }

    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public boolean isPremium() { return isPremium; }
    public String getSisaKamar() { return sisaKamar; }
    public int getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(int availableRooms) { this.availableRooms = availableRooms; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public double getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(double ratingAverage) { this.ratingAverage = ratingAverage; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getDurationText() { return durationText; }
    public void setDurationText(String durationText) { this.durationText = durationText; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}
