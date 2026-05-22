package com.koshub.psdku;

import android.content.Context;
import android.util.Log;

public class MapboxTokenHelper {
    private static final String TAG = "MapboxSafety";
    private static final String PLACEHOLDER = "YOUR_MAPBOX_PUBLIC_TOKEN_HERE";

    /**
     * Checks if the Mapbox token is valid.
     * A valid token is not empty, not the placeholder, and starts with "pk.".
     */
    public static boolean hasValidMapboxToken(Context context) {
        if (context == null) return false;
        try {
            int resId = context.getResources().getIdentifier("mapbox_access_token", "string", context.getPackageName());
            if (resId == 0) {
                Log.e(TAG, "Mapbox token resource not found (mapbox_access_token)");
                return false;
            }
            
            String token = context.getString(resId);
            boolean isValid = token != null 
                    && !token.trim().isEmpty() 
                    && !token.equals(PLACEHOLDER) 
                    && token.startsWith("pk.");
            
            if (!isValid) {
                Log.e(TAG, "Mapbox token is invalid or missing.");
            }
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error validating Mapbox token", e);
            return false;
        }
    }
}
