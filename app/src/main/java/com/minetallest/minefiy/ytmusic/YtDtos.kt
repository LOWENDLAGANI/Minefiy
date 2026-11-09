package com.minetallest.minefiy.ytmusic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the yt-proxy backend (see docs/04-stage-1-yt-proxy.md).
 *
 * Shapes were captured from live ytmusicapi 1.12.3 responses. Every field is
 * nullable or defaulted because ytmusicapi omits keys unpredictably, and the
 * JSON parser runs with `ignoreUnknownKeys = true`.
 *
 * Watch the genuinely different shapes:
 *  - `/search?filter=songs`  -> `album` is an object (`YtAlbumRefDto`)
 *  - `/album/{id}` tracks    -> `album` is a plain string
 *  - `/watch/{id}` tracks    -> `length` + singular `thumbnail`
 */
@Serializable
data class YtThumbnailDto(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class YtArtistRefDto(
    val name: String? = null,
    val id: String? = null,
)

@Serializable
data class YtAlbumRefDto(
    val name: String? = null,
    val id: String? = null,
)

/** One item from `/search?filter=songs`. */
@Serializable
data class YtSongDto(
    val videoId: String? = null,
    val title: String? = null,
    val artists: List<YtArtistRefDto> = emptyList(),
    val album: YtAlbumRefDto? = null,
    val duration: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val thumbnails: List<YtThumbnailDto> = emptyList(),
    @SerialName("isExplicit") val isExplicit: Boolean = false,
    val year: String? = null,
)

/** One item from `/search?filter=albums`. */
@Serializable
data class YtAlbumSummaryDto(
    val browseId: String? = null,
    val title: String? = null,
    val type: String? = null,
    val year: String? = null,
    val artists: List<YtArtistRefDto> = emptyList(),
    val thumbnails: List<YtThumbnailDto> = emptyList(),
    @SerialName("isExplicit") val isExplicit: Boolean = false,
)

/** One item from `/search?filter=artists`. Note the `artist` key, not `title`. */
@Serializable
data class YtArtistSummaryDto(
    val browseId: String? = null,
    val artist: String? = null,
    val thumbnails: List<YtThumbnailDto> = emptyList(),
)

/** One item from `/search?filter=playlists`. */
@Serializable
data class YtPlaylistSummaryDto(
    val browseId: String? = null,
    val title: String? = null,
    val author: String? = null,
    @SerialName("itemCount") val itemCount: Int? = null,
    val thumbnails: List<YtThumbnailDto> = emptyList(),
)

/** One track inside `/album/{browseId}`; `album` here is a plain string. */
@Serializable
data class YtAlbumTrackDto(
    val videoId: String? = null,
    val title: String? = null,
    val artists: List<YtArtistRefDto> = emptyList(),
    val album: String? = null,
    val duration: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("trackNumber") val trackNumber: Int? = null,
    val thumbnails: List<YtThumbnailDto>? = null,
    @SerialName("isExplicit") val isExplicit: Boolean = false,
)

@Serializable
data class YtAlbumDetailDto(
    val title: String? = null,
    val type: String? = null,
    val year: String? = null,
    val description: String? = null,
    val artists: List<YtArtistRefDto> = emptyList(),
    val thumbnails: List<YtThumbnailDto> = emptyList(),
    @SerialName("trackCount") val trackCount: Int? = null,
    val tracks: List<YtAlbumTrackDto> = emptyList(),
)

@Serializable
data class YtSongShelfDto(
    val results: List<YtSongDto> = emptyList(),
)

@Serializable
data class YtAlbumShelfDto(
    val results: List<YtAlbumSummaryDto> = emptyList(),
)

/** `/artist/{channelId}`. `monthlyListeners` is deliberately not mapped. */
@Serializable
data class YtArtistDetailDto(
    val name: String? = null,
    val channelId: String? = null,
    val description: String? = null,
    val subscribers: String? = null,
    val thumbnails: List<YtThumbnailDto> = emptyList(),
    val songs: YtSongShelfDto? = null,
    val albums: YtAlbumShelfDto? = null,
)

/** One item from `/watch/{videoId}`; uses `length` and a singular `thumbnail`. */
@Serializable
data class YtQueueTrackDto(
    val videoId: String? = null,
    val title: String? = null,
    val length: String? = null,
    val artists: List<YtArtistRefDto> = emptyList(),
    val thumbnail: List<YtThumbnailDto> = emptyList(),
)

@Serializable
data class YtWatchDto(
    val tracks: List<YtQueueTrackDto> = emptyList(),
    val lyrics: String? = null,
    val playlistId: String? = null,
)

@Serializable
data class YtLyricsDto(
    val lyrics: String? = null,
    val source: String? = null,
    @SerialName("hasTimestamps") val hasTimestamps: Boolean? = null,
)

/** FastAPI error envelope: `{"detail": "..."}`. */
@Serializable
data class YtErrorDto(
    val detail: String? = null,
)

@Serializable
data class YtHealthDto(
    val ok: Boolean = false,
    val version: String? = null,
    val ytmusicapi: String? = null,
)
