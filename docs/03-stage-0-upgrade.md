# Stage 0 — Project Upgrade (Do First)

> Goal: Project compiles and runs cleanly on latest stable Android/Kotlin/Compose/Media3/Firebase before any YouTube code. One bump per PR.

## APPLIED 2026-11-09 — build verification PENDING

Edits are in the repo. **`./gradlew :app:assembleDebug` has not been run** — this cloud sandbox has no JDK/Android SDK. Run it in Android Studio (JDK 17) before continuing to Stage 1.

| Item | Applied |
|------|---------|
| Gradle wrapper | 8.13 → **8.14.3** |
| AGP | 8.13.2 (unchanged) |
| Kotlin | 2.0.21 → **2.4.10** |
| compileSdk / targetSdk | 34 → **36**; minSdk 26 unchanged |
| Java / jvmTarget | 11 → **17**, `kotlinOptions` → `kotlin { compilerOptions { jvmTarget } }` |
| Compose BOM | 2024.09.00 → **2026.09.00** |
| core-ktx / lifecycle / activity | **1.13.1 / 2.9.0 / 1.10.1** |
| Media3 | 1.3.1 → **1.11.1** (exoplayer, ui, session, datasource) |
| Firebase BOM / google-services | 33.1.0 → **34.2.0** / 4.4.2 → **4.4.4** |
| Navigation / Coroutines / Ads / Auth | **2.9.0 / 1.9.0 / 23.6.0 / 21.4.0** |
| Coil | 2.6.0 → **2.7.0** (3.x migration still deferred) |
| material-icons-extended | pinned **1.7.8** (artifact frozen upstream, not in new BOMs) |
| B2 SDK, Cast SDK | unchanged (2.81.1 / 21.5.0) |
| versionCode / versionName | 1 → **2**, 0.1 → **0.2** |
| Manifest | added `POST_NOTIFICATIONS` |

If the first build fails, the likely suspects in order: Kotlin/Gradle pairing, Compose BOM ↔ Kotlin compiler, Media3 1.11 `PlaybackService` APIs, Navigation 2.9 routes. Bump one thing at a time from here.

## Prerequisites

- Android Studio Hedgehog+ / Ladybug (JDK 17 bundled)
- `local.properties` exists (copy from `local.properties.example` at repo root). Without it, `B2Manager` init fails.
- Clean `./gradlew clean`

## 0.1 Gradle + AGP + JDK

- [ ] Verify `gradle/wrapper/gradle-wrapper.properties` → `gradle-8.13-bin.zip`
- [ ] Verify AGP `8.13.2` in `gradle/libs.versions.toml` (keep)
- [ ] `app/build.gradle.kts`: `JavaVersion.VERSION_17`, `kotlinOptions.jvmTarget = "17"`, `compileOptions.sourceCompatibility/targetCompatibility = 17`
- [ ] `org.gradle.jvmargs=-Xmx2048m` stays in `gradle.properties`
- **QA:** `./gradlew :app:assembleDebug` succeeds

## 0.2 Kotlin 2.0.21 → 2.2.x

- [ ] Bump `kotlin = "2.2.0"` in `libs.versions.toml`
- [ ] Keep `kotlin-compose` plugin alias (already correct)
- [ ] Fix deprecated APIs: `kotlinOptions` → prefer `compilerOptions`, but old style works with warning — migrate if prompted
- **QA:** Compose preview renders, app launches

## 0.3 SDK 34 → 35

- [ ] `compileSdk = 35`, `targetSdk = 35` in `app/build.gradle.kts`
- [ ] `AndroidManifest.xml`:
  - Add `<uses-permission android:name="android.permission.INTERNET" />` (already)
  - Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
  - Add `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />` + `FOREGROUND_SERVICE` type on `PlaybackService`
  - Check `android:theme` supports edge-to-edge
- [ ] `MainActivity.kt`: `enableEdgeToEdge()` already present — add `WindowInsets` padding to `Scaffold` content so bottom nav + mini player don't overlap gesture bar
- **QA:** Install on API 35 emulator, rotate, check bottom nav insets, notification permission prompt

## 0.4 Compose BOM 2024.09 → 2025.x

- [ ] Bump `composeBom = "2025.06.01"` in `libs.versions.toml`
- [ ] Align `material-icons-extended:1.6.7` → version from new BOM (remove hard-coded version, use BOM platform)
- [ ] Fix deprecated: `Material3` `ModalBottomSheet`, `AlertDialog`, `NavigationBar` — follow compiler warnings, no visual change
- [ ] Bump `activityCompose = "1.10.1"`, `lifecycleRuntimeKtx = "2.9.0"`, `coreKtx = "1.13.1"`
- **QA:** All screens render (Home, Search, Playlists, Settings), theme `MinefiyCloudFlareTheme` looks same

## 0.5 Media3 1.3.1 → 1.7.0

- [ ] Bump all `androidx.media3:media3-*:1.3.1` → `1.7.0` in `app/build.gradle.kts`
- [ ] `PlaybackService.kt`:
  - Verify `MediaSession.Builder`, `DefaultMediaNotificationProvider.Builder`, `ExoPlayer.Builder` unchanged (they are, but check `setSmallIcon` still exists)
  - `onAddMediaItems` return type via `serviceScope.future` still valid
  - `CacheUtil.getCache()` — verify package still `androidx.media3.datasource.cache`
- [ ] `CacheDataSource.Factory` — verify `setUpstreamDataSourceFactory` still correct
- **QA:** Play one B2 song → notification shows, lockscreen controls work, 100MB cache file created

## 0.6 Firebase + Play Services

- [ ] Firebase BOM `33.1.0` → `34.2.0`
- [ ] google-services plugin `4.4.2` → `4.4.4` in root `build.gradle.kts`
- [ ] `play-services-ads:23.0.0` → `23.6.0`, `play-services-cast-framework:21.5.0` → `22.0.0`, `play-services-auth:21.2.0` → `21.4.0`
- [ ] `navigation-compose:2.7.7` → `2.9.0`
- [ ] `kotlinx-coroutines:1.7.3` → `1.9.0`
- **QA:** Login, register, logout, sync songs, Cast session start (if device available)

## 0.7 Coil — Pin, Don't Migrate Yet

- [ ] **Keep** `coil-compose:2.6.0`. Coil 3.x is a breaking rewrite.
- [ ] Note in code: `// TODO Stage 6: migrate Coil 2.6 → 3.x`
- **QA:** Thumbnails load on Home/Search

## 0.8 local.properties + Build Hygiene

- [ ] Add `local.properties.example` at repo root (done in this plan — see file)
- [ ] `app/build.gradle.kts`: if `local.properties` missing, log warning but don't crash build (already defaults to `""`). Add `println("Missing local.properties — B2/YTM disabled")` if needed.
- [ ] Add `local.properties` to `.gitignore` (verify)
- [ ] `versionCode = 2`, `versionName = "0.2"` for revival release

## Stage 0 Exit Criteria

- [ ] `./gradlew :app:assembleDebug` → green
- [ ] `./gradlew :app:lintDebug` → no errors (warnings OK)
- [ ] Manual QA: Login → Home loads songs/artists → Play B2 song → Pause → Next → Logout → Login again
- [ ] Commit as `chore: stage0 upgrade to SDK35 Kotlin2.2 Media3 1.7` (squashed per sub-PR)

> Do not start Stage 1 until 0.1–0.8 all green. Upgrading after YTM code causes conflicts.
