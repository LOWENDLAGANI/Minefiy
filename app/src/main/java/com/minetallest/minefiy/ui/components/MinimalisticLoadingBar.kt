package com.minetallest.minefiy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun MinimalisticLoadingBar(
    modifier: Modifier = Modifier,
    color: Color,
    backgroundColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minimalistic_loading_bar_transition")

    // This animation will drive the position of the moving bar.
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "minimalistic_loading_bar_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth(0.6f) // Make the bar 60% of the parent width
            .height(4.dp)
    ) {
        val canvasWidth = size.width
        val barWidth = canvasWidth * 0.4f // The moving part is 40% of the bar's width
        val startX = (canvasWidth + barWidth) * progress - barWidth

        // Background line
        drawLine(
            color = backgroundColor,
            start = Offset(0f, center.y),
            end = Offset(canvasWidth, center.y),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )

        // The moving line that animates across the background
        drawLine(
            color = color,
            start = Offset(startX.coerceIn(0f, canvasWidth - barWidth), center.y),
            end = Offset((startX + barWidth).coerceIn(barWidth, canvasWidth), center.y),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
    }
}