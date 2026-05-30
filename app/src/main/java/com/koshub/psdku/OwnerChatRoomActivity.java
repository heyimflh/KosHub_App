package com.koshub.psdku;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.adapters.MessageAdapter;
import com.koshub.psdku.models.Chat;
import com.koshub.psdku.models.Message;
import com.koshub.psdku.repositories.ChatRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.NavigationTransitionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * OwnerChatRoomActivity - Improved dynamic detail conversation room.
 * Refactored to support realtime Firestore chat.
 */
public class OwnerChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "KosHubChatRoom";

    private ImageView btnBackChat, btnSendMessage, btnAttach, btnMoreOptions;
    private TextView tvRoomUserName, tvRoomUserStatus, tvRoomAvatarInitial;
    private TextView tvContextTitle, tvContextSubtitle, tvContextStatus;
    private TextView btnTemplatePesan, btnTerimaBooking, btnTolakBooking, btnViewBookingDetail;
    private EditText etMessage;
    
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private ListenerRegistration messageListener;

    private RelativeLayout layoutImagePreview;
    private ImageView ivPreview, btnCancelPreview;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private String chatId, opponentName, kosName, roomNumber, status, initial;
    private String userRole = DatabaseConstants.ROLE_OWNER; // Default to owner if opened from Owner activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_chat_room);

        initImagePicker();
        handleIntentData();
        initViews();
        setupRecyclerView();
        setupListeners();
        determineRoleAndListen();
    }

    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        showImagePreview(uri);
                    }
                }
        );
    }

    private void showImagePreview(Uri uri) {
        layoutImagePreview.setVisibility(View.VISIBLE);
        ivPreview.setImageURI(uri);
    }

    private void hideImagePreview() {
        selectedImageUri = null;
        layoutImagePreview.setVisibility(View.GONE);
        ivPreview.setImageURI(null);
    }

    private void initViews() {
        btnBackChat = findViewById(R.id.btnBackChat);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnAttach = findViewById(R.id.btnAttach);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);

        tvRoomUserName = findViewById(R.id.tvRoomUserName);
        tvRoomUserStatus = findViewById(R.id.tvRoomUserStatus);
        tvRoomAvatarInitial = findViewById(R.id.tvRoomAvatarInitial);
        
        tvContextTitle = findViewById(R.id.tvContextTitle);
        tvContextSubtitle = findViewById(R.id.tvContextSubtitle);
        tvContextStatus = findViewById(R.id.tvContextStatus);

        btnTemplatePesan = findViewById(R.id.btnTemplatePesan);
        btnTerimaBooking = findViewById(R.id.btnTerimaBooking);
        btnTolakBooking = findViewById(R.id.btnTolakBooking);
        btnViewBookingDetail = findViewById(R.id.btnViewBookingDetail);

        etMessage = findViewById(R.id.etMessage);
        rvMessages = findViewById(R.id.rvMessages);

        layoutImagePreview = findViewById(R.id.layoutImagePreview);
        ivPreview = findViewById(R.id.ivPreview);
        btnCancelPreview = findViewById(R.id.btnCancelPreview);
        
        populateHeader();
        populateContextCard();
    }

    private void handleIntentData() {
        chatId = getIntent().getStringExtra("CHAT_ID");
        opponentName = getIntent().getStringExtra("USER_NAME");
        kosName = getIntent().getStringExtra("KOS_NAME");
        roomNumber = getIntent().getStringExtra("KAMAR");
        status = getIntent().getStringExtra("STATUS");
        initial = getIntent().getStringExtra("INITIAL");

        // If opened from student side, we might need to adjust role detection
        // For simplicity, we'll check the current user's role in Firestore if needed, 
        // but usually, the activity knows its context.
    }

    private void setupRecyclerView() {
        String uid = FirebaseAuth.getInstance().getUid();
        adapter = new MessageAdapter(messages, uid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void determineRoleAndListen() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    userRole = documentSnapshot.getString(DatabaseConstants.FIELD_ROLE);
                    if (chatId != null) {
                        startListening();
                        markAsRead();
                    } else {
                        // If no chatId, we might be coming from Kos Detail or Booking
                        handleInitializationFromIds();
                    }
                });
    }

    private void handleInitializationFromIds() {
        String kosId = getIntent().getStringExtra("KOS_ID");
        String bookingId = getIntent().getStringExtra("BOOKING_ID");

        if (bookingId != null) {
            ChatRepository.getInstance().getOrCreateChatFromBooking(bookingId, new ChatRepository.ChatCallback() {
                @Override
                public void onSuccess(Chat chat) {
                    chatId = chat.getId();
                    startListening();
                    markAsRead();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(OwnerChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (kosId != null) {
            ChatRepository.getInstance().getOrCreateChatFromKos(kosId, new ChatRepository.ChatCallback() {
                @Override
                public void onSuccess(Chat chat) {
                    chatId = chat.getId();
                    startListening();
                    markAsRead();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(OwnerChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startListening() {
        if (chatId == null) return;
        
        messageListener = ChatRepository.getInstance().listenMessages(chatId, new ChatRepository.MessageListListener() {
            @Override
            public void onMessagesUpdated(List<Message> updatedMessages) {
                messages.clear();
                messages.addAll(updatedMessages);
                adapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, message);
            }
        });
    }

    private void markAsRead() {
        if (chatId == null) return;
        ChatRepository.getInstance().markMessagesAsRead(chatId, userRole, new ChatRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Done
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to mark as read: " + message);
            }
        });
    }

    private void populateHeader() {
        tvRoomUserName.setText(opponentName != null ? opponentName : "Chat Room");
        tvRoomAvatarInitial.setText(initial != null ? initial : "?");
        tvRoomUserStatus.setText(status != null ? status : "Realtime Chat");
    }

    private void populateContextCard() {
        if (kosName == null) {
            findViewById(R.id.cardContext).setVisibility(View.GONE);
            return;
        }
        tvContextTitle.setText(kosName);
        tvContextStatus.setText(status != null ? status : "Aktif");
        tvContextSubtitle.setText(roomNumber != null ? "Kamar " + roomNumber : kosName);
    }

    private void setupListeners() {
        btnBackChat.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));
        btnSendMessage.setOnClickListener(v -> sendMessage());
        btnTemplatePesan.setOnClickListener(v -> showTemplateDialog());
        
        btnAttach.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnCancelPreview.setOnClickListener(v -> hideImagePreview());
        btnMoreOptions.setOnClickListener(v -> showToast("⚙️ Opsi lainnya segera hadir."));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        
        if (selectedImageUri != null) {
            uploadAndSendImage(selectedImageUri, text);
            return;
        }

        if (text.isEmpty()) return;

        if (chatId == null) {
            Toast.makeText(this, "Gagal mengidentifikasi ruang chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendMessage.setEnabled(false);
        ChatRepository.getInstance().sendMessage(chatId, text, DatabaseConstants.MESSAGE_TYPE_TEXT, new ChatRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                etMessage.setText("");
                btnSendMessage.setEnabled(true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(OwnerChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                btnSendMessage.setEnabled(true);
            }
        });
    }

    private void uploadAndSendImage(Uri imageUri, String text) {
        btnSendMessage.setEnabled(false);
        showToast("Mengirim gambar...");
        
        CloudinaryRepository.getInstance().uploadChatMessage(this, imageUri, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    hideImagePreview();
                    etMessage.setText("");
                    btnSendMessage.setEnabled(true);
                    
                    ChatRepository.getInstance().sendMessage(chatId, text, imageUrl, DatabaseConstants.MESSAGE_TYPE_IMAGE, new ChatRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            rvMessages.smoothScrollToPosition(messages.size());
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Gagal mengirim gambar: " + message);
                        }
                    });
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnSendMessage.setEnabled(true);
                    showToast("Gagal mengunggah gambar: " + message);
                });
            }
        });
    }

    private void showTemplateDialog() {
        String[] templates = {
                "Halo, apakah kamar masih tersedia?",
                "Kapan saya bisa cek lokasi?",
                "Baik Pak, saya segera kabari.",
                "Terima kasih atas informasinya."
        };

        new AlertDialog.Builder(this)
                .setTitle("Pilih Template")
                .setItems(templates, (dialog, which) -> {
                    etMessage.setText(templates[which]);
                    etMessage.setSelection(templates[which].length());
                })
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
