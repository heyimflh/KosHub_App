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
    // public static final String COLLECTION_BOOKINGS = "bookings"; // Already exists above

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
    public static final String ROOM_MAINTENANCE = "maintenance";

    // Booking Status
    public static final String BOOKING_PENDING = "pending";
    public static final String BOOKING_ACCEPTED = "accepted";
    public static final String BOOKING_REJECTED = "rejected";
    public static final String BOOKING_WAITING_CHECKIN = "waiting_checkin";
    public static final String BOOKING_ACTIVE = "active";
    public static final String BOOKING_COMPLETED = "completed";
    public static final String BOOKING_CANCELLED = "cancelled";

    // Payment Status
    public static final String PAYMENT_UNPAID = "unpaid";
    public static final String PAYMENT_PENDING = "pending";
    public static final String PAYMENT_PAID = "paid";
    public static final String PAYMENT_REFUNDED = "refunded";

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
    // Providers
    public static final String PROVIDER_EMAIL = "email";
    public static final String PROVIDER_GOOGLE = "google";

    // User Fields
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_PROFILE_IMAGE_URL = "profileImageUrl";
    public static final String FIELD_PROVIDER = "provider";
    public static final String FIELD_EMAIL_VERIFIED = "emailVerified";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    // Kos & Room Fields
    public static final String FIELD_OWNER_ID = "ownerId";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_FACILITIES = "facilities";
    public static final String FIELD_IMAGE_URLS = "imageUrls";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_LATITUDE = "latitude";
    public static final String FIELD_LONGITUDE = "longitude";
    public static final String FIELD_AVAILABLE_ROOMS = "availableRooms";
    public static final String FIELD_IS_PREMIUM = "isPremium";
    public static final String FIELD_KOS_ID = "kosId";
    public static final String FIELD_KOS_NAME = "kosName";
    public static final String FIELD_ROOM_NAME = "roomName";
    public static final String FIELD_STATUS = "status";

    // Complaint Fields
    public static final String FIELD_STUDENT_ID = "studentId";
    public static final String FIELD_STUDENT_NAME = "studentName";
    public static final String FIELD_STUDENT_EMAIL = "studentEmail";
    public static final String FIELD_BOOKING_ID = "bookingId";
    public static final String FIELD_ROOM_ID = "roomId";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_EVIDENCE_IMAGE_URLS = "evidenceImageUrls";
    public static final String FIELD_OWNER_RESPONSE = "ownerResponse";
    public static final String FIELD_RESOLVED_AT = "resolvedAt";

    // Audit Fields
    public static final String FIELD_UPDATED_BY = "updatedBy";
    public static final String FIELD_STATUS_HISTORY = "statusHistory";

    // Chat Fields
    public static final String FIELD_OWNER_NAME = "ownerName";
    public static final String FIELD_LAST_MESSAGE = "lastMessage";
    public static final String FIELD_LAST_MESSAGE_AT = "lastMessageAt";
    public static final String FIELD_LAST_SENDER_ID = "lastSenderId";
    public static final String FIELD_STUDENT_UNREAD_COUNT = "studentUnreadCount";
    public static final String FIELD_OWNER_UNREAD_COUNT = "ownerUnreadCount";
    
    // Message Fields
    public static final String FIELD_SENDER_ID = "senderId";
    public static final String FIELD_SENDER_NAME = "senderName";
    public static final String FIELD_RECEIVER_ID = "receiverId";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_IS_READ = "isRead";

    // Message Types
    public static final String MESSAGE_TYPE_TEXT = "text";
}
