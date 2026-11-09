package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@Composable
fun SongSelectionEditorScreen(selectionId: String, viewModel: MainViewModel, onSave: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val selection = viewModel.songSelections.collectAsState().value.find { it.id == selectionId }
    var selectedSongIds by remember { mutableStateOf(selection?.items ?: emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = if (searchQuery.isBlank()) {
        songs
    } else {
        songs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editing ${selection?.name}", style = MaterialTheme.typography.titleLarge)

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Songs") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredSongs) { song ->
                SongListItem(
                    trackNumber = 0, 
                    song = song, 
                    onItemClick = { 
                        selectedSongIds = if (selectedSongIds.contains(song.key)) {
                            selectedSongIds - song.key
                        } else {
                            selectedSongIds + song.key
                        }
                    },
                    onToggleSelection = {
                        selectedSongIds = if (selectedSongIds.contains(song.key)) {
                            selectedSongIds - song.key
                        } else {
                            selectedSongIds + song.key
                        }
                    },
                    isSelected = selectedSongIds.contains(song.key)
                )
            }
        }
        Button(onClick = {
            viewModel.updateSongSelection(selectionId, selectedSongIds)
            onSave()
        }) {
            Text("Save")
        }
    }
}
