package com.gamefocus.data.repository

import com.gamefocus.data.local.GameSessionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AnalyticsRepository(private val sessionDao: GameSessionDao) {

    suspend fun getDailyPlayTime(): Long {
        val dayStart = getStartOfDay(System.currentTimeMillis())
        return sessionDao.getDailyPlayTime(dayStart) ?: 0L
    }

    suspend fun getDailyPlayTimeWithActiveSession(
        activeSessionStartTime: Long?,
        dayStart: Long = getStartOfDay(System.currentTimeMillis())
    ): Long {
        val baseTime = sessionDao.getDailyPlayTime(dayStart) ?: 0L
        val activeElapsed = if (activeSessionStartTime != null && activeSessionStartTime >= dayStart) {
            System.currentTimeMillis() - activeSessionStartTime
        } else 0L
        return baseTime + activeElapsed
    }

    suspend fun getWeeklyPlayTime(): Long {
        val weekStart = getStartOfWeek(System.currentTimeMillis())
        return sessionDao.getWeeklyPlayTime(weekStart) ?: 0L
    }

    suspend fun getMonthlyPlayTime(): Long {
        val monthStart = getStartOfMonth(System.currentTimeMillis())
        return sessionDao.getMonthlyPlayTime(monthStart) ?: 0L
    }

    suspend fun getTotalPlayTime(): Long {
        return sessionDao.getTotalPlayTime() ?: 0L
    }

    suspend fun getTotalSessionCount(): Int {
        return sessionDao.getTotalSessionCount()
    }

    suspend fun getAverageSessionDuration(): Double {
        return sessionDao.getAverageSessionDuration() ?: 0.0
    }

    suspend fun getMostPlayedGame(): MostPlayedResult? {
        val stat = sessionDao.getMostPlayedGame() ?: return null
        return MostPlayedResult(stat.packageName, stat.totalMillis, stat.count)
    }

    suspend fun getLongestSession(): Long {
        val session = sessionDao.getLongestSession()
        return session?.durationMillis ?: 0L
    }

    suspend fun getAverageDailyPlayTime(): Long {
        val sessionCount = sessionDao.getTotalSessionCount()
        if (sessionCount == 0) return 0L
        val totalPlayTime = sessionDao.getTotalPlayTime() ?: 0L
        val allSessions = sessionDao.getAllSessions().firstOrNull() ?: return 0L
        val firstSession = allSessions.minByOrNull { it.startTime }
        if (firstSession == null) return 0L
        val daysActive = ((System.currentTimeMillis() - firstSession.startTime) / (24 * 60 * 60 * 1000)) + 1
        return if (daysActive > 0) totalPlayTime / daysActive else 0L
    }

    suspend fun getGamingStreak(): Int {
        val allSessions = sessionDao.getAllSessions().firstOrNull() ?: return 0
        if (allSessions.isEmpty()) return 0

        val calendar = java.util.Calendar.getInstance()
        val today = calendar.apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val daySet = allSessions.map { session ->
            calendar.timeInMillis = session.startTime
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.toSet()

        var streak = 0
        var checkDate = today
        while (daySet.contains(checkDate)) {
            streak++
            calendar.timeInMillis = checkDate
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = calendar.timeInMillis
        }

        return streak
    }

    suspend fun getTopPlayedGames(): List<TopGameResult> {
        return sessionDao.getTopPlayedGames().map {
            TopGameResult(it.packageName, it.totalMillis)
        }
    }

    data class MostPlayedResult(
        val packageName: String,
        val totalMillis: Long,
        val sessionCount: Int
    )

    data class TopGameResult(
        val packageName: String,
        val totalMillis: Long
    )

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfWeek(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
