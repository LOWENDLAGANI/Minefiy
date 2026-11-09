package com.minetallest.minefiy.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheUtil {

    @Volatile
    private var instance: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        return instance ?: synchronized(this) {
            instance ?: SimpleCache(
                File(context.cacheDir, "media"),
                LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), // 100MB cache
                StandaloneDatabaseProvider(context)
            ).also { instance = it }
        }
    }
}