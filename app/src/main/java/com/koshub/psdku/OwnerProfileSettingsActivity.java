package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * OwnerProfileSettingsActivity - Halaman Profil & Pengaturan Pemilik Kos (Improved)
 */
public class OwnerProfileSettingsActivity extends AppCompatActivity {

    private View btnBack, btnEditProfile, btnCompleteLegal;
    private LinearLayout menuLegal, menuPayment, menuSecurity, menuHelp;
    private LinearLayout btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_profile_settings);

        initViews();
        setupListeners();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.PROFILE);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackProfile);
        btnEditProfile = findViewById(R.id.btnEditProfileOwner);
        btnCompleteLegal = findViewById(R.id.btnCompleteLegal);
        
        menuLegal = findViewById(R.id.menuOwnerLegal);
        menuPayment = findViewById(R.id.menuOwnerPayment);
        menuSecurity = findViewById(R.id.menuOwnerSecurity);
        menuHelp = findViewById(R.id.menuOwnerHelp);
        
        btnLogout = findViewById(R.id.btnLogoutOwner);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(this));
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showToast("✏️ Membuka Edit Profil..."));
        }

        if (btnCompleteLegal != null) {
            btnCompleteLegal.setOnClickListener(v -> showToast("📄 Silakan upload SIUP/TDP Anda."));
        }

        menuLegal.setOnClickListener(v -> showToast("📜 Membuka Dokumen Legalitas..."));
        menuPayment.setOnClickListener(v -> showToast("💳 Membuka Metode Pencairan..."));
        menuSecurity.setOnClickListener(v -> showToast("🔒 Membuka Keamanan & Password..."));
        menuHelp.setOnClickListener(v -> showToast("❓ Menghubungi CS KosHub..."));

        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Yakin ingin keluar dari akun?")
                    .setPositiveButton("Ya", (dialog, which) -> {
                        showToast("👋 Keluar dari akun...");
                        com.koshub.psdku.repositories.AuthRepository.getInstance().logout(this, new com.koshub.psdku.repositories.AuthRepository.AuthCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Intent intent = new Intent(OwnerProfileSettingsActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                showToast("Gagal logout: " + message);
                            }
                        });
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }



    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
