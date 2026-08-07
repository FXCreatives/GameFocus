package com.gamefocus.ui.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.SessionManager
import com.gamefocus.data.models.GameSession
import com.gamefocus.services.DetectionManager
import com.gamefocus.services.DetectionState
import com.gamefocus.utils.UsagePermissionHelper
import com.gamefocus.utils.FocusModeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DebugViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager.getInstance(application, db.gameSessionDao())
    private val focusModeManager = FocusModeManager(application)

    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    private val _activeSession = MutableStateFlow<GameSession?>(null)
    val activeSession: StateFlow<GameSession?> = _activeSession.asStateFlow()

    private val _totalSessionsCount = MutableStateFlow(0)
    val totalSessionsCount: StateFlow<Int> = _totalSessionsCount.asStateFlow()

    private val _permissions = MutableStateFlow(mapOf<String, Boolean>())
    val permissions: StateFlow<Map<String, Boolean>> = _permissions.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            DetectionManager.state.collectLatest { _detectionState.value = it }
        }
        viewModelScope.launch {
            sessionManager.activeSession.collectLatest { _activeSession.value = it }
        }
        viewModelScope.launch {
            db.gameSessionDao().getAllSessions().collect { sessions ->
                _totalSessionsCount.value = sessions.size
            }
        }
        viewModelScope.launch {
            while (true) {
                _permissions.value = mapOf(
                    "Usage Access" to UsagePermissionHelper.hasUsagePermission(getApplication()),
                    "Do Not Disturb" to focusModeManager.hasDndAccess(),
                    "Battery Optimization" to focusModeManager.isIgnoringBatteryOptimizations()
                )
                kotlinx.coroutines.delay(2000)
            }
        }
    }
}
