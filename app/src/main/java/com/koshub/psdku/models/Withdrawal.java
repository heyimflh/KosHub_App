package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for withdrawal requests by owners.
 */
public class Withdrawal implements Serializable {
    private String id;
    private String ownerId;
    private double amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String status; // "pending", "processing", "success", "failed"
    private String note;
    private long createdAt;
    private long updatedAt;
    private long processedAt;

    public Withdrawal() {
        // Required for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getProcessedAt() { return processedAt; }
    public void setProcessedAt(long processedAt) { this.processedAt = processedAt; }
}
