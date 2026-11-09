package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.ui.components.YtAlbumRow
import com.minetallest.minefiy.ui.components.YtArtwork
import com.minetallest.minefiy.ui.components.YtDetailTopBar
import com.minetallest.minefiy.ui.components.YtSongRow
import com.minetallest.minefiy.ui.components.YtStatePlaceholder
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import com.minetallest.minefiy.ytmusic.YtArtist
import com.minetallest.minefiy.ytmusic.YtUiState

@Composable
fun YtArtistScreen(
    channelId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val state by viewModel.ytArtistState.collectAsState()

    LaunchedEffect(channelId) {
        viewModel.loadYtArtist(channelId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        YtDetailTopBar(title = "Artist", onBack = onBack)

        when (val current = state) {
            is YtUiState.Success -> ArtistContent(
                artist = current.data,
                viewModel = viewModel,
                onOpenAlbum = onOpenAlbum,
            )

            else -> YtStatePlaceholder(
                state = current,
                onRetry = { viewModel.loadYtArtist(channelId) },
            )
        }
    }
}

@Composable
private fun ArtistContent(
    artist: YtArtist,
    viewModel: MainViewModel,
    onOpenAlbum: (String) -> Unit,
) {
    val favorites by viewModel.ytFavorites.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                YtArtwork(
                    url = artist.artworkUrl,
                    modifier = Modifier.size(120.dp),
                    circular = true,
                )
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                listOfNotNull(artist.subscribers, artist.description)
                    .firstOrNull { it.isNotBlank() }
                    ?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
        }

        if (artist.topSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { viewModel.playYtArtist(artist) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick = { viewModel.playYtArtist(artist, shuffle = true) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
            }
            item {
                Text(
                    text = "Top songs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(artist.topSongs) { song ->
                YtSongRow(
                    song = song,
                    onClick = { viewModel.playYtSong(song, artist.topSongs) },
                    isFavorite = favorites.any { it.videoId == song.videoId },
                    onToggleFavorite = { viewModel.toggleYtFavorite(song) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (artist.albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(artist.albums) { album ->
                YtAlbumRow(
                    album = album,
                    onClick = { onOpenAlbum(album.browseId) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.size(24.dp)) }
    }
}
