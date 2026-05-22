package com.koshub.psdku;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OwnerWithdrawActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etNominal, etBank, etNoRek, etNamaRek, etNote;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_withdraw);

        initViews();
        setupListeners();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.NONE);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
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
                showToast("Permintaan tarik saldo berhasil diajukan");
                NavigationTransitionHelper.finishWithBackTransition(this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }

    private boolean validateForm() {
        if (etNominal.getText().toString().isEmpty()) {
            showToast("Nominal wajib diisi");
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
