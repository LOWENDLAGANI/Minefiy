package com.minetallest.minefiy.playback

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Playlist(
    val id: String = "",
    val name: String = "",
    val items: List<String> = emptyList()
)
