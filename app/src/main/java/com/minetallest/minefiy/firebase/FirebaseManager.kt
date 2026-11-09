package com.minetallest.minefiy.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.minetallest.minefiy.BuildConfig
import com.minetallest.minefiy.data.Artist
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.data.SongSelection
import com.minetallest.minefiy.playback.Playlist
import kotlinx.coroutines.tasks.await

class FirebaseManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val adminEmail: String

    companion object {
        const val LIKED_SONGS_PLAYLIST_ID = "liked_songs"
    }

    init {
        adminEmail = BuildConfig.ADMIN_EMAIL
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun isAdmin(): Boolean = auth.currentUser?.email == adminEmail

    fun getUserEmail(): String? = auth.currentUser?.email

    fun getDisplayName(): String? = auth.currentUser?.displayName

    suspend fun updateDisplayName(newName: String) {
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()
        user?.updateProfile(profileUpdates)?.await()
    }

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            if (authResult.user?.isEmailVerified == true) {
                Result.success(Unit)
            } else {
                auth.signOut() // Ensure user is signed out if email is not verified
                Result.failure(Exception("Please verify your email before logging in."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            authResult.user?.sendEmailVerification()?.await()
            auth.signOut() // Sign out immediately after registration
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getSongs(): List<Song> {
        val snapshot = db.collection("songs").get().await()
        return snapshot.toObjects(Song::class.java)
    }

    suspend fun addSong(song: Song) {
        db.collection("songs").document(song.key).set(song).await()
    }

    suspend fun deleteSong(song: Song) {
        db.collection("songs").document(song.key).delete().await()
    }

    suspend fun clearAllSongs() {
        val snapshot = db.collection("songs").get().await()
        for (document in snapshot.documents) {
            document.reference.delete().await()
        }
    }

    suspend fun addArtist(artist: Artist) {
        db.collection("artists").document(artist.name).set(artist).await()
    }

    suspend fun getArtists(): List<Artist> {
        val snapshot = db.collection("artists").get().await()
        return snapshot.toObjects(Artist::class.java)
    }

    suspend fun deleteArtist(artist: Artist) {
        db.collection("artists").document(artist.name).delete().await()
    }

    suspend fun createDefaultLikedSongsPlaylistIfNotExist() {
        auth.currentUser?.uid?.let { userId ->
            val likedSongsRef = db.collection("users").document(userId).collection("playlists").document(LIKED_SONGS_PLAYLIST_ID)
            val snapshot = likedSongsRef.get().await()
            if (!snapshot.exists()) {
                val likedSongsPlaylist = Playlist(id = LIKED_SONGS_PLAYLIST_ID, name = "Liked Songs", items = emptyList())
                savePlaylist(likedSongsPlaylist)
            }
        }
    }

    suspend fun savePlaylist(playlist: Playlist) {
        auth.currentUser?.uid?.let {
            db.collection("users").document(it).collection("playlists").document(playlist.id).set(playlist).await()
        }
    }

    suspend fun getPlaylists(): List<Playlist> {
        return auth.currentUser?.uid?.let {
            try {
                val snapshot = db.collection("users").document(it).collection("playlists").get().await()
                snapshot.toObjects(Playlist::class.java)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun deletePlaylist(playlistId: String) {
        if (playlistId == LIKED_SONGS_PLAYLIST_ID) return

        auth.currentUser?.uid?.let {
            db.collection("users").document(it).collection("playlists").document(playlistId).delete().await()
        }
    }

    suspend fun saveListeningHistory(history: List<String>) {
        auth.currentUser?.uid?.let {
            val historyData = mapOf("songKeys" to history)
            db.collection("users").document(it).collection("history").document("listening").set(historyData).await()
        }
    }

    suspend fun getListeningHistory(): List<String> {
        return auth.currentUser?.uid?.let {
            try {
                val snapshot = db.collection("users").document(it).collection("history").document("listening").get().await()
                (snapshot.get("songKeys") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun saveSongSelection(songSelection: SongSelection) {
        db.collection("songSelections").document(songSelection.id).set(songSelection).await()
    }

    suspend fun getSongSelections(): List<SongSelection> {
        return try {
            val snapshot = db.collection("songSelections").get().await()
            snapshot.toObjects(SongSelection::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteSongSelection(selectionId: String) {
        db.collection("songSelections").document(selectionId).delete().await()
    }
}