package com.gamefocus.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GamingColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    secondary = EmeraldAccent,
    tertiary = EmeraldDark,
    background = GamingBackground,
    surface = GamingSurface,
    surfaceVariant = GamingCard,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = GamingError
)

@Composable
fun GameFocusTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = GamingBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GamingColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
