package com.koshub.psdku.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.koshub.psdku.BuildConfig;

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

    public static final double CAMPUS_LAT = -7.681778;
    public static final double CAMPUS_LNG = 109.664618;
    public static final String CAMPUS_NAME = "UNS PGSD Kebumen";
    public static final String CAMPUS_ADDRESS = "Jl. Kepodang No.67a, Panjer, Kec. Kebumen, Kab. Kebumen, Jawa Tengah 54312";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DurationCallback {
        void onSuccess(String durationText, int durationMinutes);
        void onFailure(String errorMessage);
    }

    /**
     * Fetches walking duration from a Kos location to the campus.
     */
    public static void fetchWalkingDuration(Context context, double kosLat, double kosLng, DurationCallback callback) {
        // Validasi: jangan panggil API jika koordinat invalid
        if ((kosLat == 0.0 && kosLng == 0.0) || (kosLat > 90 || kosLat < -90) || (kosLng > 180 || kosLng < -180)) {
            postFailure(callback, "Invalid coordinates");
            return;
        }

        String apiKey = BuildConfig.GOOGLE_MAPS_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            postFailure(callback, "API key not configured");
            return;
        }

        String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=walking&language=id&key=%s",
                kosLat, kosLng, CAMPUS_LAT, CAMPUS_LNG, apiKey);

        Request request = new Request.Builder().url(url).build();

        android.util.Log.d("KosLocationUtils", "Requesting duration for lat=" + kosLat + " lng=" + kosLng);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("KosLocationUtils", "Network error: " + e.getMessage());
                postFailure(callback, "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    android.util.Log.e("KosLocationUtils", "Unexpected response code: " + response.code());
                    postFailure(callback, "Response error: " + response.code());
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String status = jsonObject.getString("status");

                    android.util.Log.d("KosLocationUtils", "API Status: " + status);

                    if (!status.equals("OK")) {
                        // Handle error status dengan pesan yang lebih informatif
                        String errorMessage = "API Error: " + status;

                        if (status.equals("REQUEST_DENIED")) {
                            errorMessage = "API key invalid atau Directions API tidak diaktifkan. " +
                                    "Silakan enable Directions API di Google Cloud Console.";
                        } else if (status.equals("ZERO_RESULTS")) {
                            errorMessage = "Rute tidak ditemukan (koordinat mungkin invalid)";
                        } else if (status.equals("OVER_QUERY_LIMIT")) {
                            errorMessage = "Quota API tercapai";
                        }

                        android.util.Log.e("KosLocationUtils", errorMessage);
                        postFailure(callback, errorMessage);
                        return;
                    }

                    JSONArray routes = jsonObject.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONArray legs = route.getJSONArray("legs");
                        if (legs.length() > 0) {
                            JSONObject leg = legs.getJSONObject(0);
                            JSONObject duration = leg.getJSONObject("duration");

                            int seconds = duration.getInt("value");
                            int minutes = (int) Math.round(seconds / 60.0);
                            String durationText = (seconds < 60) ? "< 1 mnt" : minutes + " mnt";

                            android.util.Log.d("KosLocationUtils", "Duration: " + durationText);
                            postSuccess(callback, durationText, minutes);
                        } else {
                            postFailure(callback, "No legs in route");
                        }
                    } else {
                        postFailure(callback, "No routes found");
                    }
                } catch (Exception e) {
                    android.util.Log.e("KosLocationUtils", "Parse error: " + e.getMessage());
                    postFailure(callback, "Parse error");
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

    private static void postSuccess(DurationCallback callback, String durationText, int durationMinutes) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onSuccess(durationText, durationMinutes);
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
