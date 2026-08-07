package com.gamefocus.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_sessions",
    indices = [
        Index(value = ["sessionDate"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["packageName"])
    ]
)
data class GameSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val gameName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val sessionDate: Long,
    val focusModeStart: Long = 0L,
    val focusModeEnd: Long = 0L
)
