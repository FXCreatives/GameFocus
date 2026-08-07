package com.gamefocus.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gamefocus.ui.theme.Divider

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Divider.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.1f),
            Divider.copy(alpha = 0.3f)
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 200f, 0f)
    )

    Box(
        modifier = modifier.background(shimmerBrush, RoundedCornerShape(8.dp))
    )
}
