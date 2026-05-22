package com.koshub.psdku;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

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

        // Navigate to LoginActivity after 1800ms
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            // Use fade transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 1800);
    }
}
