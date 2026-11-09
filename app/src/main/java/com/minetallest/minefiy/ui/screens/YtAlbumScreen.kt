package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.minetallest.minefiy.ui.components.YtArtwork
import com.minetallest.minefiy.ui.components.YtDetailTopBar
import com.minetallest.minefiy.ui.components.YtSongRow
import com.minetallest.minefiy.ui.components.YtStatePlaceholder
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import com.minetallest.minefiy.ytmusic.YtAlbum
import com.minetallest.minefiy.ytmusic.YtUiState

@Composable
fun YtAlbumScreen(
    browseId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.ytAlbumState.collectAsState()

    LaunchedEffect(browseId) {
        viewModel.loadYtAlbum(browseId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        YtDetailTopBar(title = "Album", onBack = onBack)

        when (val current = state) {
            is YtUiState.Success -> AlbumContent(
                album = current.data,
                viewModel = viewModel,
            )

            else -> YtStatePlaceholder(
                state = current,
                onRetry = { viewModel.loadYtAlbum(browseId) },
            )
        }
    }
}

@Composable
private fun AlbumContent(album: YtAlbum, viewModel: MainViewModel) {
    val favorites by viewModel.ytFavorites.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                YtArtwork(url = album.artworkUrl, modifier = Modifier.size(140.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val meta = listOfNotNull(
                        album.year,
                        album.trackCount?.let { "$it tracks" },
                    ).filter { it.isNotBlank() }.joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { viewModel.playYtAlbum(album) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
                OutlinedButton(
                    onClick = { viewModel.playYtAlbum(album, shuffle = true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
        }

        itemsIndexed(album.tracks) { index, track ->
            YtSongRow(
                song = track,
                trackNumber = index + 1,
                onClick = { viewModel.playYtSong(track, album.tracks) },
                isFavorite = favorites.any { it.videoId == track.videoId },
                onToggleFavorite = { viewModel.toggleYtFavorite(track) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
