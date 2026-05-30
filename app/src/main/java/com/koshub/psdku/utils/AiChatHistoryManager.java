package com.koshub.psdku.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.koshub.psdku.models.AiMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages saving and loading AI Assistant chat history in SharedPreferences.
 */
public class AiChatHistoryManager {

    private static final String TAG = "AiChatHistoryManager";
    private static final String PREF_NAME = "ai_chat_history";
    private static final int MAX_MESSAGES = 30;

    public static void saveMessages(Context context, String role, List<AiMessage> messages) {
        if (context == null || messages == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            JSONArray jsonArray = new JSONArray();
            
            // Get last 30 messages
            int start = Math.max(0, messages.size() - MAX_MESSAGES);
            for (int i = start; i < messages.size(); i++) {
                AiMessage msg = messages.get(i);
                
                // Skip temporary/null messages
                if (msg == null || msg.getMessage() == null || msg.getMessage().isEmpty()) continue;
                if ("loading".equals(msg.getId())) continue; // Don't save typing indicator

                JSONObject obj = new JSONObject();
                obj.put("id", msg.getId());
                obj.put("message", msg.getMessage());
                obj.put("senderType", msg.getSenderType());
                obj.put("timestamp", msg.getTimestamp());
                jsonArray.put(obj);
            }

            editor.putString(getHistoryKey(role), jsonArray.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving messages", e);
        }
    }

    public static List<AiMessage> loadMessages(Context context, String role) {
        List<AiMessage> messages = new ArrayList<>();
        if (context == null) return messages;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(getHistoryKey(role), null);

        if (json == null || json.isEmpty()) return messages;

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                AiMessage msg = new AiMessage(
                        obj.optString("id"),
                        obj.optString("message"),
                        obj.optString("senderType"),
                        obj.optLong("timestamp")
                );
                messages.add(msg);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading messages, clearing corrupt data", e);
            clearMessages(context, role);
        }

        return messages;
    }

    public static void clearMessages(Context context, String role) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(getHistoryKey(role)).apply();
    }

    public static String getHistoryKey(String role) {
        if ("owner".equalsIgnoreCase(role)) {
            return "ai_chat_history_owner";
        }
        return "ai_chat_history_student";
    }
}
