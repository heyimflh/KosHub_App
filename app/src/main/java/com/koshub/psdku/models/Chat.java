package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for Chat sessions between student and owner.
 */
public class Chat implements Serializable {
    private String id;
    private String studentId;
    private String studentName;
    private String ownerId;
    private String ownerName;
    private String kosId;
    private String kosName;
    private String bookingId;
    private String lastMessage;
    private long lastMessageAt;
    private String lastSenderId;
    private int studentUnreadCount;
    private int ownerUnreadCount;
    private long createdAt;
    private long updatedAt;

    public Chat() {
        // Required for Firebase
    }

    public Chat(String id, String studentId, String studentName, String ownerId, String ownerName, String kosId, String kosName, String bookingId) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.kosId = kosId;
        this.kosName = kosName;
        this.bookingId = bookingId;
        this.lastMessage = "";
        this.lastMessageAt = 0;
        this.lastSenderId = "";
        this.studentUnreadCount = 0;
        this.ownerUnreadCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getKosName() { return kosName; }
    public void setKosName(String kosName) { this.kosName = kosName; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    public int getStudentUnreadCount() { return studentUnreadCount; }
    public void setStudentUnreadCount(int studentUnreadCount) { this.studentUnreadCount = studentUnreadCount; }

    public int getOwnerUnreadCount() { return ownerUnreadCount; }
    public void setOwnerUnreadCount(int ownerUnreadCount) { this.ownerUnreadCount = ownerUnreadCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
