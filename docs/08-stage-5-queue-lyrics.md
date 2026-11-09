# Stage 5 — Queue / Radio, Lyrics, Favorites

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
