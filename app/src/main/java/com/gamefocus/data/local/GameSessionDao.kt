package com.gamefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamefocus.data.models.GameSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions ORDER BY sessionDate DESC")
    fun getAllSessions(): Flow<List<GameSession>>

    @Query("SELECT * FROM game_sessions ORDER BY sessionDate DESC")
    suspend fun getAllSessionsList(): List<GameSession>

    @Query("SELECT * FROM game_sessions WHERE endTime = 0 LIMIT 1")
    fun getActiveSessionFlow(): Flow<GameSession?>

    @Query("SELECT * FROM game_sessions WHERE endTime = 0 LIMIT 1")
    suspend fun getActiveSession(): GameSession?

    @Insert
    suspend fun insertSessionAndGetId(session: GameSession): Long

    @Insert
    suspend fun insertSession(session: GameSession)

    @Update
    suspend fun updateSession(session: GameSession)

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): GameSession?

    @Query("UPDATE game_sessions SET endTime = :endTime, durationMillis = :durationMillis WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long, durationMillis: Long)

    @Query("SELECT * FROM game_sessions ORDER BY sessionDate DESC LIMIT 5")
    fun getRecentSessions(): Flow<List<GameSession>>

    @Query("SELECT SUM(durationMillis) FROM game_sessions WHERE startTime >= :dayStart")
    suspend fun getDailyPlayTime(dayStart: Long): Long?

    @Query("SELECT SUM(durationMillis) FROM game_sessions WHERE startTime >= :weekStart")
    suspend fun getWeeklyPlayTime(weekStart: Long): Long?

    @Query("SELECT SUM(durationMillis) FROM game_sessions WHERE startTime >= :monthStart")
    suspend fun getMonthlyPlayTime(monthStart: Long): Long?

    @Query("SELECT SUM(durationMillis) FROM game_sessions")
    suspend fun getTotalPlayTime(): Long?

    @Query("SELECT COUNT(*) FROM game_sessions")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT COUNT(*) FROM game_sessions WHERE sessionDate >= :dayStart")
    suspend fun getDailySessionCount(dayStart: Long): Int

    @Query("SELECT AVG(durationMillis) FROM game_sessions")
    suspend fun getAverageSessionDuration(): Double?

    @Query("SELECT packageName, SUM(durationMillis) as totalMillis FROM game_sessions GROUP BY packageName ORDER BY totalMillis DESC LIMIT 5")
    suspend fun getTopPlayedGames(): List<TopGameStat>

    @Query("SELECT packageName, SUM(durationMillis) as totalMillis, COUNT(*) as count FROM game_sessions GROUP BY packageName ORDER BY totalMillis DESC LIMIT 1")
    suspend fun getMostPlayedGame(): MostPlayedGameStat?

    @Query("SELECT * FROM game_sessions ORDER BY durationMillis DESC LIMIT 1")
    suspend fun getLongestSession(): GameSession?

    data class TopGameStat(
        val packageName: String,
        val totalMillis: Long
    )

    data class MostPlayedGameStat(
        val packageName: String,
        val totalMillis: Long,
        val count: Int
    )
}
