package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.koshub.psdku.repositories.AuthRepository;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authRepository = AuthRepository.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize Views
        View logo = findViewById(R.id.splash_logo);
        View title = findViewById(R.id.splash_title);
        View tagline = findViewById(R.id.splash_tagline);
        View progress = findViewById(R.id.splash_progress);

        // Load Animations
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_enter);
        Animation textAnim = AnimationUtils.loadAnimation(this, R.anim.splash_text_fade_in);

        // Run Animations with staggered offsets
        logo.startAnimation(logoAnim);
        
        // Staggered fade in for text
        new Handler().postDelayed(() -> {
            title.setVisibility(View.VISIBLE);
            title.startAnimation(textAnim);
        }, 250);

        new Handler().postDelayed(() -> {
            tagline.setVisibility(View.VISIBLE);
            tagline.startAnimation(textAnim);
        }, 400);

        // Fade in progress bar
        progress.setAlpha(0f);
        progress.animate().alpha(1f).setStartDelay(600).setDuration(400).start();

        // Navigate after 1800ms
        new Handler().postDelayed(this::checkAuthAndNavigate, 1800);
    }

    private void checkAuthAndNavigate() {
        if (authRepository.isLoggedIn()) {
            FirebaseUser user = authRepository.getCurrentUser();
            if (user != null) {
                user.reload().addOnCompleteListener(task -> {
                    if (user.isEmailVerified() || user.getProviderData().size() > 1) {
                        fetchRoleAndNavigate(user.getUid());
                    } else {
                        // Email not verified
                        authRepository.logout(this, new AuthRepository.AuthCallback<Void>() {
                            @Override public void onSuccess(Void result) { navigateToLogin(); }
                            @Override public void onError(String msg) { navigateToLogin(); }
                        });
                    }
                }).addOnFailureListener(e -> {
                    authRepository.logout(this, new AuthRepository.AuthCallback<Void>() {
                        @Override public void onSuccess(Void result) { navigateToLogin(); }
                        @Override public void onError(String msg) { navigateToLogin(); }
                    });
                });
            } else {
                navigateToLogin();
            }
        } else {
            navigateToLogin();
        }
    }

    private void fetchRoleAndNavigate(String uid) {
        authRepository.getUserRole(uid, new AuthRepository.UserRoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Intent intent;
                if (DatabaseConstants.ROLE_OWNER.equals(role)) {
                    intent = new Intent(SplashActivity.this, OwnerDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, StudentHomeActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                authRepository.logout(SplashActivity.this, new AuthRepository.AuthCallback<Void>() {
                    @Override public void onSuccess(Void result) { navigateToLogin(); }
                    @Override public void onError(String msg) { navigateToLogin(); }
                });
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        // Use fade transition
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
