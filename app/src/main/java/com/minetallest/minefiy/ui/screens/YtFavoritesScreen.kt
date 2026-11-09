package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.ui.components.YtDetailTopBar
import com.minetallest.minefiy.ui.components.YtSongRow
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

/**
 * Liked YouTube tracks, stored in Firestore (`users/{uid}/ytFavorites`).
 * Separate from the B2 "Liked Songs" playlist because those are uploaded files.
 */
@Composable
fun YtFavoritesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val favorites by viewModel.ytFavorites.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadYtFavorites()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        YtDetailTopBar(title = "Liked from YouTube", onBack = onBack)

        if (favorites.isEmpty()) {
            Text(
                text = "Nothing liked yet. Tap the heart on a track to save it here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { favorites.firstOrNull()?.let { viewModel.playYtSong(it, favorites) } },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play all")
                }
                OutlinedButton(
                    onClick = {
                        val shuffled = favorites.shuffled()
                        shuffled.firstOrNull()?.let { viewModel.playYtSong(it, shuffled) }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(favorites, key = { it.videoId }) { song ->
                    YtSongRow(
                        song = song,
                        onClick = { viewModel.playYtSong(song, favorites) },
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleYtFavorite(song) },
                    )
                }
            }
        }
    }
}
