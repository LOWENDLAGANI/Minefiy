package com.minetallest.minefiy.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.minetallest.minefiy.R

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed to Top to allow content to flow down
    ) {
        Spacer(modifier = Modifier.height(64.dp)) // Add some space from the top
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Minefiy Logo",
            modifier = Modifier
                .size(96.dp) // Adjusted size for better fit
                .padding(bottom = 8.dp)
        )
        Text(
            text = "Minefiy",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp), // Adjusted font size
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Developed by Minetallest",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // Placeholder for version info. In a real app, this would be retrieved from BuildConfig.
        Text(
            text = "Version: 1.0.0 (Placeholder)",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                AboutItem(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    onClick = { navController.navigate("privacy_policy") }
                )
                AboutItem(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    onClick = { navController.navigate("terms_of_service") }
                )
                AboutItem(
                    icon = Icons.Default.Info,
                    title = "Open Source Licenses",
                    onClick = { /* TODO: Navigate to OSS licenses screen */ }
                )
                AboutItem(
                    icon = Icons.Default.Refresh,
                    title = "Check for Updates",
                    onClick = { /* TODO: Implement update check logic */ }
                )
                AboutItem(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    onClick = { shareApp(context) }
                )
            }
        }
    }
}

@Composable
fun AboutItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun shareApp(context: Context) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out Minefiy: your app link here") // Replace with actual app link
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@Preview(showBackground = true)
@Composable
fun PreviewAboutScreen() {
    // Provide a mock NavController for the preview
    AboutScreen(navController = rememberNavController())
}
