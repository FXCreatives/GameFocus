package com.gamefocus.data.models

import android.graphics.drawable.Drawable

data class GameApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val category: String,
    val installDate: Long = 0L,
    val updateDate: Long = 0L,
    val lastPlayed: Long = 0L,
    val lastSessionDate: Long = 0L,
    val lastSessionDuration: Long = 0L,
    val playTimeMillis: Long = 0L,
    val sessionCount: Int = 0,
    val isFavorite: Boolean = false
)

data class UserProfile(
    val userId: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val joinedDate: Long = System.currentTimeMillis(),
    val totalSessions: Int = 0,
    val totalPlayTimeMillis: Long = 0L
)