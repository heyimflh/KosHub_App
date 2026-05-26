package com.koshub.psdku.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.koshub.psdku.models.Chat;
import com.koshub.psdku.models.Message;
import com.koshub.psdku.repositories.NotificationRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Realtime Chat functionality.
 */
public class ChatRepository {
    private static final String TAG = "KosHubChat";
    private static ChatRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private ChatRepository() {
        this.db = FirebaseService.getFirestore();
        this.auth = FirebaseService.getAuth();
    }

    public static synchronized ChatRepository getInstance() {
        if (instance == null) {
            instance = new ChatRepository();
        }
        return instance;
    }

    public interface ChatCallback {
        void onSuccess(Chat chat);
        void onError(String message);
    }

    public interface ChatListListener {
        void onChatsUpdated(List<Chat> chats);
        void onError(String message);
    }

    public interface MessageListListener {
        void onMessagesUpdated(List<Message> messages);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    private String generateChatId(String studentId, String ownerId, String kosId) {
        if (kosId == null || kosId.isEmpty()) {
            // Fallback strategy could be studentId + ownerId + bookingId but let's stick to kosId for now
            return studentId + "_" + ownerId + "_general";
        }
        return studentId + "_" + ownerId + "_" + kosId;
    }

    public void getOrCreateChatRoom(String studentId, String ownerId, String kosId, String kosName, String bookingId, ChatCallback callback) {
        String chatId = generateChatId(studentId, ownerId, kosId);
        DocumentReference chatRef = db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId);

        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Chat chat = documentSnapshot.toObject(Chat.class);
                if (chat != null) {
                    chat.setId(documentSnapshot.getId());
                    callback.onSuccess(chat);
                    return;
                }
            }

            // Room doesn't exist, create it. Need names first.
            fetchNamesAndCreateRoom(chatId, studentId, ownerId, kosId, kosName, bookingId, callback);
        }).addOnFailureListener(e -> {
            if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                callback.onError("Kamu tidak memiliki izin untuk mengakses ruang chat ini.");
            } else {
                callback.onError("Gagal mengecek ruang chat: " + e.getMessage());
            }
        });
    }

    private void fetchNamesAndCreateRoom(String chatId, String studentId, String ownerId, String kosId, String kosName, String bookingId, ChatCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_USERS).document(studentId).get()
                .addOnSuccessListener(studentDoc -> {
                    String studentName = studentDoc.getString(DatabaseConstants.FIELD_NAME);
                    db.collection(DatabaseConstants.COLLECTION_USERS).document(ownerId).get()
                            .addOnSuccessListener(ownerDoc -> {
                                String ownerName = ownerDoc.getString(DatabaseConstants.FIELD_NAME);

                                Chat chat = new Chat(chatId, studentId, studentName, ownerId, ownerName, kosId, kosName, bookingId);
                                db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId).set(chat)
                                        .addOnSuccessListener(aVoid -> callback.onSuccess(chat))
                                        .addOnFailureListener(e -> callback.onError("Gagal membuat ruang chat."));
                            })
                            .addOnFailureListener(e -> callback.onError("Gagal memuat profil pemilik."));
                })
                .addOnFailureListener(e -> callback.onError("Gagal memuat profil student."));
    }

    public void getOrCreateChatFromKos(String kosId, ChatCallback callback) {
        String studentId = auth.getUid();
        if (studentId == null) {
            callback.onError("Kamu harus login terlebih dahulu.");
            return;
        }

        db.collection(DatabaseConstants.COLLECTION_KOS).document(kosId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String ownerId = documentSnapshot.getString(DatabaseConstants.FIELD_OWNER_ID);
                    String kosName = documentSnapshot.getString(DatabaseConstants.FIELD_NAME);
                    if (ownerId == null) {
                        callback.onError("Data pemilik kos tidak ditemukan.");
                        return;
                    }
                    getOrCreateChatRoom(studentId, ownerId, kosId, kosName, null, callback);
                })
                .addOnFailureListener(e -> callback.onError("Gagal memuat data kos."));
    }

    public void getOrCreateChatFromBooking(String bookingId, ChatCallback callback) {
        String studentId = auth.getUid();
        if (studentId == null) {
            callback.onError("Kamu harus login terlebih dahulu.");
            return;
        }

        db.collection(DatabaseConstants.COLLECTION_BOOKINGS).document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String bStudentId = documentSnapshot.getString(DatabaseConstants.FIELD_STUDENT_ID);
                    if (!studentId.equals(bStudentId)) {
                        callback.onError("Akses tidak diizinkan.");
                        return;
                    }
                    String ownerId = documentSnapshot.getString(DatabaseConstants.FIELD_OWNER_ID);
                    String kosId = documentSnapshot.getString(DatabaseConstants.FIELD_KOS_ID);
                    String kosName = documentSnapshot.getString(DatabaseConstants.FIELD_KOS_NAME);
                    getOrCreateChatRoom(studentId, ownerId, kosId, kosName, bookingId, callback);
                })
                .addOnFailureListener(e -> callback.onError("Gagal memuat data booking."));
    }

    public void sendMessage(String chatId, String text, SimpleCallback callback) {
        String senderId = auth.getUid();
        if (senderId == null || text.trim().isEmpty()) {
            callback.onError("Pesan tidak valid.");
            return;
        }

        db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId).get()
                .addOnSuccessListener(chatDoc -> {
                    Chat chat = chatDoc.toObject(Chat.class);
                    if (chat == null) {
                        callback.onError("Ruang chat tidak ditemukan.");
                        return;
                    }

                    String receiverId = senderId.equals(chat.getStudentId()) ? chat.getOwnerId() : chat.getStudentId();
                    
                    // Fetch sender name from current user profile or use field from chat
                    db.collection(DatabaseConstants.COLLECTION_USERS).document(senderId).get()
                            .addOnSuccessListener(userDoc -> {
                                String senderName = userDoc.getString(DatabaseConstants.FIELD_NAME);

                                String messageId = db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId)
                                        .collection(DatabaseConstants.COLLECTION_MESSAGES).document().getId();

                                Message message = new Message(messageId, chatId, senderId, senderName, receiverId, text, DatabaseConstants.MESSAGE_TYPE_TEXT);

                                WriteBatch batch = db.batch();

                                // 1. Add Message
                                DocumentReference msgRef = db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId)
                                        .collection(DatabaseConstants.COLLECTION_MESSAGES).document(messageId);
                                batch.set(msgRef, message);

                                // 2. Update Chat Metadata
                                Map<String, Object> chatUpdates = new HashMap<>();
                                chatUpdates.put(DatabaseConstants.FIELD_LAST_MESSAGE, text);
                                chatUpdates.put(DatabaseConstants.FIELD_LAST_MESSAGE_AT, message.getCreatedAt());
                                chatUpdates.put(DatabaseConstants.FIELD_LAST_SENDER_ID, senderId);
                                chatUpdates.put(DatabaseConstants.FIELD_UPDATED_AT, message.getCreatedAt());

                                if (receiverId.equals(chat.getOwnerId())) {
                                    chatUpdates.put(DatabaseConstants.FIELD_OWNER_UNREAD_COUNT, chat.getOwnerUnreadCount() + 1);
                                } else {
                                    chatUpdates.put(DatabaseConstants.FIELD_STUDENT_UNREAD_COUNT, chat.getStudentUnreadCount() + 1);
                                }

                                DocumentReference chatRef = db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId);
                                batch.update(chatRef, chatUpdates);

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            // Trigger Notification
                                            NotificationRepository.getInstance().createNotification(
                                                    receiverId,
                                                    senderId,
                                                    DatabaseConstants.NOTIF_CHAT_MESSAGE,
                                                    "Pesan baru",
                                                    senderName + ": " + (text.length() > 60 ? text.substring(0, 60) + "..." : text),
                                                    DatabaseConstants.TARGET_CHAT,
                                                    chatId
                                            );
                                            callback.onSuccess();
                                        })
                                        .addOnFailureListener(e -> callback.onError("Gagal mengirim pesan: " + e.getMessage()));
                            });
                });
    }

    public ListenerRegistration listenMessages(String chatId, MessageListListener listener) {
        return db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId)
                .collection(DatabaseConstants.COLLECTION_MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen messages error: " + error.getMessage());
                        if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            listener.onError("Akses ditolak. Kamu bukan bagian dari chat ini.");
                        } else {
                            listener.onError("Gagal memuat pesan realtime.");
                        }
                        return;
                    }

                    if (value != null) {
                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            messages.add(doc.toObject(Message.class));
                        }
                        listener.onMessagesUpdated(messages);
                    }
                });
    }

    public ListenerRegistration listenChatsForCurrentUser(ChatListListener listener) {
        String uid = auth.getUid();
        if (uid == null) {
            listener.onError("User tidak login.");
            return null;
        }

        // We need to query by role. Let's find out the role first.
        db.collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString(DatabaseConstants.FIELD_ROLE);
                    String field = DatabaseConstants.ROLE_OWNER.equals(role) ? DatabaseConstants.FIELD_OWNER_ID : DatabaseConstants.FIELD_STUDENT_ID;

                    Query query = db.collection(DatabaseConstants.COLLECTION_CHATS).whereEqualTo(field, uid);

                    query.addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Listen chats error: " + error.getMessage());
                            if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                listener.onError("Akses ditolak. Silakan login ulang.");
                            } else {
                                listener.onError("Gagal memuat daftar chat.");
                            }
                            return;
                        }

                        if (value != null) {
                            List<Chat> chats = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                Chat c = doc.toObject(Chat.class);
                                c.setId(doc.getId());
                                chats.add(c);
                            }
                            // Sort manual by lastMessageAt descending
                            Collections.sort(chats, (c1, c2) -> Long.compare(c2.getLastMessageAt(), c1.getLastMessageAt()));
                            listener.onChatsUpdated(chats);
                        }
                    });
                });
        
        return null; // Note: This implementation is tricky because of the async role fetch.
        // Better: Caller should pass role if known, or we fetch once at start.
    }
    
    // Improved listenChats with role parameter
    public ListenerRegistration listenChatsByRole(String uid, String role, ChatListListener listener) {
        String field = DatabaseConstants.ROLE_OWNER.equals(role) ? DatabaseConstants.FIELD_OWNER_ID : DatabaseConstants.FIELD_STUDENT_ID;
        return db.collection(DatabaseConstants.COLLECTION_CHATS)
                .whereEqualTo(field, uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen chats error: " + error.getMessage());
                        if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            listener.onError("Akses ditolak. Silakan login ulang.");
                        } else {
                            listener.onError("Gagal memuat daftar chat.");
                        }
                        return;
                    }

                    if (value != null) {
                        List<Chat> chats = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Chat c = doc.toObject(Chat.class);
                            c.setId(doc.getId());
                            chats.add(c);
                        }
                        Collections.sort(chats, (c1, c2) -> Long.compare(c2.getLastMessageAt(), c1.getLastMessageAt()));
                        listener.onChatsUpdated(chats);
                    }
                });
    }

    public void markMessagesAsRead(String chatId, String role, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) return;

        DocumentReference chatRef = db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId);
        String unreadField = DatabaseConstants.ROLE_OWNER.equals(role) ? DatabaseConstants.FIELD_OWNER_UNREAD_COUNT : DatabaseConstants.FIELD_STUDENT_UNREAD_COUNT;

        chatRef.update(unreadField, 0)
                .addOnSuccessListener(aVoid -> {
                    // Also mark messages as isRead = true where receiverId == uid
                    db.collection(DatabaseConstants.COLLECTION_CHATS).document(chatId)
                            .collection(DatabaseConstants.COLLECTION_MESSAGES)
                            .whereEqualTo(DatabaseConstants.FIELD_RECEIVER_ID, uid)
                            .whereEqualTo(DatabaseConstants.FIELD_IS_READ, false)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (queryDocumentSnapshots.isEmpty()) {
                                    callback.onSuccess();
                                    return;
                                }
                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    batch.update(doc.getReference(), DatabaseConstants.FIELD_IS_READ, true);
                                }
                                batch.commit().addOnSuccessListener(v -> callback.onSuccess());
                            });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
