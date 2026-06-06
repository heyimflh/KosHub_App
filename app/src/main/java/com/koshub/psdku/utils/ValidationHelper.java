package com.koshub.psdku.utils;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Utility for input validation.
 */
public class ValidationHelper {
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 8;
    }

    public static boolean isEmpty(String text) {
        return TextUtils.isEmpty(text);
    }
}
