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
    private int imageRes;
    private String imageUrl; // New field for remote images
    private boolean isFavorite;
    private boolean isPremium;
    private String sisaKamar; // null if not shown
    private double latitude;
    private double longitude;

    public KosItem(String name, String address, String price, int priceValue,
                   String distance, int distanceMinutes, String rating, String category,
                   List<String> facilities, int imageRes, boolean isPremium, String sisaKamar,
                   double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.price = price;
        this.priceValue = priceValue;
        this.distance = distance;
        this.distanceMinutes = distanceMinutes;
        this.rating = rating;
        this.category = category;
        this.facilities = facilities;
        this.imageRes = imageRes;
        this.imageUrl = null;
        this.isFavorite = false;
        this.isPremium = isPremium;
        this.sisaKamar = sisaKamar;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPrice() { return price; }
    public int getPriceValue() { return priceValue; }
    public String getDistance() { return distance; }
    public int getDistanceMinutes() { return distanceMinutes; }
    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public List<String> getFacilities() { return facilities; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public boolean isPremium() { return isPremium; }
    public String getSisaKamar() { return sisaKamar; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
