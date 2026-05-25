package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for financial transactions.
 */
public class Transaction implements Serializable {
    private String id;
    private String ownerId;
    private String studentId;
    private String bookingId;
    private double amount;
    private String type; // "booking_payment"
    private String status; // "pending", "available", "withdrawn"
    private long createdAt;

    public Transaction() {
        // Required for Firebase
    }

    public Transaction(String id, String ownerId, String studentId, String bookingId, double amount, String type, String status) {
        this.id = id;
        this.ownerId = ownerId;
        this.studentId = studentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
