package com.gamefocus.data.repository

import com.gamefocus.data.local.AchievementEntity
import com.gamefocus.data.local.AchievementDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AchievementChecker(
    private val achievementDao: AchievementDao,
    private val analyticsRepository: AnalyticsRepository
) {
    suspend fun checkAndUnlockAchievements(): List<AchievementEntity> = withContext(Dispatchers.IO) {
        val newlyUnlocked = mutableListOf<AchievementEntity>()

        val totalSessions = analyticsRepository.getTotalSessionCount()
        val totalPlayTime = analyticsRepository.getTotalPlayTime()
        val streak = analyticsRepository.getGamingStreak()

        // Session milestones
        checkAndUnlock("first_session", "First Session", totalSessions >= 1)?.let { newlyUnlocked.add(it) }
        checkAndUnlock("10_sessions", "10 Sessions", totalSessions >= 10)?.let { newlyUnlocked.add(it) }
        checkAndUnlock("50_sessions", "50 Sessions", totalSessions >= 50)?.let { newlyUnlocked.add(it) }
        checkAndUnlock("100_sessions", "100 Sessions", totalSessions >= 100)?.let { newlyUnlocked.add(it) }

        // Play time milestones (in milliseconds)
        checkAndUnlock("10_hours", "10 Hours Played", totalPlayTime >= 10 * 60 * 60 * 1000)?.let { newlyUnlocked.add(it) }
        checkAndUnlock("50_hours", "50 Hours Played", totalPlayTime >= 50 * 60 * 60 * 1000)?.let { newlyUnlocked.add(it) }
        checkAndUnlock("100_hours", "100 Hours Played", totalPlayTime >= 100 * 60 * 60 * 1000)?.let { newlyUnlocked.add(it) }

        // Streak milestones
        checkAndUnlock("7_day_streak", "7-Day Streak", streak >= 7)?.let { newlyUnlocked.add(it) }
        checkAndUnlock("30_day_streak", "30-Day Streak", streak >= 30)?.let { newlyUnlocked.add(it) }

        newlyUnlocked
    }

    private suspend fun checkAndUnlock(id: String, title: String, condition: Boolean): AchievementEntity? {
        if (!condition) return null
        val existing = achievementDao.getAchievementById(id)
        if (existing != null && existing.unlocked) return null
        val rowsUpdated = achievementDao.unlockAchievement(id, System.currentTimeMillis())
        return if (rowsUpdated > 0) {
            AchievementEntity(id = id, title = title, description = "", iconRes = "", unlocked = true, unlockedAt = System.currentTimeMillis())
        } else null
    }
}
