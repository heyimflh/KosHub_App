package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for notifications stored in Firestore.
 */
public class AppNotification implements Serializable {
    private String id;
    private String recipientId;
    private String senderId;
    private String type;
    private String title;
    private String body;
    private String targetType;
    private String targetId;
    private boolean isRead;
    private boolean isDelivered;
    private long createdAt;
    private long readAt;
    private long deliveredAt;

    public AppNotification() {
        // Required for Firebase
    }

    public AppNotification(String id, String recipientId, String senderId, String type, String title, String body, String targetType, String targetId) {
        this.id = id;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetType = targetType;
        this.targetId = targetId;
        this.isRead = false;
        this.isDelivered = false;
        this.createdAt = System.currentTimeMillis();
        this.readAt = 0;
        this.deliveredAt = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getReadAt() { return readAt; }
    public void setReadAt(long readAt) { this.readAt = readAt; }

    public long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(long deliveredAt) { this.deliveredAt = deliveredAt; }
}
