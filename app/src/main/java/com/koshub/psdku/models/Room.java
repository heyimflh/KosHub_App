package com.koshub.psdku.models;

import java.io.Serializable;

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
}
