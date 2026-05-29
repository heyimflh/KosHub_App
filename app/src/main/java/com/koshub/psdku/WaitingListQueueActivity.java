package com.koshub.psdku;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.Booking;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.List;

public class WaitingListQueueActivity extends AppCompatActivity {

    private FrameLayout btnNotification;
    
    // Queue Card
    private LinearLayout cardQueueStatus;
    private TextView tvQueueStatus, tvPropertyName, tvPropertyAddress;

    // Available Card
    private LinearLayout cardAvailableStatus;
    private TextView tvAvailableStatus, tvAvailablePropertyName, tvAvailablePropertyAddress;
    private TextView btnPayAvailable;
    private TextView tvQueuePosition, tvEstimatedAvailability, tvQueueMicrocopy;
    
    // Timeline steps and content
    private View stepWaitingList, stepAvailability, stepOwnerConfirm, stepPayment, stepCompleted;
    private TextView tvStepWaitingListTitle, tvStepAvailabilityTitle, tvStepOwnerConfirmTitle, tvStepPaymentTitle, tvStepCompletedTitle;
    private ImageView ivStepWaitingListIcon, ivStepAvailabilityIcon, ivStepOwnerConfirmIcon, ivStepPaymentIcon, ivStepCompletedIcon;

    private View layoutEmptyState;
    private View sectionNeedsAction, sectionActiveQueue, sectionTimeline, sectionNextSteps;
    private View cardNextStep, cardGuide, cardInfo;

    private View btnPrimaryAction;
    private TextView btnSecondaryAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list_queue);

        initViews();
        handleWindowInsets();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRealBookings();
    }

    private void initViews() {
        btnNotification = findViewById(R.id.btnNotification);

        cardQueueStatus = findViewById(R.id.cardQueueStatus);
        tvQueueStatus = findViewById(R.id.tvQueueStatus);
        tvPropertyName = findViewById(R.id.tvPropertyName);
        tvPropertyAddress = findViewById(R.id.tvPropertyAddress);

        cardAvailableStatus = findViewById(R.id.cardAvailableStatus);
        tvAvailableStatus = findViewById(R.id.tvAvailableStatus);
        tvAvailablePropertyName = findViewById(R.id.tvAvailablePropertyName);
        tvAvailablePropertyAddress = findViewById(R.id.tvAvailablePropertyAddress);
        btnPayAvailable = findViewById(R.id.btnPayAvailable);

        tvQueuePosition = findViewById(R.id.tvQueuePosition);
        tvEstimatedAvailability = findViewById(R.id.tvEstimatedAvailability);
        tvQueueMicrocopy = findViewById(R.id.tvQueueMicrocopy);

        stepWaitingList = findViewById(R.id.stepWaitingList);
        stepAvailability = findViewById(R.id.stepAvailability);
        stepOwnerConfirm = findViewById(R.id.stepOwnerConfirm);
        stepPayment = findViewById(R.id.stepPayment);
        stepCompleted = findViewById(R.id.stepCompleted);

        tvStepWaitingListTitle = findViewById(R.id.tvStepWaitingListTitle);
        tvStepAvailabilityTitle = findViewById(R.id.tvStepAvailabilityTitle);
        tvStepOwnerConfirmTitle = findViewById(R.id.tvStepOwnerConfirmTitle);
        tvStepPaymentTitle = findViewById(R.id.tvStepPaymentTitle);
        tvStepCompletedTitle = findViewById(R.id.tvStepCompletedTitle);

        ivStepWaitingListIcon = findViewById(R.id.ivStepWaitingListIcon);
        ivStepAvailabilityIcon = findViewById(R.id.ivStepAvailabilityIcon);
        ivStepOwnerConfirmIcon = findViewById(R.id.ivStepOwnerConfirmIcon);
        ivStepPaymentIcon = findViewById(R.id.ivStepPaymentIcon);
        ivStepCompletedIcon = findViewById(R.id.ivStepCompletedIcon);

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        
        sectionNeedsAction = findViewById(R.id.sectionNeedsAction);
        sectionActiveQueue = findViewById(R.id.sectionActiveQueue);
        sectionTimeline = findViewById(R.id.queueTimelineContainer);
        sectionNextSteps = findViewById(R.id.sectionNextSteps);
        cardNextStep = findViewById(R.id.cardNextStep);
        cardGuide = findViewById(R.id.cardGuide);
        cardInfo = findViewById(R.id.cardInfo);

        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnSecondaryAction = findViewById(R.id.btnSecondaryAction);
    }

    private void handleWindowInsets() {
        View root = findViewById(R.id.navbar);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void setupListeners() {
        NavigationHelper.setupBottomNav(this, NavigationHelper.Tab.WAITLIST);

        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationActivity.class);
            NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
        });

        if (btnPrimaryAction != null) {
            btnPrimaryAction.setOnClickListener(v -> {
                // Get most recent booking to chat
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid == null) return;
                
                BookingRepository.getInstance().getBookingsByStudent(uid, new BookingRepository.BookingListCallback() {
                    @Override
                    public void onSuccess(List<Booking> bookings) {
                        if (!bookings.isEmpty()) {
                            Booking latest = bookings.get(0);
                            openChatFromBooking(latest);
                        } else {
                            showCustomToast("Belum ada booking untuk dikonsultasikan.");
                        }
                    }

                    @Override
                    public void onError(String message) {
                        showCustomToast(message);
                    }
                });
            });
        }
    }

    private void openChatFromBooking(Booking b) {
        Intent intent = new Intent(this, OwnerChatRoomActivity.class);
        intent.putExtra("BOOKING_ID", b.getId());
        intent.putExtra("USER_NAME", "Pemilik Kos"); // Fallback, repository will fetch real name
        intent.putExtra("KOS_NAME", b.getKosName());
        intent.putExtra("STATUS", b.getStatus());
        intent.putExtra("INITIAL", "P");
        NavigationTransitionHelper.navigateDetailWithIntent(this, intent);
    }

    private void loadRealBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        BookingRepository.getInstance().getBookingsByStudent(uid, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                if (bookings.isEmpty()) {
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                    findViewById(R.id.scrollWaitingList).setVisibility(View.GONE);
                    hideAllSections();
                } else {
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                    findViewById(R.id.scrollWaitingList).setVisibility(View.VISIBLE);
                    showAllSections();
                    Booking b = bookings.get(0); // Show most recent
                    updateUIWithBooking(b);
                }
            }

            @Override
            public void onError(String message) {
                showCustomToast(message);
            }
        });
    }

    private void hideAllSections() {
        if (sectionNeedsAction != null) sectionNeedsAction.setVisibility(View.GONE);
        if (sectionActiveQueue != null) sectionActiveQueue.setVisibility(View.GONE);
        if (sectionTimeline != null) sectionTimeline.setVisibility(View.GONE);
        if (sectionNextSteps != null) sectionNextSteps.setVisibility(View.GONE);
        if (cardNextStep != null) cardNextStep.setVisibility(View.GONE);
        if (cardQueueStatus != null) cardQueueStatus.setVisibility(View.GONE);
        if (cardAvailableStatus != null) cardAvailableStatus.setVisibility(View.GONE);
        if (cardGuide != null) cardGuide.setVisibility(View.GONE);
        if (cardInfo != null) cardInfo.setVisibility(View.GONE);
    }

    private void showAllSections() {
        if (sectionNeedsAction != null) sectionNeedsAction.setVisibility(View.VISIBLE);
        if (sectionActiveQueue != null) sectionActiveQueue.setVisibility(View.VISIBLE);
        if (sectionTimeline != null) sectionTimeline.setVisibility(View.VISIBLE);
        if (sectionNextSteps != null) sectionNextSteps.setVisibility(View.VISIBLE);
        if (cardNextStep != null) cardNextStep.setVisibility(View.VISIBLE);
        if (cardGuide != null) cardGuide.setVisibility(View.VISIBLE);
        if (cardInfo != null) cardInfo.setVisibility(View.VISIBLE);
    }

    private void updateUIWithBooking(Booking b) {
        String status = b.getStatus();
        
        // Populate standard fields
        tvPropertyName.setText(b.getKosName());
        tvPropertyAddress.setText(b.getKosAddress());
        
        // Remove dummy text
        if (tvQueuePosition != null) tvQueuePosition.setText("#1");
        if (tvQueueMicrocopy != null) tvQueueMicrocopy.setVisibility(View.GONE);
        if (tvEstimatedAvailability != null) tvEstimatedAvailability.setText("Menunggu Konfirmasi");

        if (DatabaseConstants.BOOKING_ACCEPTED.equals(status)) {
            cardQueueStatus.setVisibility(View.GONE);
            cardAvailableStatus.setVisibility(View.VISIBLE);
            tvAvailablePropertyName.setText(b.getKosName());
            tvAvailablePropertyAddress.setText(b.getKosAddress());
            tvAvailableStatus.setText("BOOKING DITERIMA");
            btnPayAvailable.setVisibility(View.VISIBLE);
            btnPayAvailable.setEnabled(true);
            btnPayAvailable.setText("Bayar Sekarang");
            btnPayAvailable.setOnClickListener(v -> handlePayment(b));
            
            btnSecondaryAction.setText("Batalkan Booking");
            btnSecondaryAction.setOnClickListener(v -> showCancelDialog(b));
            updateTimeline(status);
        } else if (DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(status)) {
            cardQueueStatus.setVisibility(View.GONE);
            cardAvailableStatus.setVisibility(View.VISIBLE);
            tvAvailablePropertyName.setText(b.getKosName());
            tvAvailablePropertyAddress.setText(b.getKosAddress());
            tvAvailableStatus.setText("SIAP AMBIL KUNCI");
            btnPayAvailable.setVisibility(View.VISIBLE);
            btnPayAvailable.setText("Sudah Bayar");
            btnPayAvailable.setEnabled(false);
            
            btnSecondaryAction.setText("Sudah Ambil Kunci");
            btnSecondaryAction.setOnClickListener(v -> handleKeyTaken(b));
            updateTimeline(status);
        } else if (DatabaseConstants.BOOKING_PENDING.equals(status)) {
            cardQueueStatus.setVisibility(View.VISIBLE);
            cardAvailableStatus.setVisibility(View.GONE);
            tvQueueStatus.setText("MENUNGGU KONFIRMASI");
            btnSecondaryAction.setText("Batalkan Antrean");
            btnSecondaryAction.setOnClickListener(v -> showCancelDialog(b));
            updateTimeline(status);
        } else if (DatabaseConstants.BOOKING_ACTIVE.equals(status)) {
            cardQueueStatus.setVisibility(View.VISIBLE);
            cardAvailableStatus.setVisibility(View.GONE);
            tvQueueStatus.setText("SEWA AKTIF");
            tvEstimatedAvailability.setText("Kamar Ditempati");
            btnSecondaryAction.setVisibility(View.VISIBLE);
            btnSecondaryAction.setText("Hubungi Pemilik");
            btnSecondaryAction.setOnClickListener(v -> openChatFromBooking(b));
            updateTimeline(status);
        } else {
            // Rejected, Cancelled, etc.
            cardQueueStatus.setVisibility(View.VISIBLE);
            cardAvailableStatus.setVisibility(View.GONE);
            tvQueueStatus.setText("STATUS: " + status.toUpperCase());
            btnSecondaryAction.setVisibility(View.GONE);
            updateTimeline(status);
        }
    }

    private void updateTimeline(String status) {
        if (stepWaitingList == null) return;

        // Step 1: Waiting List always done if booking exists
        setStepDone(stepWaitingList, tvStepWaitingListTitle, ivStepWaitingListIcon);

        if (DatabaseConstants.BOOKING_PENDING.equals(status)) {
            setStepActive(stepAvailability, tvStepAvailabilityTitle, ivStepAvailabilityIcon);
            setStepPending(stepOwnerConfirm, tvStepOwnerConfirmTitle, ivStepOwnerConfirmIcon);
            setStepPending(stepPayment, tvStepPaymentTitle, ivStepPaymentIcon);
            setStepPending(stepCompleted, tvStepCompletedTitle, ivStepCompletedIcon);
        } else if (DatabaseConstants.BOOKING_ACCEPTED.equals(status)) {
            setStepDone(stepAvailability, tvStepAvailabilityTitle, ivStepAvailabilityIcon);
            setStepDone(stepOwnerConfirm, tvStepOwnerConfirmTitle, ivStepOwnerConfirmIcon);
            setStepActive(stepPayment, tvStepPaymentTitle, ivStepPaymentIcon);
            setStepPending(stepCompleted, tvStepCompletedTitle, ivStepCompletedIcon);
        } else if (DatabaseConstants.BOOKING_WAITING_CHECKIN.equals(status)) {
            setStepDone(stepAvailability, tvStepAvailabilityTitle, ivStepAvailabilityIcon);
            setStepDone(stepOwnerConfirm, tvStepOwnerConfirmTitle, ivStepOwnerConfirmIcon);
            setStepDone(stepPayment, tvStepPaymentTitle, ivStepPaymentIcon);
            setStepActive(stepCompleted, tvStepCompletedTitle, ivStepCompletedIcon);
        } else if (DatabaseConstants.BOOKING_ACTIVE.equals(status)) {
            setStepDone(stepAvailability, tvStepAvailabilityTitle, ivStepAvailabilityIcon);
            setStepDone(stepOwnerConfirm, tvStepOwnerConfirmTitle, ivStepOwnerConfirmIcon);
            setStepDone(stepPayment, tvStepPaymentTitle, ivStepPaymentIcon);
            setStepDone(stepCompleted, tvStepCompletedTitle, ivStepCompletedIcon);
        } else {
            // Cancelled or Rejected - maybe hide timeline or show all pending
            setStepPending(stepAvailability, tvStepAvailabilityTitle, ivStepAvailabilityIcon);
            setStepPending(stepOwnerConfirm, tvStepOwnerConfirmTitle, ivStepOwnerConfirmIcon);
            setStepPending(stepPayment, tvStepPaymentTitle, ivStepPaymentIcon);
            setStepPending(stepCompleted, tvStepCompletedTitle, ivStepCompletedIcon);
        }
    }

    private void setStepDone(View step, TextView title, ImageView icon) {
        if (step == null) return;
        step.setAlpha(1.0f);

        View dot = getDotView(step);
        if (dot != null) {
            dot.setBackgroundResource(R.drawable.bg_waiting_timeline_dot_done);
        }

        if (icon != null) {
            icon.setImageResource(R.drawable.ic_waiting_check_circle);
            icon.setVisibility(View.VISIBLE);
        }
        if (title != null) {
            title.setTextColor(ContextCompat.getColor(this, R.color.md_on_surface));
            title.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setStepActive(View step, TextView title, ImageView icon) {
        if (step == null) return;
        step.setAlpha(1.0f);

        View dot = getDotView(step);
        if (dot != null) {
            dot.setBackgroundResource(R.drawable.bg_waiting_timeline_dot_active);
        }

        if (icon != null) {
            icon.setImageResource(R.drawable.ic_waiting_schedule);
            icon.setVisibility(View.VISIBLE);
        }
        if (title != null) {
            title.setTextColor(ContextCompat.getColor(this, R.color.md_primary));
            title.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void setStepPending(View step, TextView title, ImageView icon) {
        if (step == null) return;
        step.setAlpha(0.5f);

        View dot = getDotView(step);
        if (dot != null) {
            dot.setBackgroundResource(R.drawable.bg_waiting_timeline_dot_pending);
        }

        if (icon != null) icon.setVisibility(View.GONE);
        if (title != null) {
            title.setTextColor(ContextCompat.getColor(this, R.color.md_outline));
            title.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private View getDotView(View step) {
        if (step instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) step;
            if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof ViewGroup) {
                ViewGroup leftCol = (ViewGroup) vg.getChildAt(0);
                if (leftCol.getChildCount() > 0) {
                    return leftCol.getChildAt(0);
                }
            }
        }
        return null;
    }

    private void handlePayment(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Simulasi Pembayaran")
                .setMessage("Lakukan pembayaran simulasi sebesar Rp " + b.getTotalPrice() + "?")
                .setPositiveButton("Bayar Sekarang", (dialog, which) -> {
                    BookingRepository.getInstance().simulatePayment(b.getId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showCustomToast("Pembayaran Berhasil!");
                            loadRealBookings();
                        }

                        @Override
                        public void onError(String message) {
                            showCustomToast(message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void handleKeyTaken(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Ambil Kunci")
                .setMessage("Apakah kamu sudah menerima kunci dari pemilik?")
                .setPositiveButton("Ya, Sudah", (dialog, which) -> {
                    BookingRepository.getInstance().markKeyTaken(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showCustomToast("Selamat! Sewa kamu kini Aktif.");
                            loadRealBookings();
                        }

                        @Override
                        public void onError(String message) {
                            showCustomToast(message);
                        }
                    });
                })
                .setNegativeButton("Belum", null)
                .show();
    }

    private void showCancelDialog(Booking b) {
        new AlertDialog.Builder(this)
                .setTitle("Batalkan Booking?")
                .setMessage("Yakin ingin membatalkan booking/antrean ini?")
                .setPositiveButton("Ya, Batalkan", (dialog, which) -> {
                    BookingRepository.getInstance().cancelBooking(b.getId(), b.getRoomId(), new BookingRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            showCustomToast("Berhasil dibatalkan");
                            loadRealBookings();
                        }

                        @Override
                        public void onError(String message) {
                            showCustomToast(message);
                        }
                    });
                })
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void showCustomToast(String message) {
        TextView toastView = new TextView(this);
        toastView.setText(message);
        toastView.setTextColor(Color.WHITE);
        toastView.setTextSize(14f);
        toastView.setBackgroundResource(R.drawable.bg_toast);
        int paddingH = dpToPx(24);
        int paddingV = dpToPx(12);
        toastView.setPadding(paddingH, paddingV, paddingH, paddingV);
        toastView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(this);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dpToPx(100));
        toast.show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
