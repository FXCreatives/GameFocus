package com.gamefocus.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.SessionManager
import com.gamefocus.data.local.SettingsManager
import com.gamefocus.data.repository.AnalyticsRepository
import com.gamefocus.data.repository.GamesRepository
import com.gamefocus.data.sync.FirebaseSyncManager
import com.gamefocus.data.sync.SyncStatus
import com.gamefocus.services.DetectionManager
import com.gamefocus.services.DetectionState
import com.gamefocus.utils.FocusModeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = GamesRepository(application.applicationContext, db.gameDao())
    private val analyticsRepo = AnalyticsRepository(db.gameSessionDao())
    private val sessionManager = SessionManager.getInstance(application, db.gameSessionDao())
    private val focusModeManager = FocusModeManager(application)
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(application)
    private val settingsManager = SettingsManager(application)

    private val _gameCount = MutableStateFlow(0)
    val gameCount: StateFlow<Int> = _gameCount.asStateFlow()

    private val _dailyPlayTime = MutableStateFlow(0)
    val dailyPlayTime: StateFlow<Int> = _dailyPlayTime.asStateFlow()

    private val _weeklyPlayTime = MutableStateFlow(0)
    val weeklyPlayTime: StateFlow<Int> = _weeklyPlayTime.asStateFlow()

    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _lastPlayedGame = MutableStateFlow("None")
    val lastPlayedGame: StateFlow<String> = _lastPlayedGame.asStateFlow()

    private val _lastSessionDuration = MutableStateFlow("0m")
    val lastSessionDuration: StateFlow<String> = _lastSessionDuration.asStateFlow()

    private val _mostPlayedGame = MutableStateFlow("None")
    val mostPlayedGame: StateFlow<String> = _mostPlayedGame.asStateFlow()

    private val _mostPlayedGameTime = MutableStateFlow(0L)
    val mostPlayedGameTime: StateFlow<Long> = _mostPlayedGameTime.asStateFlow()

    private val _favoriteGame = MutableStateFlow("None")
    val favoriteGame: StateFlow<String> = _favoriteGame.asStateFlow()

    private val _longestSession = MutableStateFlow("0m")
    val longestSession: StateFlow<String> = _longestSession.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var tickerJob: Job? = null
    private var baseDailyMillis: Long = 0L
    private var baseWeeklyMillis: Long = 0L

    init {
        observeGameData()
        observeSessionData()
        observeDetectionStatus()
        observeFirebaseSync()
        refreshAnalytics()
        startLiveTicker()
        triggerInitialSync()
    }

    private fun triggerInitialSync() {
        viewModelScope.launch {
            // Small delay to let Firebase initialize
            delay(2000)
            val sessions = try {
                db.gameSessionDao().getAllSessionsList()
            } catch (e: Exception) {
                emptyList()
            }
            firebaseSyncManager.triggerSync(sessions)
        }
    }

    private fun observeGameData() {
        viewModelScope.launch {
            repo.getGamesCount().collectLatest { count ->
                if (_isLoading.value) _isLoading.value = false
                _gameCount.value = count
            }
        }
        viewModelScope.launch {
            repo.getFavoriteGames().collectLatest { favorites ->
                if (_isLoading.value) _isLoading.value = false
                _favoriteGame.value = favorites.firstOrNull()?.appName ?: "None"
            }
        }
    }

    private fun observeSessionData() {
        var previousActiveSessionId: Long? = null

        viewModelScope.launch {
            sessionManager.activeSession.collectLatest { active ->
                val currentId = active?.id
                if (currentId != null && currentId != previousActiveSessionId) {
                    previousActiveSessionId = currentId
                } else if (currentId == null && previousActiveSessionId != null) {
                    previousActiveSessionId = null
                    val sessions = try {
                        db.gameSessionDao().getAllSessionsList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    firebaseSyncManager.triggerSync(sessions)
                }
                refreshAnalytics()
            }
        }
        viewModelScope.launch {
            sessionManager.lastSession.collectLatest { session ->
                if (session != null) {
                    _lastPlayedGame.value = session.gameName
                    val totalSeconds = session.durationMillis / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    _lastSessionDuration.value = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
                }
            }
        }
        viewModelScope.launch {
            val longest = analyticsRepo.getLongestSession()
            val totalSeconds = longest / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            _longestSession.value = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }
    }

    private fun observeDetectionStatus() {
        viewModelScope.launch {
            DetectionManager.state.collectLatest { state ->
                _detectionState.value = state
            }
        }
    }

    private fun observeFirebaseSync() {
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
    }

    private fun startLiveTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val active = sessionManager.activeSession.value
                if (active != null) {
                    val elapsed = System.currentTimeMillis() - active.startTime
                    _dailyPlayTime.value = ((baseDailyMillis + elapsed) / 60000).toInt()
                    _weeklyPlayTime.value = ((baseWeeklyMillis + elapsed) / 60000).toInt()
                }
                delay(1000) // Update every second for better feedback
            }
        }
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            val active = sessionManager.activeSession.value
            baseDailyMillis = analyticsRepo.getDailyPlayTimeWithActiveSession(
                activeSessionStartTime = active?.startTime
            )
            baseWeeklyMillis = analyticsRepo.getWeeklyPlayTime()

            _dailyPlayTime.value = (baseDailyMillis / 60000).toInt()
            _weeklyPlayTime.value = (baseWeeklyMillis / 60000).toInt()

            analyticsRepo.getMostPlayedGame()?.let { result ->
                val appLabel = try {
                    val pm = getApplication<Application>().packageManager
                    val appInfo = pm.getApplicationInfo(result.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    result.packageName
                }
                _mostPlayedGame.value = appLabel
                _mostPlayedGameTime.value = result.totalMillis
            } ?: run {
                _mostPlayedGame.value = "None"
                _mostPlayedGameTime.value = 0L
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}