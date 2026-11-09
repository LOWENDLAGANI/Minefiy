package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.minetallest.minefiy.R
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun FullScreenPlayer(viewModel: MainViewModel, onCollapse: () -> Unit) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    currentSong?.let { song ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF21000C))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.Close, contentDescription = "Collapse", tint = Color.White)
                }
                Text("Now Playing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add to playlist", tint = Color.White)
                }
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
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            )

            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = song.artist,
                    fontSize = 18.sp,
                    color = Color.LightGray
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0f),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatDuration(currentPosition), color = Color.White)
                    Text(text = formatDuration(totalDuration), color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::toggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = viewModel::skipPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp), tint = Color.White)
                }
                IconButton(onClick = viewModel::playPause, modifier = Modifier.size(72.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )
                }
                IconButton(onClick = viewModel::skipNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp), tint = Color.White)
                }
                IconButton(onClick = viewModel::toggleRepeatMode) {
                    Icon(
                        imageVector = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAddToPlaylistDialog) {
        currentSong?.let { song ->
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialog = false },
                onSelectPlaylist = { playlist ->
                    viewModel.addSongToPlaylist(song, playlist)
                    showAddToPlaylistDialog = false
                }
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
