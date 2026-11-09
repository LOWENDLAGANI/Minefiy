package com.minetallest.minefiy.ytmusic

/**
 * DTO -> domain mapping.
 *
 * Kept separate from the DTOs so a ytmusicapi shape change only touches this
 * file, and so the mapping is unit-testable without a network client.
 */

/** Highest-resolution thumbnail URL, or null when the shelf has none. */
internal fun List<YtThumbnailDto>?.bestThumbnailUrl(): String? =
    this?.asSequence()
        ?.filter { !it.url.isNullOrBlank() }
        ?.maxByOrNull { it.width ?: 0 }
        ?.url

private fun List<YtArtistRefDto>.joinedArtistName(): String =
    mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } }
        .joinToString(", ")
        .ifBlank { "Unknown artist" }

/** Parses `"4:03"` / `"1:02:03"` into seconds. */
internal fun parseDurationToSeconds(duration: String?): Int? {
    val parts = duration?.split(':')?.mapNotNull { it.trim().toIntOrNull() } ?: return null
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> null
    }
}

/** Returns null when the shelf item has no playable id. */
fun YtSongDto.toYtSong(fallbackArtworkUrl: String? = null): YtSong? {
    val id = videoId?.takeIf { it.isNotBlank() } ?: return null
    return YtSong(
        videoId = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = artists.joinedArtistName(),
        album = album?.name,
        durationSeconds = durationSeconds ?: parseDurationToSeconds(duration),
        artworkUrl = thumbnails.bestThumbnailUrl() ?: fallbackArtworkUrl,
        isExplicit = isExplicit,
    )
}

fun YtAlbumTrackDto.toYtSong(albumTitle: String?, fallbackArtworkUrl: String? = null): YtSong? {
    val id = videoId?.takeIf { it.isNotBlank() } ?: return null
    return YtSong(
        videoId = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = artists.joinedArtistName(),
        album = album ?: albumTitle,
        durationSeconds = durationSeconds ?: parseDurationToSeconds(duration),
        artworkUrl = thumbnails.bestThumbnailUrl() ?: fallbackArtworkUrl,
        isExplicit = isExplicit,
    )
}

fun YtQueueTrackDto.toYtSong(): YtSong? {
    val id = videoId?.takeIf { it.isNotBlank() } ?: return null
    return YtSong(
        videoId = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = artists.joinedArtistName(),
        album = null,
        durationSeconds = parseDurationToSeconds(length),
        artworkUrl = thumbnail.bestThumbnailUrl(),
    )
}

fun YtAlbumSummaryDto.toYtAlbumSummary(): YtAlbumSummary? {
    val id = browseId?.takeIf { it.isNotBlank() } ?: return null
    return YtAlbumSummary(
        browseId = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown album",
        artist = artists.joinedArtistName(),
        year = year,
        artworkUrl = thumbnails.bestThumbnailUrl(),
    )
}

fun YtArtistSummaryDto.toYtArtistSummary(): YtArtistSummary? {
    val id = browseId?.takeIf { it.isNotBlank() } ?: return null
    return YtArtistSummary(
        channelId = id,
        name = artist?.takeIf { it.isNotBlank() } ?: "Unknown artist",
        artworkUrl = thumbnails.bestThumbnailUrl(),
    )
}

fun YtAlbumDetailDto.toYtAlbum(): YtAlbum {
    val artwork = thumbnails.bestThumbnailUrl()
    return YtAlbum(
        browseId = "",
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown album",
        artist = artists.joinedArtistName(),
        year = year,
        trackCount = trackCount ?: tracks.size,
        artworkUrl = artwork,
        description = description,
        tracks = tracks.mapNotNull { it.toYtSong(albumTitle = title, fallbackArtworkUrl = artwork) },
    )
}

fun YtArtistDetailDto.toYtArtist(): YtArtist {
    val artwork = thumbnails.bestThumbnailUrl()
    return YtArtist(
        channelId = channelId ?: "",
        name = name?.takeIf { it.isNotBlank() } ?: "Unknown artist",
        description = description,
        subscribers = subscribers,
        artworkUrl = artwork,
        topSongs = songs?.results.orEmpty().mapNotNull { it.toYtSong(fallbackArtworkUrl = artwork) },
        albums = albums?.results.orEmpty().mapNotNull { it.toYtAlbumSummary() },
    )
}
