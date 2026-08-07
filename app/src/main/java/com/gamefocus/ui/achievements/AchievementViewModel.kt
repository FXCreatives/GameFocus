package com.gamefocus.ui.achievements

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.AchievementEntity
import com.gamefocus.data.repository.AchievementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AchievementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = AchievementRepository(db.achievementDao())

    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()

    private val _unlockedCount = MutableStateFlow(0)
    val unlockedCount: StateFlow<Int> = _unlockedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeAchievements()
        observeCounts()
        viewModelScope.launch {
            repository.initializeAchievements()
        }
    }

    private fun observeAchievements() {
        viewModelScope.launch {
            repository.getAllAchievements().collectLatest { list ->
                if (_isLoading.value) _isLoading.value = false
                _achievements.value = list
            }
        }
    }

    private fun observeCounts() {
        viewModelScope.launch {
            _achievements.collectLatest { list ->
                _unlockedCount.value = list.count { it.unlocked }
                _totalCount.value = list.size
            }
        }
    }

    fun unlockAchievement(id: String) {
        viewModelScope.launch {
            repository.unlockAchievement(id)
        }
    }
}
