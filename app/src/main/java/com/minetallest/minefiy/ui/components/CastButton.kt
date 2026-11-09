package com.minetallest.minefiy.ui.components

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.minetallest.minefiy.R

@Composable
fun CastButton() {
    AndroidView(factory = { context ->
        val mediaRouteButton = MediaRouteButton(context)
        CastButtonFactory.setUpMediaRouteButton(context, mediaRouteButton)
        mediaRouteButton
    })
}
