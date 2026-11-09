# Stage 5 — Queue / Radio, Lyrics, Favorites

## APPLIED 2026-11-09 — Kotlin compile NOT run (no JDK/SDK in sandbox)

### 5.1 Radio / auto-queue

- `playYtSong(song, queue = emptyList())` — the tapped track always leads, the surrounding
  list (album, artist, search results, favorites) becomes the local queue. `buildList` +
  `filter` keeps it duplicate-free.
- `startYtQueue(tracks, startIndex)` pushes all tracks as `MediaItem`s and sets
  `ytQueue` to the remainder for the "Up next" UI.
- `enqueueYtRadio(seed, exclude)` then fetches `GET /watch/{videoId}?radio=true&limit=10`
  (`YT_RADIO_LIMIT`) and `addMediaItems` to the controller — so a track ending continues
  into radio without needing a `STATE_ENDED` listener, which would race with
  `MediaController`'s own gapless advance.
- Failures are swallowed deliberately (`Log.d`): no radio is not an error.
- `FullScreenPlayer` shows the first 3 upcoming YT tracks under "Up next".

### 5.2 Lyrics

- `lyricsCache: ConcurrentHashMap<videoId, String>` — one fetch per track per session.
- `loadYtLyrics()` resolves the lyrics **browseId** via `/watch/{videoId}` first, because
  `ytmusicapi.get_lyrics()` takes `MPLYt_...`, not a videoId (the proxy accepts either, but
  the explicit two-step matches how the payload actually arrives).
- `ui/screens/LyricsScreen.kt` → `LyricsDialog`: selectable monospace text, scrollable,
  with explicit Loading / Error / NotConfigured states. No timestamping — `ytmusicapi`
  only timestamps a minority of tracks.
- Opened from a `Lyrics` button in `FullScreenPlayer`, only for YT tracks.

### 5.3 Favorites

- `FirebaseManager`: `getYtFavorites()`, `addYtFavorite()`, `removeYtFavorite()` on
  `users/{uid}/ytFavorites/{videoId}` with `{videoId, title, artist, album,
  durationSeconds, artworkUrl, savedAt}`.
- Read manually (`documents.mapNotNull { getString(...) }`) rather than `toObjects`: the
  document id *is* the videoId, and this avoids reflection/no-arg-class pitfalls.
  Sorted newest-first by `savedAt`.
- `MainViewModel`: `ytFavorites`, `isYtFavorite()`, `toggleYtFavorite()` with optimistic
  list update + snackbar; `loadYtFavorites()` runs during initial load and after login.
- Hearts on `YtSongRow` (opt-in `isFavorite`/`onToggleFavorite`), on album/artist/search
  rows, and in `FullScreenPlayer`.
- New `ui/screens/YtFavoritesScreen.kt` (route `yt-favorites`) with Play all / Shuffle,
  reachable from the YouTube tab's empty state ("Liked songs (n)").

### 5.4 Album / artist play + shuffle

`playYtAlbum(album, shuffle)` and `playYtArtist(artist, shuffle)` with Play / Shuffle
buttons on both detail screens. Shuffle shuffles *then* starts, so the whole set is in the
queue rather than one shuffled track.

### 5.5 Mixed playlists + history

- `playMediaIds()` resolves each stored id: `yt:` → cached `YtSong`, otherwise a B2 key.
  `playPlaylist` / `playPlaylistShuffled` both go through it.
- `playSong()` routes `yt:` keys to `playYtSong`, so a mixed playlist row plays correctly.
- `PlaylistDetailScreen` builds its list with `viewModel.songForMediaId(id)`.
- History now stores either kind of id: `historyKeys` keeps the raw list and
  `refreshHistory()` re-maps it, so YT rows appear once their metadata is known
  (`loadYtFavorites()` calls `refreshHistory()` after the YT cache is populated).

### Verification performed (no compiler available)

- 80 distinct `viewModel.*` members referenced across all UI files — **all resolve**.
- All 11 touched/new Kotlin files brace- and paren-balanced.
- `FirebaseManager` exposes all three YT favorite methods; `resetYtState()` is called from
  `logout()`.

### Still required

- `./gradlew :app:assembleDebug` — **not run**.
- **Firestore rules** must permit `users/{uid}/ytFavorites/**`. If the existing rules only
  scope `playlists`/`history`, likes will silently fail — check before shipping.
- On-device: track ends → radio continues; lyrics open; heart survives restart; a
  playlist containing both a B2 key and a `yt:` id plays through.


> Goal: Full music-app feel. Optional but expected for "complete" revival.

## 5.1 Radio / Auto-Queue

**Backend:** `GET /watch/{videoId}` → `ytmusicapi.get_watch_playlist(videoId)` → `tracks[]`

**Android:**
- On `Player.STATE_ENDED` or last item in queue → fetch `watch(videoId)` → append 5 songs to `MediaController` queue
- `MainViewModel.playYtSong()` loads single song + radio queue behind it
- Show "Up next" in `FullScreenPlayer.kt` (existing bottom sheet) — list `MediaController.getUpcomingMediaItems()`

**Files:** `MainViewModel`, `PlaybackService` (or `Player.Listener`), `FullScreenPlayer.kt`

## 5.2 Lyrics

**Backend:** `GET /lyrics/{videoId}` → `ytmusicapi.get_lyrics(videoId)` → `{"lyrics": "..."}`

**Android:**
- New screen `LyricsScreen.kt` — bottom sheet from `FullScreenPlayer` or tab in player
- `Text` with selectable lyrics, monospaced font. Synced (timed) lyrics from `ytmusicapi` often lack timestamps — show plain text.
- Cache lyrics per `videoId` in-memory.

## 5.3 Favorites / Liked Songs Sync

YouTube songs are not in Firestore `songs` collection. Store favorites separately:

```
users/{uid}/ytFavorites/{videoId} = { title, artist, album, artworkUrl, savedAt }
```

In `FirebaseManager.kt` add:
```kotlin
suspend fun getYtFavorites(): List<YtSong>
suspend fun toggleYtFavorite(ytSong: YtSong)
```

**UI:** Heart button on `YtSongRow` + `FullScreenPlayer`. New "Liked" tab or section on Home.

**History:** Existing history stores B2 `key`s. For YT, store `yt:$videoId` keys in same `history` list, lookup merges B2 + YT maps.

## 5.4 Album Play / Shuffle

- In `YtAlbumScreen.kt`: "Play" + "Shuffle" buttons → `playYtAlbum(tracks)` → `MediaController.setMediaItems(tracks.map { it.toMediaItem() })`
- Same for `YtArtistScreen.kt` top songs.

## 5.5 Playlists With Mixed Content

Allow adding `yt:ID` to existing Firestore `Playlist.items: List<String>`.
- `PlaylistDetailScreen.kt` must resolve both B2 and YT items (lookup in `_songs` + YT cache).
- `playPlaylist()` must handle mixed `mediaId` prefixes.

## Exit Criteria

- [ ] Song ends → radio songs auto-play
- [ ] Lyrics screen shows text
- [ ] Favorite survives app restart + shows in Liked list
- [ ] Play mixed playlist (B2 + YT) works
- [ ] Shuffle album plays tracks in random order

## Files

- Modified: `MainViewModel.kt`, `FirebaseManager.kt`, `FullScreenPlayer.kt`, `PlaylistDetailScreen.kt`, `YtAlbumScreen.kt`
- New: `ui/screens/LyricsScreen.kt`, `ytmusic/YtRadioHelper.kt`
