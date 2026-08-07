package com.gamefocus.data.repository

import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.AchievementEntity
import com.gamefocus.data.local.AchievementDao
import kotlinx.coroutines.flow.Flow

class AchievementRepository(private val achievementDao: AchievementDao) {
    fun getAllAchievements(): Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()

    suspend fun getUnlockedCount(): Int = achievementDao.getUnlockedCount()

    suspend fun getTotalCount(): Int = achievementDao.getTotalCount()

    suspend fun unlockAchievement(id: String, timestamp: Long = System.currentTimeMillis()): Boolean {
        val rowsUpdated = achievementDao.unlockAchievement(id, timestamp)
        return rowsUpdated > 0
    }

    suspend fun insertAchievement(achievement: AchievementEntity) {
        achievementDao.insertAchievement(achievement)
    }

    suspend fun isUnlocked(id: String): Boolean {
        val achievement = achievementDao.getAchievementById(id)
        return achievement?.unlocked ?: false
    }

    suspend fun initializeAchievements() {
        val existing = achievementDao.getAchievementById("first_session")
        if (existing == null) {
            val defaults = listOf(
                AchievementEntity(id = "first_session", title = "First Session", description = "Complete your first gaming session", iconRes = "🎮"),
                AchievementEntity(id = "10_sessions", title = "10 Sessions", description = "Complete 10 gaming sessions", iconRes = "🔥"),
                AchievementEntity(id = "50_sessions", title = "50 Sessions", description = "Complete 50 gaming sessions", iconRes = "💪"),
                AchievementEntity(id = "100_sessions", title = "100 Sessions", description = "Complete 100 gaming sessions", iconRes = "🏆"),
                AchievementEntity(id = "10_hours", title = "10 Hours Played", description = "Play for 10 hours total", iconRes = "⏰"),
                AchievementEntity(id = "50_hours", title = "50 Hours Played", description = "Play for 50 hours total", iconRes = "🕒"),
                AchievementEntity(id = "100_hours", title = "100 Hours Played", description = "Play for 100 hours total", iconRes = "🕰️"),
                AchievementEntity(id = "7_day_streak", title = "7-Day Streak", description = "Play games for 7 days in a row", iconRes = "📅"),
                AchievementEntity(id = "30_day_streak", title = "30-Day Streak", description = "Play games for 30 days in a row", iconRes = "🌟")
            )
            defaults.forEach { achievementDao.insertAchievement(it) }
        }
    }
}
