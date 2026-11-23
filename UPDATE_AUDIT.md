# DaveBoostz paired project update

## Changes made in this pass

### Android
- Replaced the opposing full-height left/right overlay panels with one asymmetrical curved floating command island.
- Kept the compact telemetry pill and draggable launcher bubble.
- Added a short vector lightning convergence sequence and reveal of the existing DaveBoostz emblem at startup.
- Reused the admin project's existing emblem image as the Android drawable and corrected manifest launcher icon references to that resource (the original ZIP referenced a missing `@mipmap/ic_launcher`).
- Added a 60-second published-config refresh loop while the Android process is alive and a refresh on Activity resume.

### Admin web app
- Added a restrained dark/red motion and glass-surface foundation with reduced-motion support.
- Added Supabase Realtime listeners for draft changes, session inserts, and error-report inserts. Draft updates are only applied automatically when the current browser has no unsaved edits.
- Added idempotent SQL to include config, sessions, and error reports in the `supabase_realtime` publication when it exists.

## Findings requiring device/cloud verification
- The Supabase MCP connection returned no projects in the connected account, so no live database schema, RLS, credentials, Storage bucket, or Realtime publication could be verified or changed remotely.
- Android config retrieval currently uses REST polling, not a native Realtime websocket subscription. The 60-second refresh improves propagation but is not instantaneous; it only runs while the app process is alive.
- FPS is only displayed as a measured value when the Shizuku service is active and returns parseable SurfaceFlinger data. It is not guaranteed on every Android build/device.
- CPU frequency reads `/sys/devices/system/cpu/...`; many retail Android builds deny access, in which case readings can be zero/unavailable.
- The boost engine uses Android background-process APIs or allowlisted Shizuku actions. Android may restrict process management; memory reclaimed is measured as a before/after system delta and cannot be promised as a fixed amount.
- The overlay, Shizuku, game-mode, recorder, VPN, and notification features require real-device permission and compatibility tests.

## Verification performed
- ZIP extraction and source-tree inspection completed for both uploaded projects.
- Kotlin parser pass found no syntax/parser errors in the two newly written Compose files. Full Kotlin/Android type compilation was not possible because this runtime has no Gradle executable or Gradle wrapper in the supplied project.
- Admin TypeScript build/tests could not run: dependency installation timed out and required local packages (`vite/client`, `tsx`) are unavailable. Global `tsc` therefore stopped before project type-checking.
- No Firebase references were found in the extracted source trees outside package-lock scanning; the projects already contain Supabase code.

## Before production
1. Connect/select the correct Supabase project in ChatGPT so schema and live synchronization can be verified.
2. Apply the updated `admin/supabase/schema.sql` to that project after reviewing it.
3. Configure `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, `SUPABASE_URL`, and `SUPABASE_ANON_KEY` as appropriate; use publishable/anon keys only in client apps and never expose service-role/secret keys.
4. Run the admin `npm ci`, tests, lint, and production build in an environment with package access.
5. Run Android debug/release builds and test overlay touch pass-through, startup sequence, and Shizuku-dependent controls on target devices.
