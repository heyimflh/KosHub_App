package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for reviews and ratings left by students.
 */
public class Review implements Serializable {
    private String id;
    private String studentId;
    private String kosId;
    private String bookingId;
    private double rating;
    private String comment;
    private long createdAt;

    public Review() {
        // Required for Firebase
    }

    public Review(String id, String studentId, String kosId, String bookingId, double rating, String comment) {
        this.id = id;
        this.studentId = studentId;
        this.kosId = kosId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getKosId() { return kosId; }
    public void setKosId(String kosId) { this.kosId = kosId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
