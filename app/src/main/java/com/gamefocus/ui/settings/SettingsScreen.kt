package com.gamefocus.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gamefocus.data.sync.SyncProgress
import com.gamefocus.data.sync.SyncStatus
import com.gamefocus.ui.components.PremiumErrorCard
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*
import com.gamefocus.utils.BatteryOptimizationHelper
import com.gamefocus.utils.FocusModeManager
import com.gamefocus.utils.UsagePermissionHelper
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    navController: NavController
) {
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    val hasDndAccess by viewModel.hasDndAccess.collectAsState()
    val isDetectionEnabled by viewModel.isDetectionEnabled.collectAsState()
    val isAutoFocusEnabled by viewModel.isAutoFocusEnabled.collectAsState()
    val isSyncEnabled by viewModel.isSyncEnabled.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val view = LocalView.current

    var showAuthDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    Column(modifier = Modifier.fillMaxSize().background(GamingBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SettingsSectionHeader(title = "Permissions")
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    PermissionCard(
                        title = "Usage Access",
                        status = if (hasUsagePermission) "Granted" else "Required",
                        icon = Icons.Default.Lock,
                        granted = hasUsagePermission,
                        onClick = { UsagePermissionHelper.openUsageSettings(context) }
                    )
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    PermissionCard(
                        title = "Do Not Disturb",
                        status = if (hasDndAccess) "Granted" else "Not Setup",
                        icon = Icons.Default.Notifications,
                        granted = hasDndAccess,
                        onClick = { FocusModeManager(context).openDndSettings() }
                    )
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SettingsSectionHeader(title = "Features")
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SettingsToggle(
                        title = "Background Detection",
                        subtitle = "Monitor active games automatically",
                        checked = isDetectionEnabled,
                        onCheckedChange = { viewModel.setDetectionEnabled(it) },
                        icon = Icons.Default.Search
                    )
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SettingsToggle(
                        title = "Auto Focus Mode",
                        subtitle = "Silence distractions during gameplay",
                        checked = isAutoFocusEnabled,
                        onCheckedChange = { viewModel.setAutoFocusEnabled(it) },
                        icon = Icons.Default.AutoMode
                    )
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SettingsSectionHeader(title = "Cloud Sync")
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    FirebaseSyncSection(
                        isSyncEnabled = isSyncEnabled,
                        isSignedIn = isSignedIn,
                        syncStatus = syncStatus,
                        lastSyncTime = lastSyncTime,
                        syncProgress = syncProgress,
                        snackbarHostState = snackbarHostState,
                        onSyncEnabledChange = { viewModel.setSyncEnabled(it) },
                        onSignInClick = { showAuthDialog = true },
                        onSyncClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            viewModel.syncSessions()
                        },
                        onSignOutClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            viewModel.signOut()
                        }
                    )
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SettingsSectionHeader(title = "Maintenance")
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    ClickableCard(
                        label = "Battery Optimization",
                        subtitle = "Prevent service from being killed",
                        icon = Icons.Default.Settings,
                        onClick = { BatteryOptimizationHelper.openBatteryOptimizationSettings(context) }
                    )
                }
            }
            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    ClickableCard(
                        label = "About GameFocus",
                        subtitle = "Version 1.0.0",
                        icon = Icons.Default.Info,
                        onClick = { navController.navigate("about") }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        SnackbarHost(snackbarHostState)
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onSignInAnonymously = {
                viewModel.signInToFirebase()
                showAuthDialog = false
            },
            onSignInGoogle = { token ->
                viewModel.signInWithGoogle(token)
                showAuthDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
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
private fun PermissionCard(
    title: String,
    status: String,
    icon: ImageVector,
    granted: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "$title: $status" }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (granted) EmeraldGreen.copy(alpha = 0.12f) else GamingError.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (granted) EmeraldGreen else GamingError)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = status, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = EmeraldGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen)
            )
        }
    }
}

@Composable
private fun FirebaseSyncSection(
    isSyncEnabled: Boolean,
    isSignedIn: Boolean,
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    syncProgress: SyncProgress?,
    snackbarHostState: SnackbarHostState,
    onSyncEnabledChange: (Boolean) -> Unit,
    onSignInClick: () -> Unit,
    onSyncClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Cloud Sync", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Coming Soon", style = MaterialTheme.typography.bodySmall, color = EmeraldAccent)
                }
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Coming Soon",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Cloud backup and multi-device synchronization are currently under development and will be available in a future update.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            SyncInfoRow("Status", "Coming Soon", EmeraldAccent)

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Cloud Sync is coming in a future update.")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Sync Now", color = TextSecondary)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Cloud Sync is coming in a future update.")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Sign Out", color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Cloud Sync is coming in a future update.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Divider),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Sign In", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SyncInfoRow(label: String, value: String, color: Color = TextSecondary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun ClickableCard(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "$label: $subtitle" }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = EmeraldGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun AuthDialog(
    onDismiss: () -> Unit,
    onSignInAnonymously: () -> Unit,
    onSignInGoogle: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sign In", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column {
                Text("Choose how you'd like to sign in:", color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSignInAnonymously,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue Anonymously", color = GamingBackground, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Sign in with Google")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = GamingCard
    )
}
