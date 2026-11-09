# 00 — Overview, Goals, Decisions

## Problem

Minefiy is an old Android music app (Kotlin/Compose, Firebase, B2 MP3 storage, Media3, Cast) that only plays files uploaded to B2 and indexed in Firestore. It has no YouTube Music search or streaming. Goal is to revive it with YouTube Music without rewriting the existing library/playback path.

## Goals

1. Search YouTube Music (songs, albums, artists, playlists) from Android.
2. Play a YouTube Music song in the existing Media3 `Player` / `MediaSession` pipeline.
3. Keep existing B2 + Firestore library, playlists, history, Cast, ads working.
4. No audio proxying (no bandwidth cost). Audio goes Google → phone direct.
5. Stageable: every stage builds and is testable.

## Non-Goals

- No video playback (audio-only).
- No YouTube account login / personal library import in MVP (Phase 5+).
- No offline YouTube downloads (YouTube blocks it, DRM/helpers needed).
- No Play Store submission as YouTube clone (ToS). Sideload distribution.

## Decisions Log

| Decision | Choice | Why | Alternative rejected |
|----------|--------|-----|----------------------|
| Search backend | Python FastAPI + `ytmusicapi` on Cloud Run | `ytmusicapi` is Python-only, best coverage, easy to update | Kotlin re-implementation first = 2x work |
| Hosting | Cloud Run | Docker, long timeout, scales to zero, free tier | Firebase Hosting = static only ❌; Functions Gen2 = OK but shorter timeouts |
| Stream resolution | Android Kotlin InnerTube `player` (phone IP) + NewPipeExtractor fallback | Server IPs get blocked; phone IP works like real app | Server-side yt-dlp = easy to get 403 on Cloud IPs |
| HTTP client Android | Ktor + kotlinx.serialization | Same as InnerTune, coroutines-native | Retrofit/Moshi = fine but Ktor preferred |
| Upgrade first? | Yes — Stage 0 before any YTM code | Old SDK/Media3/Compose will conflict with new libs | Adding features on old stack = merge hell |
| Data model | Extend `Song`, add `Yt*` sealed results | Reuse player, playlists, history | Separate parallel player = duplication |

## Success Criteria (MVP)

- [ ] Search "coldplay" → YouTube tab shows songs with thumbnails in <2s.
- [ ] Tap song → audio starts via ExoPlayer, notification shows title/artist/artwork.
- [ ] Pause/resume, next/prev, seek work.
- [ ] Existing B2 songs still play (regression pass).
- [ ] App builds on Android Studio latest (Stage 0 complete).
- [ ] `local.properties.example` lets a fresh clone build (empty keys = graceful error, not crash).

## Key Constraint

`ytmusicapi` = metadata only. Playback comes from a **different** API (InnerTube player). Plan treats them as two separate integrations. See [02-architecture.md](02-architecture.md) and [10-risks-legal-cost.md](10-risks-legal-cost.md).
