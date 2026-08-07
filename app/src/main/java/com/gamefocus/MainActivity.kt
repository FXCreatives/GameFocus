package com.gamefocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.navigation.GameFocusNavHost
import com.gamefocus.services.GameDetectionService
import com.gamefocus.ui.onboarding.OnboardingScreen
import com.gamefocus.ui.splash.SplashScreen
import com.gamefocus.ui.theme.GameFocusTheme
import com.gamefocus.ui.games.GamesViewModel
import com.gamefocus.utils.AchievementPopupManager
import com.gamefocus.data.local.AchievementEntity
import com.gamefocus.ui.achievements.AchievementPopup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager(this)

        setContent {
            var onboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
            val scope = rememberCoroutineScope()
            val gamesViewModel: GamesViewModel = viewModel()
            val achievementPopupManager = remember { AchievementPopupManager.getInstance() }
            var currentAchievement by remember { mutableStateOf<AchievementEntity?>(null) }

            LaunchedEffect(Unit) {
                achievementPopupManager.currentPopup.collect { achievement ->
                    currentAchievement = achievement
                }
            }

            LaunchedEffect(Unit) {
                try {
                    onboardingCompleted = settingsManager.onboardingCompleted.first()
                    if (onboardingCompleted == true) {
                        GameDetectionService.startService(this@MainActivity)
                        gamesViewModel.refreshGames()
                    }
                } catch (e: Exception) {
                    onboardingCompleted = false
                }
            }

            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted == true) {
                    GameDetectionService.startService(this@MainActivity)
                }
            }

            GameFocusTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (onboardingCompleted) {
                        true -> GameFocusNavHost()
                        false -> OnboardingScreen(onComplete = {
                            onboardingCompleted = true
                            scope.launch {
                                settingsManager.setDetectionEnabled(true)
                                GameDetectionService.startService(this@MainActivity)
                                gamesViewModel.refreshGames()
                            }
                        })
                        null -> SplashScreen {
                            if (onboardingCompleted == false) {
                                onboardingCompleted = false
                            }
                        }
                    }

                    // Achievement popup overlay
                    currentAchievement?.let { achievement ->
                        AchievementPopup(
                            achievement = achievement,
                            onDismiss = { currentAchievement = null }
                        )
                    }
                }
            }
        }
    }
}