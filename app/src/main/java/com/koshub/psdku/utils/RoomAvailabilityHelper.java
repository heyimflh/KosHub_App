package com.koshub.psdku.utils;

/**
 * Utility class to provide consistent formatting for room availability across the app.
 */
public class RoomAvailabilityHelper {

    /**
     * Format for Home Screen / Kos Card.
     * Example: "2 kamar", "1 kamar", "Penuh"
     */
    public static String formatAvailabilityShort(int availableRooms) {
        if (availableRooms > 0) {
            return availableRooms + " kamar";
        } else {
            return "Penuh";
        }
    }

    /**
     * Format for Property Detail Top Badge.
     * Example: "Sisa 2 kamar", "Sisa 1 kamar", "Penuh"
     */
    public static String formatAvailabilityDetail(int availableRooms) {
        if (availableRooms > 0) {
            return "Sisa " + availableRooms + " kamar";
        } else {
            return "Penuh";
        }
    }

    /**
     * Format for Property Detail Bottom Booking Bar.
     * Example: "Tersisa 2 kamar", "Tersisa 1 kamar", "Kos penuh"
     */
    public static String formatAvailabilityBottom(int availableRooms) {
        if (availableRooms > 0) {
            return "Tersisa " + availableRooms + " kamar";
        } else {
            return "Kos penuh";
        }
    }
}
