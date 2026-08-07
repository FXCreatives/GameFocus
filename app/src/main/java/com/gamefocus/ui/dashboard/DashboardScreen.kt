package com.gamefocus.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.services.DetectionState
import com.gamefocus.ui.components.AnimatedNumber
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onScanGames: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val gameCount by viewModel.gameCount.collectAsState()
    val dailyMinutes by viewModel.dailyPlayTime.collectAsState()
    val weeklyMinutes by viewModel.weeklyPlayTime.collectAsState()
    val detectionState by viewModel.detectionState.collectAsState()
    val lastPlayed by viewModel.lastPlayedGame.collectAsState()
    val lastDuration by viewModel.lastSessionDuration.collectAsState()
    val mostPlayedGame by viewModel.mostPlayedGame.collectAsState()
    val mostPlayedTime by viewModel.mostPlayedGameTime.collectAsState()
    val favoriteGame by viewModel.favoriteGame.collectAsState()
    val longestSession by viewModel.longestSession.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(GamingBackground)
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGreen.copy(alpha = 0.08f), Color.Transparent),
                    center = center.copy(y = 0f),
                    radius = size.width * 0.8f
                )
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                PremiumHeader()
            }

            item {
                if (isLoading) {
                    ShimmerCard()
                } else {
                    TodayPlayTimeCard(dailyMinutes)
                }
            }

            item {
                if (isLoading) {
                    ShimmerStatusCard()
                } else {
                    LiveStatusCard(detectionState)
                }
            }

            item {
                if (isLoading) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) {
                            ShimmerStatCard()
                        }
                    }
                } else {
                    StatsGrid(
                        gameCount = gameCount,
                        weeklyMinutes = weeklyMinutes,
                        favoriteGame = favoriteGame,
                        longestSession = longestSession
                    )
                }
            }

            item {
                if (isLoading) {
                    ShimmerActivityCard()
                } else {
                    RecentActivityCard(lastPlayed, lastDuration, mostPlayedGame, mostPlayedTime, favoriteGame, longestSession)
                }
            }

            item {
                if (isLoading) {
                    ShimmerQuickActions()
                } else {
                    QuickActionSection(
                        gameCount = gameCount,
                        onScanGames = onScanGames,
                        onSettingsClick = onSettingsClick
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumHeader() {
    Column {
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "GameFocus",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
    }
}

@Composable
fun TodayPlayTimeCard(dailyMinutes: Int) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.2f)),
        modifier = Modifier.semantics { contentDescription = "Today play time: $dailyMinutes minutes" }
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Play Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (dailyMinutes > 0) "${dailyMinutes}m" else "< 1m",
                        style = MaterialTheme.typography.headlineLarge,
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
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsGrid(
    gameCount: Int,
    weeklyMinutes: Int,
    favoriteGame: String,
    longestSession: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Games Tracked",
                value = gameCount.toString(),
                animatedValue = gameCount,
                suffix = "",
                icon = Icons.Default.List,
                color = EmeraldAccent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Weekly Time",
                value = "${weeklyMinutes}m",
                animatedValue = weeklyMinutes,
                suffix = "m",
                icon = Icons.Default.DateRange,
                color = EmeraldDark,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Favorite Game",
                value = favoriteGame.ifEmpty { "None" },
                animatedValue = null,
                suffix = "",
                icon = Icons.Default.Star,
                color = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Longest Session",
                value = longestSession,
                animatedValue = null,
                suffix = "",
                icon = Icons.Default.Timer,
                color = EmeraldAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, animatedValue: Int?, suffix: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (animatedValue != null) {
                AnimatedNumber(
                    targetValue = animatedValue,
                    suffix = suffix,
                    durationMillis = 800
                )
            } else {
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun LiveStatusCard(state: DetectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard.copy(alpha = 0.9f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state.isServiceRunning) EmeraldGreen.copy(alpha = 0.25f) else GamingError.copy(alpha = 0.25f)
        ),
        modifier = Modifier.semantics { contentDescription = if (state.isServiceRunning) "Monitoring Active" else "Monitor Offline" }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clip(CircleShape)
                            .background(if (state.isServiceRunning) EmeraldGreen else GamingError)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (state.isServiceRunning) "Monitoring Active" else "Monitor Offline",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.isServiceRunning) EmeraldGreen else GamingError,
                        letterSpacing = 0.5.sp
                    )
                }

                if (state.isFocusModeActive) {
                    Surface(
                        color = EmeraldDark.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "FOCUS ACTIVE",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            StatusItem(label = "Current Game", value = state.currentDetectedGame.ifEmpty { "None" }, color = EmeraldGreen)
            Spacer(modifier = Modifier.height(12.dp))
            StatusItem(label = "Package", value = state.currentForegroundPackage.ifEmpty { "None" }, color = TextSecondary)
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun RecentActivityCard(
    lastGame: String,
    lastDuration: String,
    mostPlayed: String,
    mostPlayedTime: Long,
    favoriteGame: String,
    longestSession: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        modifier = Modifier.semantics { contentDescription = "Recent activity card" }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (lastGame.isNotEmpty()) {
                ActivityRow(Icons.Default.Refresh, "Last Played", lastGame, EmeraldGreen)
            }
            if (mostPlayed != "None") {
                Spacer(modifier = Modifier.height(12.dp))
                ActivityRow(Icons.Default.Star, "Most Played", mostPlayed, EmeraldAccent)
            }
            if (favoriteGame != "None") {
                Spacer(modifier = Modifier.height(12.dp))
                ActivityRow(Icons.Default.StarBorder, "Favorite", favoriteGame, EmeraldGreen)
            }
            Spacer(modifier = Modifier.height(12.dp))
            ActivityRow(Icons.Default.Timer, "Longest Session", longestSession, TextSecondary)
        }
    }
}

@Composable
fun ActivityRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "$label: $value"
    }) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GamingSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 0.5.sp)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun QuickActionSection(gameCount: Int, onScanGames: () -> Unit, onSettingsClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = EmeraldGreen,
            onClick = onScanGames
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Scan games", tint = GamingBackground, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Scan Games", color = GamingBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = GamingCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
            onClick = onSettingsClick
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ShimmerCard() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ShimmerStatusCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        ShimmerEffect(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ShimmerStatCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        ShimmerEffect(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ShimmerActivityCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        ShimmerEffect(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ShimmerQuickActions() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        }
    }
}
