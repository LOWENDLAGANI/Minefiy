package com.minetallest.minefiy.ytmusic

/** Album header + tracks, from `/album/{browseId}`. */
data class YtAlbum(
    val browseId: String,
    val title: String,
    val artist: String,
    val year: String?,
    val trackCount: Int?,
    val artworkUrl: String?,
    val description: String?,
    val tracks: List<YtSong> = emptyList(),
)

/** Album card used in search results and artist pages. */
data class YtAlbumSummary(
    val browseId: String,
    val title: String,
    val artist: String,
    val year: String?,
    val artworkUrl: String?,
)
