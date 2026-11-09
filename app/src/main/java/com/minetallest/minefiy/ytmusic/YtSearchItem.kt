package com.minetallest.minefiy.ytmusic

/**
 * One rendered row in the YouTube Music search results.
 *
 * The active [YtFilter] decides which subtype appears, but the UI is written
 * against the sealed type so a mixed shelf (e.g. a future "top results" tab)
 * needs no extra plumbing.
 */
sealed interface YtSearchItem {
    data class Song(val song: YtSong) : YtSearchItem
    data class Album(val album: YtAlbumSummary) : YtSearchItem
    data class Artist(val artist: YtArtistSummary) : YtSearchItem
    data class Playlist(val playlist: YtPlaylistSummary) : YtSearchItem
}
