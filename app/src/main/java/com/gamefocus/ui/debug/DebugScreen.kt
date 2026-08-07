package com.gamefocus.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(viewModel: DebugViewModel = viewModel()) {
    val detectionState by viewModel.detectionState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val totalSessionsCount by viewModel.totalSessionsCount.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(GamingBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DebugSectionHeader(title = "Core Service")
            }
            item {
                DebugCard {
                    DebugRow("Service Running", detectionState.isServiceRunning.toString(), if(detectionState.isServiceRunning) EmeraldGreen else GamingError)
                    DebugRow("Foreground Pkg", detectionState.currentForegroundPackage.ifEmpty { "None" }, TextSecondary)
                    DebugRow("Detected Game", detectionState.currentDetectedGame.ifEmpty { "None" }, TextSecondary)
                    DebugRow("Last Detection", formatTime(detectionState.lastDetectionTime), TextSecondary)
                }
            }

            item {
                DebugSectionHeader(title = "Session Management")
            }
            item {
                DebugCard {
                    val sessionColor = if (activeSession != null) EmeraldGreen else TextSecondary.copy(alpha = 0.5f)
                    DebugRow("Active Session", if(activeSession != null) "YES" else "NO", sessionColor)
                    DebugRow("Session ID", activeSession?.id?.toString() ?: "None", TextSecondary)
                    DebugRow("Session Pkg", activeSession?.packageName ?: "None", TextSecondary)
                    DebugRow("Start Time", formatTime(activeSession?.startTime ?: 0), TextSecondary)
                    DebugRow("Total DB Sessions", totalSessionsCount.toString(), TextSecondary)
                }
            }

            item {
                DebugSectionHeader(title = "Permissions")
            }
            permissions.forEach { (name, granted) ->
                item {
                    DebugCard {
                        DebugRow(name, if(granted) "GRANTED" else "MISSING", if(granted) EmeraldGreen else GamingError)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = EmeraldGreen,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun DebugCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DebugRow(label: String, value: String, valueColor: Color = TextSecondary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

fun formatTime(millis: Long): String {
    if (millis == 0L) return "Never"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
