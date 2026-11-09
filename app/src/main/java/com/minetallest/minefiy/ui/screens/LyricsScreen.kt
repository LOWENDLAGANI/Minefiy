package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import com.minetallest.minefiy.ytmusic.YtSong
import com.minetallest.minefiy.ytmusic.YtUiState

/**
 * Lyrics for a YouTube track, shown from the full-screen player.
 *
 * Lyrics are plain text: `ytmusicapi` only returns timestamps for a small
 * subset of tracks, so no synced highlighting is attempted.
 */
@Composable
fun LyricsDialog(
    song: YtSong,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.ytLyricsState.collectAsState()

    LaunchedEffect(song.videoId) {
        viewModel.loadYtLyrics(song)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearYtLyrics() }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearYtLyrics()
            onDismiss()
        },
        title = {
            Text(
                text = song.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 420.dp),
            ) {
                when (val current = state) {
                    is YtUiState.Success -> SelectionContainer {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = current.data,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 22.sp,
                                ),
                            )
                        }
                    }

                    is YtUiState.Error -> Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    is YtUiState.NotConfigured -> Text(
                        text = "Set `yt.proxy.base.url` in local.properties to load lyrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.clearYtLyrics()
                    onDismiss()
                },
            ) {
                Text("Close")
            }
        },
    )
}
