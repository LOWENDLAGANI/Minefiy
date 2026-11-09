package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minetallest.minefiy.R
import com.minetallest.minefiy.data.Artist
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.firebase.FirebaseManager
import com.minetallest.minefiy.playback.Playlist
import com.minetallest.minefiy.ui.components.CastButton
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (String) -> Unit,
    onAccountClick: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val artists by viewModel.artists.collectAsState()
    val featuredSongs by viewModel.featuredSongs.collectAsState()
    val listeningHistory by viewModel.listeningHistory.collectAsState()
    val displayName by viewModel.displayName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hello, ${displayName.ifEmpty { "There" }} 😊") },
                navigationIcon = {
                    IconButton(onClick = onAccountClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Account", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    CastButton()
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { ArtistSection("Artist", artists, onArtistClick) }
            item { SongSection("Today's hits", featuredSongs, viewModel) }
            item { SongSection("Recently played", listeningHistory, viewModel) }
        }
    }
}

@Composable
fun ArtistSection(title: String, artists: List<Artist>, onArtistClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists) { artist ->
                ArtistCard(artist = artist, onClick = { onArtistClick(artist.name) })
            }
        }
    }
}

@Composable
fun ArtistCard(artist: Artist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
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
                .size(80.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SongSection(title: String, songs: List<Song>, viewModel: MainViewModel) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(songs) { song ->
                SongCard(song = song, onClick = { viewModel.playSong(song) })
            }
        }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
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
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PlaylistCover(playlist: Playlist, songs: List<Song>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (playlist.id == FirebaseManager.LIKED_SONGS_PLAYLIST_ID) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF8A2BE2), Color(0xFF4169E1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.liked_songs_artwork),
                    contentDescription = "Liked Songs",
                    modifier = Modifier.fillMaxSize(0.5f),
                    tint = Color.White
                )
            }
        } else {
            when (songs.size) {
                0 -> PlaylistCoverPlaceholder()
                1 -> {
                    PlaylistGridItem(
                        imageUrl = songs[0].artworkUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            PlaylistGridItem(
                                imageUrl = songs.getOrNull(0)?.artworkUrl,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            PlaylistGridItem(
                                imageUrl = songs.getOrNull(1)?.artworkUrl,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.weight(1f)) {
                            PlaylistGridItem(
                                imageUrl = songs.getOrNull(2)?.artworkUrl,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            PlaylistGridItem(
                                imageUrl = songs.getOrNull(3)?.artworkUrl,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistGridItem(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Playlist Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlaylistCoverPlaceholder()
        }
    }
}

@Composable
fun PlaylistCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF6A006A), Color(0xFF0D0D2B))
                )
            )
    )
}

@Composable
fun PlaylistHeader(playlist: Playlist, songs: List<Song>, viewModel: MainViewModel) {
    val displayName by viewModel.displayName.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistCover(
            playlist = playlist,
            songs = songs,
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Playlist", style = MaterialTheme.typography.labelSmall)
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Curator",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${displayName.ifEmpty { "Minetallest" }} • ${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
