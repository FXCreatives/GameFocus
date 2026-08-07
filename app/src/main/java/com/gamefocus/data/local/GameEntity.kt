package com.gamefocus.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["appName"]),
        Index(value = ["lastPlayed"]),
        Index(value = ["packageName"], unique = true)
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val lastPlayed: Long = 0L,
    val installDate: Long = 0L,
    val updateDate: Long = 0L,
    val isFavorite: Boolean = false
)
