# Refactoring Property Detail Booking Activity

Improve the `PropertyDetailBookingActivity` with dynamic data, Mapbox integration, layout refinements, and status bar compatibility.

## User Review Required

> [!IMPORTANT]
> - **Mapbox Integration**: I am adding the Mapbox dependency. This requires specific repository configurations in `settings.gradle.kts` and a secret token in `gradle.properties`.
> - **API Key**: The user provided a Mapbox API key, which will be added to `strings.xml`.

## Proposed Changes

### Build Configuration

#### [gradle.properties](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/gradle.properties)
- Add Mapbox secret token (using the same public token for now as a placeholder for download access if required, though usually, a separate secret token is needed for downloads. I'll use the provided one for the map display).

#### [settings.gradle.kts](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/settings.gradle.kts)
- Add Mapbox Maven repository.

#### [build.gradle.kts](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/build.gradle.kts)
- Add Mapbox SDK dependency: `implementation("com.mapbox.maps:android:11.10.0")`.

---

### UI Resources

#### [strings.xml](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/res/values/strings.xml)
- Add Mapbox access token.

#### [include_property_top_nav.xml](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/res/layout/include_property_top_nav.xml)
- Adjust height and add top padding/margin to handle status bar.

#### [include_property_amenities.xml](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/res/layout/include_property_amenities.xml)
- Redesign amenities section to be more aesthetic (e.g., using a Flexbox-like flow or a cleaner grid).

#### [include_property_location.xml](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/res/layout/include_property_location.xml)
- Replace `ImageView` with Mapbox `MapView`.

---

### Logic and Data

#### [KosItem.java](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/java/com/koshub/psdku/KosItem.java)
- Implement `Serializable` or `Parcelable` to allow passing the whole object to the detail activity.

#### [StudentHomeActivity.java](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/java/com/koshub/psdku/StudentHomeActivity.java)
- Pass the `KosItem` object via `Intent`.

#### [PropertyDetailBookingActivity.java](file:///C:/Users/mufal/AndroidStudioProjects/KosHub/app/src/main/java/com/koshub/psdku/PropertyDetailBookingActivity.java)
- Receive the `KosItem` and populate all UI fields dynamically.
- Implement price formatting (Jt for millions).
- Provide unique dummy reviews per kos item.
- Initialize Mapbox `MapView`.

---

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to ensure build success.

### Manual Verification
- Click on different kos cards (e.g., Kos 01, Kos 03) and verify:
    - Images and names match the clicked card.
    - Prices are formatted correctly (e.g., "1.2jt").
    - Amenities are displayed cleanly.
    - Mapbox Map loads correctly at the location section.
    - Reviews are unique and look "human-like".
    - The top navbar does not overlap with the status bar icons.
