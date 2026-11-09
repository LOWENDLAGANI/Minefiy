# TODO — Minefiy Revival (Executable Checklist)

Copy to GitHub Issues per stage. Check off as you go.

## Stage 0 — Upgrade

- [x] 0.1 Gradle 8.14.3 + AGP 8.13.2 + JDK 17 (`JavaVersion.VERSION_17`)
- [x] 0.2 Kotlin 2.0.21 → 2.4.10
- [x] 0.3 SDK 34 → 36 (+ `POST_NOTIFICATIONS`)
- [x] 0.4 Compose BOM 2026.09.00, activity 1.10.1 / lifecycle 2.9.0 / core 1.13.1
- [x] 0.5 Media3 1.3.1 → 1.11.1
- [x] 0.6 Firebase BOM 34.2.0, Ads 23.6.0 / Auth 21.4.0 / Nav 2.9.0 / Coroutines 1.9.0
- [x] 0.7 Coil 2.7.0 (3.x migration deferred), icons pinned 1.7.8
- [x] 0.8 `local.properties.example`, versionCode 2, versionName 0.2
- [ ] **`./gradlew :app:assembleDebug` green** — NOT RUN (sandbox has no JDK/SDK); run in Android Studio
- [ ] Manual QA: login → Home → play B2 song → pause → next → logout

## Stage 1 — yt-proxy

- [x] Create `yt-proxy/main.py` (FastAPI, 7 endpoints + TTL cache + CORS)
- [x] Create `yt-proxy/requirements.txt` — **ytmusicapi 1.12.3** (1.11.x breaks `/watch` with `KeyError: 'endpoint'`)
- [x] Create `yt-proxy/Dockerfile`, `.dockerignore`, `README.md`
- [x] Local run verified: `/health`, `/search` (songs/albums/artists/playlists), `/album`, `/artist`, `/song`, `/watch`, `/lyrics` (videoId + browseId), 400 on bad filter, cache 0.45s → 0.003s
- [x] Fix `/lyrics` contract: `get_lyrics()` needs a browseId, so resolve videoId → `/watch` → `lyrics`, 404 when unavailable
- [x] Honor `limit` for `/search` and `/watch` (ytmusicapi ignores it)
- [ ] Deploy: `gcloud run deploy minefiy-yt --source ./yt-proxy --region us-central1 --allow-unauthenticated` — needs your GCP project
- [ ] Save URL to `local.properties` as `yt.proxy.base.url`

## Stage 2 — Android Network

- [x] Add Ktor 3.4.0 + kotlinx-serialization 1.9.0 deps and the serialization plugin (root + app)
- [x] Add `BuildConfig.YT_PROXY_BASE_URL` from `local.properties`
- [x] Create `ytmusic/YtMusicApi.kt` (Ktor/OkHttp client, `YtNotConfiguredException`, `YtApiException`)
- [x] Create `ytmusic/YtDtos.kt`, `YtSong.kt`, `YtAlbum.kt`, `YtArtist.kt`, `YtFilter.kt`, `YtMappers.kt`
- [x] Unit tests `YtDtoParsingTest` (8 tests) built from live proxy JSON
- [x] Verified every DTO wire name against live payloads (caught + fixed `YtArtistSummaryDto.subscribers`)
- [ ] `./gradlew :app:testDebugUnitTest` — NOT RUN (sandbox has no JDK/SDK)

## Stage 3 — Search UI

- [x] `MainViewModel`: `searchTab`, `ytFilter`, `ytSearchState`, `ytAlbumState`, `ytArtistState`, `ytMessage`, `searchYouTube()` (300 ms debounce), `loadYtAlbum()`, `loadYtArtist()`, `playYtSong()` stub
- [x] `SearchResult.kt` — **intentionally unchanged**; YouTube uses typed `YtUiState<List<YtSearchItem>>` instead (see docs/06)
- [x] `SearchScreen.kt`: My library / YouTube Music tabs + filter chips + snackbar + all 5 states
- [x] `YtAlbumScreen.kt`, `YtArtistScreen.kt` + NavHost routes `yt-album/{browseId}`, `yt-artist/{channelId}`
- [x] `ui/components/YtRows.kt`: song/album/artist/playlist rows, top bar, state placeholder, duration formatting
- [x] Static checks: 69 `viewModel.*` references resolve, all composables declared, 15 files brace-balanced
- [ ] `./gradlew :app:assembleDebug` — NOT RUN (no JDK/SDK)
- [ ] On-device QA: search "coldplay", filters, album, artist, Stage 4 snackbar on tap

## Stage 4 — Playback

- [x] **No NewPipeExtractor needed** — `StreamResolver` talks to InnerTube directly with the Ktor client already present
- [x] `ytmusic/StreamResolver.kt` (ANDROID_VR→IOS→ANDROID fallback, 45 min in-memory cache, cipher detection)
- [x] Fix `MainViewModel.Song.toMediaItem()` — `setUri(artworkUrl)` removed (it made ExoPlayer play a JPEG)
- [x] `PlaybackService.onAddMediaItems` — branch `yt:` → StreamResolver, else B2 presigned
- [x] `playYtSong()` + Cast branch + `ytSongsById` for now-playing metadata
- [x] Verified live: datacenter IPs get `LOGIN_REQUIRED` on all six InnerTube clients → resolution must stay on-device (docs/07)
- [ ] `./gradlew :app:assembleDebug` — NOT RUN (no JDK/SDK)
- [ ] On-device QA: YT play <3s, B2 still plays after a YT track, notification/lock screen, airplane-mode error, Cast a YT track

## Stage 5 — Queue / Lyrics / Favorites

- [x] Radio auto-queue: `startYtQueue()` + `enqueueYtRadio()` (`/watch?radio=true`, limit 10)
- [x] `ytQueue` state + "Up next" (3 rows) in `FullScreenPlayer`
- [x] Lyrics: `LyricsDialog` + per-videoId cache, browseId resolved via `/watch`
- [x] Favorites: `users/{uid}/ytFavorites` + `toggleYtFavorite()` + hearts + `yt-favorites` screen
- [x] Album/artist Play + Shuffle (`playYtAlbum`, `playYtArtist`)
- [x] Mixed playlists: `playMediaIds()` + `songForMediaId()` in `PlaylistDetailScreen`
- [x] Mixed history: `historyKeys` + `refreshHistory()`
- [x] Static checks: 80 `viewModel.*` refs resolve, 11 files balanced, `resetYtState()` in `logout()`
- [ ] Firestore rules for `users/{uid}/ytFavorites/**` — verify before shipping
- [ ] `./gradlew :app:assembleDebug` — NOT RUN (no JDK/SDK)
- [ ] On-device: radio continues after a track, lyrics, heart persists, mixed playlist plays


- [ ] Radio: `GET /watch/{id}` → append 5 songs on track end
- [ ] Lyrics: `GET /lyrics/{id}` → `LyricsScreen.kt`
- [ ] Favorites: `FirebaseManager` `users/{uid}/ytFavorites`, heart toggle
- [ ] Album play/shuffle, mixed playlist (B2+YT) support
- [ ] QA: Radio auto-queue, lyrics text, favorite persists, mixed playlist

## Stage 6 — Infra / Harden

- [ ] Cloud Run prod config (concurrency 80, timeout 60, memory 512Mi)
- [ ] `functions/index.js` — keep Gen1 or migrate to Gen2 (document choice)
- [ ] `local.properties` prod URL, `.gitignore` verified
- [ ] GitHub Actions `./gradlew assembleDebug`, Dependabot
- [ ] Uptime check `/health`, Cloud Logging review
- [ ] Release APK sideload test on API 35 device, README + `yt-proxy/README.md` updated

## Data Source Preference (B2 vs YouTube) — Implemented

- [x] `data/DataSource.kt` enum + `data/UserPreferencesManager.kt` (SharedPreferences)
- [x] `MainViewModel.dataSource: StateFlow<DataSource?>` + `setDataSource()`
- [x] `ui/screens/DataSourceSelectionScreen.kt` (onboarding + change-source)
- [x] `MainActivity`: `choose-source` / `change-source` routes, startDestination + login gate
- [x] `SettingsScreen`: Music Source row → change source
- [x] `SearchScreen` / `NewHomeScreen`: YT placeholder when YOUTUBE_MUSIC selected
- [x] `docs/11-data-source-preference.md`
- [ ] QA: fresh install picker, restart persistence, Settings swap, logout/login no re-prompt

## Docs

- [x] `docs/PLAN.md`, `00-overview.md`, `01-upgrade-audit.md`, `02-architecture.md`
- [x] Stage files `03-stage-0-upgrade.md` → `09-stage-6-infra-deploy.md`
- [x] `10-risks-legal-cost.md`
- [x] `11-data-source-preference.md`
- [x] `local.properties.example` at repo root
