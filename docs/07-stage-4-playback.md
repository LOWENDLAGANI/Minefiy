# Stage 4 — Playback

## APPLIED 2026-11-09 — Kotlin compile NOT run (no JDK/SDK in sandbox)

### The decisive finding

The InnerTube `player` endpoint was probed **live from this sandbox** with six
client identities (ANDROID, ANDROID_VR, IOS, TVHTML5_SIMPLY_EMBEDDED_PLAYER, WEB,
MWEB):

| client | result |
|--------|--------|
| ANDROID, ANDROID_VR, IOS, WEB, MWEB | `playabilityStatus = LOGIN_REQUIRED`, "Sign in to confirm you're not a bot", `adaptiveFormats = 0` |
| TVHTML5_SIMPLY_EMBEDDED_PLAYER | `ERROR`, "YouTube is no longer supported in this app" |

That is a datacenter-IP block, and it is exactly why audio must never be
resolved (or proxied) server-side. A phone on mobile/wifi gets `OK` + direct
`adaptiveFormats[].url`. The architecture in `02-architecture.md` holds: **metadata
via yt-proxy (Cloud Run), audio resolved on-device.**

### `ytmusic/StreamResolver.kt` (new, no new dependency)

- POSTs `https://www.youtube.com/youtubei/v1/player?prettyPrint=false` with Ktor
  (OkHttp engine already on the classpath from Stage 2).
- Client fallback order **ANDROID_VR → IOS → ANDROID**: these still hand out direct
  `adaptiveFormats[].url` without a PO token. WEB is deliberately excluded.
- Picks audio-only formats, prefers `audio/mp4` (seekable m4a for ExoPlayer), then
  highest bitrate.
- Detects `signatureCipher`/`cipher` and fails with an explicit message instead of
  returning an unplayable URL.
- Maps `playabilityStatus` to human advice (bot flag → "try mobile data",
  `UNPLAYABLE` → removed/private/region-locked).
- In-memory cache, 45 min TTL (URLs are IP-bound, ~6 h validity) — never persisted.
- `Mutex` gate so a burst of tracks does not stampede YouTube.
- `StreamResolutionException` carries the videoId + every client's failure reason.

### PlaybackService

- `streamUrlFor(mediaId)` branches: `yt:<videoId>` → `StreamResolver.resolve()`,
  anything else → presigned B2 URL. Both go through `onAddMediaItems`, so queueing,
  skip-next and shuffle work identically for both sources.
- Resolution failures are logged and the URI-less item is kept, so ExoPlayer reports a
  real playback error instead of playing the artwork.

### MainViewModel

- `playYtSong()` no longer shows "arrives in the next stage". Local playback sets one
  `MediaItem` (no URI) and lets the service resolve; Cast resolves eagerly and loads
  `audio/mp4` with title/artist/album/artwork metadata.
- `ytSongsById` remembers every YT track seen (search, album, artist) so
  `updateState()` can populate `currentSong` for `yt:` media ids — the mini player and
  full-screen player now work for YouTube tracks.
- **Bug fixed:** `Song.toMediaItem()` had `setUri(artworkUrl)` — ExoPlayer would play a
  JPEG whenever the service failed to override the URI, and `Uri.parse(null)` could
  throw. It now sets no URI at all (the service owns resolution) and artwork is set
  only when present. `album` is now passed as `setAlbumTitle`.

### Still required

- `./gradlew :app:assembleDebug` — not run here.
- **On-device only:** search a song, tap it, confirm audio starts; skip next; lock
  screen controls; then try on mobile data if a network is blocked.
- Casting a YT track is untested (needs a Cast device).
 (StreamResolver)

> Goal: YouTube song actually plays in ExoPlayer. This is the hardest part — budget accordingly.

## Why ytmusicapi Can't Do This

`ytmusicapi` = metadata. Audio URL comes from **InnerTube `player` endpoint**:
`POST https://youtubei.googleapis.com/v1/player?key=AIza...&prettyPrint=false`
Body: `{"context": {"client": {"clientName": "ANDROID", "clientVersion": "20.10.00"}}, "videoId": "VIDEO_ID", "playback_context": {"contentPlaybackContext": {"vis": 0}}}`
Response: `streamingData.adaptiveFormats[]` → pick highest `mimeType` starting with `audio/`.

YouTube requires cipher/signature + client fallback. Do not re-implement from scratch.

## Dependencies

Add one of:

**Option A (preferred): InnerTune `innertube` module**
- Copy/port open-source Kotlin module from `z-huang/InnerTune` (Apache-2.0)
- Gives you `YouTube.player(videoId)` returning formats directly
- You own the code, can patch when YouTube breaks

**Option B: NewPipeExtractor**
- Maven: `org.schabi.newpipe:NewPipeExtractor:0.24.3`
- `NewPipe.init(DownloadImpl())`, `ExtractorFactory.getServiceByUrl().getStreamExtractor()`
- Mature, maintained by NewPipe team, handles many edge cases

**Recommendation:** Start Option B for MVP speed, migrate to A if NewPipe API too heavy. Both are pure Java/Kotlin, no Python.

## New File: `ytmusic/StreamResolver.kt`

```kotlin
class StreamResolver {
  private val cache = mutableMapOf<String, CachedUrl>() // videoId -> (url, expiresAt)
  
  suspend fun resolve(videoId: String): String {
    cache[videoId]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let { return it.url }
    // 1. Try ANDROID client
    // 2. Fallback WEB_REAT
    // 3. Fallback NewPipeExtractor
    // Cache result 1h, return
  }
  data class CachedUrl(val url: String, val expiresAt: Long)
}
```

- Use Ktor or OkHttp (already in app). Respect per-client headers.
- Log which client succeeded (for debugging YouTube breaks).
- Timeout: 10s. Retry once per client.

## PlaybackService.kt Fix

**Current bug:** `MainViewModel.Song.toMediaItem()` does `setUri(artworkUrl)` and `PlaybackService.onAddMediaItems` does `b2Manager.getPresignedUrl(mediaId)`.

**Fix:**

1. `MainViewModel.Song.toMediaItem()` — remove URI or set placeholder. MediaItem must carry only `mediaId` + metadata. URI is resolved later.
   ```kotlin
   MediaItem.Builder().setMediaId(key).setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(Uri.parse(artworkUrl)).build()).build()
   ```
2. `PlaybackService.onAddMediaItems` — branch resolver:
   ```kotlin
   val streamUrl = when {
     mediaItem.mediaId.startsWith("yt:") -> streamResolver.resolve(mediaItem.mediaId.removePrefix("yt:"))
     else -> b2Manager.getPresignedUrl(mediaItem.mediaId)
   }
   ```
   Use `yt:` prefix for YouTube IDs to avoid collision with B2 keys. Or check `streamResolver.isYouTubeId(mediaId)` (11-char alphanumeric check).

3. `setPlaylist` / `playSong` — pass metadata correctly. Ensure `MediaController.setMediaItems()` gets items without URI, service adds URI.

4. Cache: `CacheUtil.getCache(this)` + `CacheDataSource` already set up — keep. Stream URLs flow through cache same as B2.

5. Cast: `playSong` currently casts B2 presigned URL via `RemoteMediaClient.load(mediaInfo)`. For YouTube, cast needs stream URL too — resolve before `load()`.

## MediaItem Contract (New)

| Source | `mediaId` | `uri` (after service) | metadata |
|--------|-----------|----------------------|----------|
| B2 song | `song.key` (e.g. `"a1b2c3"`) | `b2Manager.getPresignedUrl(key)` | title/artist/artworkUrl |
| YouTube | `"yt:$videoId"` | `StreamResolver.resolve(videoId)` | title/artist/artworkUrl from YT |

`_currentSong` lookup in `MainViewModel.updateState()` already matches `mediaId` against `_songs`. For YouTube, maintain separate `currentYtSong` flow.

## Error Handling

- Resolve fails (YouTube cipher changed): `onAddMediaItems` returns original item, ExoPlayer errors. Log + show `Snackbar "Can't play this song right now"`.
- Expired URL mid-play: ExoPlayer will retry. If 403, re-resolve on `onPlayerError` (future polish).
- Rate limit: If 429, backoff 2s, try next client.

## Testing

- [ ] Play YouTube song → audio starts <3s
- [ ] Pause, next, prev, seek 10s fwd/back work
- [ ] Notification shows title/artist/artwork
- [ ] Play B2 song immediately after YT song → still works (cache collision check)
- [ ] Airplane mode → error snackbar, no crash
- [ ] Throttle network (Dev Options → slow) → loading state shows

## Maintenance

InnerTube breaks every few months. When playback stops:
1. Check `StreamResolver` logs — which client failed?
2. Compare against latest InnerTune `Player.kt` diff
3. Bump client versions (e.g. `ANDROID 20.10 → 20.30`)
4. If NewPipeExtractor: bump `NewPipeExtractor` version

Budget 1 day per YouTube break.

## Files Modified/Created

- New: `ytmusic/StreamResolver.kt`
- Modified: `PlaybackService.kt`, `MainViewModel.kt` (`playYtSong`, `toMediaItem` fix, `currentYtSong`)
