package com.gamefocus.ui.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestDndPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎮 GameFocus",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureCard(
            title = "Game Detection",
            description = "Scans your device for installed games and tracks play sessions automatically."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            title = "Usage Access",
            description = "Required to detect when you're playing games. Allows the app to monitor app usage.",
            action = "Grant Permission"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            title = "Do Not Disturb",
            description = "Optional: Blocks notifications while gaming to maintain focus. Enable in Settings.",
            action = "Learn More"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            title = "Cloud Sync",
            description = "Optionally sync your gaming data to the cloud. Sign in anonymously in Settings.",
            action = null
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    action: String? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
    }
}

object OnboardingManager {
    private const val PREFS_NAME = "gamefocus_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    }
}