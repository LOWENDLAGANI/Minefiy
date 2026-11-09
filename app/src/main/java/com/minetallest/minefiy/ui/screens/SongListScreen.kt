package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minetallest.minefiy.R
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListScreen(viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (isAdmin) {
                item {
                    Button(
                        onClick = { viewModel.syncSongs() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Text("Sync Songs")
                    }
                }
            }
            itemsIndexed(songs) { index, song ->
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    SongListItem(
                        trackNumber = index + 1,
                        song = song,
                        onItemClick = { viewModel.playSong(song) },
                        onItemLongClick = { showMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                viewModel.deleteSong(song)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    trackNumber: Int,
    song: Song,
    onItemClick: () -> Unit,
    onToggleSelection: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onItemLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (trackNumber > 0) {
            Text(
                text = trackNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
            placeholder = painterResource(id = R.drawable.ic_music_note),
            error = painterResource(id = R.drawable.ic_music_note),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        onToggleSelection?.let {
            IconButton(onClick = it) {
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Select")
                }
            }
        }
    }
}