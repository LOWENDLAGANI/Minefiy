package com.minetallest.minefiy.data

sealed class SearchResult {
    data class SongResult(val song: Song) : SearchResult()
    data class ArtistResult(val artist: Artist) : SearchResult()
}
