# Stage 3 — Search, Album, Artist UI

> Goal: User can search YouTube Music and browse album/artist. Playback not yet wired (shows spinner, Stage 4 fixes).

## APPLIED 2026-11-09 — Kotlin compile NOT run (no JDK/SDK in sandbox)

### MainViewModel

| Added | Behaviour |
|-------|-----------|
| `searchTab` | `LIBRARY` / `YOUTUBE`, drives the tab row |
| `ytFilter` | `SONGS / ALBUMS / ARTISTS / PLAYLISTS`; changing it re-runs the search |
| `ytSearchState` | `YtUiState<List<YtSearchItem>>` |
| `ytAlbumState`, `ytArtistState` | `YtUiState<YtAlbum>` / `YtUiState<YtArtist>` |
| `ytMessage` | one-shot snackbar text, cleared by the UI |
| `searchYouTube(query)` | cancels the previous job, 300 ms debounce, 20 results, maps to `YtSearchItem` |
| `loadYtAlbum`, `loadYtArtist` | fetch + map, error captured into state |
| `playYtSong` | **Stage 4 stub** — logs the videoId and shows "playback arrives in the next stage" |
| `isYtConfigured` | surfaces a blank `yt.proxy.base.url` without crashing |

`onSearchQueryChange` now also triggers the YouTube search while the YOUTUBE tab is active, and `logout()` resets all YT state.

### UI

- `SearchScreen.kt` — rewritten: `TabRow` (My library / YouTube Music), per-tab field label, filter chips, and explicit rendering for `Idle / Loading / NotConfigured / Error / Success`. A `SnackbarHost` surfaces `ytMessage`. If `dataSource == YOUTUBE_MUSIC` it auto-selects the YouTube tab once.
- `ui/components/YtRows.kt` — `YtSongRow` (track number, explicit marker, duration `4:03`), `YtAlbumRow`, `YtArtistRow` (circular art), `YtPlaylistRow`, `YtDetailTopBar`, `YtStatePlaceholder`, `formatDuration`.
- `ui/screens/YtAlbumScreen.kt` — header (140 dp art, title, artist, year • tracks) + numbered track list.
- `ui/screens/YtArtistScreen.kt` — circular header, subscribers/description, Top songs, Albums (tap → album).
- `MainActivity.kt` — new routes `yt-album/{browseId}`, `yt-artist/{channelId}`; search screen passes album/artist navigation callbacks.

### Deviation from the original note

This file previously said to add `YtSongResult/YtAlbumResult/YtArtistResult` to the `SearchResult` sealed class. That was **not** done: the YouTube tab returns a different type per filter, so a typed `YtUiState<List<YtSearchItem>>` is used instead. `SearchResult` stays untouched and the library search path is unchanged.

### Verification performed (no compiler available)

- 69 distinct `viewModel.*` members referenced across screens/components/`MainActivity` — **all resolve** to declarations in `MainViewModel.kt`.
- `YtSongRow, YtAlbumRow, YtArtistRow, YtPlaylistRow, YtDetailTopBar, YtStatePlaceholder, YtArtwork, YtAlbumScreen, YtArtistScreen, SearchScreen, SongListItem, ArtistListItem, MiniPlayer` — all declared.
- Every `YtMusicApi` method the ViewModel calls exists in `YtMusicApi.kt`.
- All 15 touched/new Kotlin files brace- and paren-balanced; no leftover placeholder calls.
- Routes `yt-album/{browseId}` and `yt-artist/{channelId}` present with `navArgument` type declarations.

### Still required

- `./gradlew :app:assembleDebug` — **not run** here.
- On-device check: search "coldplay", switch filters, open album, open artist, confirm tap shows the Stage 4 snackbar.
- Playback itself is Stage 4; `playYtSong` currently only reports that.

## MainViewModel.kt Changes

Add alongside existing `_songs`, `searchResults` (local):

```kotlin
private val _ytState = MutableStateFlow<YtUiState>(YtUiState.Idle)
val ytState: StateFlow<YtUiState> = _ytState.asStateFlow()

private val _ytFilter = MutableStateFlow<YtFilter>(YtFilter.SONGS)
val ytFilter: StateFlow<YtFilter> = _ytFilter.asStateFlow()

private val _ytTab = MutableStateFlow<YtTab>(YtTab.LIBRARY)
val ytTab: StateFlow<YtTab> = _ytTab.asStateFlow()

fun setYtTab(tab: YtTab) { _ytTab.value = tab }
fun setYtFilter(f: YtFilter) { _ytFilter.value = f; if (_searchQuery.value.isNotBlank()) searchYouTube(_searchQuery.value) }

fun searchYouTube(query: String) {
  viewModelScope.launch {
    _ytState.value = YtUiState.Loading
    try {
      val songs = ytMusicApi.search(query, _ytFilter.value, 20)
      _ytState.value = YtUiState.Success(songs)
    } catch (e: Exception) {
      _ytState.value = YtUiState.Error(e.message ?: "Search failed")
    }
  }
}

fun loadAlbum(browseId: String) { ... }   // same pattern
fun loadArtist(channelId: String) { ... }
```

Debounce: Use `Flow.debounce(300)` on `_searchQuery` or manual `delay` + job cancel. Cancel previous job on new query.

Define `YtUiState` sealed: `Idle`, `Loading`, `Success(songs)`, `Error(msg)`. Same for album/artist or single generic.

## SearchResult.kt Extension

```kotlin
sealed class SearchResult {
  data class SongResult(val song: Song) : SearchResult()             // existing
  data class ArtistResult(val artist: Artist) : SearchResult()       // existing
  data class YtSongResult(val ytSong: YtSong, val index: Int) : SearchResult()
  data class YtAlbumResult(val album: YtAlbum) : SearchResult()
  data class YtArtistResult(val artist: YtArtist) : SearchResult()
}
```

Keep existing `searchResults` (local) separate from new `ytState` — don't merge flows. UI switches tabs.

## SearchScreen.kt Changes

- Add tab row: `TabRow` or `SegmentedButton` — **Library** (existing `LazyColumn` with `searchResults`) vs **YouTube Music** (new `LazyColumn` with `ytState`)
- In YouTube tab: filter chips row (`SingleChoice` chips: Songs/Albums/Artists/Playlists)
- YouTube song row: `AsyncImage(Coil)` thumbnail + `Text(title)` + `Text("${artist} • ${album}")` + duration `Text`
- Click → `viewModel.playYtSong(ytSong)` (stub in Stage 3, real in Stage 4)
- Album/artist click → navigate to detail screens (new routes `album/{browseId}`, `artist-yt/{channelId}` — note existing `artist/{artistName}` is for Firestore artists, don't collide)

## New Detail Screens

- `YtAlbumScreen.kt` — Header (cover, title, artist, year) + `LazyColumn` track list (tap to play)
- `YtArtistScreen.kt` — Header (avatar, name, subscribers) + sections: Top Songs, Albums

Register routes in `MainActivity.kt` `NavHost`.

## Navigation

Existing routes: `home, search, playlists, playlist/{id}, artist/{name}, settings, admin, ...`
Add: `yt-album/{browseId}`, `yt-artist/{channelId}` — must `android:exported`? No, internal only. Use `NavType.StringType`.

## Offline / Error Handling

- Show `Snackbar` with retry on `YtUiState.Error`
- Empty state: "No results for 'query'"
- Loading: `CircularProgressIndicator` centered

## Exit Criteria

- [ ] Library tab unchanged (local search still works)
- [ ] YouTube tab: type query → results with thumbnails in <2s
- [ ] Filter chips switch Songs/Albums/Artists
- [ ] Album detail shows tracks
- [ ] Artist detail shows top songs
- [ ] Tap song does nothing harmful yet (logs "Stage 4")

## Files Modified/Created

- Modified: `MainViewModel.kt`, `SearchScreen.kt`, `SearchResult.kt`, `MainActivity.kt` (NavHost)
- New: `ui/screens/YtAlbumScreen.kt`, `ui/screens/YtArtistScreen.kt`, `ui/components/YtSongRow.kt`
