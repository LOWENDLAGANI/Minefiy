# Stage 2 — Android Network Layer

> Goal: Ktor client that talks to Stage 1 backend + Kotlin DTOs/domain models. No UI yet.

## APPLIED 2026-11-09 — Kotlin compile NOT run (no JDK/SDK in sandbox)

### Files

| File | Contents |
|------|----------|
| `ytmusic/YtFilter.kt` | `SONGS/ALBUMS/ARTISTS/PLAYLISTS` with backend `apiValue` |
| `ytmusic/YtDtos.kt` | 17 `@Serializable` wire models matching live ytmusicapi 1.12.3 payloads |
| `ytmusic/YtSong.kt` | Domain model, `mediaId = "yt:$videoId"`, `toSong()` |
| `ytmusic/YtAlbum.kt` | `YtAlbum` (header + tracks), `YtAlbumSummary` (card) |
| `ytmusic/YtArtist.kt` | `YtArtist` (top songs + albums), `YtArtistSummary` |
| `ytmusic/YtMappers.kt` | DTO → domain, thumbnail picker, `"4:03"` → 243 |
| `ytmusic/YtMusicApi.kt` | Ktor/OkHttp client: search×4, album, artist, queue, lyrics, health |
| `app/src/test/.../YtDtoParsingTest.kt` | 8 JUnit tests over captured JSON |

### Wiring

- `kotlin("plugin.serialization")` from `libs.plugins.kotlin.serialization` (root + app).
- Ktor **3.4.0**, kotlinx-serialization-json **1.9.0** (both verified on Maven Central).
- `BuildConfig.YT_PROXY_BASE_URL` from `local.properties` → `yt.proxy.base.url`.
- Blank URL is not a crash: every call throws `YtNotConfiguredException` and `isConfigured` is `false`.

### Contract details this stage encodes

1. `/search?filter=songs` → `album` is an **object** (`{name,id}`); `/album/...` tracks → `album` is a **string**. Separate DTOs.
2. `/search?filter=artists` uses the key **`artist`**, not `title`.
3. `/watch/...` tracks use **`length`** + singular **`thumbnail`**.
4. `duration_seconds` (snake_case) is mapped via `@SerialName`; `"4:03"` is a fallback.
5. `/lyrics/{id}` returns `{lyrics, source, hasTimestamps}`; a 404 maps to `null`, not an exception.
6. `utc`-free: all fields nullable/defaulted with `ignoreUnknownKeys = true`, so a new ytmusicapi field cannot break parsing.

### Verification performed (without a compiler)

- Parsed every DTO class out of `YtDtos.kt` and checked each declared wire name against live payloads from `/search` (all filters), `/album`, `/artist`, `/watch`, `/lyrics`. This caught `YtArtistSummaryDto.subscribers`, which search payloads never contain — removed.
- Every JSON fixture embedded in the unit test parses as valid JSON.
- `libs.versions.toml` parses as TOML; Kotlin files brace-balanced.

### Still required

- `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` on a machine with JDK 17 + Android SDK — **not run here**.
- Stage 3 wires `YtMusicApi` into `MainViewModel`; nothing calls it yet.

## Dependencies to Add

In `app/build.gradle.kts` (after Stage 0 upgrades):

```kotlin
// Ktor (same stack as InnerTune)
implementation("io.ktor:ktor-client-core:3.0.3")
implementation("io.ktor:ktor-client-okhttp:3.0.3")
implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

// kotlinx-serialization plugin (root build.gradle.kts)
id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
```

> Alternative: Retrofit + OkHttp + Moshi. Ktor preferred for coroutines + multiplatform future.

## New Package: `ytmusic/`

```
com/minetallest/minefiy/ytmusic/
├── YtMusicApi.kt    # Ktor client, singleton, suspend fns
├── YtDtos.kt        # @Serializable raw DTOs matching ytmusicapi JSON
├── YtSong.kt        # Domain model + mapper from DTO + toSong()
├── YtAlbum.kt
├── YtArtist.kt
└── YtFilter.kt      # enum SONGS, ALBUMS, ARTISTS, PLAYLISTS
```

### YtMusicApi.kt

```kotlin
class YtMusicApi(baseUrl: String) {
  private val client = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { requestTimeoutMillis = 15000; connectTimeoutMillis = 10000 }
    defaultRequest { header("Accept", "application/json") }
    defaultRequest { url(baseUrl) }
  }
  suspend fun search(q: String, filter: YtFilter, limit: Int = 20): List<YtSong> { ... }
  suspend fun getAlbum(browseId: String): YtAlbum { ... }
  suspend fun getArtist(channelId: String): YtArtist { ... }
  suspend fun getWatch(videoId: String): List<YtSong> { ... }
  suspend fun getLyrics(videoId: String): String? { ... }
}
```

Base URL from `BuildConfig.YT_PROXY_BASE_URL` (add to `app/build.gradle.kts` reading `local.properties` `yt.proxy.base.url`).

### YtDtos.kt

`@Serializable` with `@SerialName` matching ytmusicapi keys exactly:

```kotlin
@Serializable data class YtSearchSongDto(
  val videoId: String?, val title: String?,
  val artists: List<YtArtistDto> = emptyList(),
  val album: YtAlbumDto? = null,
  val duration_seconds: Int? = null, // ytmusicapi uses snake_case
  val thumbnails: List<YtThumbnailDto> = emptyList()
)
```

Use `ignoreUnknownKeys = true` everywhere — ytmusicapi adds fields often.

### YtSong.kt

```kotlin
data class YtSong(
  val videoId: String, val title: String, val artist: String,
  val album: String?, val durationSeconds: Int?, val artworkUrl: String?
) { fun toSong() = Song(key = videoId, title = title, artist = artist, album = album ?: "YouTube", artworkUrl = artworkUrl) }
```

## BuildConfig Addition

In `app/build.gradle.kts` `defaultConfig`:
```kotlin
val ytProxyUrl = localProperties.getProperty("yt.proxy.base.url") ?: ""
buildConfigField("String", "YT_PROXY_BASE_URL", "\"$ytProxyUrl\"")
```

## Tests

- Unit test DTO parsing with sample JSON from Stage 1 curl output (save fixture `yt-proxy/sample_search.json`)
- Instrumented: `YtMusicApi.search("coldplay")` returns non-empty on device with internet

## Exit Criteria

- [ ] `YtMusicApi` compiles, `BuildConfig.YT_PROXY_BASE_URL` set
- [ ] Sample JSON → DTO → `YtSong` round-trip test passes
- [ ] Logcat shows successful `GET /search` from device
- [ ] Error path: offline → `IOException` propagates to ViewModel (UI handles in Stage 3)

## Files Created/Modified

- New: `ytmusic/YtMusicApi.kt`, `YtDtos.kt`, `YtSong.kt`, `YtAlbum.kt`, `YtArtist.kt`, `YtFilter.kt`
- Modified: `app/build.gradle.kts`, root `build.gradle.kts` (serialization plugin)
- Not yet: `MainViewModel`, `SearchScreen`, `PlaybackService` (Stages 3–4)
