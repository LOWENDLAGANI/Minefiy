package com.minetallest.minefiy.ui.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.minetallest.minefiy.b2.B2Manager
import com.minetallest.minefiy.data.Artist
import com.minetallest.minefiy.data.SearchResult
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.data.SongSelection
import com.minetallest.minefiy.firebase.FirebaseManager
import com.minetallest.minefiy.metadata.MetadataExtractor
import com.minetallest.minefiy.playback.PlaybackService
import com.minetallest.minefiy.playback.Playlist
import com.minetallest.minefiy.sync.SyncManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var mediaControllerFuture: ListenableFuture<MediaController>
    private val firebaseManager = FirebaseManager()
    private val b2Manager = B2Manager()
    private val syncManager: SyncManager

    private val castContext: CastContext by lazy {
        CastContext.getSharedInstance(application)
    }
    private var castSession: CastSession? = null
    private val sessionManagerListener: SessionManagerListener<CastSession> = SessionManagerListenerImpl()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _songSelections = MutableStateFlow<List<SongSelection>>(emptyList())
    val songSelections: StateFlow<List<SongSelection>> = _songSelections.asStateFlow()

    private val _featuredSongs = MutableStateFlow<List<Song>>(emptyList())
    val featuredSongs: StateFlow<List<Song>> = _featuredSongs.asStateFlow()

    private val _newlyAddedSongs = MutableStateFlow<List<Song>>(emptyList())
    val newlyAddedSongs: StateFlow<List<Song>> = _newlyAddedSongs.asStateFlow()

    private val _listeningHistory = MutableStateFlow<List<Song>>(emptyList())
    val listeningHistory: StateFlow<List<Song>> = _listeningHistory.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isInitiallyLoading = MutableStateFlow(true)
    val isInitiallyLoading: StateFlow<Boolean> = _isInitiallyLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Initializing...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSyncingNewSongs = MutableStateFlow(false)
    val isSyncingNewSongs: StateFlow<Boolean> = _isSyncingNewSongs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _showNoInternetDialog = MutableStateFlow(false)
    val showNoInternetDialog: StateFlow<Boolean> = _showNoInternetDialog.asStateFlow()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _showNoInternetDialog.value = false
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _showNoInternetDialog.value = true
        }
    }

    val searchResults: StateFlow<List<SearchResult>> = _searchQuery
        .combine(songs) { query, songList ->
            if (query.isBlank()) {
                emptyList()
            } else {
                val songResults = songList.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
                    .map { SearchResult.SongResult(it) }
                val artistResults = artists.value.filter { it.name.contains(query, ignoreCase = true) }
                    .map { SearchResult.ArtistResult(it) }
                artistResults + songResults
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allArtistNames: StateFlow<List<String>> = songs.map { songs ->
        songs.map { it.artist }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState()
            mediaItem?.mediaId?.let { addToHistory(it) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = playbackState
            _isLoading.value = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_READY) {
                _totalDuration.value = mediaControllerFuture.get()?.duration?.coerceAtLeast(0) ?: 0L
            }
        }
    }

    private val remoteMediaClientListener = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            val playerState = castSession?.remoteMediaClient?.playerState ?: return
            _isPlaying.value = playerState == com.google.android.gms.cast.MediaStatus.PLAYER_STATE_PLAYING
            _currentPosition.value = castSession?.remoteMediaClient?.approximateStreamPosition ?: 0
            _totalDuration.value = castSession?.remoteMediaClient?.mediaInfo?.streamDuration ?: 0
        }
    }

    init {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        mediaControllerFuture.addListener(
            {
                val mediaController = mediaControllerFuture.get()
                mediaController.addListener(playerListener)
                updateState()
                viewModelScope.launch {
                    while (true) {
                        if (castSession == null || !castSession!!.isConnected) {
                            _currentPosition.value = mediaController.currentPosition.coerceAtLeast(0L)
                        }
                        delay(1000)
                    }
                }
            },
            MoreExecutors.directExecutor()
        )

        val metadataExtractor = MetadataExtractor(application)
        syncManager = SyncManager(b2Manager, firebaseManager, metadataExtractor)

        _isAdmin.value = firebaseManager.isAdmin()

        _userEmail.value = firebaseManager.getUserEmail() ?: ""
        _displayName.value = firebaseManager.getDisplayName() ?: ""

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        if (isUserLoggedIn()) {
            if (isNetworkAvailable()) {
                loadInitialData()
            } else {
                _isOnline.value = false
                _showNoInternetDialog.value = true
                _isInitiallyLoading.value = false
            }
        } else {
            _isInitiallyLoading.value = false
        }

        castContext.sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    fun dismissNoInternetDialog() {
        _showNoInternetDialog.value = false
    }

    fun isUserLoggedIn(): Boolean = firebaseManager.isUserLoggedIn()

    fun login(email: String, pass: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = firebaseManager.login(email, pass)
            onResult(result)
            if (result.isSuccess) {
                _isAdmin.value = firebaseManager.isAdmin()
                _userEmail.value = firebaseManager.getUserEmail() ?: ""
                _displayName.value = firebaseManager.getDisplayName() ?: ""
                firebaseManager.createDefaultLikedSongsPlaylistIfNotExist()
                loadInitialData()
            }
        }
    }

    fun loginWithGoogle(idToken: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = firebaseManager.signInWithGoogle(idToken)
            onResult(result)
            if (result.isSuccess) {
                _isAdmin.value = firebaseManager.isAdmin()
                _userEmail.value = firebaseManager.getUserEmail() ?: ""
                _displayName.value = firebaseManager.getDisplayName() ?: ""
                firebaseManager.createDefaultLikedSongsPlaylistIfNotExist()
                loadInitialData()
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = firebaseManager.sendPasswordResetEmail(email)
            onResult(result)
        }
    }

    fun register(email: String, pass: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = firebaseManager.register(email, pass)
            onResult(result)
        }
    }

    fun logout() {
        firebaseManager.logout()
        _songs.value = emptyList()
        _artists.value = emptyList()
        _playlists.value = emptyList()
        _featuredSongs.value = emptyList()
        _newlyAddedSongs.value = emptyList()
        _listeningHistory.value = emptyList()
        _currentSong.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun loadInitialData() {
        if (!_isOnline.value) {
            _showNoInternetDialog.value = true
            _isInitiallyLoading.value = false
            return
        }
        viewModelScope.launch {
            _isInitiallyLoading.value = true
            _loadingMessage.value = "Loading songs..."
            val songsFromDb = firebaseManager.getSongs()
            _loadingMessage.value = "Loading artists..."
            val artistsFromDb = firebaseManager.getArtists()

            _loadingMessage.value = "Loading artwork..."
            val songsWithArt = songsFromDb.map { song ->
                async { song.copy(artworkUrl = b2Manager.getPresignedUrl("artwork/${song.key}.jpg")) }
            }.awaitAll()

            val artistsWithArt = artistsFromDb.map { artist ->
                async { artist.copy(imageUrl = b2Manager.getPresignedUrl("artists/${artist.name}.jpg")) }
            }.awaitAll()

            _songs.value = songsWithArt
            _artists.value = artistsWithArt
            _featuredSongs.value = songsWithArt.shuffled().take(10)
            _newlyAddedSongs.value = songsWithArt.sortedByDescending { it.key }.take(10)
            _loadingMessage.value = "Loading playlists..."
            loadPlaylists()
            _loadingMessage.value = "Loading history..."
            loadListeningHistory()
            _loadingMessage.value = "Loading selections..."
            loadSongSelections()
            _isInitiallyLoading.value = false
        }
    }

    fun addArtist(name: String, artworkData: ByteArray) {
        viewModelScope.launch {
            val imageUrl = b2Manager.uploadArtistArtwork(name, artworkData)
            val newArtist = Artist(name, imageUrl)
            firebaseManager.addArtist(newArtist)
            _artists.value = _artists.value + newArtist
        }
    }

    fun deleteArtist(artist: Artist) {
        viewModelScope.launch {
            firebaseManager.deleteArtist(artist)
            _artists.value = _artists.value.filterNot { it.name == artist.name }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _playlists.value = firebaseManager.getPlaylists()
        }
    }

    private fun loadListeningHistory() {
        viewModelScope.launch {
            val historyKeys = firebaseManager.getListeningHistory()
            _listeningHistory.value = historyKeys.mapNotNull { key ->
                _songs.value.find { it.key == key }
            }
        }
    }

    private fun loadSongSelections() {
        viewModelScope.launch {
            _songSelections.value = firebaseManager.getSongSelections()
        }
    }

    private fun addToHistory(songId: String) {
        viewModelScope.launch {
            val currentHistory = _listeningHistory.value.map { it.key }.toMutableList()
            if (currentHistory.contains(songId)) {
                currentHistory.remove(songId)
            }
            currentHistory.add(0, songId)
            val updatedHistory = currentHistory.take(20)
            firebaseManager.saveListeningHistory(updatedHistory)
            _listeningHistory.value = updatedHistory.mapNotNull { key ->
                _songs.value.find { it.key == key }
            }
        }
    }

    fun clearSongHistory() {
        viewModelScope.launch {
            firebaseManager.saveListeningHistory(emptyList())
            _listeningHistory.value = emptyList()
        }
    }

    fun createPlaylist(name: String, songIds: List<String>) {
        viewModelScope.launch {
            val newPlaylist = Playlist(id = System.currentTimeMillis().toString(), name = name, items = songIds)
            firebaseManager.savePlaylist(newPlaylist)
            _playlists.value = _playlists.value + newPlaylist
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            firebaseManager.deletePlaylist(playlist.id)
            _playlists.value = _playlists.value.filterNot { it.id == playlist.id }
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        viewModelScope.launch {
            val updatedPlaylist = playlist.copy(name = newName)
            firebaseManager.savePlaylist(updatedPlaylist)
            val playlistIndex = _playlists.value.indexOfFirst { it.id == playlist.id }
            if (playlistIndex != -1) {
                val updatedPlaylists = _playlists.value.toMutableList()
                updatedPlaylists[playlistIndex] = updatedPlaylist
                _playlists.value = updatedPlaylists
            }
        }
    }

    fun createSongSelection(name: String): String {
        val newSelection = SongSelection(id = System.currentTimeMillis().toString(), name = name)
        viewModelScope.launch {
            firebaseManager.saveSongSelection(newSelection)
            _songSelections.value = _songSelections.value + newSelection
        }
        return newSelection.id
    }

    fun deleteSongSelection(selection: SongSelection) {
        viewModelScope.launch {
            firebaseManager.deleteSongSelection(selection.id)
            _songSelections.value = _songSelections.value.filterNot { it.id == selection.id }
        }
    }

    fun updateSongSelection(selectionId: String, songIds: List<String>) {
        val selectionIndex = _songSelections.value.indexOfFirst { it.id == selectionId }
        if (selectionIndex != -1) {
            val updatedSelection = _songSelections.value[selectionIndex].copy(items = songIds)
            viewModelScope.launch {
                firebaseManager.saveSongSelection(updatedSelection)
                val updatedSelections = _songSelections.value.toMutableList()
                updatedSelections[selectionIndex] = updatedSelection
                _songSelections.value = updatedSelections
            }
        }
    }

    fun addSongToPlaylist(song: Song, playlist: Playlist) {
        if (playlist.items.contains(song.key)) return

        viewModelScope.launch {
            val updatedPlaylist = playlist.copy(items = playlist.items + song.key)
            firebaseManager.savePlaylist(updatedPlaylist)
            val playlistIndex = _playlists.value.indexOfFirst { it.id == playlist.id }
            if (playlistIndex != -1) {
                val updatedPlaylists = _playlists.value.toMutableList()
                updatedPlaylists[playlistIndex] = updatedPlaylist
                _playlists.value = updatedPlaylists
            }
        }
    }

    fun removeSongFromPlaylist(song: Song, playlist: Playlist) {
        viewModelScope.launch {
            val updatedPlaylist = playlist.copy(items = playlist.items - song.key)
            firebaseManager.savePlaylist(updatedPlaylist)
            val playlistIndex = _playlists.value.indexOfFirst { it.id == playlist.id }
            if (playlistIndex != -1) {
                val updatedPlaylists = _playlists.value.toMutableList()
                updatedPlaylists[playlistIndex] = updatedPlaylist
                _playlists.value = updatedPlaylists
            }
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            firebaseManager.updateDisplayName(newName)
            _displayName.value = newName
        }
    }

    fun playPlaylist(playlist: Playlist) {
        val playlistSongs = playlist.items.mapNotNull { songId ->
            _songs.value.find { it.key == songId }
        }

        if (playlistSongs.isNotEmpty()) {
            val mediaController = mediaControllerFuture.get() ?: return
            mediaController.setMediaItems(playlistSongs.map { it.toMediaItem() })
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun playPlaylistShuffled(playlist: Playlist) {
        val playlistSongs = playlist.items.mapNotNull { songId ->
            _songs.value.find { it.key == songId }
        }.shuffled()

        if (playlistSongs.isNotEmpty()) {
            val mediaController = mediaControllerFuture.get() ?: return
            mediaController.setMediaItems(playlistSongs.map { it.toMediaItem() })
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun syncSongs() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncManager.sync()
            loadInitialData()
            _isSyncing.value = false
        }
    }

    fun syncNewSongs() {
        viewModelScope.launch {
            _isSyncingNewSongs.value = true
            syncManager.syncNewSongs()
            loadInitialData()
            _isSyncingNewSongs.value = false
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            firebaseManager.deleteSong(song)
            _songs.value = _songs.value.filterNot { it.key == song.key }
        }
    }

    private fun updateState() {
        val mediaController = mediaControllerFuture.get() ?: return
        _isPlaying.value = mediaController.isPlaying
        _shuffleModeEnabled.value = mediaController.shuffleModeEnabled
        _repeatMode.value = mediaController.repeatMode
        _totalDuration.value = mediaController.duration.coerceAtLeast(0L)
        mediaController.currentMediaItem?.mediaId?.let {
            val song = _songs.value.find { song -> song.key == it }
            _currentSong.value = song
        }
    }

    fun playSong(song: Song) {
        val songIndex = _songs.value.indexOfFirst { it.key == song.key }
        if (songIndex == -1) return

        if (castSession != null && castSession!!.isConnected) {
            viewModelScope.launch {
                val presignedUrl = b2Manager.getPresignedUrl(song.key)
                val mediaInfo = com.google.android.gms.cast.MediaInfo.Builder(presignedUrl)
                    .setStreamType(com.google.android.gms.cast.MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType("audio/mpeg")
                    .setMetadata(com.google.android.gms.cast.MediaMetadata(com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
                        putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE, song.title)
                        putString(com.google.android.gms.cast.MediaMetadata.KEY_ARTIST, song.artist)
                        song.artworkUrl?.let { addImage(com.google.android.gms.common.images.WebImage(Uri.parse(it))) }
                    })
                    .build()
                castSession?.remoteMediaClient?.load(mediaInfo, true)
            }
        } else {
            val mediaController = mediaControllerFuture.get() ?: return
            mediaController.setMediaItems(_songs.value.map { it.toMediaItem() }, songIndex, 0)
            mediaController.prepare()
            mediaController.play()
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(artworkUrl ?: "")
            .setMediaId(this.key)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(this.title)
                    .setArtist(this.artist)
                    .setArtworkUri(Uri.parse(artworkUrl))
                    .build()
            )
            .build()
    }

    fun playPause() {
        if (castSession != null && castSession!!.isConnected) {
            castSession?.remoteMediaClient?.togglePlayback()
        } else {
            val mediaController = mediaControllerFuture.get() ?: return
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
        }
    }

    fun skipNext() {
        if (castSession != null && castSession!!.isConnected) {
            castSession?.remoteMediaClient?.seek(0) // Simplified skip
        } else {
            mediaControllerFuture.get()?.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (castSession != null && castSession!!.isConnected) {
            castSession?.remoteMediaClient?.seek(0) // Simplified skip
        } else {
            mediaControllerFuture.get()?.seekToPreviousMediaItem()
        }
    }

    fun seekForward() {
        if (castSession != null && castSession!!.isConnected) {
            castSession?.remoteMediaClient?.seek(castSession?.remoteMediaClient?.approximateStreamPosition?.plus(10000) ?: 0)
        } else {
            mediaControllerFuture.get()?.let { it.seekTo(it.currentPosition + 10000) }
        }
    }

    fun seekBackward() {
        if (castSession != null && castSession!!.isConnected) {
            castSession?.remoteMediaClient?.seek(castSession?.remoteMediaClient?.approximateStreamPosition?.minus(10000) ?: 0)
        } else {
            mediaControllerFuture.get()?.let { it.seekTo(it.currentPosition - 10000) }
        }
    }

    fun toggleShuffle() {
        // Shuffle functionality for Cast is handled differently, not implemented here for simplicity
        if (castSession == null || !castSession!!.isConnected) {
            val mediaController = mediaControllerFuture.get() ?: return
            mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
        }
    }

    fun toggleRepeatMode() {
        // Repeat mode functionality for Cast is handled differently, not implemented here for simplicity
        if (castSession == null || !castSession!!.isConnected) {
            val mediaController = mediaControllerFuture.get() ?: return
            mediaController.repeatMode = when (mediaController.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun seekTo(position: Long) {
        if (castSession != null && castSession!!.isConnected) {
            castSession?.remoteMediaClient?.seek(position)
        } else {
            mediaControllerFuture.get()?.seekTo(position)
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        castContext.sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        mediaControllerFuture.get()?.removeListener(playerListener)
        MediaController.releaseFuture(mediaControllerFuture)
    }

    private inner class SessionManagerListenerImpl : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            castSession?.remoteMediaClient?.registerCallback(remoteMediaClientListener)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            castSession?.remoteMediaClient?.registerCallback(remoteMediaClientListener)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession?.remoteMediaClient?.unregisterCallback(remoteMediaClientListener)
            castSession = null
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            castSession?.remoteMediaClient?.unregisterCallback(remoteMediaClientListener)
            castSession = null
        }

        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }
}
