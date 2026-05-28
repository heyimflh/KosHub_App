package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for individual messages in a chat subcollection.
 */
public class Message implements Serializable {
    private String id;
    private String chatId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String text;
    private String imageUrl;
    private String type;
    private long createdAt;
    private boolean isRead;

    public Message() {
        // Required for Firebase
    }

    public Message(String id, String chatId, String senderId, String senderName, String receiverId, String text, String type) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.text = text;
        this.imageUrl = null;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.isRead = false;
    }

    public Message(String id, String chatId, String senderId, String senderName, String receiverId, String text, String imageUrl, String type) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.text = text;
        this.imageUrl = imageUrl;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
