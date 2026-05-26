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
    private String kosId;
    private String kosName;
    private double amount;
    private String type; // "booking_payment"
    private String status; // "pending", "available", "withdrawn", "cancelled"
    private long createdAt;
    private long updatedAt;
    private long availableAt;
    private String withdrawalId;

    public Transaction() {
        // Required for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getKosName() { return kosName; }
    public void setKosName(String kosName) { this.kosName = kosName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getAvailableAt() { return availableAt; }
    public void setAvailableAt(long availableAt) { this.availableAt = availableAt; }

    public String getWithdrawalId() { return withdrawalId; }
    public void setWithdrawalId(String withdrawalId) { this.withdrawalId = withdrawalId; }
}
