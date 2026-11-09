package com.minetallest.minefiy.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Song(
    val key: String = "",
    val checksum: String = "",
    val title: String = "Unknown Title",
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    var artworkUrl: String? = null // This will be populated on-the-fly, not stored.
)