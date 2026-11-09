package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.data.DataSource
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

/**
 * First-launch onboarding + Settings change-source screen.
 *
 * Shown once when [MainViewModel.dataSource] is null (via route
 * "choose-source") and from Settings (route "change-source").
 * Choice is persisted in SharedPreferences via UserPreferencesManager
 * and is switchable any time in Settings.
 *
 * @param canGoBack true when launched from Settings (back returns
 *                  without forced choice), false for first-launch onboarding.
 * @param onSelected called after persisting choice; caller navigates.
 */
@Composable
fun DataSourceSelectionScreen(
    viewModel: MainViewModel,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onSelected: () -> Unit
) {
    val current by viewModel.dataSource.collectAsState()
    var pending by remember(current) { mutableStateOf(current) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Choose your music source",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You can change this any time in Settings. Pick one to get started.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        DataSourceCard(
            icon = Icons.Default.LibraryMusic,
            title = "Your Library (B2)",
            description = "Play your uploaded MP3s from Backblaze B2. " +
                "Works with your existing artists, playlists and history. " +
                "Best if you already have a library here.",
            selected = pending == DataSource.B2,
            recommended = true,
            onClick = { pending = DataSource.B2 }
        )
        Spacer(Modifier.height(16.dp))

        DataSourceCard(
            icon = Icons.Default.PlayCircle,
            title = "YouTube Music",
            description = "Search and stream YouTube Music. " +
                "Needs internet. No uploads needed. " +
                "Uses your yt-proxy backend for search.",
            selected = pending == DataSource.YOUTUBE_MUSIC,
            recommended = false,
            onClick = { pending = DataSource.YOUTUBE_MUSIC }
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                pending?.let {
                    viewModel.setDataSource(it)
                    onSelected()
                }
            },
            enabled = pending != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
        if (canGoBack) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun DataSourceCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        elevation = CardDefaults.cardElevation(if (selected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (recommended) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
