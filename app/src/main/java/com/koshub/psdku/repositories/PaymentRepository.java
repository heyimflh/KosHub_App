package com.koshub.psdku.repositories;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.koshub.psdku.models.PaymentCreateResult;
import com.koshub.psdku.models.PaymentStatusResult;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentRepository {
    private static final String TAG = "PaymentRepository";
    private static final String BASE_URL = "https://paymentgateway.alwaysdata.net/";
    private static final String API_KEY = "kunci_rahasia_app_agis"; // TODO SECURITY: API key sebaiknya dipindahkan ke backend proxy sebelum production.
    
    private static PaymentRepository instance;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PaymentRepository() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized PaymentRepository getInstance() {
        if (instance == null) {
            instance = new PaymentRepository();
        }
        return instance;
    }

    public interface PaymentCreateCallback {
        void onSuccess(PaymentCreateResult result);
        void onError(String message);
    }

    public interface PaymentStatusCallback {
        void onSuccess(PaymentStatusResult result);
        void onError(String message);
    }

    public void createPayment(String nama, long nominal, PaymentCreateCallback callback) {
        Log.d(TAG, "createPayment nama=" + nama + ", nominal=" + nominal);
        
        RequestBody formBody = new FormBody.Builder()
                .add("api_key", API_KEY)
                .add("nama", nama)
                .add("nominal", String.valueOf(nominal))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "api_create.php")
                .post(formBody)
                .build();

        Log.d(TAG, "Request URL: " + request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "createPayment failed", e);
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    mainHandler.post(() -> callback.onError("Empty response body"));
                    return;
                }
                String bodyStr = response.body().string();
                Log.d(TAG, "Response create payment: " + bodyStr);
                try {
                    JSONObject res = new JSONObject(bodyStr);
                    String status = res.optString("status");
                    
                    if ("success".equals(status)) {
                        PaymentCreateResult createResult = new PaymentCreateResult();
                        createResult.setSuccess(true);
                        createResult.setGatewayTransactionId(res.optLong("id_transaksi"));
                        createResult.setTotalBayar(res.optDouble("total_bayar"));
                        createResult.setQrisString(res.optString("qris_string"));
                        
                        mainHandler.post(() -> callback.onSuccess(createResult));
                    } else {
                        String msg = res.optString("msg", "Gagal membuat pembayaran");
                        mainHandler.post(() -> callback.onError(msg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse create response", e);
                    String partialResponse = bodyStr.length() > 100 ? bodyStr.substring(0, 100) + "..." : bodyStr;
                    mainHandler.post(() -> callback.onError("Response payment tidak valid: " + partialResponse));
                }
            }
        });
    }

    public void checkPaymentStatus(long idTransaksi, PaymentStatusCallback callback) {
        String url = BASE_URL + "api_check.php?id=" + idTransaksi;
        Log.d(TAG, "Calling checkPaymentStatus: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "checkPaymentStatus network error", e);
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    mainHandler.post(() -> callback.onError("Empty status response body"));
                    return;
                }
                String bodyStr = response.body().string();
                Log.d(TAG, "check raw response: " + bodyStr);
                try {
                    JSONObject res = new JSONObject(bodyStr);
                    String statusRaw = res.optString("status", "").trim().toUpperCase();
                    
                    PaymentStatusResult statusResult = new PaymentStatusResult();
                    statusResult.setSuccess(true);
                    
                    // Robust status detection
                    if (statusRaw.equals("SUCCESS") || statusRaw.equals("PAID") || statusRaw.equals("BERHASIL")) {
                        statusResult.setStatus("SUCCESS");
                    } else if (statusRaw.equals("PENDING")) {
                        statusResult.setStatus("PENDING");
                    } else {
                        statusResult.setStatus(statusRaw); // Other status (EXPIRED, etc)
                    }
                    
                    statusResult.setMessage(statusRaw);
                    mainHandler.post(() -> callback.onSuccess(statusResult));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse status response", e);
                    mainHandler.post(() -> callback.onError("Failed to parse status response: " + e.getMessage()));
                }
            }
        });
    }
}
