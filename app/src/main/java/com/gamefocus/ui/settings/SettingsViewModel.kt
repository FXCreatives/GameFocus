package com.gamefocus.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.data.sync.FirebaseSyncManager
import com.gamefocus.data.sync.SyncProgress
import com.gamefocus.data.sync.SyncStatus
import com.gamefocus.utils.FocusModeManager
import com.gamefocus.utils.UsagePermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val focusModeManager = FocusModeManager(application)
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(application)
    private val settingsManager = SettingsManager(application)
    private val db = AppDatabase.getInstance(application)

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _hasDndAccess = MutableStateFlow(false)
    val hasDndAccess: StateFlow<Boolean> = _hasDndAccess.asStateFlow()

    private val _isDetectionEnabled = MutableStateFlow(false)
    val isDetectionEnabled: StateFlow<Boolean> = _isDetectionEnabled.asStateFlow()

    private val _isAutoFocusEnabled = MutableStateFlow(false)
    val isAutoFocusEnabled: StateFlow<Boolean> = _isAutoFocusEnabled.asStateFlow()

    private val _isSyncEnabled = MutableStateFlow(false)
    val isSyncEnabled: StateFlow<Boolean> = _isSyncEnabled.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: StateFlow<SyncProgress?> = _syncProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkPermissions()
        observeSettings()
        observeFirebaseSync()
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _isLoading.value = false
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            _hasUsagePermission.value = UsagePermissionHelper.hasUsagePermission(getApplication())
            _hasDndAccess.value = focusModeManager.hasDndAccess()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsManager.detectionEnabled.collectLatest { enabled ->
                if (_isLoading.value) _isLoading.value = false
                _isDetectionEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsManager.autoFocusEnabled.collectLatest { enabled ->
                if (_isLoading.value) _isLoading.value = false
                _isAutoFocusEnabled.value = enabled
            }
        }
    }

    private fun observeFirebaseSync() {
        viewModelScope.launch {
            firebaseSyncManager.isSignedIn.collectLatest { isSignedIn ->
                _isSignedIn.value = isSignedIn
            }
        }

        viewModelScope.launch {
            firebaseSyncManager.syncEnabled.collectLatest { enabled ->
                _isSyncEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            firebaseSyncManager.syncStatus.collectLatest { status ->
                _syncStatus.value = status
            }
        }

        viewModelScope.launch {
            firebaseSyncManager.lastSyncTime.collectLatest { time ->
                _lastSyncTime.value = time
            }
        }

        viewModelScope.launch {
            firebaseSyncManager.userId.collectLatest { id ->
                _userId.value = id
            }
        }

        viewModelScope.launch {
            firebaseSyncManager.syncProgress.collectLatest { progress ->
                _syncProgress.value = progress
            }
        }
    }

    fun refreshPermissionStatus() {
        _hasUsagePermission.value = UsagePermissionHelper.hasUsagePermission(getApplication())
        _hasDndAccess.value = focusModeManager.hasDndAccess()
    }

    fun setDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDetectionEnabled(enabled)
        }
    }

    fun setAutoFocusEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoFocusEnabled(enabled)
        }
    }

    fun enableFocusMode() {
        focusModeManager.enableFocusMode()
    }

    fun signInToFirebase() {
        viewModelScope.launch {
            firebaseSyncManager.signInAnonymously()
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            firebaseSyncManager.signInWithGoogle(idToken)
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            firebaseSyncManager.signUpWithEmail(email, password)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            firebaseSyncManager.signInWithEmail(email, password)
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            firebaseSyncManager.enableSync(enabled)
        }
    }

    fun syncSessions() {
        viewModelScope.launch {
            val sessions = db.gameSessionDao().getAllSessions().firstOrNull()?.toList() ?: return@launch
            firebaseSyncManager.syncSessions(sessions)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseSyncManager.signOut()
        }
    }
}