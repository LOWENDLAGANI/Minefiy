package com.minetallest.minefiy.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel, 
    onLogout: () -> Unit, 
    onAdminPanelClick: () -> Unit,
    onShowRewardedAd: () -> Unit,
    onAboutClick: () -> Unit // Added onAboutClick parameter
) {
    val userEmail by viewModel.userEmail.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Profile Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile Icon",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(text = displayName, style = MaterialTheme.typography.titleLarge)
                        Text(text = userEmail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Items
            SettingsItem(
                icon = Icons.Default.History,
                title = "Clear Song History",
                onClick = { viewModel.clearSongHistory() }
            )
            SettingsItem(
                icon = Icons.Default.SupportAgent,
                title = "Contact Support",
                onClick = { contactSupport(context) }
            )
            SettingsItem(
                icon = Icons.Default.MonetizationOn,
                title = "Sync New Songs (Ad)",
                onClick = onShowRewardedAd
            )

            if (isAdmin) {
                SettingsItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Admin Panel",
                    onClick = onAdminPanelClick
                )
            }

            // New About section item
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                onClick = onAboutClick
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            SettingsItem(
                icon = Icons.Default.Logout,
                title = "Log Out",
                onClick = {
                    viewModel.logout()
                    onLogout()
                }
            )
        }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

private fun contactSupport(context: Context) {
    val email = "myrealmetvreal@gmail.com"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
    }
    context.startActivity(intent)
}
