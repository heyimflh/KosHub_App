package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for AI Assistant messages.
 */
public class AiMessage implements Serializable {
    private String id;
    private String message;
    private String senderType; // "user" or "ai"
    private long timestamp;

    public AiMessage() {
        // Required for potential serialization
    }

    public AiMessage(String id, String message, String senderType, long timestamp) {
        this.id = id;
        this.message = message;
        this.senderType = senderType;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
