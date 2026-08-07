package com.gamefocus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY appName ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT COUNT(*) FROM games")
    fun getGamesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE lastPlayed > 0")
    fun getScannedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games")
    suspend fun clearAll()

    @Query("DELETE FROM games WHERE packageName = :packageName")
    suspend fun deleteGameByPackage(packageName: String)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)

    @Query("SELECT * FROM games WHERE packageName = :packageName LIMIT 1")
    suspend fun getGameByPackageName(packageName: String): GameEntity?

    @Query("SELECT * FROM games WHERE isFavorite = 1 ORDER BY appName ASC")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Query("UPDATE games SET lastPlayed = :lastPlayed WHERE packageName = :packageName")
    suspend fun updateLastPlayed(packageName: String, lastPlayed: Long)

    @Query("""
        SELECT g.*, s.startTime as lastSessionStart, s.durationMillis as lastSessionDuration, s.sessionDate as lastSessionDate
        FROM games g
        LEFT JOIN (
            SELECT packageName, startTime, durationMillis, sessionDate,
                   ROW_NUMBER() OVER (PARTITION BY packageName ORDER BY startTime DESC) as rn
            FROM game_sessions
        ) s ON g.packageName = s.packageName AND s.rn = 1
        ORDER BY g.appName ASC
    """)
    fun getAllGamesWithLastSession(): Flow<List<GameWithLastSession>>

    data class GameWithLastSession(
        val id: Long = 0,
        val packageName: String,
        val appName: String,
        val lastPlayed: Long = 0L,
        val installDate: Long = 0L,
        val updateDate: Long = 0L,
        val isFavorite: Boolean = false,
        val lastSessionStart: Long = 0L,
        val lastSessionDuration: Long = 0L,
        val lastSessionDate: Long = 0L
    )
}
