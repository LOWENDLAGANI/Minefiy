package com.minetallest.minefiy.playback

import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.minetallest.minefiy.R
import com.minetallest.minefiy.b2.B2Manager
import com.minetallest.minefiy.ytmusic.StreamResolver
import com.minetallest.minefiy.ytmusic.YtSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val b2Manager: B2Manager by lazy {
        B2Manager()
    }

    private val streamResolver: StreamResolver by lazy {
        StreamResolver()
    }

    private var currentPlaylist: Playlist? = null

    /**
     * B2 items resolve to a presigned URL; YouTube items (`yt:<videoId>`) are
     * resolved on-device through InnerTube — see [StreamResolver] for why that
     * must not happen on the backend.
     */
    private suspend fun streamUrlFor(mediaId: String): String =
        if (YtSong.isYouTubeMediaId(mediaId)) {
            streamResolver.resolve(YtSong.videoIdFromMediaId(mediaId))
        } else {
            b2Manager.getPresignedUrl(mediaId)
        }

    @UnstableApi
    private val callback = object : MediaSession.Callback {
        override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = serviceScope.future {
                mediaItems.map { mediaItem ->
                    try {
                        val streamUrl = streamUrlFor(mediaItem.mediaId)
                        MediaItem.Builder()
                            .setUri(streamUrl)
                            .setMediaId(mediaItem.mediaId)
                            .setMediaMetadata(mediaItem.mediaMetadata)
                            .build()
                    } catch (e: Exception) {
                        // Keep the original (URI-less) item so ExoPlayer surfaces
                        // a playback error instead of silently playing nothing.
                        Log.w("PlaybackService", "Could not resolve stream for ${mediaItem.mediaId}", e)
                        mediaItem
                    }
                }.toMutableList()
            }
            return updatedMediaItems
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val cache = CacheUtil.getCache(this)
        val upstreamDataSourceFactory = DefaultDataSource.Factory(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamDataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(cacheDataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).setCallback(callback).build()

        // Use the default provider and just set the notification icon
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this).build()
        notificationProvider.setSmallIcon(R.drawable.ic_notification_icon)
        setMediaNotificationProvider(notificationProvider)
    }

    fun setPlaylist(playlist: Playlist) {
        currentPlaylist = playlist
        mediaSession?.player?.clearMediaItems()
        serviceScope.launch {
            val mediaItems = playlist.items.map { itemId ->
                val streamUrl = b2Manager.getPresignedUrl(itemId)
                MediaItem.fromUri(streamUrl)
            }
            mediaSession?.player?.addMediaItems(mediaItems)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceJob.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}