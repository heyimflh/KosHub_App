package com.koshub.psdku;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.models.FinanceSummary;
import com.koshub.psdku.models.Withdrawal;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.DateHelper;

import java.util.List;
import java.util.Locale;

/**
 * OwnerFinanceReportActivity - Laporan Keuangan Pemilik Kos
 */
public class OwnerFinanceReportActivity extends AppCompatActivity {

    private TextView tvTotalIncomeHeader, tvWalletAvailable, tvWalletPending;
    private TextView tvTargetAchieved, tvTargetLabelValue;
    private TextView tvPaymentInValue, tvPaymentPendingValue, tvExpenseValue, tvNetIncomeValue;
    private TextView tvStatusLunasCount, tvStatusPendingCount, tvStatusLateCount, tvStatusCancelledCount;
    private LinearLayout withdrawalHistoryContainer, tvDummyTransactionContainer;
    private View btnBackFinance, btnTarikSaldo;
    private ProgressBar progressTarget;
    private double ownerTarget = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_finance_report);

        initViews();
        setupListeners();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.NONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFinanceData();
    }

    private void initViews() {
        tvTotalIncomeHeader = findViewById(R.id.tvTotalIncomeHeader);
        tvWalletAvailable = findViewById(R.id.tvWalletAvailable);
        tvWalletPending = findViewById(R.id.tvWalletPending);
        tvTargetAchieved = findViewById(R.id.tvTargetAchieved);
        tvTargetLabelValue = findViewById(R.id.tvTargetLabelValue);
        tvPaymentInValue = findViewById(R.id.tvPaymentInValue);
        tvPaymentPendingValue = findViewById(R.id.tvPaymentPendingValue);
        tvExpenseValue = findViewById(R.id.tvExpenseValue);
        tvNetIncomeValue = findViewById(R.id.tvNetIncomeValue);
        tvStatusLunasCount = findViewById(R.id.tvStatusLunasCount);
        tvStatusPendingCount = findViewById(R.id.tvStatusPendingCount);
        tvStatusLateCount = findViewById(R.id.tvStatusLateCount);
        tvStatusCancelledCount = findViewById(R.id.tvStatusCancelledCount);
        withdrawalHistoryContainer = findViewById(R.id.withdrawalHistoryContainer);
        tvDummyTransactionContainer = findViewById(R.id.tvDummyTransactionContainer);
        btnBackFinance = findViewById(R.id.btnBackFinance);
        btnTarikSaldo = findViewById(R.id.btnTarikSaldo);
        progressTarget = findViewById(R.id.progressTarget);

        if (tvDummyTransactionContainer != null) {
            tvDummyTransactionContainer.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        if (btnBackFinance != null) {
            btnBackFinance.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));
        }
        btnTarikSaldo.setOnClickListener(v -> {
            NavigationTransitionHelper.navigateDetail(this, OwnerWithdrawActivity.class);
        });
    }

    private void loadFinanceData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Fetch owner target from Firestore
        FirebaseFirestore.getInstance().collection(DatabaseConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains(DatabaseConstants.FIELD_TARGET_BULANAN)) {
                        Double val = doc.getDouble(DatabaseConstants.FIELD_TARGET_BULANAN);
                        if (val != null) ownerTarget = val;
                    }
                });

        FinanceRepository.getInstance().getFinanceSummary(uid, new FinanceRepository.FinanceSummaryCallback() {
            @Override
            public void onSuccess(FinanceSummary summary) {
                updateUI(summary);
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat ringkasan: " + message);
            }
        });

        FinanceRepository.getInstance().getWithdrawalsByOwner(uid, new FinanceRepository.WithdrawalListCallback() {
            @Override
            public void onSuccess(List<Withdrawal> withdrawals) {
                renderWithdrawals(withdrawals);
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat riwayat: " + message);
            }
        });
    }

    private void updateUI(FinanceSummary summary) {
        double totalRevenue = summary.getTotalIncome() + summary.getPendingBalance();
        tvTotalIncomeHeader.setText(CurrencyHelper.formatRupiah(totalRevenue));
        tvWalletAvailable.setText(CurrencyHelper.formatRupiah(summary.getAvailableBalance()));
        tvWalletPending.setText(CurrencyHelper.formatRupiah(summary.getPendingBalance()));

        // Update Stats Cards
        if (tvPaymentInValue != null) tvPaymentInValue.setText(CurrencyHelper.formatRupiah(summary.getTotalIncome()));
        if (tvPaymentPendingValue != null) tvPaymentPendingValue.setText(CurrencyHelper.formatRupiah(summary.getPendingBalance()));
        if (tvExpenseValue != null) tvExpenseValue.setText(CurrencyHelper.formatRupiah(summary.getTotalWithdrawn()));
        if (tvNetIncomeValue != null) tvNetIncomeValue.setText(CurrencyHelper.formatRupiah(summary.getTotalIncome() + summary.getPendingBalance() - summary.getTotalWithdrawn()));

        // Update Payment Status Counts
        if (tvStatusLunasCount != null) tvStatusLunasCount.setText(String.valueOf(summary.getLunasCount()));
        if (tvStatusPendingCount != null) tvStatusPendingCount.setText(String.valueOf(summary.getPendingCount()));
        if (tvStatusLateCount != null) tvStatusLateCount.setText(String.valueOf(summary.getLateCount()));
        if (tvStatusCancelledCount != null) tvStatusCancelledCount.setText(String.valueOf(summary.getCancelledCount()));

        // Progress Target (Fallback to total income if target not set)
        double target = ownerTarget > 0 ? ownerTarget : summary.getTotalIncome();
        if (target <= 0) target = 1; // Avoid division by zero

        double percentage = (totalRevenue / target) * 100;
        int progress = (int) percentage;
        if (progress > 100) progress = 100;

        progressTarget.setProgress(progress);
        if (tvTargetLabelValue != null) {
            tvTargetLabelValue.setText(CurrencyHelper.formatRupiah(target));
        }
        if (tvTargetAchieved != null) {
            tvTargetAchieved.setText(String.format(Locale.getDefault(), "Tercapai: %.0f%%", percentage));
        }
    }

    private void renderWithdrawals(List<Withdrawal> withdrawals) {
        withdrawalHistoryContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (withdrawals.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Belum ada riwayat penarikan.");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(0, 20, 0, 20);
            withdrawalHistoryContainer.addView(emptyText);
            return;
        }

        // Limit to 5 for the overview
        int count = Math.min(withdrawals.size(), 5);
        for (int i = 0; i < count; i++) {
            Withdrawal w = withdrawals.get(i);
            View itemView = inflater.inflate(R.layout.item_withdrawal_history, withdrawalHistoryContainer, false);

            TextView tvAmount = itemView.findViewById(R.id.tvWithdrawAmount);
            TextView tvDate = itemView.findViewById(R.id.tvWithdrawDate);
            TextView tvStatus = itemView.findViewById(R.id.tvWithdrawStatus);

            tvAmount.setText(CurrencyHelper.formatRupiah(w.getAmount()));
            tvDate.setText(DateHelper.formatDate(w.getCreatedAt()) + " • " + w.getBankName());
            tvStatus.setText(formatStatus(w.getStatus()));
            
            // Set status background and text color based on status
            setStatusStyle(tvStatus, w.getStatus());

            withdrawalHistoryContainer.addView(itemView);

            // Add divider if not last
            if (i < count - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(getResources().getColor(R.color.finance_divider));
                withdrawalHistoryContainer.addView(divider);
            }
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "Pending";
        switch (status) {
            case DatabaseConstants.WITHDRAWAL_SUCCESS: return "Berhasil";
            case DatabaseConstants.WITHDRAWAL_FAILED: return "Gagal";
            case DatabaseConstants.WITHDRAWAL_PROCESSING: return "Diproses";
            default: return "Menunggu";
        }
    }

    private void setStatusStyle(TextView tv, String status) {
        if (status == null) status = DatabaseConstants.WITHDRAWAL_PENDING;
        
        switch (status) {
            case DatabaseConstants.WITHDRAWAL_SUCCESS:
                tv.setBackgroundResource(R.drawable.bg_finance_status_success);
                tv.setTextColor(getResources().getColor(R.color.finance_income_green));
                break;
            case DatabaseConstants.WITHDRAWAL_FAILED:
                tv.setBackgroundResource(R.drawable.bg_finance_status_expense);
                tv.setTextColor(getResources().getColor(R.color.finance_expense_red));
                break;
            case DatabaseConstants.WITHDRAWAL_PROCESSING:
            case DatabaseConstants.WITHDRAWAL_PENDING:
            default:
                tv.setBackgroundResource(R.drawable.bg_finance_status_pending);
                tv.setTextColor(getResources().getColor(R.color.finance_pending_orange));
                break;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
