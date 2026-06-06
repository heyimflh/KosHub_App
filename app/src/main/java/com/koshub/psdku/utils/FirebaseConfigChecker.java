package com.koshub.psdku.utils;

import android.content.Context;
import com.google.firebase.FirebaseApp;

/**
 * Utility to check Firebase initialization status.
 */
public class FirebaseConfigChecker {

    public static boolean isFirebaseInitialized(Context context) {
        try {
            return !FirebaseApp.getApps(context).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static String getFirebaseStatus(Context context) {
        try {
            if (isFirebaseInitialized(context)) {
                return "Firebase is successfully initialized.";
            } else {
                return "Firebase is not initialized.";
            }
        } catch (Exception e) {
            return "Error checking Firebase status: " + e.getMessage();
        }
    }
}
