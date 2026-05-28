package com.koshub.psdku;

import android.content.Context;
import android.util.Log;

public class MapboxTokenHelper {
    private static final String TAG = "MapboxSafety";
    private static final String PLACEHOLDER = "YOUR_MAPBOX_PUBLIC_TOKEN_HERE";

    /**
     * Checks if the Mapbox token is valid using a 2-layer approach:
     * 1. Check BuildConfig.MAPBOX_TOKEN (from gradle.properties)
     * 2. Fallback to string resource "mapbox_access_token"
     */
    public static boolean hasValidMapboxToken(Context context) {
        if (context == null) return false;

        // Layer 1: Check BuildConfig
        String buildConfigToken = BuildConfig.MAPBOX_TOKEN;
        if (isValidToken(buildConfigToken)) {
            return true;
        }

        // Layer 2: Check String Resource (Backward Compatibility)
        try {
            int resId = context.getResources().getIdentifier("mapbox_access_token", "string", context.getPackageName());
            if (resId != 0) {
                String resToken = context.getString(resId);
                if (isValidToken(resToken)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading Mapbox token from resources", e);
        }

        Log.e(TAG, "Mapbox token is invalid or missing in both BuildConfig and Resources.");
        return false;
    }

    private static boolean isValidToken(String token) {
        return token != null 
                && !token.trim().isEmpty() 
                && !token.equals(PLACEHOLDER) 
                && token.startsWith("pk.");
    }
}
