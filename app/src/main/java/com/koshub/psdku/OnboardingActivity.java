package com.koshub.psdku;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.koshub.psdku.adapters.OnboardingAdapter;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private View btnSkip, btnNext;
    private View layoutFinalButtons;
    private OnboardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupViewPager();
        setupDots(adapter.getItemCount());
        setCurrentDot(0);
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        layoutDots = findViewById(R.id.layoutDots);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);
        layoutFinalButtons = findViewById(R.id.layoutFinalButtons);
    }

    private void setupViewPager() {
        List<OnboardingAdapter.OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingAdapter.OnboardingItem(
                getResourceId("ic_onboarding_1", R.drawable.ic_logo_koshub),
                "Temukan Kos Impianmu",
                "Cari ribuan hunian kos terbaik dengan fasilitas lengkap di sekitar kampus tujuanmu."
        ));
        items.add(new OnboardingAdapter.OnboardingItem(
                getResourceId("ic_onboarding_2", R.drawable.ic_logo_koshub),
                "Cari Berdasarkan Lokasi",
                "Tentukan lokasi kos yang paling strategis dan dekat dengan fakultas atau area kampusmu."
        ));
        items.add(new OnboardingAdapter.OnboardingItem(
                getResourceId("ic_onboarding_3", R.drawable.ic_logo_koshub),
                "Chat Langsung Pemilik",
                "Komunikasi jadi lebih mudah dan cepat melalui fitur chat langsung dengan pemilik kos."
        ));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);
    }

    private void setupListeners() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentDot(position);
                handleNavigationVisibility(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(current + 1);
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding(false));
        findViewById(R.id.btnRegisterFinal).setOnClickListener(v -> finishOnboarding(true));
        findViewById(R.id.btnLoginFinal).setOnClickListener(v -> finishOnboarding(false));
    }

    private void handleNavigationVisibility(int position) {
        if (position == adapter.getItemCount() - 1) {
            // Slide Terakhir
            btnSkip.setVisibility(View.GONE);
            btnNext.setVisibility(View.GONE);
            
            if (layoutFinalButtons.getVisibility() == View.GONE) {
                layoutFinalButtons.setVisibility(View.VISIBLE);
                layoutFinalButtons.setAlpha(0f);
                layoutFinalButtons.animate().alpha(1f).setDuration(300).start();
            }
        } else {
            // Slide 1 & 2
            btnSkip.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.VISIBLE);
            layoutFinalButtons.setVisibility(View.GONE);
        }
    }

    private void setupDots(int count) {
        layoutDots.removeAllViews();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            dot.setLayoutParams(params);
            layoutDots.addView(dot);
        }
    }

    private void setCurrentDot(int index) {
        int childCount = layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutDots.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_active));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            }
        }
    }

    private int getResourceId(String name, int fallback) {
        int resId = getResources().getIdentifier(name, "drawable", getPackageName());
        return resId != 0 ? resId : fallback;
    }

    private void finishOnboarding(boolean toRegister) {
        SharedPreferences sharedPreferences = getSharedPreferences("KosHubPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("onboarding_done", true).apply();

        Intent intent;
        if (toRegister) {
            intent = new Intent(this, RegisterActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
