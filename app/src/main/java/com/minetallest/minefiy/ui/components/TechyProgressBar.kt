package com.minetallest.minefiy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TechyProgressBar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "techy-progress-bar")
    val shimmerPosition by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-position"
    )

    val trackColor = Color.White.copy(alpha = 0.1f)
    val progressColor = Color(0xFF6A006A)
    val shimmerColor = Color.White.copy(alpha = 0.5f)

    Canvas(modifier = modifier
        .height(12.dp)
        .fillMaxWidth()) {
        val barWidth = size.width
        val barHeight = size.height
        val cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)

        // Draw the background track
        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius
        )

        // Draw the animated shimmer
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    progressColor,
                    shimmerColor,
                    progressColor
                ),
                startX = barWidth * (shimmerPosition - 0.1f),
                endX = barWidth * shimmerPosition
            ),
            cornerRadius = cornerRadius
        )
    }
}
