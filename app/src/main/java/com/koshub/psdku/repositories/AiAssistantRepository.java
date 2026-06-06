package com.koshub.psdku.repositories;

import android.util.Log;

import com.koshub.psdku.BuildConfig;
import com.koshub.psdku.models.AiMessage;
import com.koshub.psdku.utils.AiConfig;
import com.koshub.psdku.utils.AiLocalFaqEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AiAssistantRepository {

    private static final String TAG = "AiAssistantRepository";
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public interface AiCallback {
        void onSuccess(String answer);
        void onError(String errorMessage);
    }

    public AiAssistantRepository() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void askKosHubAssistant(String userMessage, String role, String userName, List<AiMessage> recentMessages, AiCallback callback) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            callback.onError("Pesan tidak boleh kosong.");
            return;
        }

        if (!AiConfig.isGeminiConfigured()) {
            Log.w(TAG, "Gemini not configured, trying Groq directly.");
            askGroqFallback(userMessage, role, userName, recentMessages, callback);
            return;
        }

        // Get local FAQ context for grounding
        String localFaqContext = AiLocalFaqEngine.findBestAnswer(userMessage, role);
        if (localFaqContext.contains("Maaf, saya belum menemukan jawaban yang tepat")) {
            localFaqContext = "";
        }

        String prompt = buildKosHubPrompt(userMessage, role, recentMessages, localFaqContext, userName);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + BuildConfig.GEMINI_MODEL + ":generateContent";

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();

            partObj.put("text", prompt);
            partsArray.put(partObj);
            contentObj.put("parts", partsArray);
            contentObj.put("role", "user");
            contentsArray.put(contentObj);
            jsonBody.put("contents", contentsArray);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.45);
            generationConfig.put("topP", 0.85);
            generationConfig.put("maxOutputTokens", 2048);
            generationConfig.put("stopSequences", new JSONArray()
                    .put("Ada hal lain yang")
                    .put("Semoga membantu ya")
                    .put("Silakan hubungi kami jika"));
            jsonBody.put("generationConfig", generationConfig);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.w(TAG, "Gemini failed, trying Groq fallback: " + e.getMessage());
                    askGroqFallback(userMessage, role, userName, recentMessages, callback);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "Gemini API error " + response.code() + ", trying Groq fallback.");
                            askGroqFallback(userMessage, role, userName, recentMessages, callback);
                            return;
                        }

                        if (responseBody == null) {
                            Log.w(TAG, "Gemini response body is null, trying Groq fallback.");
                            askGroqFallback(userMessage, role, userName, recentMessages, callback);
                            return;
                        }

                        String responseStr = responseBody.string();
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        
                        JSONArray candidates = jsonResponse.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.optJSONObject("content");
                            if (content != null) {
                                JSONArray parts = content.optJSONArray("parts");
                                if (parts != null && parts.length() > 0) {
                                    StringBuilder answerBuilder = new StringBuilder();
                                    for (int i = 0; i < parts.length(); i++) {
                                        JSONObject part = parts.getJSONObject(i);
                                        if (part.has("text")) {
                                            answerBuilder.append(part.getString("text"));
                                        }
                                    }
                                    
                                    String answer = answerBuilder.toString();
                                    if (!answer.trim().isEmpty()) {
                                        String cleaned = cleanAiResponse(answer);
                                        Log.d(TAG, "Gemini response length: " + answer.length() + " -> Cleaned: " + cleaned.length());
                                        callback.onSuccess(cleaned);
                                        return;
                                    }
                                }
                            }
                        }
                        Log.w(TAG, "Gemini response malformed, trying Groq fallback.");
                        askGroqFallback(userMessage, role, userName, recentMessages, callback);
                    } catch (Exception e) {
                        Log.w(TAG, "Gemini parse error, trying Groq fallback: " + e.getMessage());
                        askGroqFallback(userMessage, role, userName, recentMessages, callback);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Request error: " + e.getMessage());
        }
    }

    private String cleanAiResponse(String raw) {
        if (raw == null) return "";
        
        // 1. Pre-clean: Remove bold/italic markdown symbols
        String cleaned = raw.replaceAll("\\*\\*", "")
                .replaceAll("__", "")
                .replaceAll("###?\\s?", "")
                .trim();

        // 2. Line by line cleaning
        String[] lines = cleaned.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            // Remove common list bullets (*, -, #) but keep numbers (1., 2.)
            if (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ") || trimmedLine.startsWith("# ")) {
                if (trimmedLine.length() > 2) {
                    trimmedLine = trimmedLine.substring(2).trim();
                } else {
                    trimmedLine = "";
                }
            } else if (trimmedLine.startsWith("*") || trimmedLine.startsWith("-") || trimmedLine.startsWith("#")) {
                // Single char bullet without space
                if (trimmedLine.length() > 1 && !Character.isDigit(trimmedLine.charAt(1))) {
                    trimmedLine = trimmedLine.substring(1).trim();
                } else if (trimmedLine.length() == 1) {
                    trimmedLine = "";
                }
            }
            
            if (sb.length() > 0) sb.append("\n");
            sb.append(trimmedLine);
        }
        
        cleaned = sb.toString();

        // 3. Final Polish
        cleaned = cleaned.replaceAll("(?i)Kakak", "Kak")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        // 4. Truncation check (Safety fallback for hanging sentences)
        String lowerCleaned = cleaned.toLowerCase();
        if (cleaned.endsWith(":") || cleaned.endsWith(",") || 
            lowerCleaned.endsWith(" dan") || lowerCleaned.endsWith(" atau") || lowerCleaned.endsWith(" yaitu")) {
            
            // Find where the suffix starts
            int lastSpace = cleaned.lastIndexOf(' ');
            if (lastSpace != -1) {
                cleaned = cleaned.substring(0, lastSpace).trim();
                // Check again for punctuation after removing the word
                if (cleaned.endsWith(":") || cleaned.endsWith(",")) {
                    cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
                }
            } else {
                // Just remove the last character if it's single word ending with punct
                cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
            }
            cleaned += ".";
        }

        return cleaned;
    }

    private String buildKosHubPrompt(String userMessage, String role, List<AiMessage> recentMessages, String localFaqContext, String userName) {
        StringBuilder sb = new StringBuilder();

        sb.append("PERINGATAN UTAMA: Jawaban WAJIB selesai dalam 1 respons. MAKSIMAL 5 langkah jika perlu langkah. MAKSIMAL 4 kalimat untuk jawaban non-langkah. JANGAN buat kalimat menggantung.\n\n");
        sb.append("Kamu adalah Asisten KosHub, CS AI otomatis untuk aplikasi KosHub.\n\n");

        sb.append("Aturan Keras Gaya Jawaban:\n");
        sb.append("- Bahasa Indonesia ramah, ringan, dan to the point.\n");
        sb.append("- Panggil user dengan 'Kak' dengan aturan posisi: (a) Gunakan 'Kak' sebagai sapaan pembuka di awal kalimat pertama saja, contoh: 'Halo Kak!'. (b) Jika perlu menyebut user di tengah atau akhir kalimat, gunakan kata GANTI 'kamu', bukan 'Kak'. Contoh BENAR: 'Kamu bisa coba langkah ini'. Contoh SALAH: 'Kak bisa coba langkah ini'. (c) Boleh tambahkan 'Kak' di akhir kalimat penutup sebagai sapaan hangat, contoh: 'Semoga berhasil ya, Kak!'. (d) DILARANG keras menggunakan kata 'Kakak' dalam bentuk apapun.\n");
        sb.append("- JANGAN gunakan markdown, bold, italic, heading, atau tanda **.\n");
        sb.append("- Jawaban MAKSIMAL 5-8 kalimat pendek. Selesaikan jawaban sampai tuntas.\n");
        sb.append("- Jika butuh langkah, MAKSIMAL 5 langkah saja. Gunakan format angka 1, 2, 3.\n");
        sb.append("- Jangan mulai dengan 'Mohon maaf atas ketidaknyamanan' kecuali user komplain berat.\n");
        sb.append("- JANGAN buat jawaban menggantung atau terpotong. Pastikan kalimat terakhir adalah kalimat penutup yang lengkap.\n\n");
        
        sb.append("Konteks KosHub:\n");
        sb.append("KosHub adalah aplikasi pencarian dan manajemen kos.\n");
        sb.append("Fitur Student: cari kos, booking, pembayaran, chat owner, komplain.\n");
        sb.append("Fitur Owner: tambah kos/kamar, kelola booking, chat mahasiswa, laporan keuangan.\n\n");
        
        sb.append("Keamanan:\n");
        sb.append("- JANGAN minta password, OTP, KTP, atau rekening.\n");
        sb.append("- JANGAN nyatakan pembayaran berhasil jika status belum 'Berhasil'.\n\n");
        
        sb.append("Role User: ").append(role).append("\n\n");
        
        if ("student".equals(role)) {
            sb.append("INSTRUKSI ROLE STUDENT:\n");
            sb.append("- Prioritaskan jawaban untuk mahasiswa.\n");
            sb.append("- Fokus pada cari kos, detail kos, booking, pembayaran, chat owner, komplain, favorite, riwayat booking, dan ulasan.\n");
            sb.append("- Jika user bertanya fitur owner seperti tambah kos, jelaskan bahwa fitur itu hanya untuk akun owner.\n\n");
        } else if ("owner".equals(role)) {
            sb.append("INSTRUKSI ROLE OWNER:\n");
            sb.append("- Prioritaskan jawaban untuk pemilik kos.\n");
            sb.append("- Fokus pada dashboard owner, tambah/edit kos, tambah kamar, update status kamar, kelola booking, chat mahasiswa, komplain, upload foto, dan laporan/finance.\n");
            sb.append("- Jangan menjawab seolah user adalah mahasiswa.\n");
            sb.append("- Jika owner bertanya fitur student seperti booking kos, jelaskan bahwa fitur booking digunakan oleh mahasiswa dan owner bisa memantau booking masuk.\n\n");
        }

        sb.append("Nama User: ").append(userName).append(" (Gunakan nama ini hanya di sapaan pertama kalimat pertama saja. Setelah itu gunakan 'kamu'.)\n\n");
        
        if (localFaqContext != null && !localFaqContext.isEmpty()) {
            String context = localFaqContext;
            if (context.length() > 120) {
                context = context.substring(0, 120) + "...";
            }
            sb.append("Referensi (Tulis ulang dengan gaya natural, singkat, maks 5 kalimat):\n");
            sb.append(context).append("\n\n");
        }
        
        if (recentMessages != null && !recentMessages.isEmpty()) {
            sb.append("Chat History (Max 2):\n");
            int start = Math.max(0, recentMessages.size() - 2);
            for (int i = start; i < recentMessages.size(); i++) {
                AiMessage msg = recentMessages.get(i);
                String sender = "ai".equals(msg.getSenderType()) ? "Asisten" : "User";
                String text = msg.getMessage();
                if (text != null) {
                    if (text.length() > 200) text = text.substring(0, 200) + "...";
                    sb.append(sender).append(": ").append(text).append("\n");
                }
            }
            sb.append("\n");
        }
        
        sb.append("Pertanyaan user: ").append(userMessage).append("\n");
        sb.append("Jawab sekarang dengan natural, singkat, tuntas, dan tanpa markdown.");
        
        return sb.toString();
    }

    public void askGroqFallback(String userMessage, String role, String userName, List<AiMessage> recentMessages, AiCallback callback) {
        if (!AiConfig.isGroqConfigured()) {
            callback.onError("Groq API not configured.");
            return;
        }

        String localFaqContext = AiLocalFaqEngine.findBestAnswer(userMessage, role);
        if (localFaqContext.contains("Maaf, saya belum menemukan jawaban yang tepat")) {
            localFaqContext = "";
        }

        String systemPrompt = buildKosHubSystemPrompt(role, userName, localFaqContext);
        String userPromptText = buildKosHubUserPrompt(userMessage, recentMessages);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", BuildConfig.GROQ_MODEL);

            JSONArray messages = new JSONArray();

            // System message
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.put(systemMsg);

            // Add recent chat history (max 2 turns = 4 messages)
            if (recentMessages != null && !recentMessages.isEmpty()) {
                int start = Math.max(0, recentMessages.size() - 4);
                for (int i = start; i < recentMessages.size(); i++) {
                    AiMessage msg = recentMessages.get(i);
                    if (msg.getMessage() == null || msg.getMessage().isEmpty()) continue;
                    JSONObject histMsg = new JSONObject();
                    histMsg.put("role", "ai".equals(msg.getSenderType()) ? "assistant" : "user");
                    String text = msg.getMessage();
                    if (text.length() > 200) text = text.substring(0, 200) + "...";
                    histMsg.put("content", text);
                    messages.put(histMsg);
                }
            }

            // Current user message
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPromptText);
            messages.put(userMsg);

            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 1024);
            jsonBody.put("temperature", 0.45);
            jsonBody.put("top_p", 0.85);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(GROQ_API_URL)
                    .addHeader("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Groq network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful() || responseBody == null) {
                            callback.onError("Groq API Error: " + response.code());
                            return;
                        }

                        String responseStr = responseBody.string();
                        JSONObject jsonResponse = new JSONObject(responseStr);

                        JSONArray choices = jsonResponse.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject message = choice.optJSONObject("message");
                            if (message != null) {
                                String content = message.optString("content", "");
                                if (!content.trim().isEmpty()) {
                                    String cleaned = cleanAiResponse(content);
                                    Log.d(TAG, "Groq response received, length: " + cleaned.length());
                                    callback.onSuccess(cleaned);
                                    return;
                                }
                            }
                        }
                        callback.onError("Groq response empty or malformed.");
                    } catch (Exception e) {
                        callback.onError("Groq parse error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Groq request error: " + e.getMessage());
        }
    }

    private String buildKosHubSystemPrompt(String role, String userName, String localFaqContext) {
        StringBuilder sb = new StringBuilder();

        sb.append("PERINGATAN UTAMA: Jawaban WAJIB selesai dalam 1 respons. MAKSIMAL 5 langkah jika perlu langkah. MAKSIMAL 4 kalimat untuk jawaban non-langkah. JANGAN buat kalimat menggantung.\n\n");
        sb.append("Kamu adalah Asisten KosHub, CS AI otomatis untuk aplikasi KosHub.\n\n");
        sb.append("Aturan Keras Gaya Jawaban:\n");
        sb.append("- Bahasa Indonesia ramah, ringan, dan to the point.\n");
        sb.append("- Panggil user dengan 'Kak' dengan aturan posisi: (a) Gunakan 'Kak' sebagai sapaan pembuka di awal kalimat pertama saja. (b) Di tengah atau akhir kalimat, gunakan kata GANTI 'kamu', bukan 'Kak'. Contoh BENAR: 'Kamu bisa coba langkah ini'. Contoh SALAH: 'Kak bisa coba langkah ini'. (c) Boleh tambahkan 'Kak' di akhir kalimat penutup. (d) DILARANG keras menggunakan kata 'Kakak'.\n");
        sb.append("- JANGAN gunakan markdown, bold, italic, heading, atau tanda **.\n");
        sb.append("- Jika butuh langkah, MAKSIMAL 5 langkah saja. Gunakan format angka 1, 2, 3.\n");
        sb.append("- JANGAN buat jawaban menggantung atau terpotong.\n\n");
        sb.append("Konteks KosHub: aplikasi pencarian dan manajemen kos.\n");
        sb.append("Fitur Student: cari kos, booking, pembayaran, chat owner, komplain.\n");
        sb.append("Fitur Owner: tambah kos/kamar, kelola booking, chat mahasiswa, laporan keuangan.\n\n");
        sb.append("Keamanan: JANGAN minta password, OTP, KTP, atau rekening.\n\n");
        sb.append("Role User: ").append(role).append("\n");
        sb.append("Nama User: ").append(userName).append(" (pakai hanya di sapaan pertama, setelah itu gunakan 'kamu')\n\n");

        if (localFaqContext != null && !localFaqContext.isEmpty()) {
            String context = localFaqContext.length() > 120 ? localFaqContext.substring(0, 120) + "..." : localFaqContext;
            sb.append("Referensi singkat: ").append(context).append("\n\n");
        }

        sb.append("Jawab dengan natural, singkat, tuntas, dan tanpa markdown.");
        return sb.toString();
    }

    private String buildKosHubUserPrompt(String userMessage, List<AiMessage> recentMessages) {
        return "Pertanyaan: " + userMessage;
    }
}
