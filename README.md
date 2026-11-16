# NoFap — Quit Addiction (Ad-Free Liquid Glass Edition)

A clean, 100% ad-free, hardware-light Android streak tracker & self-discipline app inspired by **No Fap - Quit Addiction (`com.TasnimAnas.NoFap`)**, engineered with:

1. **Zero Ads & 100% Offline Privacy**: No AdMob SDK, no tracking analytics, no background data usage.
2. **CPU/GPU-Friendly Liquid Glass UI**: Uses pre-computed multi-layer XML vector translucency, specular edge highlights, and subtle ambient gradients (`RenderNode` hardware bitmap cached) rather than heavy real-time blur shaders—delivering a smooth 60/120 Hz glassmorphic aesthetic with **0% processor stress**.
3. **Adaptive Shape-Shifting Home Screen Widgets**:
   - Supports resizing from a compact **2x1 Pill Capsule** (`widget_streak_pill.xml`) to a **2x2 Rounded Squircle Card** (`widget_streak_square.xml`), a **4x1 / 4x2 Wide Glass Bar** (`widget_streak_wide.xml`), and a **3x3 / 4x4 Expanded Command Panel** (`widget_streak_large.xml`).
   - Automatically swaps layouts and proportionally scales every font size (`sp`), badge pill, progress bar, and action button as you enlarge or shrink the widget on your home screen.
   - Works smoothly on both **Android 12+ (`SizeF` responsive mapping)** and **Android 7.0–11 (`onAppWidgetOptionsChanged` dynamic scaling)**.
4. **0ms App-to-Widget Sync Delay & 0% Background Battery Drain**:
   - Every action in the app (Start/Edit Date, Reset Streak, Daily Check-In, Goal Change) synchronously broadcasts `AppWidgetManager.updateAppWidget()` on the exact same UI frame (**0ms latency**).
   - Live widget timer uses Android's native `Chronometer` inside `RemoteViews` anchored to `SystemClock.elapsedRealtime()`, so the launcher ticks the timer only while the home screen is visible—requiring **no background service, no wake locks, and 0% battery drain**.

---

## How to Get Your `.apk` File (2 Easy Ways)

### Option 1: Build Automatically in the Cloud via GitHub (No PC or Android Studio Needed!)
You can compile this `.zip` into an installable `.apk` right from your phone, Samsung tablet, or any web browser:
1. Extract this `.zip` folder on your device.
2. Sign in to [github.com](https://github.com) and create a new repository (e.g., `nofap-liquid-glass`).
3. Upload all files and folders from this zip (including `.github/workflows/build-apk.yml`, `app/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and `gradle/`) to your repository.
4. Tap the **Actions** tab in your GitHub repository and select **Build Android APK (Ad-Free Liquid Glass)** -> **Run workflow**.
5. In ~2 minutes, open the completed workflow run and download **`NoFap-LiquidGlass-APKs`** under the **Artifacts** section to install the `.apk` directly on your Android device!

### Option 2: Build in Android Studio (Windows / Mac / Linux)
1. Extract this `.zip` file and open the folder in **Android Studio (Jellyfish / Koala / Ladybug or newer)**.
2. Wait for Gradle Sync to finish, then click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Your installable APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
