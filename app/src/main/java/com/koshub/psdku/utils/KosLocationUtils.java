package com.koshub.psdku.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.koshub.psdku.BuildConfig;
import com.koshub.psdku.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class for location-related operations in KosHub.
 */
public class KosLocationUtils {

    public static final double CAMPUS_LAT = -7.681444;
    public static final double CAMPUS_LNG = 109.667139;
    public static final String CAMPUS_NAME = "UNS PSDKU Kebumen";
    public static final String CAMPUS_ADDRESS = "Jl. Kepodang No.67a, Panjer, Kec. Kebumen, Kab. Kebumen, Jawa Tengah 54312";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "KosLocationUtils";

    public interface DurationCallback {
        void onSuccess(String durationText, int durationMinutes, String distanceText);
        void onFailure(String errorMessage);
    }

    /**
     * Validates if the given coordinates are valid.
     */
    public static boolean isValidCoordinate(double lat, double lng) {
        if (lat == 0.0 && lng == 0.0) return false;
        if (Double.isNaN(lat) || Double.isInfinite(lat)) return false;
        if (Double.isNaN(lng) || Double.isInfinite(lng)) return false;
        return (lat >= -90 && lat <= 90) && (lng >= -180 && lng <= 180);
    }

    /**
     * Calculates distance between two points in kilometers using Haversine formula.
     */
    public static double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Estimates walking minutes based on distance.
     * Average walking speed is around 4.8 km/h (1.33 m/s).
     */
    public static int estimateWalkingMinutes(double kosLat, double kosLng) {
        if (!isValidCoordinate(kosLat, kosLng)) return 0;
        double distanceKm = calculateDistanceKm(kosLat, kosLng, CAMPUS_LAT, CAMPUS_LNG);
        // 4.8 km/h = 0.08 km/minute -> minutes = distanceKm / 0.08
        int minutes = (int) Math.ceil(distanceKm / 0.08);
        return Math.max(1, minutes);
    }

    /**
     * Formats duration to short string like "± 8 mnt"
     */
    public static String formatEtaShort(Context context, int minutes) {
        if (context == null) return "± " + minutes + " mnt";
        return context.getString(R.string.eta_short_format, minutes);
    }

    /**
     * Formats duration to detail string like "± 8 menit jalan kaki"
     */
    public static String formatEtaDetail(Context context, int minutes) {
        if (context == null) return "± " + minutes + " menit jalan kaki";
        return context.getString(R.string.eta_detail_format, minutes);
    }

    /**
     * Fetches walking duration from a Kos location to the campus.
     * Uses local estimation first as fallback, then tries Google Directions API.
     */
    public static void fetchWalkingDuration(Context context, double kosLat, double kosLng, DurationCallback callback) {
        // Step 1: Validate coordinates
        if (!isValidCoordinate(kosLat, kosLng)) {
            postFailure(callback, context.getString(R.string.eta_invalid));
            return;
        }

        // Step 2: Local Fallback Estimation (Immediate)
        int localMinutes = estimateWalkingMinutes(kosLat, kosLng);
        double localDistance = calculateDistanceKm(kosLat, kosLng, CAMPUS_LAT, CAMPUS_LNG);
        String localDistanceText = String.format(java.util.Locale.US, "%.1f km", localDistance);
        
        // Notify success with local estimation first
        postSuccess(callback, formatEtaShort(context, localMinutes), localMinutes, localDistanceText);

        // Step 3: Try Google Directions API
        String apiKey = BuildConfig.GOOGLE_MAPS_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "Google Maps API Key is missing, using local estimation.");
            return;
        }

        String url = String.format(java.util.Locale.US, 
                "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=walking&language=id&key=%s",
                kosLat, kosLng, CAMPUS_LAT, CAMPUS_LNG, apiKey);

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error calling Directions API: " + e.getMessage());
                // Don't call postFailure, we already have local estimate showing
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Directions API unsuccessful: " + response.code());
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String status = jsonObject.getString("status");

                    Log.d(TAG, "Directions API Status: " + status);

                    if (!status.equals("OK")) {
                        Log.e(TAG, "Directions API error status: " + status);
                        return;
                    }

                    JSONArray routes = jsonObject.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONArray legs = route.getJSONArray("legs");
                        if (legs.length() > 0) {
                            JSONObject leg = legs.getJSONObject(0);
                            JSONObject duration = leg.getJSONObject("duration");
                            JSONObject distance = leg.getJSONObject("distance");

                            int seconds = duration.getInt("value");
                            int minutes = (int) Math.round(seconds / 60.0);
                            if (minutes < 1) minutes = 1;

                            String durationText = minutes + " mnt";
                            String distanceText = distance.getString("text");

                            // Update with more accurate data from Google
                            postSuccess(callback, durationText, minutes, distanceText);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Directions API response: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Formats duration in seconds to a human-readable string.
     */
    public static String formatDuration(int seconds) {
        if (seconds < 60) {
            return "< 1 mnt";
        }

        int minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " mnt";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours + " jam";
        } else {
            return hours + " jam " + remainingMinutes + " mnt";
        }
    }

    private static void postSuccess(DurationCallback callback, String durationText, int durationMinutes, String distanceText) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onSuccess(durationText, durationMinutes, distanceText);
            }
        });
    }

    private static void postFailure(DurationCallback callback, String errorMessage) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onFailure(errorMessage);
            }
        });
    }
}
