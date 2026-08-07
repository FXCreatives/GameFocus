package com.gamefocus.ui.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*
import com.gamefocus.utils.FocusModeManager
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(viewModel: FocusModeViewModel = viewModel()) {
    val hasDndAccess by viewModel.hasDndAccess.collectAsState()
    val isFocusModeOn by viewModel.isFocusModeOn.collectAsState()
    val focusDuration by viewModel.focusDuration.collectAsState()
    val autoFocus by viewModel.autoFocusEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.refreshDndStatus()
    }

    Column(modifier = Modifier.fillMaxSize().background(GamingBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Silence distractions and stay in the zone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (!hasDndAccess && !isLoading) {
                item {
                    PermissionWarningCard()
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    FocusStatusCard(isActive = isFocusModeOn, duration = focusDuration)
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    FocusToggleCard(
                        title = "Enable Focus Mode",
                        subtitle = "Turn on Do Not Disturb now",
                        checked = isFocusModeOn,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                            viewModel.setFocusModeEnabled(it)
                        },
                        icon = Icons.Default.Notifications,
                        enabled = hasDndAccess
                    )
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    FocusToggleCard(
                        title = "Auto Activation",
                        subtitle = "Enable Focus automatically when a game starts",
                        checked = autoFocus,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                            viewModel.setAutoFocusEnabled(it)
                        },
                        icon = Icons.Default.AutoMode,
                        enabled = hasDndAccess
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningCard() {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GamingError.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GamingError.copy(alpha = 0.3f)),
        modifier = Modifier.semantics { contentDescription = "DND permission required. Tap to grant access." }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GamingError.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GamingError)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "DND Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GamingError
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Grant Do Not Disturb access to enable Focus Mode and block notifications during gameplay.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { FocusModeManager(context).openDndSettings() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GamingError),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Grant DND Access", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusStatusCard(isActive: Boolean, duration: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) EmeraldGreen.copy(alpha = 0.1f) else GamingCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) EmeraldGreen.copy(alpha = 0.3f) else Divider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = if (isActive) "Focus Active, ${duration} minutes elapsed" else "Focus Mode Off" }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Focus Active",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${duration}m elapsed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GamingSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Focus Mode Off",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enable to block notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun FocusToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title: ${if (checked) "On" else "Off"}" },
        colors = CardDefaults.cardColors(
            containerColor = if (checked) EmeraldGreen.copy(alpha = 0.1f) else GamingCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) EmeraldGreen.copy(alpha = 0.2f) else Divider
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (checked) EmeraldGreen.copy(alpha = 0.15f) else GamingSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) EmeraldGreen else TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen)
            )
        }
    }
}
