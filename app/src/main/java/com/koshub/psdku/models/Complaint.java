package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Complaint model based on ComplaintItem in OwnerComplaintActivity.
 */
public class Complaint implements Serializable {
    private String id;
    private String studentId;
    private String ownerId;
    private String kosId;
    private String bookingId;
    private String tenantName; // Backward compatibility
    private String kosName; // Backward compatibility
    private String roomNo; // Backward compatibility
    private String category; // Backward compatibility
    private String title;
    private String description;
    private String imageUrl;
    private String date; // Backward compatibility
    private String status; // "new", "process", "done", "rejected"
    private long createdAt;

    public Complaint() {
        // Required for Firebase
    }

    public Complaint(String id, String tenantName, String kosName, String roomNo,
                     String category, String title, String date, String status) {
        this.id = id;
        this.tenantName = tenantName;
        this.kosName = kosName;
        this.roomNo = roomNo;
        this.category = category;
        this.title = title;
        this.date = date;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getKosName() { return kosName; }
    public void setKosName(String kosName) { this.kosName = kosName; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
