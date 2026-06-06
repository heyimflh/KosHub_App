package com.koshub.psdku.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.koshub.psdku.R;

public final class SystemInsetsHelper {

    private SystemInsetsHelper() {}

    public static void applySystemBars(
            Activity activity,
            View topInsetTarget,
            View bottomInsetTarget,
            View scrollContentTarget,
            boolean lightStatusBar,
            boolean lightNavigationBar
    ) {
        if (activity == null || activity.getWindow() == null) return;

        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(lightStatusBar);
        controller.setAppearanceLightNavigationBars(lightNavigationBar);

        ViewCompat.setOnApplyWindowInsetsListener(activity.getWindow().getDecorView(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (topInsetTarget != null) {
                int initialTopPadding = getInitialPaddingTop(topInsetTarget);
                topInsetTarget.setPadding(
                        topInsetTarget.getPaddingLeft(),
                        initialTopPadding + bars.top,
                        topInsetTarget.getPaddingRight(),
                        topInsetTarget.getPaddingBottom()
                );
            }

            if (bottomInsetTarget != null) {
                int initialBottomHeight = getInitialHeight(bottomInsetTarget);
                if (initialBottomHeight > 0) {
                    ViewGroup.LayoutParams lp = bottomInsetTarget.getLayoutParams();
                    lp.height = initialBottomHeight + bars.bottom;
                    bottomInsetTarget.setLayoutParams(lp);
                    
                    // Add padding to keep content above navigation bar
                    int initialBottomPadding = getInitialPaddingBottom(bottomInsetTarget);
                    bottomInsetTarget.setPadding(
                            bottomInsetTarget.getPaddingLeft(),
                            bottomInsetTarget.getPaddingTop(),
                            bottomInsetTarget.getPaddingRight(),
                            initialBottomPadding + bars.bottom
                    );
                } else {
                    int initialBottomPadding = getInitialPaddingBottom(bottomInsetTarget);
                    bottomInsetTarget.setPadding(
                            bottomInsetTarget.getPaddingLeft(),
                            bottomInsetTarget.getPaddingTop(),
                            bottomInsetTarget.getPaddingRight(),
                            initialBottomPadding + bars.bottom
                    );
                }
            }

            if (scrollContentTarget != null) {
                int initialScrollPaddingBottom = getInitialPaddingBottom(scrollContentTarget);
                scrollContentTarget.setPadding(
                        scrollContentTarget.getPaddingLeft(),
                        scrollContentTarget.getPaddingTop(),
                        scrollContentTarget.getPaddingRight(),
                        initialScrollPaddingBottom + bars.bottom
                );
            }

            return insets;
        });
    }

    public static void applyBottomNavInsets(
            Activity activity,
            View bottomNav,
            View scrollContentTarget
    ) {
        applySystemBars(activity, null, bottomNav, scrollContentTarget, false, true);
    }

    public static void applyTopInset(
            Activity activity,
            View topTarget,
            boolean lightStatusBar
    ) {
        applySystemBars(activity, topTarget, null, null, lightStatusBar, true);
    }

    public static void applyBottomOnly(Activity activity, View bottomTarget) {
        applySystemBars(activity, null, bottomTarget, null, false, true);
    }

    private static int getInitialPaddingTop(View view) {
        Object tag = view.getTag(R.id.tag_initial_padding_top);
        if (tag instanceof Integer) return (int) tag;
        int padding = view.getPaddingTop();
        view.setTag(R.id.tag_initial_padding_top, padding);
        return padding;
    }

    private static int getInitialPaddingBottom(View view) {
        Object tag = view.getTag(R.id.tag_initial_padding_bottom);
        if (tag instanceof Integer) return (int) tag;
        int padding = view.getPaddingBottom();
        view.setTag(R.id.tag_initial_padding_bottom, padding);
        return padding;
    }

    private static int getInitialHeight(View view) {
        Object tag = view.getTag(R.id.tag_initial_height);
        if (tag instanceof Integer) return (int) tag;
        int height = view.getLayoutParams().height;
        if (height > 0) {
            view.setTag(R.id.tag_initial_height, height);
            return height;
        }
        return -1;
    }
}
