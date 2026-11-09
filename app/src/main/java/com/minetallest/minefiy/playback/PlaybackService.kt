package com.minetallest.minefiy.playback

import android.os.Bundle
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

    private var currentPlaylist: Playlist? = null

    @UnstableApi
    private val callback = object : MediaSession.Callback {
        override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = serviceScope.future {
                mediaItems.map { mediaItem ->
                    try {
                        val streamUrl = b2Manager.getPresignedUrl(mediaItem.mediaId)
                        MediaItem.Builder()
                            .setUri(streamUrl)
                            .setMediaId(mediaItem.mediaId)
                            .setMediaMetadata(mediaItem.mediaMetadata)
                            .build()
                    } catch (e: Exception) {
                        mediaItem // Keep the original if fetching fails
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