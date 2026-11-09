package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun PlayerControls(viewModel: MainViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()

    currentSong?.let {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            // Song Info
            Text(
                text = it.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = it.artist,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Seek Bar
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatDuration(currentPosition))
                Text(text = formatDuration(totalDuration))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::toggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                IconButton(onClick = viewModel::skipPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }

                IconButton(onClick = viewModel::seekBackward) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                }

                IconButton(
                    onClick = viewModel::playPause,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(onClick = viewModel::seekForward) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }

                IconButton(onClick = viewModel::skipNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }

                IconButton(onClick = viewModel::toggleRepeatMode) {
                    Icon(
                        imageVector = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}