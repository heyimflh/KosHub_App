package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;
import com.koshub.psdku.repositories.AuthRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.ValidationHelper;

public class RegisterActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private String selectedRole = DatabaseConstants.ROLE_STUDENT;
    private AuthRepository authRepository;
    private ProgressBar progressBar;
    private View btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = AuthRepository.getInstance();
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnRegister);

        // Handle window insets for status bar
        View root = findViewById(R.id.imgLogo);
        if (root != null && root.getParent() instanceof View) {
            View parent = (View) root.getParent();
            int initialPaddingTop = parent.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(parent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + initialPaddingTop, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        setupRoleToggle();
        setupPasswordToggle();
        setupRegisterButton();
        setupTermsText();
        setupNavigation();
    }

    private void setupRoleToggle() {
        TextView btnMahasiswa = findViewById(R.id.btnRoleMahasiswa);
        TextView btnOwner = findViewById(R.id.btnRoleOwner);
        View indicator = findViewById(R.id.viewRoleIndicator);
        TextView tvTitle = findViewById(R.id.tvRegisterTitle);
        TextView tvSub = findViewById(R.id.tvRegisterSub);

        // Initial setup for indicator width
        indicator.post(() -> {
            android.view.ViewGroup.LayoutParams params = indicator.getLayoutParams();
            params.width = btnMahasiswa.getWidth();
            indicator.setLayoutParams(params);
        });

        btnMahasiswa.setOnClickListener(v -> {
            if (selectedRole.equals(DatabaseConstants.ROLE_STUDENT)) return;
            selectedRole = DatabaseConstants.ROLE_STUDENT;

            // Animate indicator
            indicator.animate()
                    .translationX(0)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();

            // Color transitions
            updateToggleColors(btnMahasiswa, btnOwner);

            // Update text with fade
            tvTitle.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvTitle.setText(getString(R.string.register_title_mahasiswa));
                tvTitle.animate().alpha(1f).setDuration(150).start();
            }).start();

            tvSub.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvSub.setText(getString(R.string.register_subtitle_mahasiswa));
                tvSub.animate().alpha(1f).setDuration(150).start();
            }).start();
        });

        btnOwner.setOnClickListener(v -> {
            if (selectedRole.equals(DatabaseConstants.ROLE_OWNER)) return;
            selectedRole = DatabaseConstants.ROLE_OWNER;

            // Animate indicator
            indicator.animate()
                    .translationX(btnMahasiswa.getWidth())
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();

            // Color transitions
            updateToggleColors(btnOwner, btnMahasiswa);

            // Update text with fade
            tvTitle.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvTitle.setText(getString(R.string.register_title_owner));
                tvTitle.animate().alpha(1f).setDuration(150).start();
            }).start();

            tvSub.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvSub.setText(getString(R.string.register_subtitle_owner));
                tvSub.animate().alpha(1f).setDuration(150).start();
            }).start();
        });
    }

    private void updateToggleColors(TextView active, TextView inactive) {
        active.setTextColor(ContextCompat.getColor(this, R.color.text_white));
        active.setCompoundDrawableTintList(ContextCompat.getColorStateList(this, R.color.text_white));
        
        inactive.setTextColor(ContextCompat.getColor(this, R.color.toggle_inactive_text));
        inactive.setCompoundDrawableTintList(ContextCompat.getColorStateList(this, R.color.toggle_inactive_text));
    }


    private void setupPasswordToggle() {
        EditText etPassword = findViewById(R.id.etPassword);
        ImageView btnToggle = findViewById(R.id.btnTogglePassword);

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    btnToggle.setImageResource(R.drawable.ic_visibility);
                } else {
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    btnToggle.setImageResource(R.drawable.ic_visibility_off);
                }
                // Keep cursor at end
                etPassword.setSelection(etPassword.getText().length());
            }
        });
    }

    private void setupRegisterButton() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleRegister();
            }
        });
    }

    private void handleRegister() {
        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etPassword = findViewById(R.id.etPassword);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (ValidationHelper.isEmpty(name)) {
            etName.setError("Nama tidak boleh kosong");
            return;
        }
        if (!ValidationHelper.isValidEmail(email)) {
            etEmail.setError("Email tidak valid");
            return;
        }
        if (!ValidationHelper.isValidPassword(password)) {
            etPassword.setError("Kata sandi minimal 8 karakter");
            return;
        }

        setLoading(true);
        authRepository.registerWithEmail(name, email, phone, password, selectedRole, new AuthRepository.AuthCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser result) {
                authRepository.logout(RegisterActivity.this, new AuthRepository.AuthCallback<Void>() {
                    @Override
                    public void onSuccess(Void res) {
                        setLoading(false);
                        showSuccessDialog();
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        showSuccessDialog(); // Still show success for register even if logout failed
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Pendaftaran Berhasil")
                .setMessage("Akun Anda telah dibuat. Silakan cek email untuk verifikasi sebelum masuk.")
                .setPositiveButton("Ke Login", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void setupTermsText() {
        TextView tvTerms = findViewById(R.id.tvTerms);
        int brandGreen = ContextCompat.getColor(this, R.color.brand_green);

        String fullText = "Dengan mendaftar, kamu menyetujui Ketentuan Pengguna & Kebijakan Privasi KosHub.";
        SpannableString spannable = new SpannableString(fullText);

        // "Ketentuan Pengguna" in green
        int ketentuanStart = fullText.indexOf("Ketentuan Pengguna");
        int ketentuanEnd = ketentuanStart + "Ketentuan Pengguna".length();
        spannable.setSpan(
                new ForegroundColorSpan(brandGreen),
                ketentuanStart, ketentuanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // "Kebijakan Privasi" in green
        int kebijakanStart = fullText.indexOf("Kebijakan Privasi");
        int kebijakanEnd = kebijakanStart + "Kebijakan Privasi".length();
        spannable.setSpan(
                new ForegroundColorSpan(brandGreen),
                kebijakanStart, kebijakanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvTerms.setText(spannable);
    }

    private void setupNavigation() {
        // Navigate to Login
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);
        tvGoToLogin.setOnClickListener(v -> NavigationTransitionHelper.finishWithBackTransition(RegisterActivity.this));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationTransitionHelper.finishWithBackTransition(this);
    }
}
