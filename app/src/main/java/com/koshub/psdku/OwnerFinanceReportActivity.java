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
import com.koshub.psdku.models.FinanceSummary;
import com.koshub.psdku.models.Withdrawal;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.DateHelper;

import java.util.List;

/**
 * OwnerFinanceReportActivity - Laporan Keuangan Pemilik Kos
 */
public class OwnerFinanceReportActivity extends AppCompatActivity {

    private TextView tvTotalIncomeHeader, tvWalletAvailable, tvWalletPending;
    private LinearLayout withdrawalHistoryContainer;
    private View btnBackFinance, btnTarikSaldo;
    private ProgressBar progressTarget;

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
        withdrawalHistoryContainer = findViewById(R.id.withdrawalHistoryContainer);
        btnBackFinance = findViewById(R.id.btnBackFinance);
        btnTarikSaldo = findViewById(R.id.btnTarikSaldo);
        progressTarget = findViewById(R.id.progressTarget);
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
        tvTotalIncomeHeader.setText(CurrencyHelper.formatRupiah(summary.getTotalIncome()));
        tvWalletAvailable.setText(CurrencyHelper.formatRupiah(summary.getAvailableBalance()));
        tvWalletPending.setText(CurrencyHelper.formatRupiah(summary.getPendingBalance()));
        
        // Progress Target (Simulasi target 50jt)
        double target = 50000000;
        int progress = (int) ((summary.getTotalIncome() / target) * 100);
        if (progress > 100) progress = 100;
        progressTarget.setProgress(progress);
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
