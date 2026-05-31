package com.koshub.psdku.utils;

import android.text.TextUtils;
import com.koshub.psdku.BuildConfig;

/**
 * Helper class for AI configurations.
 * GEMINI_API_KEY is read from local.properties and should not be logged.
 */
public class AiConfig {

    /**
     * Checks if Gemini API Key is configured in local.properties.
     * @return true if configured, false otherwise.
     */
    public static boolean isGeminiConfigured() {
        return !TextUtils.isEmpty(BuildConfig.GEMINI_API_KEY);
    }

    /**
     * Gets the configured Gemini model name.
     * @return Model name string (e.g. "gemini-3.5-flash")
     */
    public static String getGeminiModel() {
        return BuildConfig.GEMINI_MODEL;
    }

    /**
     * Checks if Groq API Key is configured in local.properties.
     */
    public static boolean isGroqConfigured() {
        return !TextUtils.isEmpty(BuildConfig.GROQ_API_KEY);
    }

    /**
     * Gets the configured Groq model name.
     */
    public static String getGroqModel() {
        return BuildConfig.GROQ_MODEL;
    }
}
