package com.gamefocus.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.models.GameSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sessionDao = db.gameSessionDao()

    private val _sessions = MutableStateFlow<List<GameSession>>(emptyList())
    val sessions: StateFlow<List<GameSession>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionDao.getAllSessions().collectLatest { list ->
                if (_isLoading.value) _isLoading.value = false
                _sessions.value = list
            }
        }
    }
}
