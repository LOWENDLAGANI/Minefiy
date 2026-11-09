# 01 — Upgrade Audit (Old Project → Latest Stable)

Audited 2026-11-09 against `app/build.gradle.kts`, `gradle/libs.versions.toml`, `build.gradle.kts` (root).

## Current Versions

| Component | Current | File | Risk |
|-----------|---------|------|------|
| compileSdk / targetSdk | 34 | `app/build.gradle.kts` | Play Store requires 35+ (Android 15). Edge-to-edge enforcement changes. |
| minSdk | 26 | same | OK, but raising to 28-30 helps Cast/Media3 edge cases. Keep 26 if unsure. |
| Java source/target | 11 | same | Media3 1.4+ & AGP 8.5+ expect JDK 17 toolchain. |
| AGP | 8.13.2 | `libs.versions.toml` | ✅ Recent. Needs Gradle 8.13+, JDK 17. Verify wrapper matches. |
| Kotlin | 2.0.21 | same | Behind. 2.0 → 2.2.x has Compose compiler migration. |
| Compose BOM | 2024.09.00 | same | Old. Material3, activity-compose, icons APIs changed since. |
| lifecycle | 2.6.1 | same | Behind 2.8+. |
| activity-compose | 1.8.0 | same | Behind 1.9+. `MainActivity` uses `enableEdgeToEdge()` — verify. |
| core-ktx | 1.10.1 | same | Behind 1.13+. |
| Firebase BOM | 33.1.0 | `app/build.gradle.kts` | Behind 34.x. Auth/Firestore APIs stable, but `google-services` plugin 4.4.2 old. |
| Media3 | 1.3.1 | same | **Major gap.** 1.3→1.4+ changed `MediaSession`, `DefaultMediaNotificationProvider`, caching. Must upgrade before playback work. |
| Coil | 2.6.0 | same | Coil 3.x is rewrite (new artifacts `io.coil-kt.coil3`). Either stay on 2.6 or migrate fully. |
| Navigation Compose | 2.7.7 | same | Behind 2.9+. `navArgument`, graph APIs stable. |
| Coroutines | 1.7.3 | same | Behind 1.9+. Bugfixes for `flow` + `guava` bridge. |
| Play Services Ads | 23.0.0 | same | Behind 24+. |
| Cast Framework | 21.5.0 | same | Behind 22+. |
| Play Services Auth | 21.2.0 | same | Behind 22+. |
| aws-android-sdk-s3 (B2) | 2.81.1 | same | Amazon SDK v2 Android is legacy. Works, but consider AWS SDK v2 Kotlin or S3 presigned via backend to avoid leaking B2 keys. **Security note:** B2 keys currently baked into `BuildConfig` — visible in APK. Plan keeps this for compat but flags migration. |
| google-services plugin | 4.4.2 | root `build.gradle.kts` | Behind 4.4.4+. |
| Material icons extended | 1.6.7 | `app/build.gradle.kts` | Mismatched with Compose BOM 2024.09. Align to BOM. |

## What Breaks on Upgrade (Watch List)

1. **Kotlin 2.0 → 2.1/2.2:** Compose compiler moves to `org.jetbrains.kotlin.plugin.compose` (already used). Verify `kotlinOptions.jvmTarget` → `JavaVersion.VERSION_17` and AGP toolchain.
2. **Media3 1.3 → 1.8+:** `MediaSession.Builder`, `onAddMediaItems` signature, `DefaultMediaNotificationProvider`, `CacheDataSource` usage. `PlaybackService.kt` must be tested after bump.
3. **Compose BOM 2024.09 → 2025.x:** `Material3` app bar, `ModalBottomSheet`, `NavigationBar` edge-to-edge insets. `MainActivity.kt` uses `enableEdgeToEdge()` — must add proper `WindowInsets` handling.
4. **Coil 2 → 3:** New import paths, `AsyncImage` signature changes. If migrating, touch every image composable. Safer to pin Coil 2.6.0 in Stage 0 and migrate in Stage 6 or skip.
5. **Firebase BOM 33→34:** `firebase-firestore` snapshot listener APIs stable. Test login/logout/admin claims (`functions/index.js` uses `functions.https.onCall` — Gen1 API, migrate to Gen2 or keep as-is).
6. **compileSdk 34→35:** Android 15 enforces edge-to-edge + predictive back. Test `AndroidManifest.xml` `theme` + `windowSoftInputMode`. Also `targetSdk 35` required for new Play Store uploads from Aug 2025.
7. **JDK 11→17:** `compileOptions` + `kotlinOptions.jvmTarget = "17"`. Android Studio Hedgehog+ requires JDK 17.

## Recommended Target (Stage 0)

- `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (keep), Java 17
- AGP `8.13.2` (keep), Gradle wrapper `8.13` (verify)
- Kotlin `2.2.0` (or latest 2.x at time of implementation)
- Compose BOM `2025.06.01` (or latest 2025)
- Media3 `1.7.0` (stable, not bleeding edge) → then `1.8.x` if clean
- Firebase BOM `34.2.0`, google-services plugin `4.4.4`
- Coroutines `1.9.0`, Navigation `2.9.0`, Activity `1.10.1`, Lifecycle `2.9.0`, Core-KTX `1.13.1`
- Coil **stay 2.6.0** for Stage 0 (migrate later, separate PR)
- Ads `23.6.0`, Cast `22.0.0`, Auth `21.4.0`

> Rule: **One bump per PR.** SQL: `targetSdk` bump alone = one PR. Media3 alone = one PR. This isolates regressions.

## Files Touched in Stage 0

- `gradle/libs.versions.toml` — all version bumps
- `gradle/wrapper/gradle-wrapper.properties` — verify 8.13
- `app/build.gradle.kts` — SDK/Java/deps
- `build.gradle.kts`, `settings.gradle.kts` — plugin versions
- `app/src/main/AndroidManifest.xml` — edge-to-edge, `POST_NOTIFICATIONS` (API 33+), `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (API 34+)
- `app/src/main/java/com/minetallest/minefiy/MainActivity.kt` — insets
- `app/src/main/java/com/minetallest/minefiy/playback/PlaybackService.kt` — Media3 API fixes
- `app/src/main/java/com/minetallest/minefiy/playback/CacheUtil.kt` (if exists) — verify cache API

## Verification

Each Stage 0 sub-PR: `./gradlew :app:assembleDebug` + install on API 28/34/35 emulator + play one B2 song + login/logout smoke test. See [03-stage-0-upgrade.md](03-stage-0-upgrade.md).
