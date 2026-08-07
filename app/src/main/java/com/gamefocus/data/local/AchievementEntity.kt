package com.gamefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconRes: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null
)
