package com.gamefocus.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectionState(
    val isServiceRunning: Boolean = false,
    val currentForegroundPackage: String = "None",
    val currentDetectedGame: String = "None",
    val lastDetectionTime: Long = 0,
    val isFocusModeActive: Boolean = false
)

object DetectionManager {
    private val _state = MutableStateFlow(DetectionState())
    val state: StateFlow<DetectionState> = _state.asStateFlow()

    fun updateServiceStatus(isRunning: Boolean) {
        _state.value = _state.value.copy(isServiceRunning = isRunning)
    }

    fun updateForegroundApp(packageName: String, gameName: String = "None") {
        _state.value = _state.value.copy(
            currentForegroundPackage = packageName,
            currentDetectedGame = gameName,
            lastDetectionTime = System.currentTimeMillis()
        )
    }

    fun updateFocusMode(isActive: Boolean) {
        _state.value = _state.value.copy(isFocusModeActive = isActive)
    }
}
