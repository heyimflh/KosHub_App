package com.koshub.psdku;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.StorageRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.services.FirebaseService;
import com.google.firebase.auth.FirebaseAuth;

/**
 * OwnerProfileSettingsActivity - Halaman Profil & Pengaturan Pemilik Kos (Improved)
 */
public class OwnerProfileSettingsActivity extends AppCompatActivity {

    private CloudinaryRepository cloudinaryRepository;
    private ImageView imgProfile;

    private final ActivityResultLauncher<String> profilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Show local preview immediately with circle crop
                    if (imgProfile != null) Glide.with(this).load(uri).circleCrop().into(imgProfile);
                    uploadProfileImage(uri);
                }
            }
    );

    private View btnBack, btnEditProfile, btnCompleteLegal;
    private LinearLayout menuLegal, menuPayment, menuSecurity, menuHelp;
    private LinearLayout btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_profile_settings);

        cloudinaryRepository = CloudinaryRepository.getInstance();
        initViews();
        loadProfileData();
        setupListeners();
        OwnerBottomNavHelper.setup(this, OwnerBottomNavHelper.NavItem.PROFILE);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackProfile);
        btnEditProfile = findViewById(R.id.btnEditProfileOwner);
        btnCompleteLegal = findViewById(R.id.btnCompleteLegal);
        imgProfile = findViewById(R.id.imgOwnerAvatar);
        
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
            btnEditProfile.setOnClickListener(v -> profilePickerLauncher.launch("image/*"));
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



    private void uploadProfileImage(Uri uri) {
        showToast("Sedang mengupdate foto profil...");
        // Show local preview immediately with circle crop
        Glide.with(this).load(uri).circleCrop().into(imgProfile);

        cloudinaryRepository.uploadProfileImage(this, uri, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                showToast("Foto profil diperbarui");
                Glide.with(OwnerProfileSettingsActivity.this)
                        .load(downloadUrl)
                        .placeholder(R.drawable.bg_avatar_circle)
                        .circleCrop()
                        .into(imgProfile);
            }

            @Override
            public void onError(String message) {
                showToast("Gagal upload: " + message);
            }
        });
    }

    private void loadProfileData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString(DatabaseConstants.FIELD_NAME);
                        String email = documentSnapshot.getString(DatabaseConstants.FIELD_EMAIL);
                        String photo = documentSnapshot.getString(DatabaseConstants.FIELD_PROFILE_IMAGE_URL);

                        if (name != null) ((TextView)findViewById(R.id.tvOwnerName)).setText(name);
                        if (email != null) ((TextView)findViewById(R.id.tvOwnerEmail)).setText(email);
                        if (photo != null && !photo.isEmpty()) {
                            Glide.with(this).load(photo).placeholder(R.drawable.bg_avatar_circle).circleCrop().into(imgProfile);
                        } else {
                            Glide.with(this).load(R.drawable.bg_avatar_circle).circleCrop().into(imgProfile);
                        }
                    });
        }
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
