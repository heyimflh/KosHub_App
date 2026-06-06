package com.koshub.psdku.models;

import java.io.Serializable;

public class StudentDocument implements Serializable {
    private String id;
    private String userId;
    private String type;              // ktp, ktm, supporting
    private String title;             // KTP, KTM, Dokumen Pendukung
    private String fileUrl;
    private String fileName;
    private String status;            // not_uploaded, pending, verified, rejected
    private String rejectionReason;
    private long uploadedAt;
    private long updatedAt;
    private long verifiedAt;
    private String verifiedBy;

    public StudentDocument() {
        // Required for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public long getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(long uploadedAt) { this.uploadedAt = uploadedAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(long verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
}
