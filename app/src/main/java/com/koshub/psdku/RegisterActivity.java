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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private String selectedRole = "mahasiswa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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
            if (selectedRole.equals("mahasiswa")) return;
            selectedRole = "mahasiswa";

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
            if (selectedRole.equals("owner")) return;
            selectedRole = "owner";

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
        TextView btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Scale animation on press
                view.animate()
                        .scaleX(0.975f)
                        .scaleY(0.975f)
                        .setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100)
                                        .start();
                            }
                        })
                        .start();

                // TODO: Implement actual register logic
                EditText etEmail = findViewById(R.id.etEmail);
                EditText etPassword = findViewById(R.id.etPassword);
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (!email.isEmpty() && !password.isEmpty()) {
                    // Proceed with registration
                }
            }
        });
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
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }
}
