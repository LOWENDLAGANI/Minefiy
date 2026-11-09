package com.minetallest.minefiy.data

import com.google.firebase.firestore.PropertyName

data class Artist(
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = ""
)