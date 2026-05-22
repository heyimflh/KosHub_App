package com.koshub.psdku;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class NavigationHelper {

    public static List<KosItem> cachedKosList = new ArrayList<>();

    public enum Tab {
        HOME, MAP, WAITLIST, PROFILE
    }

    public static void setupBottomNav(final Activity activity, Tab activeTab) {
        View bottomNav = activity.findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        LinearLayout navHome = bottomNav.findViewById(R.id.navHome);
        LinearLayout navMap = bottomNav.findViewById(R.id.navMap);
        LinearLayout navWaitlist = bottomNav.findViewById(R.id.navWaitlist);
        LinearLayout navProfile = bottomNav.findViewById(R.id.navProfile);

        // Reset all
        resetTab(activity, navHome, R.id.navHomePill, R.id.navHomeIcon, R.id.navHomeLabel);
        resetTab(activity, navMap, R.id.navMapPill, R.id.navMapIcon, R.id.navMapLabel);
        resetTab(activity, navWaitlist, R.id.navWaitlistPill, R.id.navWaitlistIcon, R.id.navWaitlistLabel);
        resetTab(activity, navProfile, R.id.navProfilePill, R.id.navProfileIcon, R.id.navProfileLabel);

        // Set active
        switch (activeTab) {
            case HOME:
                setActiveTab(activity, navHome, R.id.navHomePill, R.id.navHomeIcon, R.id.navHomeLabel);
                break;
            case MAP:
                setActiveTab(activity, navMap, R.id.navMapPill, R.id.navMapIcon, R.id.navMapLabel);
                break;
            case WAITLIST:
                setActiveTab(activity, navWaitlist, R.id.navWaitlistPill, R.id.navWaitlistIcon, R.id.navWaitlistLabel);
                break;
            case PROFILE:
                setActiveTab(activity, navProfile, R.id.navProfilePill, R.id.navProfileIcon, R.id.navProfileLabel);
                break;
        }

        // Listeners
        navHome.setOnClickListener(v -> {
            if (activeTab != Tab.HOME) {
                NavigationTransitionHelper.navigateMain(activity, StudentHomeActivity.class);
            }
        });

        navMap.setOnClickListener(v -> {
            if (activeTab != Tab.MAP) {
                Intent intent = new Intent(activity, MapViewRouteNavigationActivity.class);
                if (cachedKosList != null && !cachedKosList.isEmpty()) {
                    intent.putExtra("kos_list", new ArrayList<>(cachedKosList));
                } else if (activity instanceof StudentHomeActivity) {
                    // Try to fallback if it's home activity
                    List<KosItem> list = ((StudentHomeActivity) activity).getAllKosList();
                    if (list != null) {
                        intent.putExtra("kos_list", new ArrayList<>(list));
                    }
                }
                NavigationTransitionHelper.navigateMainWithIntent(activity, intent);
            }
        });

        navWaitlist.setOnClickListener(v -> {
            if (activeTab != Tab.WAITLIST) {
                NavigationTransitionHelper.navigateMain(activity, WaitingListQueueActivity.class);
            }
        });

        navProfile.setOnClickListener(v -> {
            if (activeTab != Tab.PROFILE) {
                NavigationTransitionHelper.navigateMain(activity, ProfileHistoryActivity.class);
            }
        });
    }

    private static void resetTab(Activity activity, View root, int pillId, int iconId, int labelId) {
        FrameLayout pill = root.findViewById(pillId);
        ImageView icon = root.findViewById(iconId);
        TextView label = root.findViewById(labelId);

        pill.setBackground(null);
        pill.setPadding(0, 0, 0, 0);
        label.setTextColor(ContextCompat.getColor(activity, R.color.home_text_muted));
    }

    private static void setActiveTab(Activity activity, View root, int pillId, int iconId, int labelId) {
        FrameLayout pill = root.findViewById(pillId);
        ImageView icon = root.findViewById(iconId);
        TextView label = root.findViewById(labelId);

        pill.setBackgroundResource(R.drawable.bg_nav_active_pill);
        int ph = activity.getResources().getDimensionPixelSize(R.dimen.home_nav_active_pill_h);
        int pv = activity.getResources().getDimensionPixelSize(R.dimen.home_nav_active_pill_v);
        pill.setPadding(ph, pv, ph, pv);
        
        label.setTextColor(ContextCompat.getColor(activity, R.color.brand_green));
    }
}
