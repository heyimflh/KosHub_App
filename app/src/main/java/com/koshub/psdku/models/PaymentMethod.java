package com.koshub.psdku.models;

import java.io.Serializable;

public class PaymentMethod implements Serializable {
    private String id;
    private String userId;
    private String type;          // qris, bank, ewallet
    private String providerName;  // QRIS, BCA, BRI, DANA, OVO, GoPay
    private String accountName;
    private String accountNumber;
    private String maskedNumber;
    private boolean isDefault;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;

    public PaymentMethod() {
        // Required for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getMaskedNumber() { return maskedNumber; }
    public void setMaskedNumber(String maskedNumber) { this.maskedNumber = maskedNumber; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
