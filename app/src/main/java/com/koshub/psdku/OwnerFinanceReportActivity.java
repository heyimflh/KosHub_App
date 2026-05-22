package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * OwnerFinanceReportActivity - Laporan Keuangan Pemilik Kos
 *
 * Menampilkan ringkasan keuangan, target pendapatan, transaksi terbaru,
 * status pembayaran, insight, dan fitur export laporan.
 */
public class OwnerFinanceReportActivity extends AppCompatActivity {

    // Filter Chips
    private TextView chipToday, chipWeek, chipMonth, chipYear;

    // Summary Cards
    private LinearLayout cardPaymentIn, cardPaymentPending, cardExpense, cardNetIncome, btnTarikSaldo;

    // Target Progress
    private ProgressBar progressTarget;

    // Transactions
    private LinearLayout sectionTransactions;
    private TextView btnSeeAllTransactions;
    private LinearLayout transItem1, transItem2, transItem3;

    // Export
    private LinearLayout btnExportReport;
    private View btnFinanceExportHeader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_finance_report);

        initViews();
        setupFilterChips();
        setupSummaryCards();
        setupTarget();
        setupTransactions();
        setupExport();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.NONE);
    }

    private void initViews() {
        chipToday = findViewById(R.id.chipToday);
        chipWeek = findViewById(R.id.chipWeek);
        chipMonth = findViewById(R.id.chipMonth);
        chipYear = findViewById(R.id.chipYear);

        cardPaymentIn = findViewById(R.id.cardPaymentIn);
        cardPaymentPending = findViewById(R.id.cardPaymentPending);
        cardExpense = findViewById(R.id.cardExpense);
        cardNetIncome = findViewById(R.id.cardNetIncome);
        btnTarikSaldo = findViewById(R.id.btnTarikSaldo);

        progressTarget = findViewById(R.id.progressTarget);

        sectionTransactions = findViewById(R.id.sectionTransactions);
        btnSeeAllTransactions = findViewById(R.id.btnSeeAllTransactions);
        transItem1 = findViewById(R.id.transItem1);
        transItem2 = findViewById(R.id.transItem2);
        transItem3 = findViewById(R.id.transItem3);

        btnExportReport = findViewById(R.id.btnExportReport);
        btnFinanceExportHeader = findViewById(R.id.btnFinanceExportHeader);
    }

    private void setupFilterChips() {
        chipToday.setOnClickListener(v -> {
            resetChips();
            chipToday.setBackgroundResource(R.drawable.bg_finance_chip_active);
            chipToday.setTextColor(getResources().getColor(R.color.finance_chip_active_text));
            showToast("📅 Filter: Hari Ini");
        });
        chipWeek.setOnClickListener(v -> {
            resetChips();
            chipWeek.setBackgroundResource(R.drawable.bg_finance_chip_active);
            chipWeek.setTextColor(getResources().getColor(R.color.finance_chip_active_text));
            showToast("📅 Filter: Minggu Ini");
        });
        chipMonth.setOnClickListener(v -> {
            resetChips();
            chipMonth.setBackgroundResource(R.drawable.bg_finance_chip_active);
            chipMonth.setTextColor(getResources().getColor(R.color.finance_chip_active_text));
            showToast("📅 Filter: Bulan Ini");
        });
        chipYear.setOnClickListener(v -> {
            resetChips();
            chipYear.setBackgroundResource(R.drawable.bg_finance_chip_active);
            chipYear.setTextColor(getResources().getColor(R.color.finance_chip_active_text));
            showToast("📅 Filter: Tahun Ini");
        });
    }

    private void resetChips() {
        int inactiveColor = getResources().getColor(R.color.finance_chip_inactive_text);
        chipToday.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipToday.setTextColor(inactiveColor);
        chipWeek.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipWeek.setTextColor(inactiveColor);
        chipMonth.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipMonth.setTextColor(inactiveColor);
        chipYear.setBackgroundResource(R.drawable.bg_finance_chip_inactive);
        chipYear.setTextColor(inactiveColor);
    }

    private void setupSummaryCards() {
        cardPaymentIn.setOnClickListener(v ->
                showToast("💰 Pembayaran masuk: Rp 10.750.000"));
        cardPaymentPending.setOnClickListener(v ->
                showToast("⏳ Pembayaran pending: Rp 1.750.000"));
        cardExpense.setOnClickListener(v ->
                showToast("📤 Total pengeluaran: Rp 2.300.000"));
        cardNetIncome.setOnClickListener(v ->
                showToast("✅ Saldo bersih: Rp 10.200.000"));
        btnTarikSaldo.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerWithdrawActivity.class);
            startActivity(intent);
        });
    }

    private void setupTarget() {
        progressTarget.setProgress(83);
    }

    private void setupTransactions() {
        btnSeeAllTransactions.setOnClickListener(v ->
                showToast("📄 Memuat semua transaksi..."));
        transItem1.setOnClickListener(v ->
                showToast("✅ Sewa Kamar A-12 - Muhammad Fakhri - Berhasil"));
        transItem2.setOnClickListener(v ->
                showToast("⏳ Sewa Kamar B-04 - Raka Pratama - Pending"));
        transItem3.setOnClickListener(v ->
                showToast("📤 Perbaikan AC K02 - Maintenance - Keluar"));
    }

    private void setupExport() {
        btnExportReport.setOnClickListener(v ->
                showToast("📥 Fitur export laporan akan tersedia."));
        btnFinanceExportHeader.setOnClickListener(v ->
                showToast("📥 Download laporan keuangan..."));
    }



    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
