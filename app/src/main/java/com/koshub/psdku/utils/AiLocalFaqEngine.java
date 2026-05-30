package com.koshub.psdku.utils;

import android.text.TextUtils;
import android.util.Log;

import com.koshub.psdku.models.AiFaqItem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AiLocalFaqEngine {

    private static final String TAG = "AiLocalFaqEngine";

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "cara", "bagaimana", "kenapa", "mengapa", "apakah", "saya", "kamu", "aku", "anda",
            "tidak", "belum", "bisa", "mau", "ingin", "di", "ke", "dari", "untuk", "yang",
            "dan", "atau", "adalah", "ada", "sudah", "dengan", "ini", "itu", "saya", "kami",
            "dia", "mereka", "pun", "saja", "juga", "hanya", "masih", "kalau", "jika", "maka",
            "siapa", "dimana", "kapan", "apa"
    ));

    public static String findBestAnswer(String userMessage, String role) {
        if (TextUtils.isEmpty(userMessage)) {
            return "Silakan ketikkan pertanyaan kamu agar saya bisa membantu.";
        }

        String normalizedMessage = normalizeText(userMessage);
        
        // 1. Intent Shortcut Check (Direct High-Confidence Matching)
        String shortcutAnswer = checkIntentShortcuts(normalizedMessage, role);
        if (shortcutAnswer != null) {
            Log.d(TAG, "Match found via Intent Shortcut");
            return shortcutAnswer;
        }

        // 2. General Scoring Matching
        List<AiFaqItem> faqItems = AiKnowledgeBase.getFaqItems();
        AiFaqItem bestMatch = null;
        int highestScore = 0;

        for (AiFaqItem item : faqItems) {
            int score = calculateScore(normalizedMessage, item, role);
            if (score > highestScore) {
                highestScore = score;
                bestMatch = item;
            }
        }

        Log.d(TAG, "Highest score: " + highestScore + " for " + (bestMatch != null ? bestMatch.getTitle() : "None"));

        // Minimum score threshold to consider a match valid
        // Increased threshold to 25 to avoid weak matches
        if (bestMatch != null && highestScore >= 25) {
            return bestMatch.getAnswer();
        }

        return "Maaf, saya belum menemukan jawaban yang tepat. Coba jelaskan kendalanya lebih detail, atau gunakan menu WhatsApp/Email di Pusat Bantuan agar tim KosHub bisa membantu lebih lanjut.";
    }

    private static String checkIntentShortcuts(String message, String role) {
        // Chat Owner
        if (containsAny(message, "chat owner", "chat pemilik", "hubungi pemilik", "hubungi owner", "kontak owner", "owner tidak muncul", "chat tidak muncul")) {
            return findAnswerById("std_chat");
        }
        
        // Komplain
        if (containsAny(message, "komplain", "lapor masalah", "keluhan", "ajukan keluhan", "kirim komplain", "mengirim komplain")) {
            return findAnswerById("std_complaint");
        }
        
        // Booking
        if (containsAny(message, "booking kos", "pesan kamar", "sewa kos", "sewa kamar", "ajukan booking")) {
            return findAnswerById("std_booking");
        }
        
        // Pembayaran
        if (containsAny(message, "pembayaran", "bayar kos", "bayar booking", "qris", "transaksi", "pembayaran belum berhasil")) {
            return findAnswerById("std_payment");
        }
        
        // Owner specific
        if ("owner".equals(role)) {
            if (containsAny(message, "tambah kos", "menambah kos", "daftarkan kos", "buat kos")) {
                return findAnswerById("own_add_kos");
            }
            if (containsAny(message, "tambah kamar", "menambah kamar", "buat kamar")) {
                return findAnswerById("own_add_room");
            }
        }
        
        return null;
    }

    private static String findAnswerById(String id) {
        for (AiFaqItem item : AiKnowledgeBase.getFaqItems()) {
            if (item.getId().equals(id)) {
                return item.getAnswer();
            }
        }
        return null;
    }

    private static boolean containsAny(String message, String... phrases) {
        for (String phrase : phrases) {
            if (message.contains(phrase)) return true;
        }
        return false;
    }

    private static String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ") 
                .replaceAll("\\s+", " ")          
                .trim();
    }

    private static int calculateScore(String message, AiFaqItem item, String userRole) {
        int score = 0;

        // 1. Role match check
        boolean roleMatches = item.getRole().equals("general") || item.getRole().equals(userRole);
        if (!roleMatches) {
            return 0; // Skip if role doesn't match and not general
        }

        // 2. Title match (High score)
        String normalizedTitle = normalizeText(item.getTitle());
        if (message.contains(normalizedTitle)) {
            score += 60;
        }

        // 3. Keyword matching
        String[] messageWords = message.split(" ");
        for (String keyword : item.getKeywords()) {
            String normalizedKeyword = normalizeText(keyword);
            
            // Exact phrase match (Highest priority)
            if (message.contains(normalizedKeyword)) {
                // More words in keyword = higher score
                int wordCount = normalizedKeyword.split(" ").length;
                score += (20 * wordCount) + 10;
            } else {
                // Word by word match (Filter stop words)
                String[] keywordWords = normalizedKeyword.split(" ");
                for (String kWord : keywordWords) {
                    if (STOP_WORDS.contains(kWord)) continue;
                    
                    for (String mWord : messageWords) {
                        if (STOP_WORDS.contains(mWord)) continue;
                        
                        if (kWord.equals(mWord)) {
                            score += 10;
                        }
                    }
                }
            }
        }

        // 4. Bonus for exact match of role
        if (item.getRole().equals(userRole)) {
            score += 10;
        }

        return score;
    }
}
