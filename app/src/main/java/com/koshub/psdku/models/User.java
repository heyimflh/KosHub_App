package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * User model for KosHub.
 * TODO: Add more fields as needed for Firebase integration.
 */
public class User implements Serializable {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role; // "student" or "owner"
    private String university;
    private String nim;
    private String docKtp;
    private String docSku;
    private boolean isVerified;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String profileImageUrl;
    private String provider; // "email" or "google"
    private boolean emailVerified;
    private long createdAt;
    private long updatedAt;

    public User() {
        // Required for Firebase
    }

    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isVerified = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getNim() { return nim; }
    public void setNim(String nim) { this.nim = nim; }

    public String getDocKtp() { return docKtp; }
    public void setDocKtp(String docKtp) { this.docKtp = docKtp; }

    public String getDocSku() { return docSku; }
    public void setDocSku(String docSku) { this.docSku = docSku; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
