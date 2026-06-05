package com.koshub.psdku;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.models.Withdrawal;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.DateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.view.Gravity;

/**
 * OwnerFinanceReportActivity - Laporan Keuangan Pemilik Kos
 */
public class OwnerFinanceReportActivity extends AppCompatActivity {
    private static final String TAG = "OwnerFinanceReport";

    private TextView tvTotalIncomeHeader, tvWalletAvailable, tvWalletPending;
    private TextView tvTargetAchieved, tvTargetLabelValue;
    private TextView tvPaymentInValue, tvPaymentPendingValue, tvExpenseValue, tvNetIncomeValue;
    private TextView tvStatusLunasCount, tvStatusPendingCount, tvStatusLateCount, tvStatusCancelledCount;
    private TextView tvGrowthIndicator, tvInsight1, tvInsight2, tvInsight3;
    private LinearLayout withdrawalHistoryContainer, tvDummyTransactionContainer;
    private View btnBackFinance, btnTarikSaldo;
    private ProgressBar progressTarget;
    private double ownerTarget = 0;

    private TextView chipToday, chipWeek, chipMonth, chipYear;
    private String currentFilter = "month"; // default bulan ini
    private List<com.koshub.psdku.models.Transaction> allTransactions = new ArrayList<>();
    private List<com.koshub.psdku.models.Withdrawal> allWithdrawals = new ArrayList<>();
    private ListenerRegistration transactionListener;
    private ListenerRegistration withdrawalListener;

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

        tvGrowthIndicator = findViewById(R.id.tvGrowthIndicator);
        tvInsight1 = findViewById(R.id.tvInsight1);
        tvInsight2 = findViewById(R.id.tvInsight2);
        tvInsight3 = findViewById(R.id.tvInsight3);

        chipToday = findViewById(R.id.chipToday);
        chipWeek = findViewById(R.id.chipWeek);
        chipMonth = findViewById(R.id.chipMonth);
        chipYear = findViewById(R.id.chipYear);

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

        chipToday.setOnClickListener(v -> setFilter("today"));
        chipWeek.setOnClickListener(v -> setFilter("week"));
        chipMonth.setOnClickListener(v -> setFilter("month"));
        chipYear.setOnClickListener(v -> setFilter("year"));

        // Tambahkan listener untuk btnSeeAllTransactions
        TextView btnSeeAll = findViewById(R.id.btnSeeAllTransactions);
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> {
                // Navigate ke semua transaksi - tampilkan AlertDialog sementara dengan list
                showAllTransactionsDialog();
            });
        }

        // Tambahkan listener btnExportReport
        LinearLayout btnExport = findViewById(R.id.btnExportReport);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> generateAndDownloadPdf());
        }
    }

    private void loadFinanceData() {
        Log.d(TAG, "loadFinanceData called");
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            showToast("Silakan login ulang.");
            finish();
            return;
        }

        // Perform reconciliation first to ensure all paid bookings have transactions
        FinanceRepository.getInstance().reconcilePaidBookingsToTransactions(uid, new FinanceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Reconciliation success");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Reconciliation error: " + message);
            }
        });

        // Load target bulanan dari Firestore
        FirebaseFirestore.getInstance().collection(DatabaseConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains(DatabaseConstants.FIELD_TARGET_BULANAN)) {
                        Double val = doc.getDouble(DatabaseConstants.FIELD_TARGET_BULANAN);
                        if (val != null) ownerTarget = val;
                    }
                    // Trigger re-render after target is loaded
                    applyFilterAndUpdateUI();
                });

        // Realtime Transactions
        if (transactionListener != null) {
            transactionListener.remove();
        }
        transactionListener = FinanceRepository.getInstance().listenTransactionsByOwner(uid, new FinanceRepository.TransactionListCallback() {
            @Override
            public void onSuccess(List<com.koshub.psdku.models.Transaction> transactions) {
                Log.d(TAG, "Transactions loaded: " + (transactions != null ? transactions.size() : 0));
                allTransactions = transactions != null ? transactions : new ArrayList<>();
                applyFilterAndUpdateUI();
            }
            @Override
            public void onError(String message) {
                Log.e(TAG, "Transactions listener error: " + message);
                showToast("Gagal memuat transaksi: " + message);
            }
        });

        // Realtime Withdrawals
        if (withdrawalListener != null) {
            withdrawalListener.remove();
        }
        withdrawalListener = FinanceRepository.getInstance().listenWithdrawalsByOwner(uid, new FinanceRepository.WithdrawalListCallback() {
            @Override
            public void onSuccess(List<com.koshub.psdku.models.Withdrawal> withdrawals) {
                Log.d(TAG, "Withdrawals loaded: " + (withdrawals != null ? withdrawals.size() : 0));
                allWithdrawals = withdrawals != null ? withdrawals : new ArrayList<>();
                renderWithdrawals(allWithdrawals);
                applyFilterAndUpdateUI();
            }
            @Override
            public void onError(String message) {
                Log.e(TAG, "Withdrawals listener error: " + message);
                showToast("Gagal memuat riwayat: " + message);
            }
        });
    }

    private void setFilter(String filter) {
        currentFilter = filter;

        // Update chip visual states
        int activeColor = getResources().getColor(R.color.finance_chip_active_text);
        int inactiveColor = getResources().getColor(R.color.finance_chip_inactive_text);

        chipToday.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipToday.setTextColor(inactiveColor);
        chipToday.setTypeface(null, android.graphics.Typeface.NORMAL);

        chipWeek.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipWeek.setTextColor(inactiveColor);
        chipWeek.setTypeface(null, android.graphics.Typeface.NORMAL);

        chipMonth.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipMonth.setTextColor(inactiveColor);
        chipMonth.setTypeface(null, android.graphics.Typeface.NORMAL);

        chipYear.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipYear.setTextColor(inactiveColor);
        chipYear.setTypeface(null, android.graphics.Typeface.NORMAL);

        TextView activeChip;
        String periodLabel;
        switch (filter) {
            case "today":
                activeChip = chipToday;
                periodLabel = "Hari Ini";
                break;
            case "week":
                activeChip = chipWeek;
                periodLabel = "Minggu Ini";
                break;
            case "year":
                activeChip = chipYear;
                periodLabel = "Tahun Ini";
                break;
            default:
                activeChip = chipMonth;
                periodLabel = "Bulan Ini";
                break;
        }
        activeChip.setBackgroundResource(R.drawable.bg_finance_chip_active);
        activeChip.setTextColor(activeColor);
        activeChip.setTypeface(null, android.graphics.Typeface.BOLD);

        // Update period label di header (cari tvPeriodLabel jika ada, atau biarkan)
        TextView tvPeriod = findViewById(R.id.tvFinancePeriod);
        if (tvPeriod != null) tvPeriod.setText(periodLabel);

        // Re-filter dan re-render data dari cache
        if (!allTransactions.isEmpty() || !allWithdrawals.isEmpty()) {
            applyFilterAndUpdateUI();
        }
    }

    private long getFilterStartTime() {
        Calendar cal = Calendar.getInstance();
        switch (currentFilter) {
            case "today":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case "week":
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case "year":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            default: // month
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
        }
        return cal.getTimeInMillis();
    }

    private void applyFilterAndUpdateUI() {
        try {
            long startTime = getFilterStartTime();
            long now = System.currentTimeMillis();

            if (allTransactions == null) allTransactions = new ArrayList<>();
            if (allWithdrawals == null) allWithdrawals = new ArrayList<>();

            // Filter transactions by period
            List<com.koshub.psdku.models.Transaction> filtered = new ArrayList<>();
            for (com.koshub.psdku.models.Transaction t : allTransactions) {
                if (t != null && t.getCreatedAt() >= startTime) {
                    filtered.add(t);
                }
            }

            // Recalculate summary for filtered period
            double totalIncome = 0, availableBalance = 0, pendingBalance = 0;
            int lunasCount = 0, pendingCount = 0, lateCount = 0, cancelledCount = 0;
            long sevenDays = 7L * 24 * 60 * 60 * 1000;

            for (com.koshub.psdku.models.Transaction t : filtered) {
                if (t == null) continue;
                String status = t.getStatus() != null ? t.getStatus() : "";
                switch (status) {
                    case "pending":
                        pendingBalance += t.getAmount();
                        pendingCount++;
                        if (now - t.getCreatedAt() > sevenDays) lateCount++;
                        break;
                    case "available":
                        availableBalance += t.getAmount();
                        totalIncome += t.getAmount();
                        lunasCount++;
                        break;
                    case "withdrawn":
                        totalIncome += t.getAmount();
                        lunasCount++;
                        break;
                    case "cancelled":
                        cancelledCount++;
                        break;
                }
            }

            // Filter withdrawals by period
            double totalWithdrawnPeriod = 0;
            for (com.koshub.psdku.models.Withdrawal w : allWithdrawals) {
                if (w != null && w.getCreatedAt() >= startTime && "success".equals(w.getStatus())) {
                    totalWithdrawnPeriod += w.getAmount();
                }
            }

            double netIncome = totalIncome + pendingBalance - totalWithdrawnPeriod;
            double totalRevenue = totalIncome + pendingBalance;

            // Update UI
            if (tvTotalIncomeHeader != null) tvTotalIncomeHeader.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalRevenue));
            if (tvWalletAvailable != null) tvWalletAvailable.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(availableBalance));
            if (tvWalletPending != null) tvWalletPending.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(pendingBalance));
            if (tvPaymentInValue != null) tvPaymentInValue.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalIncome));
            if (tvPaymentPendingValue != null) tvPaymentPendingValue.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(pendingBalance));
            if (tvExpenseValue != null) tvExpenseValue.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalWithdrawnPeriod));
            if (tvNetIncomeValue != null) tvNetIncomeValue.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(netIncome));
            if (tvStatusLunasCount != null) tvStatusLunasCount.setText(String.valueOf(lunasCount));
            if (tvStatusPendingCount != null) tvStatusPendingCount.setText(String.valueOf(pendingCount));
            if (tvStatusLateCount != null) tvStatusLateCount.setText(String.valueOf(lateCount));
            if (tvStatusCancelledCount != null) tvStatusCancelledCount.setText(String.valueOf(cancelledCount));

            // Update growth indicator
            updateGrowthIndicator(startTime, totalRevenue);

            // Update target progress
            updateTargetProgress(totalRevenue);

            // Render recent transactions for this period
            renderRecentTransactions(filtered);

            // Update bar chart
            updateBarChart(filtered);
        } catch (Exception e) {
            Log.e(TAG, "Error in applyFilterAndUpdateUI", e);
        }
    }

    private void updateGrowthIndicator(long currentPeriodStart, double currentRevenue) {
        // Calculate previous period start
        long periodLength = System.currentTimeMillis() - currentPeriodStart;
        long prevPeriodStart = currentPeriodStart - periodLength;

        double prevRevenue = 0;
        for (com.koshub.psdku.models.Transaction t : allTransactions) {
            if (t.getCreatedAt() >= prevPeriodStart && t.getCreatedAt() < currentPeriodStart) {
                String status = t.getStatus() != null ? t.getStatus() : "";
                if ("available".equals(status) || "withdrawn".equals(status) || "pending".equals(status)) {
                    prevRevenue += t.getAmount();
                }
            }
        }

        if (tvGrowthIndicator != null) {
            if (prevRevenue <= 0) {
                tvGrowthIndicator.setText("Tidak ada data periode sebelumnya");
                tvGrowthIndicator.setAlpha(0.6f);
            } else {
                double pct = ((currentRevenue - prevRevenue) / prevRevenue) * 100;
                String sign = pct >= 0 ? "+" : "";
                String text = sign + String.format(Locale.getDefault(), "%.0f%%", pct) + " dari periode sebelumnya";
                tvGrowthIndicator.setText(text);
                tvGrowthIndicator.setAlpha(0.85f);
            }
        }

        // Update insight 1 (growth)
        if (tvInsight1 != null) {
            if (prevRevenue <= 0) {
                tvInsight1.setText("📊 Belum ada data periode sebelumnya untuk dibandingkan");
            } else {
                double pct = ((currentRevenue - prevRevenue) / prevRevenue) * 100;
                String sign = pct >= 0 ? "📈 Pendapatan naik" : "📉 Pendapatan turun";
                tvInsight1.setText(sign + " " + String.format(Locale.getDefault(), "%.0f%%", Math.abs(pct)) + " dari periode sebelumnya");
            }
        }
    }

    private void updateTargetProgress(double totalRevenue) {
        double target = ownerTarget > 0 ? ownerTarget : 0;

        if (progressTarget != null) {
            if (target <= 0) {
                progressTarget.setProgress(0);
                if (tvTargetAchieved != null) tvTargetAchieved.setText("Atur target di pengaturan profil");
                if (tvTargetLabelValue != null) tvTargetLabelValue.setText("Belum diatur");
            } else {
                double pct = (totalRevenue / target) * 100;
                int progress = (int) Math.min(pct, 100);
                progressTarget.setProgress(progress);
                if (tvTargetLabelValue != null) tvTargetLabelValue.setText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(target));
                if (tvTargetAchieved != null) tvTargetAchieved.setText(String.format(Locale.getDefault(), "Tercapai: %.0f%%", pct));

                // Update insight 3 (target)
                if (tvInsight3 != null) {
                    tvInsight3.setText("🎯 Target pendapatan sudah tercapai " + String.format(Locale.getDefault(), "%.0f%%", pct));
                }
            }
        }
    }

    private void renderRecentTransactions(List<com.koshub.psdku.models.Transaction> transactions) {
        try {
            LinearLayout container = findViewById(R.id.tvDummyTransactionContainer);
            if (container == null) return;

            // Always hide old dummy, show real container
            container.setVisibility(View.VISIBLE);
            container.removeAllViews();

            if (transactions == null || transactions.isEmpty()) {
                android.widget.TextView emptyView = new android.widget.TextView(this);
                emptyView.setText("Tidak ada transaksi pada periode ini.");
                emptyView.setTextColor(getResources().getColor(R.color.finance_text_muted));
                emptyView.setTextSize(12);
                emptyView.setGravity(android.view.Gravity.CENTER);
                emptyView.setPadding(0, 32, 0, 32);
                container.addView(emptyView);
                // Update insight 2
                if (tvInsight2 != null) tvInsight2.setText("✅ Tidak ada transaksi menunggu pada periode ini");
                return;
            }

            // Show max 5 recent transactions
            int count = Math.min(transactions.size(), 5);
            int pendingCount = 0;

            for (int i = 0; i < count; i++) {
                com.koshub.psdku.models.Transaction t = transactions.get(i);
                if (t == null) continue;

                if ("pending".equals(t.getStatus())) pendingCount++;

                // Transaction row
                android.widget.LinearLayout row = new android.widget.LinearLayout(this);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = 20;
                row.setLayoutParams(rowParams);

                // Left text section
                android.widget.LinearLayout textSection = new android.widget.LinearLayout(this);
                textSection.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.LinearLayout.LayoutParams textParams = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                textSection.setLayoutParams(textParams);

                android.widget.TextView tvKosName = new android.widget.TextView(this);
                tvKosName.setText(t.getKosName() != null && !t.getKosName().isEmpty() ? t.getKosName() : "Pembayaran Sewa");
                tvKosName.setTextColor(getResources().getColor(R.color.finance_text_primary));
                tvKosName.setTextSize(13);
                tvKosName.setTypeface(null, android.graphics.Typeface.BOLD);

                android.widget.TextView tvDate = new android.widget.TextView(this);
                tvDate.setText(com.koshub.psdku.utils.DateHelper.formatDate(t.getCreatedAt()));
                tvDate.setTextColor(getResources().getColor(R.color.finance_text_muted));
                tvDate.setTextSize(11);

                textSection.addView(tvKosName);
                textSection.addView(tvDate);

                // Right amount + status
                android.widget.LinearLayout rightSection = new android.widget.LinearLayout(this);
                rightSection.setOrientation(android.widget.LinearLayout.VERTICAL);
                rightSection.setGravity(android.view.Gravity.END);

                android.widget.TextView tvAmount = new android.widget.TextView(this);
                boolean isIncome = "available".equals(t.getStatus()) || "withdrawn".equals(t.getStatus());
                boolean isPending = "pending".equals(t.getStatus());
                String amountPrefix = isIncome ? "+" : (isPending ? "" : "-");
                int amountColor = isIncome ? getResources().getColor(R.color.finance_income_green) :
                                  isPending ? getResources().getColor(R.color.finance_pending_orange) :
                                  getResources().getColor(R.color.finance_expense_red);

                tvAmount.setText(amountPrefix + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(t.getAmount()));
                tvAmount.setTextColor(amountColor);
                tvAmount.setTextSize(13);
                tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);

                android.widget.TextView tvStatus = new android.widget.TextView(this);
                String statusLabel = "available".equals(t.getStatus()) ? "Lunas" :
                                     "pending".equals(t.getStatus()) ? "Pending" :
                                     "withdrawn".equals(t.getStatus()) ? "Dicairkan" : "Dibatalkan";
                tvStatus.setText(statusLabel);
                tvStatus.setTextSize(9);
                tvStatus.setTextColor(amountColor);

                rightSection.addView(tvAmount);
                rightSection.addView(tvStatus);

                row.addView(textSection);
                row.addView(rightSection);
                container.addView(row);

                // Divider (not last)
                if (i < count - 1) {
                    View divider = new View(this);
                    android.widget.LinearLayout.LayoutParams divParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    divParams.topMargin = 12;
                    divider.setLayoutParams(divParams);
                    divider.setBackgroundColor(getResources().getColor(R.color.finance_divider));
                    container.addView(divider);
                }
            }

            // Update insight 2 (pending count)
            if (tvInsight2 != null) {
                int finalPending = pendingCount;
                if (finalPending > 0) {
                    tvInsight2.setText("⏳ " + finalPending + " pembayaran masih menunggu konfirmasi");
                } else {
                    tvInsight2.setText("✅ Semua pembayaran pada periode ini sudah lunas");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in renderRecentTransactions", e);
        }
    }

    private void updateBarChart(List<com.koshub.psdku.models.Transaction> transactions) {
        LinearLayout chartContainer = findViewById(R.id.chartContainer);
        TextView chartPlaceholder = findViewById(R.id.tvChartPlaceholder);
        if (chartContainer == null) return;

        if (transactions.isEmpty()) {
            chartContainer.setVisibility(View.GONE);
            if (chartPlaceholder != null) chartPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        // Build 4-week or 4-day grouping based on filter
        // For simplicity, group into 4 buckets
        chartContainer.setVisibility(View.VISIBLE);
        if (chartPlaceholder != null) chartPlaceholder.setVisibility(View.GONE);
        chartContainer.removeAllViews();

        // Divide transactions into 4 equal time buckets
        long startTime = getFilterStartTime();
        long now = System.currentTimeMillis();
        long bucketSize = (now - startTime) / 4;
        if (bucketSize <= 0) bucketSize = 1;

        double[] buckets = new double[4];
        String[] labels = new String[4];

        for (com.koshub.psdku.models.Transaction t : transactions) {
            int idx = (int) ((t.getCreatedAt() - startTime) / bucketSize);
            if (idx >= 0 && idx < 4) {
                if ("available".equals(t.getStatus()) || "withdrawn".equals(t.getStatus()) || "pending".equals(t.getStatus())) {
                    buckets[idx] += t.getAmount();
                }
            }
        }

        // Generate labels based on filter
        SimpleDateFormat sdf;
        switch (currentFilter) {
            case "today": sdf = new SimpleDateFormat("HH:mm", Locale.getDefault()); break;
            case "week": sdf = new SimpleDateFormat("EEE", new Locale("id")); break;
            case "year": sdf = new SimpleDateFormat("MMM", new Locale("id")); break;
            default: sdf = new SimpleDateFormat("'M'd", Locale.getDefault()); break;
        }
        for (int i = 0; i < 4; i++) {
            labels[i] = sdf.format(new Date(startTime + (bucketSize * i)));
        }

        double maxVal = 0;
        for (double b : buckets) if (b > maxVal) maxVal = b;
        if (maxVal <= 0) maxVal = 1;

        int chartHeightPx = (int) (getResources().getDimensionPixelSize(R.dimen.finance_bar_chart_height) * 0.75);

        for (int i = 0; i < 4; i++) {
            android.widget.LinearLayout barColumn = new android.widget.LinearLayout(this);
            android.widget.LinearLayout.LayoutParams colParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            barColumn.setLayoutParams(colParams);
            barColumn.setOrientation(android.widget.LinearLayout.VERTICAL);
            barColumn.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);

            int barHeightPx = maxVal > 0 ? (int) ((buckets[i] / maxVal) * chartHeightPx) : 4;
            if (barHeightPx < 4) barHeightPx = 4; // minimum bar

            View bar = new View(this);
            android.widget.LinearLayout.LayoutParams barParams = new android.widget.LinearLayout.LayoutParams(24, barHeightPx);
            bar.setLayoutParams(barParams);
            bar.setBackgroundColor(getResources().getColor(R.color.brand_green));
            bar.setAlpha(0.75f + (0.25f * (float)(buckets[i] / maxVal)));

            android.widget.TextView tvLabel = new android.widget.TextView(this);
            tvLabel.setText(labels[i]);
            tvLabel.setTextSize(9);
            tvLabel.setTextColor(getResources().getColor(R.color.finance_text_muted));
            android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = 6;
            tvLabel.setLayoutParams(labelParams);

            barColumn.addView(bar);
            barColumn.addView(tvLabel);
            chartContainer.addView(barColumn);
        }
    }

    private void generateAndDownloadPdf() {
        showToast("Membuat laporan PDF...");

        long startTime = getFilterStartTime();
        List<com.koshub.psdku.models.Transaction> filtered = new ArrayList<>();
        for (com.koshub.psdku.models.Transaction t : allTransactions) {
            if (t.getCreatedAt() >= startTime) filtered.add(t);
        }

        // Run PDF generation in background thread
        new Thread(() -> {
            try {
                android.graphics.pdf.PdfDocument pdfDoc = new android.graphics.pdf.PdfDocument();
                android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
                android.graphics.pdf.PdfDocument.Page page = pdfDoc.startPage(pageInfo);
                android.graphics.Canvas canvas = page.getCanvas();

                android.graphics.Paint paint = new android.graphics.Paint();
                paint.setAntiAlias(true);

                // === HEADER ===
                paint.setColor(android.graphics.Color.parseColor("#1B6B3A")); // brand green
                canvas.drawRect(0, 0, 595, 80, paint);

                paint.setColor(android.graphics.Color.WHITE);
                paint.setTextSize(22);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                canvas.drawText("KosHub - Laporan Keuangan", 30, 40, paint);

                paint.setTextSize(12);
                paint.setTypeface(android.graphics.Typeface.DEFAULT);
                String periodText = "Periode: ";
                switch (currentFilter) {
                    case "today": periodText += "Hari Ini"; break;
                    case "week": periodText += "Minggu Ini"; break;
                    case "year": periodText += "Tahun Ini"; break;
                    default: periodText += "Bulan Ini"; break;
                }
                periodText += " | Dibuat: " + new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", new java.util.Locale("id")).format(new java.util.Date());
                canvas.drawText(periodText, 30, 62, paint);

                // === SUMMARY BOX ===
                int y = 100;
                paint.setColor(android.graphics.Color.parseColor("#F5F5F5"));
                canvas.drawRect(20, y, 575, y + 110, paint);

                paint.setColor(android.graphics.Color.parseColor("#1B6B3A"));
                paint.setTextSize(13);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                canvas.drawText("RINGKASAN KEUANGAN", 30, y + 22, paint);

                paint.setColor(android.graphics.Color.DKGRAY);
                paint.setTextSize(11);
                paint.setTypeface(android.graphics.Typeface.DEFAULT);

                // Calculate summary from filtered data
                double totalInc = 0, totalPend = 0, totalWith = 0;
                int lunasC = 0, pendC = 0, cancelC = 0;
                for (com.koshub.psdku.models.Transaction t : filtered) {
                    String s = t.getStatus() != null ? t.getStatus() : "";
                    if ("available".equals(s) || "withdrawn".equals(s)) { totalInc += t.getAmount(); lunasC++; }
                    else if ("pending".equals(s)) { totalPend += t.getAmount(); pendC++; }
                    else if ("cancelled".equals(s)) { cancelC++; }
                }
                for (com.koshub.psdku.models.Withdrawal w : allWithdrawals) {
                    if (w.getCreatedAt() >= startTime && "success".equals(w.getStatus())) totalWith += w.getAmount();
                }

                canvas.drawText("Total Pendapatan  : " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalInc + totalPend), 30, y + 42, paint);
                canvas.drawText("Pembayaran Masuk  : " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalInc), 30, y + 58, paint);
                canvas.drawText("Pembayaran Pending: " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalPend), 30, y + 74, paint);
                canvas.drawText("Total Dicairkan   : " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalWith), 30, y + 90, paint);
                canvas.drawText("Saldo Bersih      : " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(totalInc + totalPend - totalWith), 320, y + 42, paint);
                canvas.drawText("Transaksi Lunas   : " + lunasC, 320, y + 58, paint);
                canvas.drawText("Transaksi Pending : " + pendC, 320, y + 74, paint);
                canvas.drawText("Transaksi Batal   : " + cancelC, 320, y + 90, paint);

                // Target progress
                if (ownerTarget > 0) {
                    double pct = ((totalInc + totalPend) / ownerTarget) * 100;
                    canvas.drawText("Target Bulanan    : " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(ownerTarget)
                            + " (Tercapai: " + String.format(java.util.Locale.getDefault(), "%.0f%%", pct) + ")", 30, y + 106, paint);
                }

                // === TRANSACTION TABLE ===
                y += 130;
                paint.setColor(android.graphics.Color.parseColor("#1B6B3A"));
                paint.setTextSize(13);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                canvas.drawText("DAFTAR TRANSAKSI", 30, y, paint);
                y += 14;

                // Table header
                paint.setColor(android.graphics.Color.parseColor("#EEEEEE"));
                canvas.drawRect(20, y, 575, y + 20, paint);
                paint.setColor(android.graphics.Color.DKGRAY);
                paint.setTextSize(10);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                canvas.drawText("No", 28, y + 14, paint);
                canvas.drawText("Nama Kos / Deskripsi", 55, y + 14, paint);
                canvas.drawText("Tanggal", 300, y + 14, paint);
                canvas.drawText("Jumlah", 400, y + 14, paint);
                canvas.drawText("Status", 500, y + 14, paint);
                y += 22;

                paint.setTypeface(android.graphics.Typeface.DEFAULT);
                paint.setTextSize(9);

                int rowNo = 1;
                int pageNo = 1;
                for (com.koshub.psdku.models.Transaction t : filtered) {
                    // Check if we need a new page
                    if (y > 800) {
                        pdfDoc.finishPage(page);
                        pageNo++;
                        android.graphics.pdf.PdfDocument.PageInfo nextPageInfo =
                            new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNo).create();
                        page = pdfDoc.startPage(nextPageInfo);
                        canvas = page.getCanvas();
                        y = 40;
                        paint.setColor(android.graphics.Color.DKGRAY);
                        paint.setTextSize(9);
                    }

                    // Alternating row color
                    if (rowNo % 2 == 0) {
                        paint.setColor(android.graphics.Color.parseColor("#F9F9F9"));
                        canvas.drawRect(20, y - 2, 575, y + 14, paint);
                    }

                    paint.setColor(android.graphics.Color.DKGRAY);
                    canvas.drawText(String.valueOf(rowNo), 28, y + 10, paint);
                    String kosName = t.getKosName() != null ? t.getKosName() : "Pembayaran Sewa";
                    if (kosName.length() > 30) kosName = kosName.substring(0, 28) + "..";
                    canvas.drawText(kosName, 55, y + 10, paint);
                    canvas.drawText(com.koshub.psdku.utils.DateHelper.formatDate(t.getCreatedAt()), 300, y + 10, paint);
                    canvas.drawText(com.koshub.psdku.utils.CurrencyHelper.formatRupiah(t.getAmount()), 400, y + 10, paint);
                    String statusLabel = "available".equals(t.getStatus()) ? "Lunas" :
                                         "pending".equals(t.getStatus()) ? "Pending" :
                                         "withdrawn".equals(t.getStatus()) ? "Dicairkan" : "Batal";
                    int statusColor = "available".equals(t.getStatus()) || "withdrawn".equals(t.getStatus())
                            ? android.graphics.Color.parseColor("#1B6B3A")
                            : "pending".equals(t.getStatus()) ? android.graphics.Color.parseColor("#E65100")
                            : android.graphics.Color.RED;
                    paint.setColor(statusColor);
                    canvas.drawText(statusLabel, 500, y + 10, paint);
                    paint.setColor(android.graphics.Color.DKGRAY);

                    y += 18;
                    rowNo++;
                }

                // === WITHDRAWAL HISTORY ===
                if (!allWithdrawals.isEmpty()) {
                    y += 20;
                    if (y > 760) {
                        pdfDoc.finishPage(page);
                        pageNo++;
                        android.graphics.pdf.PdfDocument.PageInfo nextPageInfo =
                            new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNo).create();
                        page = pdfDoc.startPage(nextPageInfo);
                        canvas = page.getCanvas();
                        y = 40;
                    }
                    paint.setColor(android.graphics.Color.parseColor("#1B6B3A"));
                    paint.setTextSize(13);
                    paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                    canvas.drawText("RIWAYAT PENARIKAN SALDO", 30, y, paint);
                    y += 18;
                    paint.setTextSize(9);
                    paint.setTypeface(android.graphics.Typeface.DEFAULT);
                    for (com.koshub.psdku.models.Withdrawal w : allWithdrawals) {
                        if (y > 800) {
                            pdfDoc.finishPage(page);
                            pageNo++;
                            android.graphics.pdf.PdfDocument.PageInfo np =
                                new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNo).create();
                            page = pdfDoc.startPage(np);
                            canvas = page.getCanvas();
                            y = 40;
                        }
                        paint.setColor(android.graphics.Color.DKGRAY);
                        canvas.drawText(com.koshub.psdku.utils.DateHelper.formatDate(w.getCreatedAt())
                                + " | " + w.getBankName()
                                + " | " + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(w.getAmount())
                                + " | " + w.getStatus(), 30, y + 10, paint);
                        y += 18;
                    }
                }

                // === FOOTER ===
                paint.setColor(android.graphics.Color.LTGRAY);
                paint.setTextSize(8);
                canvas.drawText("Laporan ini dibuat otomatis oleh KosHub App. " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", new java.util.Locale("id")).format(new java.util.Date()), 30, 830, paint);

                pdfDoc.finishPage(page);

                // Save to file
                java.io.File dir = new java.io.File(getExternalFilesDir(null), "KosHub/Reports");
                if (!dir.exists()) dir.mkdirs();
                String fileName = "LaporanKeuangan_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date()) + ".pdf";
                java.io.File file = new java.io.File(dir, fileName);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                pdfDoc.writeTo(fos);
                fos.close();
                pdfDoc.close();

                // Open the PDF
                runOnUiThread(() -> {
                    showToast("Laporan berhasil dibuat: " + fileName);
                    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.setFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(android.content.Intent.createChooser(intent, "Buka Laporan PDF"));
                    } catch (android.content.ActivityNotFoundException e) {
                        showToast("Tidak ada aplikasi PDF. File tersimpan di: " + file.getAbsolutePath());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal membuat PDF: " + e.getMessage()));
            }
        }).start();
    }

    private void showAllTransactionsDialog() {
        if (allTransactions.isEmpty()) {
            showToast("Tidak ada data transaksi");
            return;
        }
        long startTime = getFilterStartTime();
        List<com.koshub.psdku.models.Transaction> filtered = new ArrayList<>();
        for (com.koshub.psdku.models.Transaction t : allTransactions) {
            if (t.getCreatedAt() >= startTime) filtered.add(t);
        }
        if (filtered.isEmpty()) {
            showToast("Tidak ada transaksi pada periode ini");
            return;
        }
        String[] items = new String[filtered.size()];
        for (int i = 0; i < filtered.size(); i++) {
            com.koshub.psdku.models.Transaction t = filtered.get(i);
            items[i] = (t.getKosName() != null ? t.getKosName() : "Sewa") + " • "
                     + com.koshub.psdku.utils.CurrencyHelper.formatRupiah(t.getAmount()) + " • "
                     + com.koshub.psdku.utils.DateHelper.formatDate(t.getCreatedAt());
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Semua Transaksi")
                .setItems(items, null)
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void renderWithdrawals(List<Withdrawal> withdrawals) {
        try {
            if (withdrawalHistoryContainer == null) return;
            withdrawalHistoryContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            if (withdrawals == null || withdrawals.isEmpty()) {
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
                if (w == null) continue;
                View itemView = inflater.inflate(R.layout.item_withdrawal_history, withdrawalHistoryContainer, false);
                if (itemView == null) continue;

                TextView tvAmount = itemView.findViewById(R.id.tvWithdrawAmount);
                TextView tvDate = itemView.findViewById(R.id.tvWithdrawDate);
                TextView tvStatus = itemView.findViewById(R.id.tvWithdrawStatus);

                if (tvAmount != null) tvAmount.setText(CurrencyHelper.formatRupiah(w.getAmount()));
                if (tvDate != null) tvDate.setText(DateHelper.formatDate(w.getCreatedAt()) + " • " + w.getBankName());
                if (tvStatus != null) {
                    tvStatus.setText(formatStatus(w.getStatus()));
                    // Set status background and text color based on status
                    setStatusStyle(tvStatus, w.getStatus());
                }

                withdrawalHistoryContainer.addView(itemView);

                // Add divider if not last
                if (i < count - 1) {
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(getResources().getColor(R.color.finance_divider));
                    withdrawalHistoryContainer.addView(divider);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in renderWithdrawals", e);
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

    @Override
    protected void onDestroy() {
        if (transactionListener != null) transactionListener.remove();
        if (withdrawalListener != null) withdrawalListener.remove();
        super.onDestroy();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
