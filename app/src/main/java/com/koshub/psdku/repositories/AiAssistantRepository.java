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

    public void askKosHubAssistant(String userMessage, String role, List<AiMessage> recentMessages, AiCallback callback) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            callback.onError("Pesan tidak boleh kosong.");
            return;
        }

        if (!AiConfig.isGeminiConfigured()) {
            callback.onError("Gemini API not configured.");
            return;
        }

        // Get local FAQ context for grounding
        String localFaqContext = AiLocalFaqEngine.findBestAnswer(userMessage, role);
        if (localFaqContext.contains("Maaf, saya belum menemukan jawaban yang tepat")) {
            localFaqContext = "";
        }

        String prompt = buildKosHubPrompt(userMessage, role, recentMessages, localFaqContext);
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
            generationConfig.put("maxOutputTokens", 1024);
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
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            callback.onError("API Error: " + response.code());
                            return;
                        }

                        if (responseBody == null) {
                            callback.onError("Response body is null.");
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
                        callback.onError("Response empty or malformed.");
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
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
        if (cleaned.endsWith(":") || cleaned.endsWith(",")) {
            cleaned += " dan ikuti petunjuk selanjutnya ya, Kak.";
        }

        return cleaned;
    }

    private String buildKosHubPrompt(String userMessage, String role, List<AiMessage> recentMessages, String localFaqContext) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Kamu adalah Asisten KosHub, CS AI otomatis untuk aplikasi KosHub.\n\n");
        
        sb.append("Aturan Keras Gaya Jawaban:\n");
        sb.append("- Bahasa Indonesia ramah, ringan, dan to the point.\n");
        sb.append("- Panggil user HANYA dengan 'Kak'. DILARANG memakai kata 'Kakak'.\n");
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
        
        if (localFaqContext != null && !localFaqContext.isEmpty()) {
            sb.append("Referensi (Tulis ulang dengan gaya natural, singkat, maks 5 kalimat):\n");
            sb.append(localFaqContext).append("\n\n");
        }
        
        if (recentMessages != null && !recentMessages.isEmpty()) {
            sb.append("Chat History (Max 4):\n");
            int start = Math.max(0, recentMessages.size() - 4);
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
}
