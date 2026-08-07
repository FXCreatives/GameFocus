package com.gamefocus.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamefocus.R
import com.gamefocus.ui.theme.EmeraldAccent
import com.gamefocus.ui.theme.EmeraldGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Glow pulse animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Logo scale animation
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Fade in/out state
    val splashAlphaState = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // Fade in
        splashAlphaState.value = 0f
        while (splashAlphaState.value < 1f) {
            splashAlphaState.value = (splashAlphaState.value + 0.05f).coerceAtMost(1f)
            delay(16)
        }
        splashAlphaState.value = 1f

        // Hold
        delay(1200)

        // Fade out
        while (splashAlphaState.value > 0f) {
            splashAlphaState.value = (splashAlphaState.value - 0.05f).coerceAtLeast(0f)
            delay(16)
        }
        splashAlphaState.value = 0f

        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Emerald radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EmeraldGreen.copy(alpha = 0.15f), Color.Transparent),
                        radius = 600f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(splashAlphaState.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "GameFocus Logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(logoScale)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "GameFocus",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Stay Focused.\nPlay Smarter.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = EmeraldAccent,
                letterSpacing = 1.sp
            )
        }
    }
}
