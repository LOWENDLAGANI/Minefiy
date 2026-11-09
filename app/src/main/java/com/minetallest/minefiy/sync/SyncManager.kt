package com.minetallest.minefiy.sync

import com.minetallest.minefiy.b2.B2Manager
import com.minetallest.minefiy.firebase.FirebaseManager
import com.minetallest.minefiy.metadata.MetadataExtractor
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class SyncManager(
    private val b2Manager: B2Manager,
    private val firebaseManager: FirebaseManager,
    private val metadataExtractor: MetadataExtractor
) {

    suspend fun sync() {
        val storageFiles = b2Manager.listMp3s()
        val firebaseSongs = firebaseManager.getSongs().associateBy { it.key }

        storageFiles.asFlow().map { storageFile ->
            val existingSong = firebaseSongs[storageFile.key]
            if (existingSong == null || existingSong.checksum != storageFile.checksum) {
                val streamUrl = b2Manager.getPresignedUrl(storageFile.key)
                metadataExtractor.extractMetadata(storageFile, streamUrl)?.let { (song, artworkData) ->
                    artworkData?.let { b2Manager.uploadArtwork(song.key, it) }
                    firebaseManager.addSong(song)
                }
            }
        }.toList()

        val storageKeys = storageFiles.map { it.key }.toSet()
        val songsToDelete = firebaseSongs.values.filter { it.key !in storageKeys }
        songsToDelete.asFlow().map { song ->
            firebaseManager.deleteSong(song)
        }.toList()
    }

    suspend fun syncNewSongs() {
        val storageFiles = b2Manager.listMp3s()
        val firebaseSongs = firebaseManager.getSongs().associateBy { it.key }

        storageFiles.asFlow().map { storageFile ->
            if (!firebaseSongs.containsKey(storageFile.key)) {
                val streamUrl = b2Manager.getPresignedUrl(storageFile.key)
                metadataExtractor.extractMetadata(storageFile, streamUrl)?.let { (song, artworkData) ->
                    artworkData?.let { b2Manager.uploadArtwork(song.key, it) }
                    firebaseManager.addSong(song)
                }
            }
        }.toList()
    }

    suspend fun clearDatabase() {
        firebaseManager.clearAllSongs()
    }
}