package com.gamefocus.data.local

import android.content.Context
import android.util.Log
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.models.GameSession
import com.gamefocus.data.repository.AchievementChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionManager(private val context: Context, private val sessionDao: GameSessionDao) {
    private val _activeSession = MutableStateFlow<GameSession?>(null)
    val activeSession: StateFlow<GameSession?> = _activeSession

    private var activeSessionId: Long? = null

    private val _lastSession = MutableStateFlow<GameSession?>(null)
    val lastSession: StateFlow<GameSession?> = _lastSession

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var achievementChecker: AchievementChecker? = null

    fun setAchievementChecker(checker: AchievementChecker?) {
        achievementChecker = checker
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context, sessionDao: GameSessionDao, achievementChecker: AchievementChecker? = null): SessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionManager(context.applicationContext, sessionDao)
                instance.achievementChecker = achievementChecker
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context, sessionDao: GameSessionDao): SessionManager {
            return getInstance(context, sessionDao, null)
        }
    }

    init {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        scope.launch {
            sessionDao.getAllSessions().collect { sessions ->
                val active = sessions.find { it.endTime == 0L }
                _activeSession.value = active
                activeSessionId = active?.id
                _lastSession.value = sessions.firstOrNull { it.endTime > 0 }
            }
        }
    }

    suspend fun startSession(packageName: String, gameName: String) {
        val existing = sessionDao.getActiveSession()
        if (existing != null) {
            endSession(existing.id)
        }

        val session = GameSession(
            packageName = packageName,
            gameName = gameName,
            startTime = System.currentTimeMillis(),
            endTime = 0L,
            durationMillis = 0L,
            sessionDate = System.currentTimeMillis()
        )
        val id = sessionDao.insertSessionAndGetId(session)
    }

    suspend fun endSession(id: Long) {
        val session = sessionDao.getSessionById(id)
        val targetSession = session ?: run {
            Log.e("SessionManager", "Could not find session $id to end")
            return
        }

        if (targetSession.endTime != 0L) {
            Log.w("SessionManager", "Session $id already ended at ${targetSession.endTime}")
            return
        }

        val endTime = System.currentTimeMillis()
        val durationMillis = endTime - targetSession.startTime
        sessionDao.endSession(id, endTime, durationMillis)

        try {
            val db = AppDatabase.getInstance(context)
            db.gameDao().updateLastPlayed(targetSession.packageName, endTime)
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to update lastPlayed: ${e.message}")
        }

        // Check achievements after session ends
        achievementChecker?.let { checker ->
            scope.launch {
                val newlyUnlocked = checker.checkAndUnlockAchievements()
                if (newlyUnlocked.isNotEmpty()) {
                     com.gamefocus.utils.AchievementPopupManager.getInstance().enqueueAll(newlyUnlocked)
                }
            }
        }
    }

    suspend fun endCurrentSession() {
        val active = sessionDao.getActiveSession()
        active?.let {
            endSession(it.id)
        }
    }
}
