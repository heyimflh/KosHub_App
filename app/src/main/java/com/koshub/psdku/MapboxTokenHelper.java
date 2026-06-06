package com.koshub.psdku;

import android.content.Context;
import android.util.Log;

public class MapboxTokenHelper {
    private static final String TAG = "MapboxSafety";

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

        Log.w(TAG, "Mapbox token is invalid or missing. Features requiring Mapbox will be disabled.");
        return false;
    }

    private static boolean isValidToken(String token) {
        // A valid token must:
        // 1. Not be null or empty
        // 2. Start with 'pk.' (public) or 'sk.' (secret)
        // 3. Have a minimum reasonable length for a real token (usually > 50 chars)
        // 4. Not be a generic placeholder
        return token != null 
                && !token.trim().isEmpty() 
                && (token.startsWith("pk.") || token.startsWith("sk."))
                && token.length() > 20
                && !token.toUpperCase().contains("YOUR_")
                && !token.toUpperCase().contains("_HERE");
    }
}
