package com.minetallest.minefiy.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class SongSelection(
    val id: String = "",
    val name: String = "",
    val items: List<String> = emptyList()
)
