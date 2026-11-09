package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@Composable
fun ArtistDetailScreen(artistName: String, viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsState()
    val artistSongs = songs.filter { it.artist == artistName }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = artistName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            itemsIndexed(artistSongs) { index, song ->
                SongListItem(
                    trackNumber = index + 1,
                    song = song,
                    onItemClick = { viewModel.playSong(song) }
                )
            }
        }
    }
}
