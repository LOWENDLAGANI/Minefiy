package com.minetallest.minefiy.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minetallest.minefiy.R
import com.minetallest.minefiy.data.Artist
import com.minetallest.minefiy.data.SongSelection
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@Composable
fun AdminPanelScreen(viewModel: MainViewModel, onSelectionClick: (String) -> Unit) {
    val songSelections by viewModel.songSelections.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val allArtistNames by viewModel.allArtistNames.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isSyncingNewSongs by viewModel.isSyncingNewSongs.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddArtistDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = { viewModel.syncSongs() },
            enabled = !isSyncing && !isSyncingNewSongs,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Sync Songs")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.syncNewSongs() },
            enabled = !isSyncing && !isSyncingNewSongs,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncingNewSongs) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Sync New Songs")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Song Selection")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showAddArtistDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Artist")
        }

        Text("Artists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(artists) { artist ->
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    ArtistListItem(artist = artist, onItemClick = {}, onItemLongClick = { showMenu = true })

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                viewModel.deleteArtist(artist)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        )
                    }
                }
            }
        }

        Text("Song Selections", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(songSelections) { selection ->
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    SongSelectionItem(
                        selection = selection,
                        onItemClick = { onSelectionClick(selection.id) },
                        onItemLongClick = { showMenu = true }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                viewModel.deleteSongSelection(selection)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSongSelectionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                val newId = viewModel.createSongSelection(name)
                showCreateDialog = false
                onSelectionClick(newId)
            }
        )
    }

    if (showAddArtistDialog) {
        AddArtistDialog(
            artistNames = allArtistNames,
            onDismiss = { showAddArtistDialog = false },
            onAdd = { name, artworkUri ->
                viewModel.addArtist(name, artworkUri)
                showAddArtistDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistListItem(
    artist: Artist,
    onItemClick: () -> Unit,
    onItemLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onItemClick, onLongClick = onItemLongClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Artist Image",
                placeholder = painterResource(id = R.drawable.ic_music_note),
                error = painterResource(id = R.drawable.ic_music_note),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = artist.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongSelectionItem(
    selection: SongSelection,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onItemClick, onLongClick = onItemLongClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Song Selection",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = selection.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun CreateSongSelectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Song Selection") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Selection Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onCreate(text)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddArtistDialog(
    artistNames: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, ByteArray) -> Unit
) {
    var selectedArtistName by remember { mutableStateOf("") }
    var artworkUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> artworkUri = uri }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Artist") },
        text = {
            Column {
                Box {
                    TextField(
                        value = selectedArtistName,
                        onValueChange = { selectedArtistName = it },
                        readOnly = true,
                        label = { Text("Artist Name") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Artist")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        artistNames.forEach { artistName ->
                            DropdownMenuItem(
                                text = { Text(artistName) },
                                onClick = {
                                    selectedArtistName = artistName
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Select Artwork")
                }
                artworkUri?.let {
                    Text("Artwork selected", modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedArtistName.isNotBlank() && artworkUri != null) {
                        context.contentResolver.openInputStream(artworkUri!!)?.use { inputStream ->
                            onAdd(selectedArtistName, inputStream.readBytes())
                        }
                    }
                },
                enabled = selectedArtistName.isNotBlank() && artworkUri != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
