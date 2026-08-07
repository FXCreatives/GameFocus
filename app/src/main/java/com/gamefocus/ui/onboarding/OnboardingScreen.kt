package com.gamefocus.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.ui.theme.*
import com.gamefocus.utils.BatteryOptimizationHelper
import com.gamefocus.utils.FocusModeManager
import com.gamefocus.utils.UsagePermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    Box(modifier = Modifier.fillMaxSize().background(DarkGray)) {
        // Aesthetic Glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 150.dp)
                .background(Brush.radialGradient(listOf(PrimaryNeon.copy(alpha = 0.1f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Progress Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalSteps) {
                    val width by animateDpAsState(if (i == currentStep) 24.dp else 8.dp)
                    val color by animateColorAsState(if (i <= currentStep) PrimaryNeon else CardOverlay)
                    Box(
                        modifier = Modifier
                            .size(height = 8.dp, width = width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() with slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() with slideOutHorizontally { it } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    }
                ) { step ->
                    when (step) {
                        1 -> StepContent(
                            icon = Icons.Default.PlayArrow,
                            title = "SYSTEM INITIALIZED",
                            description = "Welcome to GameFocus. We're about to optimize your gaming environment for peak performance.",
                            buttonText = "INITIATE SETUP",
                            onButtonClick = { currentStep++ }
                        )
                        2 -> StepContent(
                            icon = Icons.Default.Lock,
                            title = "SENSORY ACCESS",
                            description = "We need Usage Access to detect when your games are active. Your data stays strictly on-device.",
                            buttonText = "GRANT ACCESS",
                            onButtonClick = { UsagePermissionHelper.openUsageSettings(context) },
                            secondaryButtonText = "CONFIRM & NEXT",
                            onSecondaryButtonClick = {
                                if (UsagePermissionHelper.hasUsagePermission(context)) {
                                    currentStep++
                                }
                            }
                        )
                         3 -> {
                             var notificationPermissionDenied by remember { mutableStateOf(false) }
                             val notificationLauncher = rememberLauncherForActivityResult(
                                 ActivityResultContracts.RequestPermission()
                             ) { isGranted ->
                                 if (isGranted) {
                                     currentStep++
                                 } else {
                                     notificationPermissionDenied = true
                                 }
                             }
                             
                             StepContent(
                                 icon = Icons.Default.Notifications,
                                 title = "COMMS CHANNEL",
                                 description = buildString {
                                     append("Enable notifications so our background monitoring system stays connected.")
                                     if (notificationPermissionDenied) {
                                         append("\n\nYou can still use GameFocus without notifications, but the app may not receive detection updates when minimized.")
                                     }
                                 },
                                 buttonText = if (notificationPermissionDenied) "CONTINUE ANYWAY" else "ENABLE COMMS",
                                 onButtonClick = {
                                     if (notificationPermissionDenied) {
                                         currentStep++
                                     } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                         notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                     } else {
                                         currentStep++
                                     }
                                 },
                                 secondaryButtonText = if (notificationPermissionDenied) null else "SKIP",
                                 onSecondaryButtonClick = { currentStep++ }
                             )
                         }
                        4 -> StepContent(
                            icon = Icons.Default.Warning,
                            title = "STEALTH MODE",
                            description = "Focus Mode uses DND to silence distractions. Highly recommended for tactical gameplay.",
                            buttonText = "GRANT DND",
                            onButtonClick = { FocusModeManager(context).openDndSettings() },
                            secondaryButtonText = "NEXT",
                            onSecondaryButtonClick = { currentStep++ }
                        )
                        5 -> StepContent(
                            icon = Icons.Default.Settings,
                            title = "CORE STABILITY",
                            description = "Exempt GameFocus from battery optimization to prevent system shutdowns during gameplay.",
                            buttonText = "OPTIMIZE POWER",
                            onButtonClick = { BatteryOptimizationHelper.openBatteryOptimizationSettings(context) },
                            secondaryButtonText = "FINALIZE",
                            onSecondaryButtonClick = {
                                scope.launch {
                                    settingsManager.setOnboardingCompleted(true)
                                    settingsManager.setDetectionEnabled(true)
                                    onComplete()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = PrimaryNeon
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
        ) {
            Text(buttonText, fontWeight = FontWeight.Black, color = DarkGray)
        }
        
        if (secondaryButtonText != null && onSecondaryButtonClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onSecondaryButtonClick) {
                Text(secondaryButtonText, color = PrimaryNeon, fontWeight = FontWeight.Bold)
            }
        }
    }
}
