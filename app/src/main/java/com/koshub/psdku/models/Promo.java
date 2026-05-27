package com.koshub.psdku.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Promo {
    private String id;
    private String kosId;
    private String ownerId;
    private String name;
    private int discountPercent;
    private long startDate;
    private long endDate;
    private String description;
    private boolean isActive;
    private long createdAt;

    public Promo() {
        // Required for Firestore
    }

    public Promo(String id, String kosId, String ownerId, String name, int discountPercent, long startDate, long endDate, String description, boolean isActive) {
        this.id = id;
        this.kosId = kosId;
        this.ownerId = ownerId;
        this.name = name;
        this.discountPercent = discountPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
