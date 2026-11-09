package com.minetallest.minefiy

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.Player
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minetallest.minefiy.ads.RewardedAdManager
import com.minetallest.minefiy.ui.screens.*
import com.minetallest.minefiy.ui.theme.MinefiyCloudFlareTheme
import com.minetallest.minefiy.ui.viewmodels.MainViewModel
import com.minetallest.minefiy.ui.viewmodels.MainViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }
    private lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        rewardedAdManager = RewardedAdManager(this)
        rewardedAdManager.loadRewardedAd()

        setContent {
            MinefiyCloudFlareTheme {
                val isInitiallyLoading by viewModel.isInitiallyLoading.collectAsState()
                val loadingMessage by viewModel.loadingMessage.collectAsState()
                val showNoInternetDialog by viewModel.showNoInternetDialog.collectAsState()

                if (showNoInternetDialog) {
                    NoInternetDialog(onDismiss = { viewModel.dismissNoInternetDialog() })
                }

                if (isInitiallyLoading && viewModel.isUserLoggedIn()) {
                    LoadingScreen(loadingMessage = loadingMessage)
                }
                else {
                    val navController = rememberNavController()
                    val startDestination = when {
                        !viewModel.isUserLoggedIn() -> "login"
                        viewModel.needsDataSourceSelection() -> "choose-source"
                        else -> "home"
                    }
                    var showFullScreenPlayer by remember { mutableStateOf(false) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val scope = rememberCoroutineScope()
                    val playbackState by viewModel.playbackState.collectAsState()

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val bottomNavRoutes = setOf("home", "search", "playlists")

                    Scaffold(
                        bottomBar = {
                            if (currentDestination?.route in bottomNavRoutes) {
                                Column {
                                    if (playbackState != Player.STATE_IDLE) {
                                        MiniPlayer(viewModel = viewModel) {
                                            showFullScreenPlayer = true
                                        }
                                    }
                                    NavigationBar {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                            label = { Text("Home") },
                                            selected = currentDestination?.route == "home",
                                            onClick = {
                                                navController.navigate("home") {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            label = { Text("Search") },
                                            selected = currentDestination?.route == "search",
                                            onClick = {
                                                navController.navigate("search") {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                            label = { Text("Playlists") },
                                            selected = currentDestination?.route == "playlists",
                                            onClick = {
                                                navController.navigate("playlists") {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            NavHost(navController = navController, startDestination = startDestination) {
                                composable("choose-source") {
                                    DataSourceSelectionScreen(
                                        viewModel = viewModel,
                                        canGoBack = false,
                                        onSelected = {
                                            navController.navigate("home") {
                                                popUpTo("choose-source") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("change-source") {
                                    DataSourceSelectionScreen(
                                        viewModel = viewModel,
                                        canGoBack = true,
                                        onBack = { navController.popBackStack() },
                                        onSelected = { navController.popBackStack() }
                                    )
                                }
                                composable("login") {
                                    LoginScreen(
                                        viewModel = viewModel,
                                        onLoginSuccess = {
                                            val dest = if (viewModel.needsDataSourceSelection()) "choose-source" else "home"
                                            navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                                        },
                                        onNavigateToRegister = { navController.navigate("register") }
                                    )
                                }
                                composable("register") {
                                    RegistrationScreen(viewModel = viewModel) {
                                        navController.popBackStack()
                                    }
                                }
                                composable("home") {
                                    NewHomeScreen(
                                        viewModel = viewModel,
                                        onPlaylistClick = { playlistId -> navController.navigate("playlist/$playlistId") },
                                        onAccountClick = { navController.navigate("settings") },
                                        onArtistClick = { artistName -> navController.navigate("artist/$artistName") }
                                    )
                                }
                                composable("playlists") {
                                    PlaylistScreen(viewModel = viewModel) { playlistId ->
                                        navController.navigate("playlist/$playlistId")
                                    }
                                }
                                composable("search") {
                                    SearchScreen(
                                        viewModel = viewModel,
                                        onArtistClick = { artistName ->
                                            navController.navigate("artist/$artistName")
                                        },
                                        onOpenYtAlbum = { browseId ->
                                            navController.navigate("yt-album/$browseId")
                                        },
                                        onOpenYtArtist = { channelId ->
                                            navController.navigate("yt-artist/$channelId")
                                        },
                                        onOpenFavorites = {
                                            navController.navigate("yt-favorites")
                                        }
                                    )
                                }
                                composable("yt-favorites") {
                                    YtFavoritesScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    "yt-album/{browseId}",
                                    arguments = listOf(navArgument("browseId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    YtAlbumScreen(
                                        browseId = backStackEntry.arguments?.getString("browseId") ?: "",
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    "yt-artist/{channelId}",
                                    arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    YtArtistScreen(
                                        channelId = backStackEntry.arguments?.getString("channelId") ?: "",
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        onOpenAlbum = { browseId ->
                                            navController.navigate("yt-album/$browseId")
                                        }
                                    )
                                }
                                composable(
                                    "playlist/{playlistId}",
                                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                                    PlaylistDetailScreen(playlistId = playlistId, viewModel = viewModel)
                                }
                                composable(
                                    "artist/{artistName}",
                                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                                    ArtistDetailScreen(artistName = artistName, viewModel = viewModel)
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onLogout = { navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true } } },
                                        onAdminPanelClick = { navController.navigate("admin") },
                                        onShowRewardedAd = { rewardedAdManager.showRewardedAd { viewModel.syncNewSongs() } },
                                        onAboutClick = { navController.navigate("about") },
                                        onMusicSourceClick = { navController.navigate("change-source") }
                                    )
                                }
                                composable("admin") {
                                    AdminPanelScreen(viewModel = viewModel) { selectionId ->
                                        navController.navigate("selectionEditor/$selectionId")
                                    }
                                }
                                composable(
                                    "selectionEditor/{selectionId}",
                                    arguments = listOf(navArgument("selectionId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val selectionId = backStackEntry.arguments?.getString("selectionId") ?: ""
                                        SongSelectionEditorScreen(selectionId = selectionId, viewModel = viewModel) {
                                        navController.popBackStack()
                                    }
                                }
                                composable("about") { // New AboutScreen composable
                                    AboutScreen(navController = navController)
                                }
                                composable("privacy_policy") {
                                    PrivacyPolicyScreen()
                                }
                                composable("terms_of_service") {
                                    TermsOfServiceScreen()
                                }
                            }
                        }
                    }

                    if (showFullScreenPlayer) {
                        ModalBottomSheet(
                            onDismissRequest = { showFullScreenPlayer = false },
                            sheetState = sheetState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FullScreenPlayer(viewModel = viewModel) {
                                scope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion { if (!sheetState.isVisible) showFullScreenPlayer = false }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoInternetDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No Internet Connection") },
        text = { Text("Please check your internet connection and try again.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
