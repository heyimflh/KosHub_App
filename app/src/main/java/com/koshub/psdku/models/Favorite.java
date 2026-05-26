package com.koshub.psdku.models;

import java.io.Serializable;

public class Favorite implements Serializable {
    private String id; // userId + "_" + kosId
    private String userId;
    private String kosId;
    private String kosName;
    private String kosAddress;
    private String kosImageUrl;
    private long createdAt;

    public Favorite() {
        // Required for Firebase
    }

    public Favorite(String id, String userId, String kosId, String kosName, String kosAddress, String kosImageUrl, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.kosId = kosId;
        this.kosName = kosName;
        this.kosAddress = kosAddress;
        this.kosImageUrl = kosImageUrl;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getKosName() { return kosName; }
    public void setKosName(String kosName) { this.kosName = kosName; }

    public String getKosAddress() { return kosAddress; }
    public void setKosAddress(String kosAddress) { this.kosAddress = kosAddress; }

    public String getKosImageUrl() { return kosImageUrl; }
    public void setKosImageUrl(String kosImageUrl) { this.kosImageUrl = kosImageUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
