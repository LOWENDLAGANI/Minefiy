package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.data.DataSource
import com.minetallest.minefiy.data.SearchResult
import com.minetallest.minefiy.ui.components.YtAlbumRow
import com.minetallest.minefiy.ui.components.YtArtistRow
import com.minetallest.minefiy.ui.components.YtPlaylistRow
import com.minetallest.minefiy.ui.components.YtSongRow
import com.minetallest.minefiy.ui.components.YtStatePlaceholder
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import com.minetallest.minefiy.ytmusic.SearchTab
import com.minetallest.minefiy.ytmusic.YtFilter
import com.minetallest.minefiy.ytmusic.YtSearchItem
import com.minetallest.minefiy.ytmusic.YtUiState

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onArtistClick: (String) -> Unit,
    onOpenYtAlbum: (String) -> Unit = {},
    onOpenYtArtist: (String) -> Unit = {},
    onOpenFavorites: () -> Unit = {},
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    val tab by viewModel.searchTab.collectAsState()
    val filter by viewModel.ytFilter.collectAsState()
    val ytState by viewModel.ytSearchState.collectAsState()
    val ytMessage by viewModel.ytMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Someone whose primary source is YouTube Music should land on that tab.
    LaunchedEffect(dataSource) {
        if (dataSource == DataSource.YOUTUBE_MUSIC) {
            viewModel.setSearchTab(SearchTab.YOUTUBE)
        }
    }

    LaunchedEffect(ytMessage) {
        ytMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearYtMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = if (tab == SearchTab.YOUTUBE) 1 else 0) {
                Tab(
                    selected = tab == SearchTab.LIBRARY,
                    onClick = { viewModel.setSearchTab(SearchTab.LIBRARY) },
                    text = { Text("My library") },
                )
                Tab(
                    selected = tab == SearchTab.YOUTUBE,
                    onClick = { viewModel.setSearchTab(SearchTab.YOUTUBE) },
                    text = { Text("YouTube Music") },
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = {
                    Text(
                        if (tab == SearchTab.YOUTUBE) "Search YouTube Music"
                        else "Search for songs or artists"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )

            if (tab == SearchTab.YOUTUBE) {
                YouTubeResults(
                    viewModel = viewModel,
                    filter = filter,
                    state = ytState,
                    query = searchQuery,
                    onOpenAlbum = onOpenYtAlbum,
                    onOpenArtist = onOpenYtArtist,
                    onOpenFavorites = onOpenFavorites,
                )
            } else {
                LibraryResults(
                    results = searchResults,
                    viewModel = viewModel,
                    onArtistClick = onArtistClick,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LibraryResults(
    results: List<SearchResult>,
    viewModel: MainViewModel,
    onArtistClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
        itemsIndexed(results) { index, result ->
            when (result) {
                is SearchResult.SongResult -> SongListItem(
                    song = result.song,
                    trackNumber = index + 1,
                    onItemClick = { viewModel.playSong(result.song) },
                )

                is SearchResult.ArtistResult -> ArtistListItem(
                    artist = result.artist,
                    onItemClick = { onArtistClick(result.artist.name) },
                )
            }
        }
    }
}

@Composable
private fun YouTubeResults(
    viewModel: MainViewModel,
    filter: YtFilter,
    state: YtUiState<List<YtSearchItem>>,
    query: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFavorites: () -> Unit,
) {
    val ytFavorites by viewModel.ytFavorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(YtFilter.entries) { entry ->
                FilterChip(
                    selected = filter == entry,
                    onClick = { viewModel.setYtFilter(entry) },
                    label = { Text(entry.label()) },
                )
            }
        }

        when (state) {
            is YtUiState.Success -> {
                val results = state.data
                if (results.isEmpty()) {
                    Text(
                        text = "No results for \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                        items(results) { item ->
                            when (item) {
                                is YtSearchItem.Song -> YtSongRow(
                                    song = item.song,
                                    onClick = { viewModel.playYtSong(item.song) },
                                    isFavorite = ytFavorites.any { it.videoId == item.song.videoId },
                                    onToggleFavorite = { viewModel.toggleYtFavorite(item.song) },
                                )

                                is YtSearchItem.Album -> YtAlbumRow(
                                    album = item.album,
                                    onClick = { onOpenAlbum(item.album.browseId) },
                                )

                                is YtSearchItem.Artist -> YtArtistRow(
                                    artist = item.artist,
                                    onClick = { onOpenArtist(item.artist.channelId) },
                                )

                                is YtSearchItem.Playlist -> YtPlaylistRow(
                                    playlist = item.playlist,
                                    onClick = {
                                        viewModel.showYtMessage("Opening YouTube Music playlists comes later")
                                    },
                                )
                            }
                        }
                    }
                }
            }

            YtUiState.Idle -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (query.isBlank()) {
                            "Search YouTube Music for songs, albums, artists and playlists."
                        } else {
                            "Press search to look up \"$query\""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (ytFavorites.isNotEmpty()) {
                        TextButton(onClick = onOpenFavorites) {
                            Text("Liked songs (${ytFavorites.size})")
                        }
                    }
                }
            }

            else -> YtStatePlaceholder(
                state = state,
                onRetry = { viewModel.searchYouTube(query) },
            )
        }
    }
}

private fun YtFilter.label(): String = when (this) {
    YtFilter.SONGS -> "Songs"
    YtFilter.ALBUMS -> "Albums"
    YtFilter.ARTISTS -> "Artists"
    YtFilter.PLAYLISTS -> "Playlists"
}
