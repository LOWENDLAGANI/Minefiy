package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.firebase.FirebaseManager
import com.minetallest.minefiy.playback.Playlist
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(viewModel: MainViewModel, onPlaylistClick: (String) -> Unit) {
    val playlists by viewModel.playlists.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val songs by viewModel.songs.collectAsState()

    Column {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Create Playlist")
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(playlists) { playlist ->
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    PlaylistItem(
                        playlist = playlist,
                        songs = songs.filter { playlist.items.contains(it.key) },
                        onItemClick = { onPlaylistClick(playlist.id) },
                        onItemLongClick = { 
                            if (playlist.id != FirebaseManager.LIKED_SONGS_PLAYLIST_ID) {
                                showMenu = true 
                            }
                        }
                    )

                    if (playlist.id != FirebaseManager.LIKED_SONGS_PLAYLIST_ID) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    viewModel.deletePlaylist(playlist)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Playlist"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            CreatePlaylistDialog(
                onDismiss = { showDialog = false },
                onCreate = { name ->
                    viewModel.createPlaylist(name, emptyList())
                    showDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistItem(
    playlist: Playlist,
    songs: List<Song>,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCover(
                playlist = playlist,
                songs = songs,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${playlist.items.size} songs",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
