package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(playlistId: String, viewModel: MainViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val songs by viewModel.songs.collectAsState()

    val currentPlaylist = playlists.find { it.id == playlistId }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog && currentPlaylist != null) {
        RenamePlaylistDialog(
            currentName = currentPlaylist.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renamePlaylist(currentPlaylist, newName)
                showRenameDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        currentPlaylist?.let { playlist ->
            val playlistSongs = playlist.items.mapNotNull { songId ->
                songs.find { song -> song.key == songId }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                PlaylistHeader(playlist = playlist, songs = playlistSongs, viewModel = viewModel)
                
                if (playlistSongs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.playPlaylist(playlist) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Play All")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { viewModel.playPlaylistShuffled(playlist) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Shuffle")
                        }
                    }

                    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                        itemsIndexed(playlistSongs) { index, song ->
                            var showSongMenu by remember { mutableStateOf(false) }
                            Box {
                                SongListItem(
                                    trackNumber = index + 1,
                                    song = song,
                                    onItemClick = { viewModel.playSong(song) },
                                    onItemLongClick = { showSongMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showSongMenu,
                                    onDismissRequest = { showSongMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            viewModel.removeSongFromPlaylist(song, playlist)
                                            showSongMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This playlist is empty. Add some songs!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onRename(text)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
