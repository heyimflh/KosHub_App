package com.koshub.psdku;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

/**
 * OwnerBottomNavHelper - Unified helper to manage owner bottom navigation.
 */
public class OwnerBottomNavHelper {

    public enum NavItem {
        DASHBOARD, KOS, BOOKING, CHAT, PROFILE, NONE
    }

    public static void setup(Activity activity, NavItem activeItem) {
        LinearLayout navDashboard = activity.findViewById(R.id.ownerNavDashboard);
        LinearLayout navKos = activity.findViewById(R.id.ownerNavKos);
        LinearLayout navBooking = activity.findViewById(R.id.ownerNavBooking);
        LinearLayout navChat = activity.findViewById(R.id.ownerNavChat);
        LinearLayout navProfile = activity.findViewById(R.id.ownerNavProfile);

        if (navDashboard == null) return;

        // Set click listeners
        navDashboard.setOnClickListener(v -> navigateTo(activity, OwnerDashboardActivity.class, activeItem == NavItem.DASHBOARD));
        navKos.setOnClickListener(v -> navigateTo(activity, OwnerManagementActivity.class, activeItem == NavItem.KOS));
        navBooking.setOnClickListener(v -> navigateTo(activity, OwnerBookingActivity.class, activeItem == NavItem.BOOKING));
        navChat.setOnClickListener(v -> navigateTo(activity, OwnerChatActivity.class, activeItem == NavItem.CHAT));
        navProfile.setOnClickListener(v -> navigateTo(activity, OwnerProfileSettingsActivity.class, activeItem == NavItem.PROFILE));

        // Highlight active item
        highlightItem(activity, activeItem);
    }

    private static void navigateTo(Activity activity, Class<?> targetClass, boolean isActive) {
        if (isActive) return;

        try {
            NavigationTransitionHelper.navigateMain(activity, targetClass);
        } catch (Exception e) {
            Toast.makeText(activity, "Fitur ini akan segera hadir!", Toast.LENGTH_SHORT).show();
        }
    }

    private static void highlightItem(Activity activity, NavItem activeItem) {
        // Reset all
        resetItem(activity, NavItem.DASHBOARD);
        resetItem(activity, NavItem.KOS);
        resetItem(activity, NavItem.BOOKING);
        resetItem(activity, NavItem.CHAT);
        resetItem(activity, NavItem.PROFILE);

        if (activeItem == NavItem.NONE) return;

        // Set active
        int pillId, iconId, labelId;
        switch (activeItem) {
            case DASHBOARD:
                pillId = R.id.ownerNavDashboardPill;
                iconId = R.id.ownerNavDashboardIcon;
                labelId = R.id.ownerNavDashboardLabel;
                break;
            case KOS:
                pillId = R.id.ownerNavKosPill;
                iconId = R.id.ownerNavKosIcon;
                labelId = R.id.ownerNavKosLabel;
                break;
            case BOOKING:
                pillId = R.id.ownerNavBookingPill;
                iconId = R.id.ownerNavBookingIcon;
                labelId = R.id.ownerNavBookingLabel;
                break;
            case CHAT:
                pillId = R.id.ownerNavChatPill;
                iconId = R.id.ownerNavChatIcon;
                labelId = R.id.ownerNavChatLabel;
                break;
            case PROFILE:
                pillId = R.id.ownerNavProfilePill;
                iconId = R.id.ownerNavProfileIcon;
                labelId = R.id.ownerNavProfileLabel;
                break;
            default:
                return;
        }

        FrameLayout pill = activity.findViewById(pillId);
        ImageView icon = activity.findViewById(iconId);
        TextView label = activity.findViewById(labelId);

        if (pill != null) pill.setBackgroundResource(R.drawable.bg_owner_nav_active_pill);
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(activity, R.color.brand_green));
            label.setTypeface(null, Typeface.BOLD);
        }
    }

    private static void resetItem(Activity activity, NavItem item) {
        int pillId, labelId;
        switch (item) {
            case DASHBOARD:
                pillId = R.id.ownerNavDashboardPill;
                labelId = R.id.ownerNavDashboardLabel;
                break;
            case KOS:
                pillId = R.id.ownerNavKosPill;
                labelId = R.id.ownerNavKosLabel;
                break;
            case BOOKING:
                pillId = R.id.ownerNavBookingPill;
                labelId = R.id.ownerNavBookingLabel;
                break;
            case CHAT:
                pillId = R.id.ownerNavChatPill;
                labelId = R.id.ownerNavChatLabel;
                break;
            case PROFILE:
                pillId = R.id.ownerNavProfilePill;
                labelId = R.id.ownerNavProfileLabel;
                break;
            default:
                return;
        }

        FrameLayout pill = activity.findViewById(pillId);
        TextView label = activity.findViewById(labelId);

        if (pill != null) pill.setBackground(null);
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(activity, R.color.owner_nav_inactive));
            label.setTypeface(null, Typeface.NORMAL);
        }
    }
}
