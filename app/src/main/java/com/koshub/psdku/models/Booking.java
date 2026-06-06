package com.koshub.psdku.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking model for room rental transactions.
 */
public class Booking implements Serializable {
    private String id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String ownerId;
    private String kosId;
    private String kosName;
    private String kosAddress;
    private String roomId;
    private String roomName;
    private String status; // "pending", "accepted", "rejected", "waiting_checkin", "active", "completed", "cancelled"
    private long bookingDate;
    private long checkInDate;
    private int durationMonth;
    private double totalPrice;
    private String paymentStatus; // "unpaid", "pending", "paid", "refunded"
    private Long gatewayTransactionId;
    private Double totalBayar;
    private String qrisString;
    private Timestamp paymentCreatedAt;
    private Timestamp paidAt;
    private long createdAt;
    private long updatedAt;
    private String note;
    private String updatedBy;
    private List<String> statusHistory;

    // Backward compatibility fields
    private String tenantName;
    private String tenantStatus;
    private String roomNo;
    private String bookingDateText;
    private String checkInDateText;
    private String duration;
    private String price;

    public Booking() {
        // Required for Firebase
    }

    // Full constructor for new real bookings
    public Booking(String id, String studentId, String ownerId, String kosId, String kosName, double totalPrice, String status) {
        this.id = id;
        this.studentId = studentId;
        this.ownerId = ownerId;
        this.kosId = kosId;
        this.kosName = kosName;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Legacy constructor for backward compatibility
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

    public String getKosAddress() { return kosAddress; }
    public void setKosAddress(String kosAddress) { this.kosAddress = kosAddress; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getBookingDate() { return bookingDate; }
    public void setBookingDate(long bookingDate) { this.bookingDate = bookingDate; }

    public long getCheckInDate() { return checkInDate; }
    public void setCheckInDate(long checkInDate) { this.checkInDate = checkInDate; }

    public int getDurationMonth() { return durationMonth; }
    public void setDurationMonth(int durationMonth) { this.durationMonth = durationMonth; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    @Exclude
    public String getSafeStatus() {
        return status == null || status.trim().isEmpty() ? "pending" : status;
    }

    @Exclude
    public String getSafePaymentStatus() {
        return paymentStatus == null || paymentStatus.trim().isEmpty() ? "unpaid" : paymentStatus;
    }

    @Exclude
    public long getSafeGatewayTransactionId() {
        return gatewayTransactionId == null ? 0L : gatewayTransactionId;
    }

    @Exclude
    public double getSafeTotalBayar() {
        return totalBayar == null ? 0.0 : totalBayar;
    }

    public Long getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(Long gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }

    public Double getTotalBayar() { return totalBayar; }
    public void setTotalBayar(Double totalBayar) { this.totalBayar = totalBayar; }

    public String getQrisString() { return qrisString; }
    public void setQrisString(String qrisString) { this.qrisString = qrisString; }

    public Timestamp getPaymentCreatedAt() { return paymentCreatedAt; }
    public void setPaymentCreatedAt(Timestamp paymentCreatedAt) { this.paymentCreatedAt = paymentCreatedAt; }

    public Timestamp getPaidAt() { return paidAt; }
    public void setPaidAt(Timestamp paidAt) { this.paidAt = paidAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public List<String> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<String> statusHistory) { this.statusHistory = statusHistory; }

    // Legacy Getters/Setters
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantStatus() { return tenantStatus; }
    public void setTenantStatus(String tenantStatus) { this.tenantStatus = tenantStatus; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public String getBookingDateText() { return bookingDateText; }
    public void setBookingDateText(String bookingDateText) { this.bookingDateText = bookingDateText; }

    public String getCheckInDateText() { return checkInDateText; }
    public void setCheckInDateText(String checkInDateText) { this.checkInDateText = checkInDateText; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}
