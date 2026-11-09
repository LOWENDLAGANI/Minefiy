package com.minetallest.minefiy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minetallest.minefiy.ui.viewmodels.MainViewModel

@Composable
fun AccountScreen(viewModel: MainViewModel, onLogout: () -> Unit, onSettingsClick: () -> Unit, onAdminPanelClick: () -> Unit) {
    val isAdmin by viewModel.isAdmin.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onSettingsClick) {
            Text("Settings")
        }
        Button(onClick = {
            viewModel.logout()
            onLogout()
        }) {
            Text("Log Out")
        }
        if (isAdmin) {
            Button(onClick = onAdminPanelClick) {
                Text("Admin Panel")
            }
        }
    }
}
