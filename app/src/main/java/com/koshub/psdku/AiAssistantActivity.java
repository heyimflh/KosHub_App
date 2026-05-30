package com.koshub.psdku;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

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
import com.koshub.psdku.utils.AiLocalFaqEngine;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get role from intent if available
        if (getIntent() != null && getIntent().hasExtra("role")) {
            String role = getIntent().getStringExtra("role");
            if (!TextUtils.isEmpty(role)) {
                currentRole = role;
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
        showGreeting();
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

        findViewById(R.id.qqBooking).setOnClickListener(v -> sendQuickQuestion("Bagaimana cara booking kos?"));
        findViewById(R.id.qqPayment).setOnClickListener(v -> sendQuickQuestion("Kenapa pembayaran saya belum berhasil?"));
        findViewById(R.id.qqChat).setOnClickListener(v -> sendQuickQuestion("Kenapa chat owner tidak muncul?"));
        findViewById(R.id.qqComplaint).setOnClickListener(v -> sendQuickQuestion("Bagaimana cara mengirim komplain?"));
    }

    private void showGreeting() {
        addAiMessage("Halo! Saya Asisten KosHub. Saya bisa bantu menjawab kendala seputar booking, pembayaran, chat owner, komplain, profil, dan pengelolaan kos. Ada yang bisa saya bantu?");
    }

    private void sendMessage() {
        String text = etMessageAi.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            addUserMessage(text);
            etMessageAi.setText("");
            processAiResponse(text);
        }
    }

    private void sendQuickQuestion(String text) {
        addUserMessage(text);
        processAiResponse(text);
    }

    private void processAiResponse(String userMessage) {
        // Disable send button while processing
        btnSendAi.setEnabled(false);
        btnSendAi.setAlpha(0.5f);

        // Try Gemini API first
        aiRepository.askKosHubAssistant(userMessage, currentRole, messageList, new AiAssistantRepository.AiCallback() {
            @Override
            public void onSuccess(String answer) {
                runOnUiThread(() -> {
                    addAiMessage(answer);
                    btnSendAi.setEnabled(true);
                    btnSendAi.setAlpha(1.0f);
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Fallback to Local FAQ Engine
                runOnUiThread(() -> {
                    String localAnswer = AiLocalFaqEngine.findBestAnswer(userMessage, currentRole);
                    String fallbackMessage = localAnswer + "\n\n(Catatan: Jawaban ini dari pusat bantuan lokal karena AI online sedang tidak tersedia.)";
                    addAiMessage(fallbackMessage);
                    btnSendAi.setEnabled(true);
                    btnSendAi.setAlpha(1.0f);
                });
            }
        });
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
    }
}
