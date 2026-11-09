package com.minetallest.minefiy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModernLoadingAnimation(modifier: Modifier = Modifier, text: String = "Loading...") {
    val infiniteTransition = rememberInfiniteTransition(label = "modern_loading_animation_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "modern_loading_animation_angle"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(degrees = angle) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF8E2DE2), // A nice purple
                            Color(0xFF4A00E0), // A darker purple
                            Color(0xFF8E2DE2)
                        )
                    ),
                )
            }
            drawCircle(
                color = Color.Black,
                radius = size.minDimension / 2.2f
            )
        }
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}