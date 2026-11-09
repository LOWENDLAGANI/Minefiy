package com.minetallest.minefiy.ytmusic

import com.minetallest.minefiy.data.Song

/**
 * A playable YouTube Music track.
 *
 * [mediaId] is what gets stored in playlists/history and set as
 * `MediaItem.mediaId`, prefixed so playback can tell it apart from a B2 key
 * (see docs/02-architecture.md, Stage 4).
 */
data class YtSong(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationSeconds: Int?,
    val artworkUrl: String?,
    val isExplicit: Boolean = false,
) {
    val mediaId: String
        get() = MEDIA_ID_PREFIX + videoId

    /** Reuses the existing player/playlist/history path with a `yt:` key. */
    fun toSong(): Song = Song(
        key = mediaId,
        title = title,
        artist = artist,
        album = album ?: "YouTube Music",
        artworkUrl = artworkUrl,
    )

    companion object {
        const val MEDIA_ID_PREFIX = "yt:"

        fun isYouTubeMediaId(mediaId: String): Boolean = mediaId.startsWith(MEDIA_ID_PREFIX)

        fun videoIdFromMediaId(mediaId: String): String = mediaId.removePrefix(MEDIA_ID_PREFIX)
    }
}
