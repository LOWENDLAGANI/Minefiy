package com.minetallest.minefiy.ytmusic

import com.minetallest.minefiy.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Thrown when `yt.proxy.base.url` was never configured at build time. */
class YtNotConfiguredException : IllegalStateException(
    "YouTube Music is not configured. Set `yt.proxy.base.url` in local.properties " +
        "and rebuild (see docs/04-stage-1-yt-proxy.md)."
)

/** Non-2xx response from the proxy, carrying the FastAPI `detail` message. */
class YtApiException(
    val status: Int,
    val detail: String,
) : Exception("yt-proxy returned $status: $detail")

/**
 * Ktor client for the yt-proxy backend.
 *
 * Metadata only: this class never resolves audio URLs (that is
 * [com.minetallest.minefiy.ytmusic.StreamResolver]'s job, Stage 4).
 *
 * @param baseUrl proxy origin, defaults to the `YT_PROXY_BASE_URL` build field.
 * @param httpClient injectable for tests; the default is an OkHttp client with
 *   JSON negotiation and timeouts.
 */
class YtMusicApi(
    baseUrl: String = BuildConfig.YT_PROXY_BASE_URL,
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    private val base: String = baseUrl.trim().trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** False when no proxy URL was baked into the build. */
    val isConfigured: Boolean
        get() = base.isNotEmpty()

    suspend fun health(): YtHealthDto = getJson("/health")

    suspend fun searchSongs(query: String, limit: Int = 20): List<YtSong> =
        getJson<List<YtSongDto>>("/search") {
            parameter("q", query)
            parameter("filter", YtFilter.SONGS.apiValue)
            parameter("limit", limit)
        }.mapNotNull { it.toYtSong() }

    suspend fun searchAlbums(query: String, limit: Int = 20): List<YtAlbumSummary> =
        getJson<List<YtAlbumSummaryDto>>("/search") {
            parameter("q", query)
            parameter("filter", YtFilter.ALBUMS.apiValue)
            parameter("limit", limit)
        }.mapNotNull { it.toYtAlbumSummary() }

    suspend fun searchArtists(query: String, limit: Int = 20): List<YtArtistSummary> =
        getJson<List<YtArtistSummaryDto>>("/search") {
            parameter("q", query)
            parameter("filter", YtFilter.ARTISTS.apiValue)
            parameter("limit", limit)
        }.mapNotNull { it.toYtArtistSummary() }

    suspend fun searchPlaylists(query: String, limit: Int = 20): List<YtPlaylistSummary> =
        getJson<List<YtPlaylistSummaryDto>>("/search") {
            parameter("q", query)
            parameter("filter", YtFilter.PLAYLISTS.apiValue)
            parameter("limit", limit)
        }.mapNotNull { dto ->
            dto.browseId?.takeIf { it.isNotBlank() }?.let { id ->
                YtPlaylistSummary(
                    browseId = id,
                    title = dto.title ?: "Unknown playlist",
                    author = dto.author,
                    itemCount = dto.itemCount,
                    artworkUrl = dto.thumbnails.bestThumbnailUrl(),
                )
            }
        }

    suspend fun getAlbum(browseId: String): YtAlbum =
        getJson<YtAlbumDetailDto>("/album/$browseId").toYtAlbum()

    suspend fun getArtist(channelId: String): YtArtist =
        getJson<YtArtistDetailDto>("/artist/$channelId").toYtArtist()

    /** Radio / "up next" queue for a track (Stage 5 auto-queue). */
    suspend fun getQueue(videoId: String, limit: Int = 25, radio: Boolean = false): List<YtSong> =
        getJson<YtWatchDto>("/watch/$videoId") {
            parameter("limit", limit)
            parameter("radio", radio)
        }.tracks.mapNotNull { it.toYtSong() }

    /** Lyrics browseId for a track, when the proxy can resolve one. */
    suspend fun getLyricsBrowseId(videoId: String): String? =
        getJson<YtWatchDto>("/watch/$videoId") { parameter("limit", 1) }.lyrics

    /** Plain-text lyrics; null when the track has none (proxy returns 404). */
    suspend fun getLyrics(trackId: String): String? = try {
        getJson<YtLyricsDto>("/lyrics/$trackId").lyrics
    } catch (e: YtApiException) {
        if (e.status == 404) null else throw e
    }

    fun close() {
        httpClient.close()
    }

    private suspend inline fun <reified T> getJson(path: String): T =
        getJson<T>(path) { }

    private suspend inline fun <reified T> getJson(
        path: String,
        crossinline configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): T {
        if (!isConfigured) throw YtNotConfiguredException()
        val response = httpClient.get("$base$path") { configure() }
        if (!response.status.isSuccess()) {
            throw YtApiException(response.status.value, response.errorDetail(json))
        }
        return response.body()
    }

    /** Best-effort FastAPI `detail`, falling back to the raw body. */
    private fun io.ktor.client.statement.HttpResponse.errorDetail(json: Json): String {
        val raw = runCatching { bodyAsText() }.getOrNull().orEmpty()
        return runCatching { json.decodeFromString<YtErrorDto>(raw).detail }.getOrNull()
            ?: raw.takeIf { it.isNotBlank() }
            ?: "unexpected error"
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
        }
    }
}

/** Playlist card from `/search?filter=playlists`. */
data class YtPlaylistSummary(
    val browseId: String,
    val title: String,
    val author: String?,
    val itemCount: Int?,
    val artworkUrl: String?,
)
