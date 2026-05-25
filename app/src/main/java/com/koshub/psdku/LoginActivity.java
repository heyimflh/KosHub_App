package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.koshub.psdku.repositories.AuthRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.SessionManager;
import com.koshub.psdku.utils.ValidationHelper;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private String selectedRole = DatabaseConstants.ROLE_STUDENT;
    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    
    private ProgressBar progressBar;
    private View btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = AuthRepository.getInstance();
        sessionManager = new SessionManager(this);
        progressBar = findViewById(R.id.progressBar);
        btnLogin = findViewById(R.id.btnLogin);

        setupGoogleSignIn();

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
        setupLoginButton();
        setupNavigation();
    }

    private void setupRoleToggle() {
        TextView btnMahasiswa = findViewById(R.id.btnRoleMahasiswa);
        TextView btnOwner = findViewById(R.id.btnRoleOwner);
        View indicator = findViewById(R.id.viewRoleIndicator);
        TextView tvTitle = findViewById(R.id.tvGreetingTitle);
        TextView tvSub = findViewById(R.id.tvGreetingSub);

        // Initial setup for indicator width
        indicator.post(() -> {
            ViewGroup.LayoutParams params = indicator.getLayoutParams();
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
                tvTitle.setText(getString(R.string.login_title_mahasiswa));
                tvTitle.animate().alpha(1f).setDuration(150).start();
            }).start();

            tvSub.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvSub.setText(getString(R.string.login_subtitle_mahasiswa));
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
                tvTitle.setText(getString(R.string.login_title_owner));
                tvTitle.animate().alpha(1f).setDuration(150).start();
            }).start();

            tvSub.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvSub.setText(getString(R.string.login_subtitle_owner));
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

    private void setupLoginButton() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleLogin();
            }
        });

        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void handleLogin() {
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!ValidationHelper.isValidEmail(email)) {
            etEmail.setError("Email tidak valid");
            return;
        }
        if (ValidationHelper.isEmpty(password)) {
            etPassword.setError("Kata sandi tidak boleh kosong");
            return;
        }

        setLoading(true);
        authRepository.loginWithEmail(email, password, new AuthRepository.AuthCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (user.isEmailVerified()) {
                    fetchUserRoleAndRedirect(user);
                } else {
                    setLoading(false);
                    showVerificationDialog();
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        setLoading(true);
        authRepository.loginWithGoogle(acct, selectedRole, new AuthRepository.AuthCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser user) {
                fetchUserRoleAndRedirect(user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchUserRoleAndRedirect(FirebaseUser user) {
        authRepository.getUserRole(user.getUid(), new AuthRepository.UserRoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                setLoading(false);
                sessionManager.createLoginSession(user.getUid(), user.getDisplayName(), user.getEmail(), role, 
                    user.getProviderData().size() > 1 ? user.getProviderData().get(1).getProviderId() : "email");
                
                redirectByRole(role);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                authRepository.logout(LoginActivity.this, null);
                Toast.makeText(LoginActivity.this, "Gagal mengambil data user: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void redirectByRole(String role) {
        Intent intent;
        if (DatabaseConstants.ROLE_OWNER.equals(role)) {
            intent = new Intent(this, OwnerDashboardActivity.class);
        } else {
            intent = new Intent(this, StudentHomeActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showVerificationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Email Belum Diverifikasi")
                .setMessage("Silakan verifikasi email Anda terlebih dahulu. Belum menerima email?")
                .setPositiveButton("Kirim Ulang", (dialog, which) -> resendVerification())
                .setNegativeButton("Batal", (dialog, which) -> authRepository.logout(this, null))
                .setCancelable(false)
                .show();
    }

    private void resendVerification() {
        authRepository.resendEmailVerification(new AuthRepository.AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                authRepository.logout(LoginActivity.this, new AuthRepository.AuthCallback<Void>() {
                    @Override
                    public void onSuccess(Void res) {
                        Toast.makeText(LoginActivity.this, "Email verifikasi telah dikirim ulang.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(LoginActivity.this, "Email dikirim, silakan login kembali.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, "Gagal mengirim email: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        EditText etResetEmail = new EditText(this);
        etResetEmail.setHint("Masukkan email Anda");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = padding;
        params.rightMargin = padding;
        etResetEmail.setLayoutParams(params);
        container.addView(etResetEmail);

        new AlertDialog.Builder(this)
                .setTitle("Lupa Kata Sandi")
                .setView(container)
                .setPositiveButton("Kirim Link Reset", (dialog, which) -> {
                    String email = etResetEmail.getText().toString().trim();
                    if (ValidationHelper.isValidEmail(email)) {
                        sendPasswordReset(email);
                    } else {
                        Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void sendPasswordReset(String email) {
        authRepository.sendPasswordReset(email, new AuthRepository.AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(LoginActivity.this, "Link reset password telah dikirim ke email Anda.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, "Gagal: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void setupNavigation() {
        // Navigate to Register
        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvGoToRegister.setOnClickListener(v -> NavigationTransitionHelper.navigateDetail(LoginActivity.this, RegisterActivity.class));
    }
}
