package com.minetallest.minefiy.ytmusic

/** Artist page, from `/artist/{channelId}`. */
data class YtArtist(
    val channelId: String,
    val name: String,
    val description: String?,
    val subscribers: String?,
    val artworkUrl: String?,
    val topSongs: List<YtSong> = emptyList(),
    val albums: List<YtAlbumSummary> = emptyList(),
)

/** Artist card used in search results. */
data class YtArtistSummary(
    val channelId: String,
    val name: String,
    val artworkUrl: String?,
)
