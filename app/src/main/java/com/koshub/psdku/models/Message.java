package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for individual messages in a chat subcollection.
 */
public class Message implements Serializable {
    private String id;
    private String chatId;
    private String senderId;
    private String text;
    private long createdAt;
    private boolean isRead;

    public Message() {
        // Required for Firebase
    }

    public Message(String id, String chatId, String senderId, String text) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.createdAt = System.currentTimeMillis();
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
