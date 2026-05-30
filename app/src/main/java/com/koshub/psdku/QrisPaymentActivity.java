package com.koshub.psdku;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.models.PaymentCreateResult;
import com.koshub.psdku.models.PaymentStatusResult;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.repositories.PaymentRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class QrisPaymentActivity extends AppCompatActivity {

    private static final String TAG = "QrisPaymentActivity";
    private String bookingId;
    private long gatewayTransactionId;
    private Booking currentBooking;
    
    private TextView tvTotalBayar, tvStatus, tvScanInstruction, tvManualCheck;
    private TextView tvTransactionId, tvGatewayStatus;
    private Button btnCopyLink;
    private ImageView ivQris;
    private ProgressBar pbQris;
    private Button btnCancel;

    private final Handler pollHandler = new Handler();
    private Runnable pollRunnable;
    private boolean isPolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qris_payment);

        bookingId = getIntent().getStringExtra("BOOKING_ID");
        Log.d(TAG, "onCreate bookingId=" + bookingId);
        
        initViews();

        if (bookingId == null || bookingId.trim().isEmpty()) {
            runOnUiThread(() -> {
                tvStatus.setText(R.string.payment_error_booking_id);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.md_error));
                Toast.makeText(this, "Booking ID is missing", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        loadBookingAndProcess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gatewayTransactionId != 0 && !isPolling) {
            startPolling();
        }
    }

    @Override
    protected void onPause() {
        stopPolling();
        super.onPause();
    }

    private void initViews() {
        tvTotalBayar = findViewById(R.id.tvTotalBayar);
        tvStatus = findViewById(R.id.tvStatus);
        tvScanInstruction = findViewById(R.id.tvScanInstruction);
        tvManualCheck = findViewById(R.id.tvManualCheck);
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvGatewayStatus = findViewById(R.id.tvGatewayStatus);
        btnCopyLink = findViewById(R.id.btnCopyLink);
        ivQris = findViewById(R.id.ivQris);
        pbQris = findViewById(R.id.pbQris);
        btnCancel = findViewById(R.id.btnCancel);

        tvTotalBayar.setText(R.string.payment_total_loading);
        tvStatus.setText(R.string.payment_status_preparing);
        ivQris.setVisibility(View.GONE);
        if (tvScanInstruction != null) tvScanInstruction.setVisibility(View.GONE);
        if (tvManualCheck != null) {
            tvManualCheck.setVisibility(View.GONE);
            tvManualCheck.setOnClickListener(v -> checkCurrentStatus());
        }

        if (btnCopyLink != null) {
            btnCopyLink.setOnClickListener(v -> copyCheckLink());
        }

        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadBookingAndProcess() {
        pbQris.setVisibility(View.VISIBLE);
        tvStatus.setText("Mengambil data booking...");
        
        BookingRepository.getInstance().getBookingById(bookingId, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(Booking booking) {
                currentBooking = booking;
                
                // REUSE PENDING PAYMENT
                if (booking.getGatewayTransactionId() != 0 && 
                    (DatabaseConstants.PAYMENT_PENDING.equals(booking.getPaymentStatus()) || 
                     DatabaseConstants.BOOKING_WAITING_PAYMENT.equals(booking.getStatus()))) {
                    
                    Log.d(TAG, "Reusing pending payment: " + booking.getGatewayTransactionId());
                    gatewayTransactionId = booking.getGatewayTransactionId();
                    
                    PaymentCreateResult result = new PaymentCreateResult();
                    result.setGatewayTransactionId(booking.getGatewayTransactionId());
                    result.setTotalBayar(booking.getTotalBayar());
                    result.setQrisString(booking.getQrisString());
                    
                    displayPaymentInfo(result);
                    tvStatus.setText(R.string.payment_waiting_status);
                    tvStatus.setTextColor(ContextCompat.getColor(QrisPaymentActivity.this, R.color.md_primary));
                    pbQris.setVisibility(View.GONE);
                    if (tvManualCheck != null) tvManualCheck.setVisibility(View.VISIBLE);
                    if (btnCopyLink != null) btnCopyLink.setVisibility(View.VISIBLE);
                    
                    updateDebugInfo(gatewayTransactionId, "PENDING (Firestore)");
                    startPolling();
                } else {
                    validateAndCreatePayment();
                }
            }

            @Override
            public void onError(String message) {
                handleError(message);
            }
        });
    }

    private void updateDebugInfo(long id, String status) {
        runOnUiThread(() -> {
            if (tvTransactionId != null) tvTransactionId.setText("ID Transaksi: " + id);
            if (tvGatewayStatus != null) tvGatewayStatus.setText("Status gateway terakhir: " + status);
        });
    }

    private void copyCheckLink() {
        if (gatewayTransactionId == 0) return;
        String link = "https://paymentgateway.alwaysdata.net/api_check.php?id=" + gatewayTransactionId;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Payment Link", link);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Link cek status disalin ke clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCurrentStatus() {
        if (gatewayTransactionId == 0) return;
        
        tvStatus.setText("Mengecek status pembayaran...");
        PaymentRepository.getInstance().checkPaymentStatus(gatewayTransactionId, new PaymentRepository.PaymentStatusCallback() {
            @Override
            public void onSuccess(PaymentStatusResult result) {
                updateDebugInfo(gatewayTransactionId, result.getMessage());
                if ("SUCCESS".equals(result.getStatus())) {
                    stopPolling();
                    handlePaymentSuccess();
                } else {
                    tvStatus.setText("Status gateway: " + result.getStatus());
                    Toast.makeText(QrisPaymentActivity.this, "Status: " + result.getStatus() + "\nID: " + gatewayTransactionId, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                tvStatus.setText("Gagal mengecek: " + message);
                Toast.makeText(QrisPaymentActivity.this, "Belum bisa mengecek status. Coba lagi beberapa saat.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void validateAndCreatePayment() {
        try {
            if (currentBooking == null) {
                handleError("Data booking tidak ditemukan.");
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                handleError("Sesi berakhir. Silakan login ulang.");
                return;
            }

            if (currentBooking.getStudentId() != null && !user.getUid().equals(currentBooking.getStudentId())) {
                handleError("Kamu tidak punya akses ke booking ini.");
                return;
            }

            String nama = currentBooking.getStudentName();
            if (nama == null || nama.isEmpty()) {
                nama = user.getDisplayName();
            }
            if (nama == null || nama.isEmpty()) {
                nama = "Penyewa KosHub";
            }

            long nominal = resolvePaymentAmount(currentBooking);
            if (nominal < 1000) {
                handleError("Nominal pembayaran minimal Rp 1.000.");
                return;
            }

            tvStatus.setText(R.string.payment_status_requesting);
            PaymentRepository.getInstance().createPayment(nama, nominal, new PaymentRepository.PaymentCreateCallback() {
                @Override
                public void onSuccess(PaymentCreateResult result) {
                    if (isFinishing()) return;
                    
                    pbQris.setVisibility(View.GONE);
                    ivQris.setVisibility(View.VISIBLE);
                    if (tvScanInstruction != null) tvScanInstruction.setVisibility(View.VISIBLE);
                    if (tvManualCheck != null) tvManualCheck.setVisibility(View.VISIBLE);
                    if (btnCopyLink != null) btnCopyLink.setVisibility(View.VISIBLE);
                    gatewayTransactionId = result.getGatewayTransactionId();
                    
                    updateDebugInfo(gatewayTransactionId, "CREATED");
                    
                    // SAVE DRAFT TO FIRESTORE IMMEDIATELY
                    BookingRepository.getInstance().savePaymentDraft(bookingId, gatewayTransactionId, result.getTotalBayar(), result.getQrisString(), null);
                    
                    displayPaymentInfo(result);
                    tvStatus.setText(R.string.payment_waiting_status);
                    tvStatus.setTextColor(ContextCompat.getColor(QrisPaymentActivity.this, R.color.md_primary));
                    startPolling();
                }

                @Override
                public void onError(String message) {
                    handleError(message);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in validateAndCreatePayment", e);
            handleError("Terjadi kesalahan internal: " + e.getMessage());
        }
    }

    private long resolvePaymentAmount(Booking booking) {
        if (booking == null) return 0;
        if (booking.getTotalPrice() >= 1000) return (long) booking.getTotalPrice();
        if (booking.getPrice() != null) {
            long p = parseRupiahToLong(booking.getPrice());
            if (p >= 1000) return p;
        }
        return (long) booking.getTotalPrice();
    }

    private long parseRupiahToLong(Object value) {
        if (value == null) return 0;
        String s = String.valueOf(value);
        String clean = s.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return 0;
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void displayPaymentInfo(PaymentCreateResult result) {
        runOnUiThread(() -> {
            try {
                tvTotalBayar.setText(String.format(Locale.getDefault(), "Total: Rp %,.0f", result.getTotalBayar()));
                String encodedQris = URLEncoder.encode(result.getQrisString(), StandardCharsets.UTF_8.name());
                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=" + encodedQris;
                
                Glide.with(this).load(qrUrl).into(ivQris);
                ivQris.setVisibility(View.VISIBLE);
                if (tvScanInstruction != null) tvScanInstruction.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error rendering QR", e);
                handleError("Gagal merender QR: " + e.getMessage());
            }
        });
    }

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling) return;
                PaymentRepository.getInstance().checkPaymentStatus(gatewayTransactionId, new PaymentRepository.PaymentStatusCallback() {
                    @Override
                    public void onSuccess(PaymentStatusResult result) {
                        updateDebugInfo(gatewayTransactionId, result.getMessage());
                        if ("SUCCESS".equals(result.getStatus())) {
                            stopPolling();
                            handlePaymentSuccess();
                        } else {
                            pollHandler.postDelayed(pollRunnable, 5000);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pollHandler.postDelayed(pollRunnable, 5000);
                    }
                });
            }
        };
        pollHandler.postDelayed(pollRunnable, 5000);
    }

    private void handlePaymentSuccess() {
        try {
            runOnUiThread(() -> {
                tvStatus.setText("Pembayaran Berhasil! Memproses...");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.md_primary));
            });
            
            BookingRepository.getInstance().updateBookingToPaid(bookingId, gatewayTransactionId, new BookingRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    FinanceRepository.getInstance().createTransactionAfterPayment(currentBooking, gatewayTransactionId, currentBooking.getTotalPrice(), new FinanceRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            openSuccessActivity();
                        }

                        @Override
                        public void onError(String message) {
                            openSuccessActivity();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(QrisPaymentActivity.this, "Gagal mengupdate status: " + message, Toast.LENGTH_LONG).show());
                    openSuccessActivity();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in handlePaymentSuccess", e);
            openSuccessActivity();
        }
    }

    private void stopPolling() {
        isPolling = false;
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void handleError(String message) {
        Log.e(TAG, "handleError: " + message);
        runOnUiThread(() -> {
            if (isFinishing()) return;
            pbQris.setVisibility(View.GONE);
            ivQris.setVisibility(View.GONE);
            if (tvScanInstruction != null) tvScanInstruction.setVisibility(View.GONE);
            tvTotalBayar.setText("Total: -");
            tvStatus.setText("Gagal membuat pembayaran: " + message);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.md_error));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void openSuccessActivity() {
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        super.onDestroy();
    }
}
