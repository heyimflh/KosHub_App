package com.koshub.psdku;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.koshub.psdku.models.FinanceSummary;
import com.koshub.psdku.models.OwnerKosStats;
import com.koshub.psdku.repositories.BookingRepository;
import com.koshub.psdku.repositories.CloudinaryRepository;
import com.koshub.psdku.repositories.FinanceRepository;
import com.koshub.psdku.repositories.KosRepository;
import com.koshub.psdku.repositories.StorageRepository;
import com.koshub.psdku.utils.CurrencyHelper;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.services.FirebaseService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

/**
 * OwnerProfileSettingsActivity - Halaman Profil & Pengaturan Pemilik Kos (Improved)
 */
public class OwnerProfileSettingsActivity extends AppCompatActivity {

    private CloudinaryRepository cloudinaryRepository;
    private ImageView imgProfile;
    private TextView tvOwnerName, tvOwnerEmail, tvOwnerProfileCompletion, tvOwnerKosCount, tvOwnerHunian, tvOwnerBookingCount, tvOwnerRevenue;
    private ProgressBar progressOwnerCompletion;

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

    private final ActivityResultLauncher<String> ktpPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadLegalDoc(uri, "ktp");
                }
            }
    );

    private final ActivityResultLauncher<String> skuPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadLegalDoc(uri, "sku");
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
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvOwnerEmail = findViewById(R.id.tvOwnerEmail);
        tvOwnerProfileCompletion = findViewById(R.id.tvOwnerProfileCompletion);
        tvOwnerKosCount = findViewById(R.id.tvOwnerKosCount);
        tvOwnerHunian = findViewById(R.id.tvOwnerHunian);
        tvOwnerBookingCount = findViewById(R.id.tvOwnerBookingCount);
        tvOwnerRevenue = findViewById(R.id.tvOwnerRevenue);
        progressOwnerCompletion = findViewById(R.id.progressOwnerCompletion);
        
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
            btnCompleteLegal.setOnClickListener(v -> showLegalDocBottomSheet());
        }

        menuLegal.setOnClickListener(v -> showLegalDocBottomSheet());
        menuPayment.setOnClickListener(v -> showBankAccountBottomSheet());
        menuSecurity.setOnClickListener(v -> showSecurityBottomSheet());
        menuHelp.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpFaqActivity.class);
            intent.putExtra("role", "owner");
            startActivity(intent);
        });

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
        showToast("Sedang mengupload foto profil...");
        // Show local preview immediately
        if (imgProfile != null) Glide.with(this).load(uri).circleCrop().into(imgProfile);

        cloudinaryRepository.uploadProfileImage(this, uri, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    showToast("Foto profil diperbarui");
                    String optimizedUrl = cloudinaryRepository.getOptimizedUrl(imageUrl, 200, 200, true);
                    Glide.with(OwnerProfileSettingsActivity.this)
                            .load(optimizedUrl)
                            .placeholder(R.drawable.bg_avatar_circle)
                            .circleCrop()
                            .into(imgProfile);
                    loadProfileData();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showToast("Gagal upload: " + message));
            }
        });
    }

    private void uploadLegalDoc(Uri uri, String type) {
        showToast("Sedang mengupload dokumen " + type.toUpperCase() + "...");
        cloudinaryRepository.uploadLegalDoc(this, uri, type, new CloudinaryRepository.SimpleUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    showToast("Dokumen " + type.toUpperCase() + " berhasil diupload");
                    loadProfileData();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showToast("Gagal upload: " + message));
            }
        });
    }

    private void loadProfileData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString(DatabaseConstants.FIELD_NAME);
                            String email = documentSnapshot.getString(DatabaseConstants.FIELD_EMAIL);
                            String photo = documentSnapshot.getString(DatabaseConstants.FIELD_PROFILE_IMAGE_URL);

                            if (name != null) tvOwnerName.setText(name);
                            if (email != null) tvOwnerEmail.setText(email);
                            if (photo != null && !photo.isEmpty()) {
                                Glide.with(this).load(photo).placeholder(R.drawable.bg_avatar_circle).circleCrop().into(imgProfile);
                            } else {
                                Glide.with(this).load(R.drawable.bg_avatar_circle).circleCrop().into(imgProfile);
                            }

                            calculateProfileCompletion(documentSnapshot);
                            loadOwnerStats(uid);
                        }
                    });
        }
    }

    private void calculateProfileCompletion(DocumentSnapshot doc) {
        int totalFields = 8;
        int filledFields = 0;

        if (doc.getString(DatabaseConstants.FIELD_NAME) != null && !doc.getString(DatabaseConstants.FIELD_NAME).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_EMAIL) != null && !doc.getString(DatabaseConstants.FIELD_EMAIL).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_PROFILE_IMAGE_URL) != null && !doc.getString(DatabaseConstants.FIELD_PROFILE_IMAGE_URL).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_BANK_NAME) != null && !doc.getString(DatabaseConstants.FIELD_BANK_NAME).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NUMBER) != null && !doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NUMBER).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NAME) != null && !doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NAME).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_DOC_KTP) != null && !doc.getString(DatabaseConstants.FIELD_DOC_KTP).isEmpty()) filledFields++;
        if (doc.getString(DatabaseConstants.FIELD_DOC_SKU) != null && !doc.getString(DatabaseConstants.FIELD_DOC_SKU).isEmpty()) filledFields++;

        int percentage = (filledFields * 100) / totalFields;
        if (tvOwnerProfileCompletion != null) tvOwnerProfileCompletion.setText(String.format(Locale.getDefault(), "%d%%", percentage));
        if (progressOwnerCompletion != null) progressOwnerCompletion.setProgress(percentage);
    }

    private void loadOwnerStats(String ownerId) {
        // 1. Kos Stats (Total Kos & Occupancy)
        KosRepository.getInstance().calculateOwnerKosStats(ownerId, new KosRepository.StatsCallback() {
            @Override
            public void onSuccess(OwnerKosStats stats) {
                if (tvOwnerKosCount != null) tvOwnerKosCount.setText(String.valueOf(stats.getTotalKos()));
                if (tvOwnerHunian != null) tvOwnerHunian.setText(String.format(Locale.getDefault(), "%.0f%%", stats.getOccupancyRate()));
            }

            @Override
            public void onError(String message) {}
        });

        // 2. Booking Count (Active Bookings)
        BookingRepository.getInstance().getBookingsByOwner(ownerId, new BookingRepository.BookingListCallback() {
            @Override
            public void onSuccess(java.util.List<com.koshub.psdku.models.Booking> bookings) {
                int activeCount = 0;
                for (com.koshub.psdku.models.Booking b : bookings) {
                    String status = b.getStatus();
                    if (status != null && (status.equals(DatabaseConstants.BOOKING_PENDING) || 
                        status.equals(DatabaseConstants.BOOKING_ACCEPTED) || 
                        status.equals(DatabaseConstants.BOOKING_WAITING_CHECKIN) || 
                        status.equals(DatabaseConstants.BOOKING_ACTIVE))) {
                        activeCount++;
                    }
                }
                if (tvOwnerBookingCount != null) tvOwnerBookingCount.setText(String.valueOf(activeCount));
            }

            @Override
            public void onError(String message) {}
        });

        // 3. Finance Summary (Revenue)
        FinanceRepository.getInstance().getFinanceSummary(ownerId, new FinanceRepository.FinanceSummaryCallback() {
            @Override
            public void onSuccess(FinanceSummary summary) {
                if (tvOwnerRevenue != null) {
                    tvOwnerRevenue.setText(CurrencyHelper.formatRupiah(summary.getTotalIncome()));
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSecurityBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_security, null);
        dialog.setContentView(view);

        android.widget.EditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        android.widget.EditText etNew = view.findViewById(R.id.etNewPassword);
        android.widget.EditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        View btnChangePassword = view.findViewById(R.id.btnChangePassword);
        View btnChangeEmail = view.findViewById(R.id.btnChangeEmail);
        View btnCancel = view.findViewById(R.id.btnCancelSecurity);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnChangePassword.setOnClickListener(v -> {
            String currentPw = etCurrent.getText().toString();
            String newPw = etNew.getText().toString();
            String confirmPw = etConfirm.getText().toString();

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                showToast("Semua field harus diisi");
                return;
            }

            if (newPw.length() < 8) {
                showToast("Password baru minimal 8 karakter");
                return;
            }

            if (!newPw.equals(confirmPw)) {
                showToast("Konfirmasi password tidak cocok");
                return;
            }

            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), currentPw);
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPw).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                showToast("Password berhasil diperbarui");
                                dialog.dismiss();
                            } else {
                                showToast("Gagal update password: " + updateTask.getException().getMessage());
                            }
                        });
                    } else {
                        showToast("Password lama salah");
                    }
                });
            }
        });

        btnChangeEmail.setOnClickListener(v -> {
            String currentPw = etCurrent.getText().toString();
            if (currentPw.isEmpty()) {
                showToast("Masukkan password saat ini untuk ubah email");
                return;
            }

            android.widget.EditText etEmail = new android.widget.EditText(this);
            etEmail.setHint("Email Baru");
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Ubah Email")
                    .setView(etEmail)
                    .setPositiveButton("Simpan", (d, w) -> {
                        String newEmail = etEmail.getText().toString().trim();
                        if (newEmail.isEmpty()) return;

                        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), currentPw);
                            user.reauthenticate(credential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    user.updateEmail(newEmail).addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS)
                                                    .document(user.getUid())
                                                    .update(DatabaseConstants.FIELD_EMAIL, newEmail);
                                            showToast("Email berhasil diperbarui");
                                            loadProfileData();
                                            dialog.dismiss();
                                        } else {
                                            showToast("Gagal update email: " + emailTask.getException().getMessage());
                                        }
                                    });
                                } else {
                                    showToast("Password lama salah");
                                }
                            });
                        }
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        dialog.show();
    }

    private void showLegalDocBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_legal_doc, null);
        dialog.setContentView(view);

        ImageView imgPreview = view.findViewById(R.id.imgDocPreview);
        TextView tvKtpStatus = view.findViewById(R.id.tvKtpStatus);
        TextView tvSkuStatus = view.findViewById(R.id.tvSkuStatus);
        View btnKtp = view.findViewById(R.id.btnUploadKTP);
        View btnSku = view.findViewById(R.id.btnUploadSKU);
        View btnCancel = view.findViewById(R.id.btnCancelLegal);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String ktp = doc.getString(DatabaseConstants.FIELD_DOC_KTP);
                            String sku = doc.getString(DatabaseConstants.FIELD_DOC_SKU);
                            Boolean isVerifiedObj = doc.getBoolean(DatabaseConstants.FIELD_IS_VERIFIED);
                            boolean verified = isVerifiedObj != null && isVerifiedObj;

                            if (ktp != null && !ktp.isEmpty()) {
                                tvKtpStatus.setText(verified ? "Terverifikasi ✓" : "Sudah Diupload");
                                tvKtpStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
                                Glide.with(this).load(ktp).into(imgPreview);
                            }
                            if (sku != null && !sku.isEmpty()) {
                                tvSkuStatus.setText(verified ? "Terverifikasi ✓" : "Sudah Diupload");
                                tvSkuStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
                                if (ktp == null) Glide.with(this).load(sku).into(imgPreview);
                            }
                        }
                    });
        }

        btnKtp.setOnClickListener(v -> ktpPickerLauncher.launch("image/*"));
        btnSku.setOnClickListener(v -> skuPickerLauncher.launch("image/*"));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showBankAccountBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_bank_account, null);
        dialog.setContentView(view);

        android.widget.EditText etBank = view.findViewById(R.id.etBankName);
        android.widget.EditText etNum = view.findViewById(R.id.etAccountNumber);
        android.widget.EditText etName = view.findViewById(R.id.etAccountName);
        View btnSave = view.findViewById(R.id.btnSaveBankAccount);
        View btnCancel = view.findViewById(R.id.btnCancelBank);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            etBank.setText(doc.getString(DatabaseConstants.FIELD_BANK_NAME));
                            etNum.setText(doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NUMBER));
                            etName.setText(doc.getString(DatabaseConstants.FIELD_BANK_ACCOUNT_NAME));
                        }
                    });
        }

        btnSave.setOnClickListener(v -> {
            String bank = etBank.getText().toString().trim();
            String num = etNum.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (bank.isEmpty() || num.isEmpty() || name.isEmpty()) {
                showToast("Semua data bank harus diisi");
                return;
            }

            if (uid != null) {
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put(DatabaseConstants.FIELD_BANK_NAME, bank);
                updates.put(DatabaseConstants.FIELD_BANK_ACCOUNT_NUMBER, num);
                updates.put(DatabaseConstants.FIELD_BANK_ACCOUNT_NAME, name);
                updates.put(DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());

                FirebaseService.getFirestore().collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            showToast("Data rekening berhasil disimpan");
                            loadProfileData(); // Update completion %
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> showToast("Gagal menyimpan data: " + e.getMessage()));
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
