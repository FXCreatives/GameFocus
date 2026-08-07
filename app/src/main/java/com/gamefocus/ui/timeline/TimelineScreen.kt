package com.gamefocus.ui.timeline

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.data.models.GameSession
import com.gamefocus.ui.components.PremiumEmptyState
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = viewModel()) {
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = remember(context) { context.packageManager }

    val grouped = rememberGroupedSessions(sessions)

    Column(modifier = Modifier.fillMaxSize().background(GamingBackground)) {
        if (isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                items(6) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else if (grouped.isEmpty()) {
            PremiumEmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "No sessions yet",
                description = "Your gaming history will appear here."
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                grouped.forEach { group ->
                    item {
                        TimelineGroupHeader(title = group.title)
                    }
                    items(group.sessions) { session ->
                        TimelineItem(session = session, pm = pm)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineGroupHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldGreen,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TimelineItem(session: GameSession, pm: PackageManager) {
    val icon = try {
        pm.getApplicationIcon(session.packageName)
    } catch (_: Exception) {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${session.gameName}, ${formatTime(session.startTime)} to ${formatTime(session.endTime)}, Duration ${formatDurationShort(session.durationMillis)}"
            },
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = session.gameName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GamingSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = session.gameName,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.gameName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatTime(session.startTime)} - ${formatTime(session.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    text = formatDurationShort(session.durationMillis),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDurationShort(millis: Long): String {
    val totalMinutes = millis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

data class SessionGroup(
    val title: String,
    val sessions: List<GameSession>
)

@Composable
private fun rememberGroupedSessions(sessions: List<GameSession>): List<SessionGroup> {
    val calendar = Calendar.getInstance()
    val today = calendar.apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    val todaySessions = sessions.filter { it.startTime >= today }
    val yesterdaySessions = sessions.filter { it.startTime in yesterday..<today }
    val earlierSessions = sessions.filter { it.startTime < yesterday }

    return listOf(
        SessionGroup("Today", todaySessions),
        SessionGroup("Yesterday", yesterdaySessions),
        SessionGroup("Earlier", earlierSessions)
    ).filter { it.sessions.isNotEmpty() }
}
