# 02 — Architecture & Hosting

## System Diagram

```
┌─────────────────────────────────────────────────┐
│ Android App (Kotlin/Compose/Media3)             │
│  MainActivity → MainViewModel                   │
│    ├─ FirebaseManager (Auth/Firestore)          │
│    ├─ B2Manager (MP3/artwork presigned URLs)    │
│    ├─ YtMusicApi (Ktor) ──HTTPS──┐              │
│    └─ StreamResolver (InnerTube) ──┐           │
└────────────────────────────────────┼───────────┼┘
                                     │           │
                    search/metadata  │           │ streams (audio URL only)
                                     ▼           ▼
                    ┌────────────────────┐  ┌────────────────────┐
                    │ Cloud Run (Python) │  │ youtubei.googleapis│
                    │ FastAPI +          │  │ .com/v1/player     │
                    │ ytmusicapi         │  │ (InnerTube player) │
                    │  /search           │  │  streamingData →   │
                    │  /album/{id}       │  │  audio URL         │
                    │  /artist/{id}      │  └──────────┬─────────┘
                    │  /watch/{id}       │             │
                    │  /lyrics/{id}      │             ▼
                    └────────────────────┘  ┌────────────────────┐
                                             │ ExoPlayer (phone)│
                                             │ audio direct from│
                                             │ googlevideo.com  │
                                             └────────────────────┘
                                     ▲
                    ┌────────────────┴─────────────────────┐
                    │ Firebase (Auth, Firestore) + B2 (S3) │
                    │  songs, artists, playlists, history  │
                    └──────────────────────────────────────┘
```

## Why This Split

- **Metadata via backend:** `ytmusicapi` is Python-only, needs IP stability, caching, rate-limit. Cloud Run gives that.
- **Streams via phone:** YouTube throttles datacenter IPs. Phone IP resolves cleanly. Audio never proxies → zero bandwidth cost.
- **Library stays independent:** B2/Firestore path untouched. YouTube path is additive.

## Hosting Comparison

|  | Firebase Hosting | Firebase Functions Gen2 | Cloud Run (recommended) |
|---|-----------------|-------------------------|-------------------------|
| Runs Python | ❌ static only | ✅ Python 3.11 | ✅ Docker, any Python |
| Timeout | N/A | 540s max | 3600s (configurable) |
| Deploy | `firebase deploy` | `firebase deploy --only functions` | `gcloud run deploy` |
| Cost (metadata only) | — | Free tier 2M invocations | Free tier 180k req + CPU seconds |
| Scaling | CDN | Auto, scales to zero | Auto, scales to zero |
| Logs | Firebase Console | Firebase Console + Cloud Logging | Cloud Logging |
| Exit strategy | — | Migrate to Cloud Run (same code) | Portable (Docker) |

**Recommendation:** Cloud Run. Functions Gen2 is acceptable fallback if you want to stay fully in Firebase Console.

## Data Flow: Search

1. User types → `SearchScreen` → `MainViewModel.searchYouTube()` (debounce 300ms)
2. `YtMusicApi.search()` → `GET {YT_PROXY_BASE_URL}/search?q=...&filter=songs&limit=20`
3. `yt-proxy` calls `YTMusic().search(query, filter='songs', limit=20)` → returns raw dict list
4. Android maps `videoId, title, artists[], album, duration_seconds, thumbnails[]` → `YtSong`
5. Compose renders rows (Coil thumbnails)

## Data Flow: Playback

1. User taps `YtSong` → `MainViewModel.playYtSong(YtSong)`
2. Creates `MediaItem(mediaId=videoId, metadata=title/artist/artwork)`, **no URI yet**
3. `PlaybackService.onAddMediaItems` → `StreamResolver.resolve(videoId)` → InnerTube player → highest audio format URL
4. Rebuilds `MediaItem(uri=streamUrl, mediaId=videoId, metadata)`
5. ExoPlayer plays. URL cached in-memory 1h (YouTube URLs expire ~6h, IP-bound)

For B2 songs: same pipeline, but resolver returns `B2Manager.getPresignedUrl(key)`.

## API Contract (yt-proxy)

All JSON, all `GET`, no auth in MVP. Errors: `{ "error": "..." }` with proper HTTP status.

```
GET /health
GET /search?q=QUERY&filter=songs|albums|artists|playlists&limit=20
GET /album/{browseId}
GET /artist/{channelId}
GET /song/{videoId}
GET /watch/{videoId}      # for queue/radio
GET /lyrics/{videoId}
```

Full schemas in [04-stage-1-yt-proxy.md](04-stage-1-yt-proxy.md).

## Caching Strategy

- **Backend:** In-memory dict TTL 10 min for search/album/artist. Cloud Run instance-local, acceptable for MVP.
- **Android:** Coil disk cache for thumbnails (already). Search results: no persistent cache in MVP (live data), add Room in Stage 5 if needed.
- **Stream URLs:** In-memory `Map<videoId, CachedUrl(expiresAt)>` TTL 1h.

## Security Notes

- **B2 keys in BuildConfig:** visible in APK. Existing design. Mitigation: use least-privilege B2 key, presigned URLs short expiry. Future: move S3 signing to backend.
- **YT_PROXY_BASE_URL:** not secret, safe in `local.properties`.
- **No service account keys in repo.** Cloud Run deploy via `gcloud` IAM.

## Package Structure (New Code)

```
app/src/main/java/com/minetallest/minefiy/
  ytmusic/
    YtMusicApi.kt         # Ktor client
    YtDtos.kt             # @Serializable raw DTOs
    YtSong.kt             # domain model + mapper
    YtAlbum.kt
    YtArtist.kt
    StreamResolver.kt     # InnerTube player resolver
yt-proxy/                 # Python FastAPI (separate folder, not Android module)
  main.py
  requirements.txt
  Dockerfile
  README.md
```
