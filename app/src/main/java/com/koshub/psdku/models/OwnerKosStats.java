package com.koshub.psdku.models;

import java.io.Serializable;

/**
 * Model for Owner Kos Statistics.
 */
public class OwnerKosStats implements Serializable {
    private int totalKos;
    private int totalRooms;
    private int occupiedRooms;
    private int availableRooms;
    private int maintenanceRooms;
    private double occupancyRate;

    public OwnerKosStats() {
        this.totalKos = 0;
        this.totalRooms = 0;
        this.occupiedRooms = 0;
        this.availableRooms = 0;
        this.maintenanceRooms = 0;
        this.occupancyRate = 0.0;
    }

    public int getTotalKos() { return totalKos; }
    public void setTotalKos(int totalKos) { this.totalKos = totalKos; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    public int getOccupiedRooms() { return occupiedRooms; }
    public void setOccupiedRooms(int occupiedRooms) { this.occupiedRooms = occupiedRooms; }

    public int getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(int availableRooms) { this.availableRooms = availableRooms; }

    public int getMaintenanceRooms() { return maintenanceRooms; }
    public void setMaintenanceRooms(int maintenanceRooms) { this.maintenanceRooms = maintenanceRooms; }

    public double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }
}
