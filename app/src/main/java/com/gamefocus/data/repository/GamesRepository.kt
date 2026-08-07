package com.gamefocus.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.gamefocus.data.local.GameDao
import com.gamefocus.data.local.GameEntity
import com.gamefocus.data.models.GameApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class GamesRepository(private val context: Context, private val gameDao: GameDao) {

    private val fallbackPackages = listOf(
        "com.tencent.ig",
        "com.firsttouchgames.dls7",
        "com.gameloft.android.ANMP.GloftFHM",
        "com.activision.callofduty.shooter",
        "com.gameloft.android.ANMP.GloftA8HM"
    )

    suspend fun scanAndSaveGames(
        onProgress: (scanned: Int, found: Int) -> Unit = { _, _ -> }
    ): Result<ScanStats> = withContext(Dispatchers.IO) {
        return@withContext try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0)
            var totalScanned = 0
            var totalFound = 0
            val detected = mutableListOf<GameEntity>()

            for (appInfo in apps) {
                totalScanned++
                val isGame = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        appInfo.category == ApplicationInfo.CATEGORY_GAME

                if (isGame || fallbackPackages.contains(appInfo.packageName)) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon: Drawable? = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (_: Exception) {
                        null
                    }
                    val category = if (isGame) "GAME" else "UNKNOWN"
                    val pkgInfo = try {
                        pm.getPackageInfo(appInfo.packageName, 0)
                    } catch (_: Exception) { null }
                    val installDate = pkgInfo?.firstInstallTime ?: 0L
                    val updateDate = pkgInfo?.lastUpdateTime ?: 0L

                    val entity = GameEntity(
                        packageName = appInfo.packageName,
                        appName = label,
                        lastPlayed = 0L,
                        installDate = installDate,
                        updateDate = updateDate
                    )
                    detected.add(entity)

                    onProgress(totalScanned, detected.size)
                }
            }

            gameDao.clearAll()
            detected.forEach { entity ->
                try {
                    gameDao.insertGame(entity)
                    totalFound++
                } catch (_: Exception) {
                }
            }

            Result.success(ScanStats(totalScanned, totalFound))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSavedGames(): Flow<List<GameEntity>> = gameDao.getAllGames()

    fun observeSavedGamesWithLastSession(): Flow<List<GameDao.GameWithLastSession>> =
        gameDao.getAllGamesWithLastSession()

    fun getGamesCount(): Flow<Int> = gameDao.getGamesCount()

    fun getFavoriteGames(): Flow<List<GameEntity>> = gameDao.getFavoriteGames()

    suspend fun getGameByPackageName(packageName: String): GameEntity? {
        return withContext(Dispatchers.IO) {
            gameDao.getGameByPackageName(packageName)
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        val current = gameDao.getAllGames().firstOrNull()?.find { it.packageName == packageName }?.isFavorite ?: false
        gameDao.setFavorite(packageName, !current)
    }

    suspend fun updateGameLastPlayed(packageName: String, lastPlayed: Long) {
        gameDao.updateLastPlayed(packageName, lastPlayed)
    }

    data class ScanStats(val totalScanned: Int, val totalFound: Int)
}
