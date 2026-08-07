package com.gamefocus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gamefocus.data.models.GameSession

@Database(
    entities = [GameSession::class, GameEntity::class, AchievementEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun gameDao(): GameDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gamefocus.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
