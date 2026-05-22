package com.koshub.psdku;

import android.app.Activity;
import android.content.Intent;

/**
 * NavigationTransitionHelper - Global helper to manage smooth page transitions.
 */
public class NavigationTransitionHelper {

    /**
     * Navigates to a main tab activity with a smooth fade transition.
     * Use this for bottom navigation.
     */
    public static void navigateMain(Activity current, Class<?> target) {
        if (current.getClass().equals(target)) return;

        Intent intent = new Intent(current, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        current.startActivity(intent);
        current.overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
    }

    /**
     * Navigates to a detail/derived page with a slide-in transition.
     */
    public static void navigateDetail(Activity current, Class<?> target) {
        Intent intent = new Intent(current, target);
        current.startActivity(intent);
        current.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Navigates to a detail page with a slide-in transition, supporting Intent extras.
     */
    public static void navigateDetailWithIntent(Activity current, Intent intent) {
        current.startActivity(intent);
        current.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Finishes the current activity with a reverse slide-out transition.
     * Use this for back navigation from detail pages.
     */
    public static void finishWithBackTransition(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
