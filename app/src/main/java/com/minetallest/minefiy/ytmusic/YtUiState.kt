package com.minetallest.minefiy.ytmusic

/**
 * Loading state for anything fetched from yt-proxy.
 *
 * [Success] carries the payload (a list for search, a single object for
 * album/artist) so one type serves all three flows.
 */
sealed interface YtUiState<out T> {
    data object Idle : YtUiState<Nothing>
    data object Loading : YtUiState<Nothing>

    /** `yt.proxy.base.url` is blank in this build. */
    data object NotConfigured : YtUiState<Nothing>

    data class Success<T>(val data: T) : YtUiState<T>
    data class Error(val message: String) : YtUiState<Nothing>
}

/** Which search the user is looking at. */
enum class SearchTab {
    LIBRARY,
    YOUTUBE,
}
