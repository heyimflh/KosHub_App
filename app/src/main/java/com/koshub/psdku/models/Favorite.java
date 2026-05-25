package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for Kos properties favorited by users.
 */
public class Favorite implements Serializable {
    private String id;
    private String userId;
    private String kosId;
    private long createdAt;

    public Favorite() {
        // Required for Firebase
    }

    public Favorite(String id, String userId, String kosId) {
        this.id = id;
        this.userId = userId;
        this.kosId = kosId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
