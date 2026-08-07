package com.gamefocus

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.gamefocus.navigation.GameFocusNavHost
import com.gamefocus.ui.theme.GameFocusTheme

@Composable
fun GameFocusApp() {
    GameFocusTheme {
        Surface {
            GameFocusNavHost()
        }
    }
}
