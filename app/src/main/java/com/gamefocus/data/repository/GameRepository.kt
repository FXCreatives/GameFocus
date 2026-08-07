package com.gamefocus.data.repository

import com.gamefocus.data.local.GameSessionDao
import com.gamefocus.data.models.GameSession
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameSessionDao: GameSessionDao) {
    fun getSessions(): Flow<List<GameSession>> = gameSessionDao.getAllSessions()

    suspend fun addSession(session: GameSession) = gameSessionDao.insertSessionAndGetId(session)
}
