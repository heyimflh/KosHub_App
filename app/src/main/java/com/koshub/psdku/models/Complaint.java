package com.koshub.psdku.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Complaint model for tracking tenant issues.
 */
public class Complaint implements Serializable {
    private String id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String ownerId;
    private String kosId;
    private String kosName;
    private String bookingId;
    private String roomId;
    private String roomName;
    private String title;
    private String description;
    private String imageUrl;
    private List<String> evidenceImageUrls;
    private String status; // "new", "process", "done", "rejected"
    private String ownerResponse;
    private long createdAt;
    private long updatedAt;
    private long resolvedAt;

    // Backward compatibility fields
    private String tenantName;
    private String roomNo;
    private String category;
    private String date;

    public Complaint() {
        this.evidenceImageUrls = new ArrayList<>();
    }

    // Full constructor for new real complaints
    public Complaint(String id, String studentId, String ownerId, String kosId, String bookingId, String title, String description) {
        this.id = id;
        this.studentId = studentId;
        this.ownerId = ownerId;
        this.kosId = kosId;
        this.bookingId = bookingId;
        this.title = title;
        this.description = description;
        this.status = "new";
        this.evidenceImageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Legacy constructor for backward compatibility
    public Complaint(String id, String tenantName, String kosName, String roomNo,
                     String category, String title, String date, String status) {
        this.id = id;
        this.tenantName = tenantName;
        this.studentName = tenantName; // Sync new field
        this.kosName = kosName;
        this.roomNo = roomNo;
        this.roomName = roomNo; // Sync new field
        this.category = category;
        this.title = title;
        this.date = date;
        this.status = status;
        this.evidenceImageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getKosName() { return kosName; }
    public void setKosName(String kosName) { this.kosName = kosName; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getEvidenceImageUrls() { return evidenceImageUrls; }
    public void setEvidenceImageUrls(List<String> evidenceImageUrls) { this.evidenceImageUrls = evidenceImageUrls; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOwnerResponse() { return ownerResponse; }
    public void setOwnerResponse(String ownerResponse) { this.ownerResponse = ownerResponse; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }

    // Legacy Getters/Setters
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; this.studentName = tenantName; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; this.roomName = roomNo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
