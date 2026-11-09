package com.minetallest.minefiy.ytmusic

/**
 * Search shelves supported by the yt-proxy backend.
 *
 * Values must match the `filter` query parameter of `GET /search`.
 */
enum class YtFilter(val apiValue: String) {
    SONGS("songs"),
    ALBUMS("albums"),
    ARTISTS("artists"),
    PLAYLISTS("playlists"),
}
