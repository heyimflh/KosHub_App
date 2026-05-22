# Implementation Plan - Improved Owner Profile UI

Upgrade the Owner Profile & Settings page to a professional, feature-rich design that aligns with the premium look of the KosHub app.

## Proposed Changes

### Layouts

#### [activity_owner_profile_settings.xml](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/res/layout/activity_owner_profile_settings.xml)
- **Header Overhaul**:
    - Enhanced gradient background (`bg_owner_header_gradient`).
    - Circular avatar with border.
    - Added "Edit Profile" action button.
    - Added Verification Badge ("Mitra Terverifikasi").
- **Profile Completion Card**:
    - Added a card showing 80% completion with a progress bar and CTA to "Lengkapi Dokumen Legal".
- **Stats Grid (2x2)**:
    - Detailed cards for:
        - `Total Kos` (using `ic_owner_building`)
        - `Hunian` (using `ic_owner_room`)
        - `Booking` (using `ic_owner_booking`)
        - `Pendapatan` (using `ic_owner_wallet`)
- **Categorized Menu List**:
    - Sectioned groups with titles (e.g., "Informasi Bisnis", "Pengaturan Akun").
    - Menu items with Icons, Titles, and descriptive Subtexts.
    - Consistent padding and dividers.
- **Improved Logout**:
    - High-visibility logout button with a soft red background.

### Java Classes

#### [OwnerProfileSettingsActivity.java](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/java/com/koshub/psdku/OwnerProfileSettingsActivity.java)
- Update `initViews()` to find new menu items (Legalitas, Payments, etc.).
- Update `setupListeners()` to handle clicks for the new menu categories.
- Ensure the bottom navigation remains functional and active on the Profile tab.

## Verification Plan

### Manual Verification
- Log in as Owner and navigate to the Profile tab.
- Verify the header looks premium and contains all information.
- Check the Stats Grid for alignment and readability.
- Tap through every menu item to ensure Toast feedback is triggered.
- Verify the "Edit Profile" and "Logout" buttons work as expected.
