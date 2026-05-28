package com.koshub.psdku;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.koshub.psdku.models.FinanceSummary;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;

public class OwnerWithdrawActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvAvailableBalance;
    private EditText etNominal, etBank, etNoRek, etNamaRek, etNote;
    private Button btnSubmit;
    private double availableBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_withdraw);

        initViews();
        setupListeners();
        loadBalance();
        loadBankDetails();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.NONE);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAvailableBalance = findViewById(R.id.tvAvailableBalance);
        etNominal = findViewById(R.id.etNominal);
        etBank = findViewById(R.id.etBank);
        etNoRek = findViewById(R.id.etNoRek);
        etNamaRek = findViewById(R.id.etNamaRek);
        etNote = findViewById(R.id.etNote);
        btnSubmit = findViewById(R.id.btnSubmitWithdraw);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                handleWithdraw();
            }
        });
    }

    private void loadBalance() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FinanceRepository.getInstance().getFinanceSummary(uid, new FinanceRepository.FinanceSummaryCallback() {
            @Override
            public void onSuccess(FinanceSummary summary) {
                availableBalance = summary.getAvailableBalance();
                tvAvailableBalance.setText(CurrencyHelper.formatRupiah(availableBalance));
            }

            @Override
            public void onError(String message) {
                showToast("Gagal memuat saldo: " + message);
            }
        });
    }

    private void loadBankDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String bank = doc.getString(DatabaseConstants.FIELD_BANK_NAME);
                        String num = doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NUMBER);
                        String name = doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NAME);

                        if (bank != null) etBank.setText(bank);
                        if (num != null) etNoRek.setText(num);
                        if (name != null) etNamaRek.setText(name);
                    }
                });
    }

    private void handleWithdraw() {
        double amount = Double.parseDouble(etNominal.getText().toString());
        String bankName = etBank.getText().toString();
        String accountNo = etNoRek.getText().toString();
        String accountHolder = etNamaRek.getText().toString();
        
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Memproses...");

        FinanceRepository.getInstance().requestWithdraw(bankName, accountNo, accountHolder, amount, new FinanceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                showToast("Permintaan withdraw berhasil dikirim.");
                NavigationTransitionHelper.finishWithBackTransition(OwnerWithdrawActivity.this);
            }

            @Override
            public void onError(String message) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Ajukan Penarikan");
                showToast(message);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }

    private boolean validateForm() {
        String nominalStr = etNominal.getText().toString();
        if (nominalStr.isEmpty()) {
            showToast("Nominal wajib diisi");
            return false;
        }

        try {
            double amount = Double.parseDouble(nominalStr);
            if (amount <= 0) {
                showToast("Nominal tidak valid");
                return false;
            }
            if (amount > availableBalance) {
                showToast("Saldo tersedia tidak mencukupi");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Nominal harus berupa angka");
            return false;
        }

        if (etBank.getText().toString().isEmpty()) {
            showToast("Nama Bank wajib diisi");
            return false;
        }
        if (etNoRek.getText().toString().isEmpty()) {
            showToast("Nomor rekening wajib diisi");
            return false;
        }
        if (etNamaRek.getText().toString().isEmpty()) {
            showToast("Nama pemilik rekening wajib diisi");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
