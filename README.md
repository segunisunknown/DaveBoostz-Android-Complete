# DaveBoostz Android App

**Tagline:** Precision Android Gaming Control

## Supabase Remote Sync Setup

- **Android Application ID:** `com.daveboostz.app`
- **Transport:** Supabase PostgREST HTTPS API via `OkHttp 4.12.0` + `kotlinx.serialization 1.6.3` (`RemoteConfigRepository.kt`), backed by DataStore offline caching.
- **Security Boundary:** Android builds accept **only** the public `SUPABASE_URL` and `SUPABASE_ANON_KEY`. Never embed a Supabase `service_role` key or database password in the Android project.

### Local Android Studio Builds
1. Optionally copy `gradle.properties.example` values (`SUPABASE_URL`, `SUPABASE_ANON_KEY`) into your local `gradle.properties` (or `~/.gradle/gradle.properties`) so they stay out of version control.
2. Offline builds compile and run with safe factory defaults even if Supabase properties are unset.
3. Sync Gradle and run on Android API 29+.

### GitHub Actions CI (`SUPABASE_URL` / `SUPABASE_ANON_KEY` Secrets)
1. In your `DaveBoostz-Android` GitHub repository, go to **Settings → Secrets and variables → Actions → New repository secret**.
2. Add optional secrets **`SUPABASE_URL`** and **`SUPABASE_ANON_KEY`**.
3. `.github/workflows/android-ci.yml` injects them via `ORG_GRADLE_PROJECT_SUPABASE_*` during Gradle tests and APK builds without exposing values in build logs.

## Permissions & Overlay System

- `SYSTEM_ALERT_WINDOW`: Checked via `Settings.canDrawOverlays(context)` and requested via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`: Runs `OverlayForegroundService` with Stop and Open notification actions.
- Single `WindowManager` window (`TYPE_APPLICATION_OVERLAY`) renders the draggable bubble, side Game Space panels, Sub-Overlay Mini HUD, and Crosshair Studio reticle using Jetpack Compose (`ComposeView` + `OverlayLifecycleOwner`).
- `Shizuku 13.1.5`: Uses the documented `Shizuku.bindUserService` (`IShizukuUserService.aidl`) pattern; never uses `Shizuku.newProcess` or reflection.

## Alternate Launcher Icons

Android launcher icons and labels are compiled into the APK. Pre-bundled `<activity-alias>` entries (`DefaultRed`, `CyberNeon`, `CarbonBlack`, `GoldEdition`) are declared in `AndroidManifest.xml`.
