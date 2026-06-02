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
        String apiKey = BuildConfig.GOOGLE_MAPS_KEY;
        String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=walking&language=id&key=%s",
                kosLat, kosLng, CAMPUS_LAT, CAMPUS_LNG, apiKey);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postFailure(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    postFailure(callback, "Unexpected response code: " + response.code());
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String status = jsonObject.getString("status");

                    if (!status.equals("OK")) {
                        postFailure(callback, "API Error: " + status);
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
                            String durationText;

                            if (seconds < 60) {
                                durationText = "< 1 mnt";
                            } else {
                                durationText = minutes + " mnt";
                            }

                            postSuccess(callback, durationText, minutes);
                        } else {
                            postFailure(callback, "No legs found in route");
                        }
                    } else {
                        postFailure(callback, "No routes found");
                    }
                } catch (Exception e) {
                    postFailure(callback, "JSON Parse error: " + e.getMessage());
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
