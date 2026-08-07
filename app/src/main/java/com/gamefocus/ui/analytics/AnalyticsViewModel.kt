package com.gamefocus.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.SessionManager
import com.gamefocus.data.repository.AchievementChecker
import com.gamefocus.data.repository.AnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val analyticsRepo = AnalyticsRepository(db.gameSessionDao())
    private val sessionManager = SessionManager.getInstance(application, db.gameSessionDao())

    init {
        val checker = AchievementChecker(db.achievementDao(), analyticsRepo)
        sessionManager.setAchievementChecker(checker)
    }

    private val _dailyPlayTime = MutableStateFlow("0m")
    val dailyPlayTime: StateFlow<String> = _dailyPlayTime.asStateFlow()

    private val _weeklyPlayTime = MutableStateFlow("0m")
    val weeklyPlayTime: StateFlow<String> = _weeklyPlayTime.asStateFlow()

    private val _monthlyPlayTime = MutableStateFlow("0m")
    val monthlyPlayTime: StateFlow<String> = _monthlyPlayTime.asStateFlow()

    private val _totalPlayTime = MutableStateFlow("0m")
    val totalPlayTime: StateFlow<String> = _totalPlayTime.asStateFlow()

    private val _averageSessionDuration = MutableStateFlow("0m")
    val averageSessionDuration: StateFlow<String> = _averageSessionDuration.asStateFlow()

    private val _longestSession = MutableStateFlow("0m")
    val longestSession: StateFlow<String> = _longestSession.asStateFlow()

    private val _averageDailyPlayTime = MutableStateFlow("0m")
    val averageDailyPlayTime: StateFlow<String> = _averageDailyPlayTime.asStateFlow()

    private val _gamingStreak = MutableStateFlow(0)
    val gamingStreak: StateFlow<Int> = _gamingStreak.asStateFlow()

    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _mostPlayedGame = MutableStateFlow("None")
    val mostPlayedGame: StateFlow<String> = _mostPlayedGame.asStateFlow()

    private val _topGames = MutableStateFlow<List<TopGameUi>>(emptyList())
    val topGames: StateFlow<List<TopGameUi>> = _topGames.asStateFlow()

    private val _dailyChartData = MutableStateFlow<List<ChartEntry>>(emptyList())
    val dailyChartData: StateFlow<List<ChartEntry>> = _dailyChartData.asStateFlow()

    private val _noData = MutableStateFlow(true)
    val noData: StateFlow<Boolean> = _noData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeAndRefresh()
    }

    private fun observeAndRefresh() {
        viewModelScope.launch {
            db.gameSessionDao().getAllSessions().collectLatest { _ ->
                if (_isLoading.value) _isLoading.value = false
                refreshAnalytics()
            }
        }
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            val sessionCount = analyticsRepo.getTotalSessionCount()
            _totalSessions.value = sessionCount
            _noData.value = sessionCount == 0

            val active = sessionManager.activeSession.value
            _dailyPlayTime.value = formatDuration(analyticsRepo.getDailyPlayTimeWithActiveSession(active?.startTime))
            _weeklyPlayTime.value = formatDuration(analyticsRepo.getWeeklyPlayTime())
            _monthlyPlayTime.value = formatDuration(analyticsRepo.getMonthlyPlayTime())
            _totalPlayTime.value = formatDuration(analyticsRepo.getTotalPlayTime())
            _averageSessionDuration.value = formatDuration(analyticsRepo.getAverageSessionDuration().toLong())
            _longestSession.value = formatDuration(analyticsRepo.getLongestSession())
            _averageDailyPlayTime.value = formatDuration(analyticsRepo.getAverageDailyPlayTime())
            _gamingStreak.value = analyticsRepo.getGamingStreak()

            analyticsRepo.getMostPlayedGame()?.let { result ->
                val minutes = result.totalMillis / 60000
                _mostPlayedGame.value = "${result.packageName} (${minutes}m)"
            }

            _topGames.value = analyticsRepo.getTopPlayedGames().map {
                TopGameUi(packageName = it.packageName, totalMillis = it.totalMillis)
            }

            generateChartData()

            val checker = AchievementChecker(db.achievementDao(), analyticsRepo)
            checker.checkAndUnlockAchievements()
        }
    }

    private suspend fun generateChartData() {
        val dailyEntries = mutableListOf<ChartEntry>()
        val allSessions = db.gameSessionDao().getAllSessions().firstOrNull() ?: return

        repeat(7) { i ->
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -(6 - i))
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000

            val dayTotalMillis = allSessions.filter { it.startTime in dayStart..<dayEnd }.sumOf { it.durationMillis }
            dailyEntries.add(ChartEntry(
                label = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(cal.time),
                value = (dayTotalMillis / 60000).toFloat()
            ))
        }

        _dailyChartData.value = dailyEntries
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    data class TopGameUi(
        val packageName: String,
        val totalMillis: Long
    )

    data class ChartEntry(
        val label: String,
        val value: Float
    )
}
