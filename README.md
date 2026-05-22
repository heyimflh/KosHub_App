# KosHub SuperApp

KosHub is an Android application for finding and managing kos properties near campuses.

## Setup Mapbox Token

This project uses Mapbox SDK v11 for maps. To keep the project safe for GitHub, the real access token is not committed to the repository.

To enable maps locally:

1. Create a [Mapbox Public Access Token](https://account.mapbox.com/) from your Mapbox dashboard.
2. In the project folder, go to `app/src/main/res/values/`.
3. Copy `developer-config.example.xml` and rename the copy to `developer-config.xml`.
4. Open `developer-config.xml` and replace `YOUR_MAPBOX_PUBLIC_TOKEN_HERE` with your actual token (starts with `pk.`).
5. Rebuild the project.

The `developer-config.xml` file is already in `.gitignore` and will not be pushed to GitHub.

## Force Close Prevention

The app includes safety mechanisms to prevent crashes if the Mapbox token is missing:
- `MapboxTokenHelper` validates the token before initialization.
- `MapView` is initialized programmatically only if a valid token is found.
- Fallback UI is displayed if the map cannot be loaded.
- All UI updates from Mapbox callbacks are wrapped in `runOnUiThread`.
- Intent and RecyclerView data are strictly validated for null values.
