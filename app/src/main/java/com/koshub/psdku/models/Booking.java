package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Booking model based on BookingItem in OwnerBookingActivity.
 */
public class Booking implements Serializable {
    private String id;
    private String studentId;
    private String ownerId;
    private String kosId;
    private String roomId;
    private String tenantName; // Backward compatibility
    private String tenantStatus; // Backward compatibility
    private String kosName; // Backward compatibility
    private String roomNo; // Backward compatibility
    private long bookingDate;
    private long checkInDate;
    private String bookingDateText; // Backward compatibility
    private String checkInDateText; // Backward compatibility
    private String duration; // Backward compatibility
    private double totalPrice;
    private String price; // Backward compatibility
    private String status; // "pending", "accepted", "rejected", "active", "completed", "cancelled"
    private long createdAt;

    public Booking() {
        // Required for Firebase
    }

    public Booking(String id, String tenantName, String tenantStatus, String kosName, String roomNo,
                   String bookingDateText, String checkInDateText, String duration, String price, String status) {
        this.id = id;
        this.tenantName = tenantName;
        this.tenantStatus = tenantStatus;
        this.kosName = kosName;
        this.roomNo = roomNo;
        this.bookingDateText = bookingDateText;
        this.checkInDateText = checkInDateText;
        this.duration = duration;
        this.price = price;
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

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantStatus() { return tenantStatus; }
    public void setTenantStatus(String tenantStatus) { this.tenantStatus = tenantStatus; }

    public String getKosName() { return kosName; }
    public void setKosName(String kosName) { this.kosName = kosName; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public long getBookingDate() { return bookingDate; }
    public void setBookingDate(long bookingDate) { this.bookingDate = bookingDate; }

    public long getCheckInDate() { return checkInDate; }
    public void setCheckInDate(long checkInDate) { this.checkInDate = checkInDate; }

    public String getBookingDateText() { return bookingDateText; }
    public void setBookingDateText(String bookingDateText) { this.bookingDateText = bookingDateText; }

    public String getCheckInDateText() { return checkInDateText; }
    public void setCheckInDateText(String checkInDateText) { this.checkInDateText = checkInDateText; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
