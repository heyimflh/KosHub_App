package com.koshub.psdku.utils;

/**
 * Constants for Database Collections and Statuses.
 */
public class DatabaseConstants {
    // Collection Names
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_KOS = "kos";
    public static final String COLLECTION_ROOMS = "rooms";
    public static final String COLLECTION_BOOKINGS = "bookings";
    public static final String COLLECTION_FAVORITES = "favorites";
    public static final String COLLECTION_COMPLAINTS = "complaints";
    public static final String COLLECTION_CHATS = "chats";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_TRANSACTIONS = "transactions";
    public static final String COLLECTION_WITHDRAWALS = "withdrawals";
    public static final String COLLECTION_REVIEWS = "reviews";

    // Roles
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_OWNER = "owner";

    // Kos Categories
    public static final String CATEGORY_PUTRA = "putra";
    public static final String CATEGORY_PUTRI = "putri";
    public static final String CATEGORY_CAMPUR = "campur";

    // Room Status
    public static final String ROOM_AVAILABLE = "available";
    public static final String ROOM_BOOKED = "booked";
    public static final String ROOM_OCCUPIED = "occupied";

    // Booking Status
    public static final String BOOKING_PENDING = "pending";
    public static final String BOOKING_ACCEPTED = "accepted";
    public static final String BOOKING_REJECTED = "rejected";
    public static final String BOOKING_ACTIVE = "active";
    public static final String BOOKING_COMPLETED = "completed";
    public static final String BOOKING_CANCELLED = "cancelled";

    // Complaint Status
    public static final String COMPLAINT_NEW = "new";
    public static final String COMPLAINT_PROCESS = "process";
    public static final String COMPLAINT_DONE = "done";
    public static final String COMPLAINT_REJECTED = "rejected";

    // Transaction
    public static final String TRANSACTION_TYPE_BOOKING_PAYMENT = "booking_payment";
    public static final String TRANSACTION_PENDING = "pending";
    public static final String TRANSACTION_AVAILABLE = "available";
    public static final String TRANSACTION_WITHDRAWN = "withdrawn";

    // Withdrawal
    public static final String WITHDRAWAL_PENDING = "pending";
    public static final String WITHDRAWAL_PROCESSING = "processing";
    public static final String WITHDRAWAL_SUCCESS = "success";
    public static final String WITHDRAWAL_FAILED = "failed";
}
