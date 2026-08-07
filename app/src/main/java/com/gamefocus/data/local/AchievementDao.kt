package com.gamefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET unlocked = 1, unlockedAt = :timestamp WHERE id = :id AND unlocked = 0")
    suspend fun unlockAchievement(id: String, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE unlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getTotalCount(): Int
}
