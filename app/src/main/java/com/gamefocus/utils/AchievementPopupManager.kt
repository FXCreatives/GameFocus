package com.gamefocus.utils

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import com.gamefocus.data.local.AchievementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AchievementPopupManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: AchievementPopupManager? = null

        fun getInstance(): AchievementPopupManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AchievementPopupManager()
                INSTANCE = instance
                instance
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _currentPopup = MutableStateFlow<AchievementEntity?>(null)
    val currentPopup: StateFlow<AchievementEntity?> = _currentPopup.asStateFlow()

    private val queue = mutableListOf<AchievementEntity>()
    private var isShowing = false

    fun enqueue(achievement: AchievementEntity) {
        queue.add(achievement)
        processQueue()
    }

    fun enqueueAll(achievements: List<AchievementEntity>) {
        queue.addAll(achievements)
        processQueue()
    }

    private fun processQueue() {
        if (isShowing || queue.isEmpty()) return

        isShowing = true
        val achievement = queue.removeAt(0)
        _currentPopup.value = achievement

        scope.launch {
            delay(3000)
            _currentPopup.value = null
            delay(300)
            isShowing = false
            processQueue()
        }
    }

    fun dismiss() {
        _currentPopup.value = null
        isShowing = false
        queue.clear()
    }

    fun onCleared() {
        scope.cancel()
    }
}
