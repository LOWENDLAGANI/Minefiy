package com.minetallest.minefiy.metadata

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.minetallest.minefiy.data.Song
import com.minetallest.minefiy.data.StorageFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MetadataExtractor(private val context: Context) {

    // This will now return the Song and the artwork data separately.
    suspend fun extractMetadata(storageFile: StorageFile, streamUrl: String): Pair<Song, ByteArray?>? {
        return try {
            val metadata = extractMetadataWithExoPlayer(streamUrl)
            val song = Song(
                key = storageFile.key,
                checksum = storageFile.checksum,
                title = metadata.title?.toString() ?: "Unknown Title",
                artist = metadata.artist?.toString() ?: "Unknown Artist",
                album = metadata.albumTitle?.toString() ?: "Unknown Album"
            )
            // Return the song and the artwork data
            Pair(song, metadata.artworkData)
        } catch (e: Exception) {
            Log.e("MetadataExtractor", "Error extracting metadata for ${storageFile.key}", e)
            null
        }
    }

    private suspend fun extractMetadataWithExoPlayer(url: String): MediaMetadata {
        val deferred = CompletableDeferred<MediaMetadata>()

        withContext(Dispatchers.Main) {
            val player = ExoPlayer.Builder(context).build()
            val listener = object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    if (deferred.isActive) deferred.complete(mediaMetadata)
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (deferred.isActive) deferred.completeExceptionally(error)
                }
            }
            player.addListener(listener)

            deferred.invokeOnCompletion {
                player.removeListener(listener)
                player.release()
            }

            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }

        return withTimeoutOrNull(15000) { // 15-second timeout
            deferred.await()
        } ?: throw Exception("Metadata extraction timed out for url: $url")
    }
}