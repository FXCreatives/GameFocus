package com.gamefocus.ui.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.R
import com.gamefocus.ui.components.PremiumEmptyState
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*
import com.gamefocus.utils.ExportManager
import com.gamefocus.ui.analytics.ExportButton
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = viewModel()) {
    val dailyPlayTime by viewModel.dailyPlayTime.collectAsState()
    val weeklyPlayTime by viewModel.weeklyPlayTime.collectAsState()
    val monthlyPlayTime by viewModel.monthlyPlayTime.collectAsState()
    val totalPlayTime by viewModel.totalPlayTime.collectAsState()
    val averageSessionDuration by viewModel.averageSessionDuration.collectAsState()
    val longestSession by viewModel.longestSession.collectAsState()
    val averageDailyPlayTime by viewModel.averageDailyPlayTime.collectAsState()
    val gamingStreak by viewModel.gamingStreak.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val mostPlayedGame by viewModel.mostPlayedGame.collectAsState()
    val dailyChartData by viewModel.dailyChartData.collectAsState()
    val topGames by viewModel.topGames.collectAsState()
    val noData by viewModel.noData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val view = LocalView.current

    val context = LocalContext.current
    val exportManager = remember { ExportManager(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analytics_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = { viewModel.refreshAnalytics() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                }
            }
        }
        if (noData && !isLoading) {
            item {
                PremiumEmptyState(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.no_sessions_recorded),
                    description = stringResource(R.string.play_games_to_see_analytics)
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportButton(
                        context = context,
                        exportManager = exportManager,
                        label = stringResource(R.string.export_csv),
                        fileName = "analytics_${exportManager.getTimestamp()}.csv",
                        mimeType = "text/csv",
                        borderColor = EmeraldGreen,
                        textColor = EmeraldGreen
                    )
                    ExportButton(
                        context = context,
                        exportManager = exportManager,
                        label = stringResource(R.string.export_json),
                        fileName = "analytics_${exportManager.getTimestamp()}.json",
                        mimeType = "application/json",
                        borderColor = EmeraldAccent,
                        textColor = EmeraldAccent
                    )
                    ExportButton(
                        context = context,
                        exportManager = exportManager,
                        label = stringResource(R.string.export_pdf),
                        fileName = "analytics_${exportManager.getTimestamp()}.pdf",
                        mimeType = "application/pdf",
                        borderColor = EmeraldDark,
                        textColor = EmeraldDark
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isLoading) {
                        Box(modifier = Modifier.weight(1f).height(80.dp)) { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
                        Box(modifier = Modifier.weight(1f).height(80.dp)) { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
                    } else {
                        AnalyticsCard(title = stringResource(R.string.today), value = dailyPlayTime, modifier = Modifier.weight(1f), color = PrimaryNeon)
                        AnalyticsCard(title = stringResource(R.string.weekly), value = weeklyPlayTime, modifier = Modifier.weight(1f), color = SecondaryNeon)
                    }
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp)) { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
                } else {
                    AnalyticsCard(title = stringResource(R.string.monthly), value = monthlyPlayTime, modifier = Modifier.fillMaxWidth(), color = AccentPurple)
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
                } else {
                    AnalyticsInfoCard(
                        title = stringResource(R.string.most_played_game),
                        value = mostPlayedGame,
                        icon = Icons.Default.Star
                    )
                }
            }

            if (topGames.isNotEmpty() && !isLoading) {
                item {
                    SectionHeader(stringResource(R.string.top_games))
                }
                items(topGames.size) { index ->
                    TopGameItem(topGames[index])
                }
            }

            if (dailyChartData.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.weekly_trend))
                }
                item {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
                    } else {
                        BarChartCard(title = stringResource(R.string.daily_activity), data = dailyChartData)
                    }
                }
            }

            item {
                if (isLoading) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(6) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp)) { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnalyticsDetailRow(label = stringResource(R.string.total_play_time), value = totalPlayTime)
                        AnalyticsDetailRow(label = stringResource(R.string.total_sessions), value = stringResource(R.string.sessions_format, totalSessions))
                        AnalyticsDetailRow(label = stringResource(R.string.average_duration), value = averageSessionDuration)
                        AnalyticsDetailRow(label = "Longest Session", value = longestSession)
                        AnalyticsDetailRow(label = "Avg Daily Play", value = averageDailyPlayTime)
                        AnalyticsDetailRow(label = "Gaming Streak", value = "$gamingStreak days")
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.EmptyAnalyticsState() {
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        PremiumEmptyState(
            icon = Icons.Default.Info,
            title = stringResource(R.string.no_sessions_recorded),
            description = stringResource(R.string.play_games_to_see_analytics)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = EmeraldGreen,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp
    )
}

@Composable
private fun AnalyticsCard(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.semantics { contentDescription = "$title: $value" },
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
    }
}

@Composable
private fun AnalyticsInfoCard(title: String, value: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title: $value" },
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = EmeraldGreen)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.sp)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun TopGameItem(item: AnalyticsViewModel.TopGameUi) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${item.packageName}: ${item.totalMillis / 60000} minutes" },
        colors = CardDefaults.cardColors(containerColor = GamingCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(EmeraldGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = item.packageName, modifier = Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                text = stringResource(R.string.minutes_format, item.totalMillis / 60000),
                color = EmeraldGreen,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun AnalyticsDetailRow(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" },
        color = GamingCard
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(text = value, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        }
    }
}

@Composable
private fun BarChartCard(title: String, data: List<AnalyticsViewModel.ChartEntry>) {
    val maxValue = data.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GamingCard)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { entry ->
                    val targetHeight = entry.value / maxValue
                    val animatedHeight by animateFloatAsState(
                        targetValue = targetHeight,
                        animationSpec = tween(800, easing = EaseOutCubic),
                        label = "barAnimation"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val heightFraction = animatedHeight.coerceAtLeast(0.05f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .fillMaxHeight(heightFraction)
                                .background(
                                    brush = Brush.verticalGradient(listOf(EmeraldAccent, EmeraldGreen)),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = entry.label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
