package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for Chat sessions between student and owner.
 */
public class Chat implements Serializable {
    private String id;
    private String studentId;
    private String ownerId;
    private String kosId;
    private String lastMessage;
    private long lastMessageAt;
    private long createdAt;

    public Chat() {
        // Required for Firebase
    }

    public Chat(String id, String studentId, String ownerId, String kosId) {
        this.id = id;
        this.studentId = studentId;
        this.ownerId = ownerId;
        this.kosId = kosId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
