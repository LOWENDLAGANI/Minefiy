# Minefiy — Revival Plan (YouTube Music Integration)

> **Status:** Planning / Ready to implement
> **Date:** 2026-11-09
> **Project:** `com.minetallest.minefiy` — Old Android app revived with YouTube Music search + streaming
> **Stack today:** Kotlin, Jetpack Compose, Firebase (Auth/Firestore), B2 (S3 MP3 storage), Media3/ExoPlayer, Cast

## TL;DR

`ytmusicapi` gives you **metadata only** (title/artist/album/thumbnails/playlists). It cannot play audio.

Best architecture for Minefiy:

- **Search/Metadata:** Python backend on **Google Cloud Run** using `ytmusicapi` (FastAPI). Backend never touches audio.
- **Playback:** Android app resolves stream URL **directly** via Kotlin InnerTube `player` API (phone IP, like real YouTube Music). Audio streams Google → phone, no server proxy cost.
- **Keep what you have:** Firebase Auth/Firestore + B2 for your own MP3 library, playlists, history.

This is staged below so each stage builds, runs, and is testable independently. Do not skip Stage 0 (upgrade).

## Doc Index

| # | File | Purpose |
|---|------|---------|
| 00 | [00-overview.md](00-overview.md) | Goals, non-goals, decisions, success criteria |
| 01 | [01-upgrade-audit.md](01-upgrade-audit.md) | Current versions vs latest, what breaks, what to keep |
| 02 | [02-architecture.md](02-architecture.md) | System diagram, hosting choice (Cloud Run vs Firebase Functions vs Hosting) |
| 03 | [03-stage-0-upgrade.md](03-stage-0-upgrade.md) | **Stage 0:** Android/Kotlin/Compose/Media3/Firebase upgrade checklist |
| 04 | [04-stage-1-yt-proxy.md](04-stage-1-yt-proxy.md) | **Stage 1:** Python `yt-proxy/` scaffold + API contract |
| 05 | [05-stage-2-android-network.md](05-stage-2-android-network.md) | **Stage 2:** Ktor client, DTOs, domain models |
| 06 | [06-stage-3-search-ui.md](06-stage-3-search-ui.md) | **Stage 3:** ViewModel + Search/Album/Artist UI |
| 07 | [07-stage-4-playback.md](07-stage-4-playback.md) | **Stage 4:** StreamResolver (InnerTube/NewPipe) + PlaybackService fix |
| 08 | [08-stage-5-queue-lyrics.md](08-stage-5-queue-lyrics.md) | **Stage 5:** Radio/queue, lyrics, favorites sync |
| 09 | [09-stage-6-infra-deploy.md](09-stage-6-infra-deploy.md) | **Stage 6:** Cloud Run deploy, local.properties, CI, cost |
| 10 | [10-risks-legal-cost.md](10-risks-legal-cost.md) | ToS, IP blocks, maintenance, cost estimate |
| 11 | [11-data-source-preference.md](11-data-source-preference.md) | **B2 vs YouTube picker**: first-launch, remembered, Settings change |
| — | [TODO.md](TODO.md) | Executable checkbox checklist (copy to GitHub Issues) |

## Current State (Verified 2026-11-09)

- Entry: `app/src/main/java/com/minetallest/minefiy/MainActivity.kt` → `MainViewModelFactory` → `MainViewModel.init` creates `FirebaseManager`, `B2Manager`, `MetadataExtractor`, `SyncManager`, `MediaController`, `CastContext`.
- Firebase: `firebase/FirebaseManager.kt`, `app/google-services.json` (project `minefiy`, `697006955149`, package `com.minetallest.minefiy`).
- B2: `b2/B2Manager.kt` (AmazonS3Client), keys from `local.properties` → `BuildConfig` (file **missing** from repo — fresh clone fails).
- Playback: `playback/PlaybackService.kt` — `onAddMediaItems` only resolves `B2Manager.getPresignedUrl(mediaId)`. **Bug:** `MainViewModel.Song.toMediaItem()` sets `uri = artworkUrl`.
- Search: `ui/screens/SearchScreen.kt` + `MainViewModel.searchResults` — **local Firestore/B2 only** (`songs` + `artists` collections). No remote search.
- Models: `data/Song.kt` (`key, checksum, title, artist, album, artworkUrl?`), `data/SearchResult.kt` (sealed: `SongResult`, `ArtistResult`).

## Hosting Decision

| Option | Can run Python? | Verdict |
|--------|----------------|---------|
| Firebase Hosting | No — static only | ❌ Not for backend |
| Firebase Functions Gen2 (Python) | Yes | ✅ OK fallback |
| **Google Cloud Run (Python FastAPI)** | Yes, Docker | ✅ **Recommended** — longer timeout, better egress, same cost |

## Stage Order (Do Not Reorder)

```
Stage 0: UPGRADE (project compiles on latest stable)
  ↓
Stage 1: yt-proxy (Python FastAPI + ytmusicapi, metadata only)
  ↓
Stage 2: Android network (Ktor client + DTOs)
  ↓
Stage 3: Search UI (Library tab + YouTube tab, Album/Artist detail)
  ↓
Stage 4: Playback (StreamResolver + PlaybackService fix)
  ↓
Stage 5: Queue / Lyrics / Favorites
  ↓
Stage 6: Deploy + Harden (Cloud Run, caching, PO-Token fallback)
```

Each stage ends with: *Build → Run → Manual QA checklist* — see each stage file.

## Quick Start After This Plan

1. Add `local.properties` (see `local.properties.example` at repo root)
2. Finish Stage 0 upgrade first — don't add YTM code on old SDK/Media3
3. Deploy `yt-proxy/` to Cloud Run, paste URL into `local.properties` as `YT_PROXY_BASE_URL`
4. Build Android `debug`, test search tab

## Legal Note

Unofficial YouTube Music API. Violates YouTube Terms of Service. **Sideload APK only, do not submit to Play Store as YouTube Music clone.** See [10-risks-legal-cost.md](10-risks-legal-cost.md).
