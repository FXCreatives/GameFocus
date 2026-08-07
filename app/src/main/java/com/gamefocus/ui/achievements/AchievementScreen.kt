package com.gamefocus.ui.achievements

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.data.local.AchievementEntity
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(viewModel: AchievementViewModel = viewModel()) {
    val achievements by viewModel.achievements.collectAsState()
    val unlockedCount by viewModel.unlockedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val completionPercentage = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(GamingBackground)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GamingCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                modifier = Modifier.semantics { contentDescription = "Achievement progress: $unlockedCount out of $totalCount" }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$unlockedCount / $totalCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(completionPercentage * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = completionPercentage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreen,
                        trackColor = GamingSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            ShimmerEffect(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(achievements) { achievement ->
                        AchievementItem(achievement = achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(achievement: AchievementEntity) {
    val unlocked = achievement.unlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (unlocked) 8.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (unlocked) EmeraldGreen.copy(alpha = 0.2f) else Color.Transparent,
                spotColor = if (unlocked) EmeraldGreen.copy(alpha = 0.2f) else Color.Transparent
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${achievement.title}: ${achievement.description}, ${if (unlocked) "Unlocked" else "Locked"}"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) GamingCard else GamingSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (unlocked) EmeraldGreen.copy(alpha = 0.3f) else Divider
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (unlocked) EmeraldGreen.copy(alpha = 0.15f) else GamingCard
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = if (unlocked) "Unlocked" else "Locked",
                    tint = if (unlocked) EmeraldGreen else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) TextPrimary else TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unlocked) TextSecondary else TextSecondary.copy(alpha = 0.5f)
                )
            }

            if (unlocked) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Unlocked",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
