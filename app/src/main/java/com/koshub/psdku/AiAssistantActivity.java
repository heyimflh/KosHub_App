package com.koshub.psdku;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.koshub.psdku.adapters.AiMessageAdapter;
import com.koshub.psdku.models.AiMessage;
import com.koshub.psdku.repositories.AiAssistantRepository;
import com.koshub.psdku.utils.AiChatHistoryManager;
import com.koshub.psdku.utils.AiLocalFaqEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AiAssistantActivity extends AppCompatActivity {

    private RecyclerView rvAiChat;
    private AiMessageAdapter adapter;
    private List<AiMessage> messageList;
    private EditText etMessageAi;
    private View btnSendAi;
    private View navbarAi;
    private View bottomContainer;
    private View quickQuestionsArea;
    private AiAssistantRepository aiRepository;
    private String currentRole = "student";
    private String currentUserName = "Kak";
    private boolean isWaitingForAiResponse = false;

    private static final Map<String, String[]> TOPIC_CHIPS = new HashMap<String, String[]>() {{
        put("booking", new String[]{"Status booking saya", "Cara bayar", "Chat owner kos"});
        put("pembayaran", new String[]{"Kenapa bayar gagal?", "Cara cek status bayar", "Minta refund"});
        put("chat", new String[]{"Owner tidak balas", "Cara komplain", "Cara booking kos"});
        put("komplain", new String[]{"Status komplain saya", "Hubungi CS KosHub", "Cara booking kos"});
        put("cari", new String[]{"Cara booking kos", "Cara chat owner", "Simpan ke favorit"});
        put("default", new String[]{"Cara booking kos", "Masalah pembayaran", "Chat owner", "Kirim komplain"});
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Priority 1: Intent extra
        if (getIntent() != null) {
            if (getIntent().hasExtra("role")) {
                currentRole = getIntent().getStringExtra("role");
            }
            if (getIntent().hasExtra("userName")) {
                String name = getIntent().getStringExtra("userName");
                if (name != null && !name.trim().isEmpty()) {
                    currentUserName = name.trim().split(" ")[0];
                }
            }
        }

        // Priority 2: SessionManager fallback
        com.koshub.psdku.utils.SessionManager session = new com.koshub.psdku.utils.SessionManager(this);
        if ((currentRole == null || currentRole.isEmpty()) && session.isLoggedIn()) {
            currentRole = session.getUserRole();
        }

        // Priority 3: Default to student & Normalization
        if (currentRole == null || currentRole.isEmpty()) {
            currentRole = "student";
        } else {
            currentRole = currentRole.toLowerCase();
            if (!currentRole.equals("owner") && !currentRole.equals("student")) {
                currentRole = "student";
            }
        }

        // Edge-to-edge support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_ai_assistant);

        aiRepository = new AiAssistantRepository();
        initViews();
        handleSafeAreas();
        setupRecyclerView();
        setupListeners();
        setupQuickQuestions(); // Populate chips based on role
        
        // Load Chat History
        List<AiMessage> savedMessages = AiChatHistoryManager.loadMessages(this, currentRole);
        if (!savedMessages.isEmpty()) {
            messageList.addAll(savedMessages);
            adapter.notifyDataSetChanged();
            rvAiChat.scrollToPosition(messageList.size() - 1);
        } else {
            showGreeting();
        }

        updateSuggestionsVisibility();
    }

    private void initViews() {
        rvAiChat = findViewById(R.id.rvAiChat);
        etMessageAi = findViewById(R.id.etMessageAi);
        btnSendAi = findViewById(R.id.btnSendAi);
        navbarAi = findViewById(R.id.navbarAi);
        bottomContainer = findViewById(R.id.bottomContainer);
        quickQuestionsArea = findViewById(R.id.quickQuestionsArea);
        findViewById(R.id.btnBackAi).setOnClickListener(v -> finish());
    }

    private void handleSafeAreas() {
        // Handle Top Safe Area (Status Bar)
        ViewCompat.setOnApplyWindowInsetsListener(navbarAi, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        // Handle Bottom Safe Area (Navigation Bar + Keyboard)
        ViewCompat.setOnApplyWindowInsetsListener(bottomContainer, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            
            // Determine which inset is relevant (keyboard or nav bar)
            int bottomInset = Math.max(systemBars.bottom, ime.bottom);
            
            // Hide quick questions when keyboard is up to save space
            if (ime.bottom > 0) {
                quickQuestionsArea.setVisibility(View.GONE);
                // Extra padding when keyboard is up for better spacing
                v.setPadding(0, 0, 0, bottomInset);
                
                // Auto scroll to bottom when keyboard opens
                rvAiChat.postDelayed(() -> {
                    if (!messageList.isEmpty()) {
                        rvAiChat.scrollToPosition(messageList.size() - 1);
                    }
                }, 100);
            } else {
                quickQuestionsArea.setVisibility(View.VISIBLE);
                // When keyboard is down, respect system navigation bar + default spacing
                v.setPadding(0, 0, 0, bottomInset + (int)(8 * getResources().getDisplayMetrics().density));
            }
            
            return windowInsets;
        });
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new AiMessageAdapter(messageList);
        
        adapter.setFeedbackListener((position) -> {
            addAiMessage("Maaf jawabannya kurang membantu ya, Kak. Untuk kendala yang lebih spesifik, kamu bisa menghubungi tim KosHub langsung melalui menu Pusat Bantuan di halaman Profil.");
        });

        rvAiChat.setLayoutManager(new LinearLayoutManager(this));
        rvAiChat.setAdapter(adapter);

        // Hide keyboard when scrolling
        rvAiChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private void setupListeners() {
        btnSendAi.setOnClickListener(v -> sendMessage());
        
        View btnClear = findViewById(R.id.btnClearChat);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> showClearChatConfirmation());
        }
    }

    private void showClearChatConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Bersihkan chat?")
                .setMessage("Riwayat percakapan Asisten KosHub di perangkat ini akan dihapus.")
                .setPositiveButton("Bersihkan", (dialog, which) -> clearChat())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void clearChat() {
        AiChatHistoryManager.clearMessages(this, currentRole);
        messageList.clear();
        adapter.notifyDataSetChanged();
        showGreeting();
        updateSuggestionsVisibility();
        android.widget.Toast.makeText(this, "Riwayat chat dibersihkan", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setupQuickQuestions() {
        android.widget.LinearLayout container = findViewById(R.id.quickQuestionsContainer);
        if (container == null) return;
        
        container.removeAllViews();
        
        String[] questions;
        if ("owner".equals(currentRole)) {
            questions = new String[]{
                    "Cara menambah kos",
                    "Cara menambah kamar",
                    "Update status kamar",
                    "Menerima booking mahasiswa",
                    "Menolak booking mahasiswa",
                    "Chat mahasiswa tidak muncul",
                    "Upload foto kos",
                    "Lihat laporan keuangan"
            };
        } else {
            questions = new String[]{
                    "Cara booking kos",
                    "Masalah pembayaran",
                    "Chat owner tidak muncul",
                    "Kirim komplain",
                    "Lihat riwayat booking",
                    "Apa itu waiting list?",
                    "Cara memberi ulasan",
                    "Cara ubah profil"
            };
        }

        for (String q : questions) {
            TextView chip = new TextView(this);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, (int)(10 * getResources().getDisplayMetrics().density), 0);
            chip.setLayoutParams(params);
            chip.setBackgroundResource(R.drawable.bg_ai_chip);
            chip.setPadding(
                    (int)(16 * getResources().getDisplayMetrics().density),
                    (int)(10 * getResources().getDisplayMetrics().density),
                    (int)(16 * getResources().getDisplayMetrics().density),
                    (int)(10 * getResources().getDisplayMetrics().density)
            );
            chip.setText(q);
            chip.setTextColor(getResources().getColor(R.color.home_text_dark));
            chip.setTextSize(13);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> sendQuickQuestion(q));
            
            container.addView(chip);
        }
    }

    private void showGreeting() {
        String greeting;
        String namePart = currentUserName.equals("Kak") ? "Kak" : currentUserName;
        
        if ("owner".equals(currentRole)) {
            greeting = "Halo, " + namePart + "! Saya Asisten KosHub. Saya bisa bantu mengelola kos, kamar, booking mahasiswa, chat, komplain, upload foto, dan laporan. Ada yang bisa saya bantu?";
        } else {
            greeting = "Halo, " + namePart + "! Saya Asisten KosHub. Saya bisa bantu soal cari kos, booking, pembayaran, chat owner, komplain, riwayat booking, dan ulasan. Ada yang mau ditanyakan?";
        }

        addAiMessage(greeting);
        // Save history after greeting
        AiChatHistoryManager.saveMessages(this, currentRole, messageList);
    }

    private void sendMessage() {
        if (isWaitingForAiResponse) return;

        String text = etMessageAi.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (text.length() > 500) {
            android.widget.Toast.makeText(this, "Pertanyaan terlalu panjang, coba ringkas dulu ya.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        addUserMessage(text);
        etMessageAi.setText("");
        processAiResponse(text);
    }

    private void sendQuickQuestion(String text) {
        if (isWaitingForAiResponse) return;
        addUserMessage(text);
        processAiResponse(text);
    }

    private void processAiResponse(String userMessage) {
        setLoadingState(true);
        showTypingIndicator();

        // Try Gemini API first
        aiRepository.askKosHubAssistant(userMessage, currentRole, currentUserName, messageList, new AiAssistantRepository.AiCallback() {
            @Override
            public void onSuccess(String answer) {
                runOnUiThread(() -> {
                    removeTypingIndicator();
                    addAiMessage(answer);
                    updateQuickChips(answer);
                    setLoadingState(false);
                    AiChatHistoryManager.saveMessages(AiAssistantActivity.this, currentRole, messageList);
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Fallback to Local FAQ Engine
                runOnUiThread(() -> {
                    removeTypingIndicator();
                    String localAnswer = AiLocalFaqEngine.findBestAnswer(userMessage, currentRole);
                    String friendlyAnswer = "Untuk sementara saya jawab berdasarkan pusat bantuan lokal KosHub ya, Kak.\n\n" + localAnswer;
                    addAiMessage(friendlyAnswer);
                    updateQuickChips(localAnswer);
                    setLoadingState(false);
                    AiChatHistoryManager.saveMessages(AiAssistantActivity.this, currentRole, messageList);
                });
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        isWaitingForAiResponse = isLoading;
        btnSendAi.setEnabled(!isLoading);
        btnSendAi.setAlpha(isLoading ? 0.5f : 1.0f);
        etMessageAi.setEnabled(!isLoading);

        // Disable chips
        android.widget.LinearLayout container = findViewById(R.id.quickQuestionsContainer);
        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                child.setEnabled(!isLoading);
                child.setAlpha(isLoading ? 0.5f : 1.0f);
            }
        }
    }

    private void updateQuickChips(String aiResponse) {
        String lowerResponse = aiResponse.toLowerCase();
        String[] newChips;
        
        if (lowerResponse.contains("booking") || lowerResponse.contains("pesan kamar")) {
            newChips = TOPIC_CHIPS.get("booking");
        } else if (lowerResponse.contains("bayar") || lowerResponse.contains("pembayaran") || lowerResponse.contains("qris")) {
            newChips = TOPIC_CHIPS.get("pembayaran");
        } else if (lowerResponse.contains("chat") || lowerResponse.contains("owner")) {
            newChips = TOPIC_CHIPS.get("chat");
        } else if (lowerResponse.contains("komplain") || lowerResponse.contains("keluhan")) {
            newChips = TOPIC_CHIPS.get("komplain");
        } else if (lowerResponse.contains("cari") || lowerResponse.contains("filter") || lowerResponse.contains("beranda")) {
            newChips = TOPIC_CHIPS.get("cari");
        } else {
            newChips = TOPIC_CHIPS.get("default");
        }
        
        // Update chip text di layout
        // Ambil semua chip dari quickQuestionsContainer
        android.widget.LinearLayout container = findViewById(R.id.quickQuestionsContainer);
        if (container == null || newChips == null) return;
        
        for (int i = 0; i < container.getChildCount() && i < newChips.length; i++) {
            android.view.View child = container.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                final String chipText = newChips[i];
                ((android.widget.TextView) child).setText(chipText);
                child.setOnClickListener(v -> sendQuickQuestion(chipText));
            }
        }
    }

    private void showTypingIndicator() {
        AiMessage message = new AiMessage(
                "typing",
                "Asisten sedang menyiapkan jawaban...",
                "ai",
                System.currentTimeMillis()
        );
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvAiChat.post(() -> rvAiChat.smoothScrollToPosition(messageList.size() - 1));
    }

    private void removeTypingIndicator() {
        if (!messageList.isEmpty()) {
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if ("typing".equals(messageList.get(i).getId())) {
                    messageList.remove(i);
                    adapter.notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    private void addUserMessage(String text) {
        AiMessage message = new AiMessage(
                UUID.randomUUID().toString(),
                text,
                "user",
                System.currentTimeMillis()
        );
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        
        rvAiChat.post(() -> {
            if (adapter.getItemCount() > 0) {
                rvAiChat.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        // Save history after user message
        AiChatHistoryManager.saveMessages(this, currentRole, messageList);
        updateSuggestionsVisibility();
    }

    private void addAiMessage(String text) {
        AiMessage message = new AiMessage(
                UUID.randomUUID().toString(),
                text,
                "ai",
                System.currentTimeMillis()
        );
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        
        // Use post to ensure the RecyclerView has updated its layout
        rvAiChat.post(() -> {
            if (adapter.getItemCount() > 0) {
                rvAiChat.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        updateSuggestionsVisibility();
    }

    private void updateSuggestionsVisibility() {
        if (quickQuestionsArea == null) return;
        
        // Only show suggestions if chat is empty or only has the first greeting
        if (messageList.size() <= 1) {
            quickQuestionsArea.setVisibility(View.VISIBLE);
        } else {
            quickQuestionsArea.setVisibility(View.GONE);
        }
    }
}
