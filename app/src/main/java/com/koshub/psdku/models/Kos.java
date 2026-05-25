package com.koshub.psdku.models;

import java.io.Serializable;
import java.util.List;

/**
 * Backend-ready model for Kos.
 * Based on KosItem but designed for database storage.
 */
public class Kos implements Serializable {
    private String id;
    private String ownerId;
    private String name;
    private String address;
    private String description;
    private double price;
    private String priceText; // Deprecated: use price
    private int priceValue; // Deprecated: use price
    private String distanceText; // Dummy field for UI
    private int distanceMinutes; // Dummy field for UI
    private String ratingText; // Deprecated: use rating
    private double rating;
    private String category; // "putra", "putri", "campur"
    private List<String> facilities;
    private int imageRes; // Local resource for dummy data
    private List<String> imageUrls;
    private boolean isPremium;
    private String sisaKamar; // Deprecated: use availableRooms
    private int availableRooms;
    private double latitude;
    private double longitude;
    private long createdAt;
    private long updatedAt;

    public Kos() {
        // Required for Firebase
    }

    // Constructor for dummy data transition
    public Kos(String id, String name, String address, String priceText, int priceValue,
               String distanceText, int distanceMinutes, String ratingText, String category,
               List<String> facilities, int imageRes, boolean isPremium, String sisaKamar,
               double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.priceText = priceText;
        this.priceValue = priceValue;
        this.distanceText = distanceText;
        this.distanceMinutes = distanceMinutes;
        this.ratingText = ratingText;
        this.category = category;
        this.facilities = facilities;
        this.imageRes = imageRes;
        this.isPremium = isPremium;
        this.sisaKamar = sisaKamar;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        // Convert dummy rating text to double if possible
        try {
            this.rating = Double.parseDouble(ratingText);
        } catch (Exception e) {
            this.rating = 0.0;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPriceText() { return priceText; }
    public void setPriceText(String priceText) { this.priceText = priceText; }

    public int getPriceValue() { return priceValue; }
    public void setPriceValue(int priceValue) { this.priceValue = priceValue; }

    public String getDistanceText() { return distanceText; }
    public void setDistanceText(String distanceText) { this.distanceText = distanceText; }

    public int getDistanceMinutes() { return distanceMinutes; }
    public void setDistanceMinutes(int distanceMinutes) { this.distanceMinutes = distanceMinutes; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getRatingText() { return ratingText; }
    public void setRatingText(String ratingText) { this.ratingText = ratingText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getFacilities() { return facilities; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities; }

    public int getImageRes() { return imageRes; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public String getSisaKamar() { return sisaKamar; }
    public void setSisaKamar(String sisaKamar) { this.sisaKamar = sisaKamar; }

    public int getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(int availableRooms) { this.availableRooms = availableRooms; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
