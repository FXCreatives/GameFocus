package com.gamefocus.ui.achievements

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamefocus.data.local.AchievementEntity
import com.gamefocus.ui.theme.EmeraldAccent
import com.gamefocus.ui.theme.EmeraldGreen
import com.gamefocus.ui.theme.GamingCard
import com.gamefocus.ui.theme.TextPrimary

@Composable
fun AchievementPopup(
    achievement: AchievementEntity,
    onDismiss: () -> Unit
) {
    val visible = remember { mutableFloatStateOf(0f) }

    androidx.compose.runtime.LaunchedEffect(achievement) {
        visible.value = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible.value > 0f,
            enter = androidx.compose.animation.fadeIn(animationSpec = tween(300)) + androidx.compose.animation.scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300)
            ),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(300)) + androidx.compose.animation.scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(300)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GamingCard),
                border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldGreen.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emerald glow background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(EmeraldGreen.copy(alpha = 0.3f), EmeraldGreen.copy(alpha = 0.05f)),
                                    radius = 80f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Achievement Unlocked",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = achievement.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    if (achievement.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = achievement.description,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar animation
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = EmeraldGreen,
                        trackColor = EmeraldGreen.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
