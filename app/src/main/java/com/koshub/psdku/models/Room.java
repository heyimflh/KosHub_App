package com.koshub.psdku.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for Room within a Kos property.
 */
public class Room implements Serializable {
    private String id;
    private String kosId;
    private String ownerId;
    private String roomName;
    private double price;
    private String status; // "available", "booked", "occupied"
    private List<String> imageUrls;
    private long createdAt;
    private long updatedAt;

    // Relations
    private String bookingId;
    private String currentBookingId;
    private String studentId;
    private String studentName;

    // Maintenance fields
    private String maintenanceType;
    private String maintenanceNote;
    private long maintenanceStartedAt;
    private long maintenanceUpdatedAt;
    private String maintenancePreviousStatus;
    private long maintenanceCompletedAt;
    private String maintenanceStatus;

    public Room() {
        // Required for Firebase
    }

    public Room(String id, String kosId, String ownerId, String roomName, double price, String status) {
        this.id = id;
        this.kosId = kosId;
        this.ownerId = ownerId;
        this.roomName = roomName;
        this.price = price;
        this.status = status;
        this.imageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getCurrentBookingId() { return currentBookingId; }
    public void setCurrentBookingId(String currentBookingId) { this.currentBookingId = currentBookingId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    // Maintenance Getters & Setters
    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getMaintenanceNote() { return maintenanceNote; }
    public void setMaintenanceNote(String maintenanceNote) { this.maintenanceNote = maintenanceNote; }

    public long getMaintenanceStartedAt() { return maintenanceStartedAt; }
    public void setMaintenanceStartedAt(long maintenanceStartedAt) { this.maintenanceStartedAt = maintenanceStartedAt; }

    public long getMaintenanceUpdatedAt() { return maintenanceUpdatedAt; }
    public void setMaintenanceUpdatedAt(long maintenanceUpdatedAt) { this.maintenanceUpdatedAt = maintenanceUpdatedAt; }

    public String getMaintenancePreviousStatus() { return maintenancePreviousStatus; }
    public void setMaintenancePreviousStatus(String maintenancePreviousStatus) { this.maintenancePreviousStatus = maintenancePreviousStatus; }

    public long getMaintenanceCompletedAt() { return maintenanceCompletedAt; }
    public void setMaintenanceCompletedAt(long maintenanceCompletedAt) { this.maintenanceCompletedAt = maintenanceCompletedAt; }

    public String getMaintenanceStatus() { return maintenanceStatus; }
    public void setMaintenanceStatus(String maintenanceStatus) { this.maintenanceStatus = maintenanceStatus; }
}
