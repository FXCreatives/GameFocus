package com.gamefocus.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.GameEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action

        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val pm = context.packageManager
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val isGame = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                appInfo.category == ApplicationInfo.CATEGORY_GAME

                        if (isGame) {
                            val db = AppDatabase.getInstance(context)
                            val gameDao = db.gameDao()
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val installDate = try {
                                pm.getPackageInfo(packageName, 0).firstInstallTime
                            } catch (_: Exception) { 0L }
                            val updateDate = try {
                                pm.getPackageInfo(packageName, 0).lastUpdateTime
                            } catch (_: Exception) { 0L }

                            val entity = GameEntity(
                                packageName = packageName,
                                appName = label,
                                lastPlayed = 0L,
                                installDate = installDate,
                                updateDate = updateDate
                            )
                            gameDao.insertGame(entity)
                        }
                    } catch (_: Exception) {
                        // Ignore errors
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        db.gameDao().deleteGameByPackage(packageName)
                    } catch (_: Exception) {
                        // Ignore errors
                    }
                }
            }
        }
    }
}
