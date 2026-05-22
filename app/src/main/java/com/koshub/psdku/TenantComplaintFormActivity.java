package com.koshub.psdku;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TenantComplaintFormActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerCategory;
    private EditText etTitle, etDesc;
    private Button btnSubmit;
    private LinearLayout btnUploadPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_complaint_form);

        initViews();
        setupCategorySpinner();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackTenantComplaint);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etTitle = findViewById(R.id.etComplaintTitle);
        etDesc = findViewById(R.id.etComplaintDesc);
        btnSubmit = findViewById(R.id.btnSubmitComplaint);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
    }

    private void setupCategorySpinner() {
        String[] categories = {"Fasilitas", "Kebersihan", "Internet", "Pembayaran", "Keamanan", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));

        btnUploadPhoto.setOnClickListener(v -> showToast("Upload foto belum tersedia"));

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                showToast("Laporan komplain berhasil dikirim ke owner");
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
        if (etTitle.getText().toString().trim().isEmpty()) {
            showToast("Judul komplain wajib diisi");
            return false;
        }
        if (etDesc.getText().toString().trim().isEmpty()) {
            showToast("Deskripsi komplain wajib diisi");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
