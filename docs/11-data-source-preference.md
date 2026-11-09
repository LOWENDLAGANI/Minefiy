# 11 — Data Source Preference (B2 vs YouTube Music)

> **Goal:** First-time picker for primary source, remembered on device, changeable in Settings — implemented.

## The Problem

App has two libraries that cannot be merged naively:

|  | B2 (Your Library) | YouTube Music |
|---|-------------------|---------------|
| Storage | Backblaze B2 MP3s | Remote YouTube |
| Index | Firestore `songs/artists` | `yt-proxy` (ytmusicapi) |
| Search | Local `SearchResult.SongResult/ArtistResult` | Remote `YtSong/Album/Artist` (Stage 1-3) |
| Playback | `B2Manager.getPresignedUrl(key)` | `StreamResolver.resolve(videoId)` (Stage 4) |
| Playlists/History | `Playlist.items = [key]` | `yt:$videoId` |
| Offline | Artwork cached, audio presigned | No offline |

Merging both into one list on day 1 would confuse both. So picker.

## Design

### Storage

```kotlin
// data/DataSource.kt
enum class DataSource { B2, YOUTUBE_MUSIC }

// data/UserPreferencesManager.kt — SharedPreferences "minefiy_prefs", key "data_source"
fun getDataSource(): DataSource? // null = onboarding not done
fun setDataSource(source: DataSource)
```

`null` means first launch. Stored **per device**, survives logout, cleared only on app data clear.

### ViewModel

```kotlin
// ui/viewmodels/MainViewModel.kt
private val prefsManager = UserPreferencesManager(application)
private val _dataSource = MutableStateFlow(prefsManager.getDataSource())
val dataSource: StateFlow<DataSource?> = _dataSource.asStateFlow()
fun setDataSource(source: DataSource) { prefsManager.set...; _dataSource.value = source }
fun needsDataSourceSelection() = _dataSource.value == null
```

### Navigation Flow

```
App start:
  !loggedIn → "login"
  loggedIn && dataSource==null → "choose-source" (onboarding, no back)
  loggedIn && dataSource!=null → "home"

Login success → if null then "choose-source" else "home"
Register success → login screen (existing) → same gate

Settings > Music Source → "change-source" (same screen, canGoBack=true, Cancel pops back)
```

**Files:**
- `ui/screens/DataSourceSelectionScreen.kt` — two cards (B2 recommended, YT), Continue disabled until picked, shows check on selected.
- `MainActivity.kt` — `choose-source` + `change-source` routes, `startDestination` logic, Settings passes `onMusicSourceClick`.
- `SettingsScreen.kt` — row `Music Source: Your Library (B2)` / `YouTube Music` / `Select...`, opens `change-source`.

### How Source Affects UI (Current Implementation)

| Screen | B2 | YOUTUBE_MUSIC |
|--------|----|---------------|
| Home (`NewHomeScreen`) | Artists + Today's hits + Recently played (Firestore) | Banner "You're using YouTube Music" + hint. B2 sections hidden (data still loaded, not deleted). |
| Search (`SearchScreen`) | Local search (existing `searchResults`) | Label changes to "Search YouTube Music", placeholder "yt-proxy not deployed yet. See docs/04...". Local results hidden. |
| Playlists | Firestore playlists (B2 keys) | Still shows (shared). Playback will support `yt:` prefix in Stage 4. |
| Settings | Shows current source, tap to change | Same |

**Why hide, not delete?** B2 data stays in Firestore/B2. Switching back instant — just `setDataSource(B2)`. No reload needed beyond UI.

### Playback Handling (Both Sources, Stage 4)

`PlaybackService.onAddMediaItems` will branch:

```kotlin
val url = if (mediaId.startsWith("yt:")) streamResolver.resolve(mediaId.removePrefix("yt:"))
          else b2Manager.getPresignedUrl(mediaId)
```

`mediaId` contract:
- B2 → `song.key` (e.g. `"abc123"`)
- YT → `"yt:$videoId"` (11-char ID, prefix avoids collision)

History/playlists store raw `mediaId`, so mixed playlists work. `_currentSong` lookup for B2 via `_songs.find { it.key == mediaId }`; for YT, separate `currentYtSong` (Stage 3).

### Existing Users (Update Migration)

If user updates app and `dataSource==null` but `isUserLoggedIn()==true` (existing user), they will see picker **once** after update (startDestination = `choose-source`). Onboarding copy tells them: "B2 Recommended if you already have a library here." They pick B2, continue, never see again. No data loss.

Future polish: auto-set B2 on update if Firestore has songs — not needed for MVP, picker once is acceptable for revival.

### Settings Change

1. Settings shows current source.
2. Tap → `change-source` screen (pre-selects current).
3. Pick other card → Continue → `popBackStack()` → Home/Search update immediately (observe `dataSource` flow).
4. Cancel → back without change.

### Testing

- [ ] Fresh install → login → sees picker → picks B2 → Home shows library.
- [ ] Picks YT → Home shows banner, Search shows placeholder.
- [ ] Restart app → does not show picker again (persisted).
- [ ] Settings → change to other source → Home/Search swap.
- [ ] Logout → login → does NOT show picker again (per-device).
- [ ] Existing user update → sees picker once → picks B2 → continues.

## Files

- **New:** `data/DataSource.kt`, `data/UserPreferencesManager.kt`, `ui/screens/DataSourceSelectionScreen.kt`
- **Modified:** `MainViewModel.kt`, `MainActivity.kt`, `SettingsScreen.kt`, `SearchScreen.kt`, `NewHomeScreen.kt`
- **Docs:** This file, `PLAN.md`, `TODO.md`
